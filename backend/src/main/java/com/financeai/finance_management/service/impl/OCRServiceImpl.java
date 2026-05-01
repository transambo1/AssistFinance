package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.converter.DateTimeEpochConverter;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.service.*;
import com.financeai.finance_management.utils.OCRParser;
import com.financeai.finance_management.utils.TesseractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.*;
import opennlp.tools.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class OCRServiceImpl implements IOCRService {
    private final CategoryRepository categoryRepository;
    private final ITransactionService transactionService;
    private final IBudgetService budgetService;
    private final ICategoryNLPService nlpService;

    @Override
    public TransactionResponse processReceipt(MultipartFile file) {
        try {
            // 1. Lưu file tạm và Extract Text
            File temp = File.createTempFile("ocr_", ".tmp");
            file.transferTo(temp);
            String rawText = TesseractUtil.extractText(temp.getAbsolutePath());
            temp.delete();

            // 2. AI nhận diện Số tiền & Danh mục
            Long amount = OCRParser.extractAmount(rawText);
            String categoryLabel = nlpService.predictCategory(rawText);
            Category category = findBestCategory(budgetService.getCurrentUserId(), categoryLabel);

            // 3. Tạo Request lưu DB
            UpsertTransactionRequest req = new UpsertTransactionRequest();
            req.setAmount(BigDecimal.valueOf(amount));
            req.setCategoryId(category.getId());
            req.setType(TransactionType.EXPENSE);
            req.setNote("Tự động: " + category.getName());
            req.setTransactionDate(parseOcrDate(OCRParser.extractDate(rawText)));
            req.setIsAuto(true);
            return transactionService.createTransaction(req).getData();
        } catch (Exception e) {
            throw new RuntimeException("OCR Process Fail: " + e.getMessage());
        }
    }

    private Category findBestCategory(String userId, String label) {
        String cleanLabel = label.toLowerCase().replace("_", " ");
        return categoryRepository.findByUserId(userId).stream()
                .filter(c -> c.getName().toLowerCase().contains(cleanLabel))
                .findFirst()
                .orElseGet(() -> categoryRepository.findByUserId(userId).get(0));
    }

    private Long parseOcrDate(String dateStr) {
        try {
            if (dateStr == null) return System.currentTimeMillis();
            return java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) { return System.currentTimeMillis(); }
    }
}