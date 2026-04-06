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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OCRServiceImpl implements IOCRService {

    private final IAiService aiService;
    private final CategoryRepository categoryRepository;
    private final ITransactionService transactionService;
    private final IBudgetService budgetService;
    private final ICategoryNLPService nlpService;

    private final DateTimeEpochConverter dateTimeEpochConverter;

    @Override
    public TransactionResponse processReceipt(MultipartFile file) {
        File temp = null;
        try {
            // 1. Lưu file tạm (Dùng Files.copy để tránh lỗi FileNotFound khi dọn dẹp)
            temp = File.createTempFile("ocr_upload_", ".jpg");
            Files.copy(file.getInputStream(), temp.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // 2. Trích xuất Text
            String rawText = TesseractUtil.extractText(temp.getAbsolutePath());
            String userId = budgetService.getCurrentUserId();

            log.info("--- [OCR START] ---");

            // 3. Ưu tiên AI Service (OpenAI/Gemini)
            try {
                AiParseRequest aiRequest = new AiParseRequest();
                aiRequest.setText(rawText);
                List<TransactionResponse> aiResults = aiService.parseAndSaveTransaction(aiRequest);

                if (aiResults != null && !aiResults.isEmpty()) {
                    // CÁCH SỬA: Nếu AI trả về nhiều món, ta cộng dồn lại để lấy TỔNG
                    if (aiResults.size() > 1) {
                        log.info("AI trả về {} món, tiến hành gộp thành tổng hóa đơn...", aiResults.size());
                        BigDecimal totalAmount = aiResults.stream()
                                .map(TransactionResponse::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                        TransactionResponse summaryResponse = aiResults.get(0);
                        summaryResponse.setAmount(totalAmount);
                        summaryResponse.setNote(summaryResponse.getNote());

                        if (summaryResponse.getTransactionDate() == null) {
                            summaryResponse.setTransactionDate(dateTimeEpochConverter
                                    .convertToEntityAttribute(Instant.now().toEpochMilli()));
                        }
                        return summaryResponse;
                    }

                    // Nếu chỉ có 1 món (đã là tổng), xử lý bình thường
                    TransactionResponse aiResponse = aiResults.get(0);
                    if (aiResponse.getTransactionDate() == null) {
                        aiResponse.setTransactionDate(dateTimeEpochConverter
                                .convertToEntityAttribute(Instant.now().toEpochMilli()));
                    }
                    return aiResponse;
                }
            } catch (Exception e) {
                log.warn("AI Service lỗi, dùng NLP nội bộ: {}", e.getMessage());
            }

            // 4. Xử lý bằng NLP Machine Learning nội bộ
            Long extractedAmount = OCRParser.extractAmount(rawText);
            String extractedDateStr = OCRParser.extractDate(rawText);

            // Dự đoán nhãn và format tên category
            String predictedLabel = nlpService.predictCategory(rawText);


            // 5. Tìm Category thật trong DB
            Category category = findBestCategory(userId, predictedLabel);

            // 6. Tạo request để lưu
            UpsertTransactionRequest saveRequest = new UpsertTransactionRequest();
            saveRequest.setAmount(BigDecimal.valueOf(extractedAmount));
            saveRequest.setCategoryId(category.getId());
            saveRequest.setType(TransactionType.EXPENSE);
            saveRequest.setNote("Quét hóa đơn tự động: " + (category.getName() != null ? category.getName() : "Không xác định"));
            saveRequest.setIsAuto(true);

            // Xử lý ngày: Ưu tiên ngày trên hóa đơn, ko có thì lấy ngày hiện tại
            if (extractedDateStr != null && !extractedDateStr.isEmpty()) {
                saveRequest.setTransactionDate(parseOcrDate(extractedDateStr));
            }

            log.info("Lưu giao dịch OCR với ngày: {}", saveRequest.getTransactionDate());

            // 7. Gọi TransactionService để lưu vào DB
            var result = transactionService.createTransaction(saveRequest);

            if (result == null || result.getData() == null) {
                throw new RuntimeException("Lưu giao dịch thất bại");
            }

            return result.getData();

        } catch (Exception e) {
            log.error("Lỗi xử lý OCR: ", e);
            throw new RuntimeException("Không thể nhận diện hóa đơn: " + e.getMessage());
        } finally {
            // Dọn dẹp file tạm
            if (temp != null && temp.exists()) {
                temp.delete();
            }
        }
    }
    private String formatCategoryName(String label) {
        return switch (label) {
            case "An_uong" -> "Ăn uống";
            case "Di_chuyen" -> "Di chuyển";
            case "Giai_tri" -> "Giải trí";
            default -> "Chi phí khác";
        };
    }

    private Category findBestCategory(String userId, String predictedLabel) {
        List<Category> userCategories = categoryRepository.findByUserId(userId);
        String cleanLabel = predictedLabel.toLowerCase().replace("_", " ");
        return userCategories.stream()
                .filter(c -> {
                    String dbCategoryName = c.getName().toLowerCase();
                    // Khớp hoàn toàn (Ăn uống == Ăn uống)
                    // hoặc Khớp không dấu (an uong == ăn uống)
                    return dbCategoryName.equals(cleanLabel) ||
                            removeAccent(dbCategoryName).equals(removeAccent(cleanLabel));
                })
                .findFirst()
                // 3. Nếu không thấy, tìm danh mục "Khác" của User đó
                .orElseGet(() -> userCategories.stream()
                        .filter(c -> c.getName().toLowerCase().contains("khác"))
                        .findFirst()
                        .orElse(userCategories.get(0))); // Cùng lắm thì lấy cái đầu tiên
    }
    private String removeAccent(String s) {
        if (s == null) return "";
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D');
    }

    private Long parseOcrDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return System.currentTimeMillis();

        // Làm sạch chuỗi: chuyển / thành -, xóa khoảng trắng thừa
        String cleanDate = dateStr.replace("/", "-")
                                    .replaceAll("\\s+", " ")
                                    .trim();

        try {
            // TRƯỜNG HỢP 1: Có cả ngày và giờ (dd-MM-yyyy HH:mm)
            if (cleanDate.contains(" ") || cleanDate.contains(":")) {
                java.time.format.DateTimeFormatter dateTimeFormatter =
                        java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
                return java.time.LocalDateTime.parse(cleanDate, dateTimeFormatter)
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            }

            // TRƯỜNG HỢP 2: Chỉ có ngày (dd-MM-yyyy)
            java.time.format.DateTimeFormatter dateFormatter =
                    java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy");
            return java.time.LocalDate.parse(cleanDate, dateFormatter)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        } catch (Exception e) {
            log.warn("Không thể parse ngày từ chuỗi: {}. Lỗi: {}", dateStr, e.getMessage());
            return System.currentTimeMillis(); // Dự phòng nếu định dạng lạ
        }
    }
}