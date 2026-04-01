package com.financeai.finance_management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.AiParseResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.service.IAiService;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.ITransactionService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements IAiService {

    private final ObjectMapper objectMapper;
    private final ITransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final IBudgetService budgetService;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Override
    public List<TransactionResponse> parseAndSaveTransaction(AiParseRequest request) {
        validateRequest(request);

        System.out.println("=== AI SAVE START ===");
        System.out.println("INPUT TEXT: " + request.getText());

        List<AiParseResponse> parsedList = parseTransactionsInternal(request.getText());
        System.out.println("PARSED SIZE: " + parsedList.size());

        List<TransactionResponse> result = new ArrayList<>();

        for (AiParseResponse item : parsedList) {
            System.out.println("AI ITEM: amount=" + item.getAmount()
                    + ", category=" + item.getCategory()
                    + ", type=" + item.getType()
                    + ", description=" + item.getDescription());

            if (!isValidAiItem(item)) {
                System.out.println("SKIP INVALID ITEM");
                continue;
            }

            Category mappedCategory = mapCategory(item.getCategory(), item.getType());
            System.out.println("MAPPED CATEGORY: id=" + mappedCategory.getId()
                    + ", name=" + mappedCategory.getName());

            UpsertTransactionRequest txRequest = new UpsertTransactionRequest();
            txRequest.setAmount(item.getAmount());
            txRequest.setType(TransactionType.valueOf(item.getType().trim().toUpperCase()));
            txRequest.setCategoryId(mappedCategory.getId());
            txRequest.setNote(item.getDescription());
            txRequest.setImageUrl(null);
            txRequest.setIsAuto(true);

            System.out.println("CALL createTransaction...");
            BaseResponse<TransactionResponse> savedResponse = transactionService.createTransaction(txRequest);
            System.out.println("SAVED OK: " + (savedResponse != null && savedResponse.getData() != null));

            if (savedResponse != null && savedResponse.getData() != null) {
                result.add(savedResponse.getData());
            }
        }

        System.out.println("=== AI SAVE END ===");
        return result;
    }

    private void validateRequest(AiParseRequest request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            throw new RuntimeException("Text không được để trống");
        }
    }

    private List<AiParseResponse> parseTransactionsInternal(String text) {
        try (Client client = Client.builder().apiKey(apiKey).build()) {
            String prompt = """
                    Bạn là hệ thống quản lý tài chính cá nhân.

                    Hãy phân tích câu tiếng Việt sau và tách tất cả giao dịch tài chính có thể nhận diện được.

                    Quy tắc:
                    - Mỗi giao dịch là một object riêng.
                    - Nếu câu có nhiều giao dịch, trả về nhiều object trong một mảng JSON.
                    - Nếu chỉ có một giao dịch, vẫn trả về mảng JSON gồm 1 phần tử.
                    - amount phải là số hợp lệ theo đơn vị VND.
                    - type chỉ được là EXPENSE hoặc INCOME.
                    - category là tên danh mục ngắn gọn.
                    - description là mô tả ngắn gọn.
                    - Không được bịa số tiền nếu câu không có số tiền rõ ràng.
                    - Chỉ trả về JSON hợp lệ, không giải thích, không markdown.

                    Input: "%s"

                    JSON format:
                    [
                      {
                        "amount": 0,
                        "category": "",
                        "type": "",
                        "description": ""
                      }
                    ]
                    """.formatted(text);

            GenerateContentResponse response =
                    client.models.generateContent("gemini-2.5-flash", prompt, null);

            String rawText = response.text();
            String cleanedJson = cleanJson(rawText);

            return objectMapper.readValue(
                    cleanedJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, AiParseResponse.class)
            );

        } catch (Exception e) {
            String message = e.getMessage() == null ? "Unknown error" : e.getMessage();

            if (message.contains("429")) {
                throw new RuntimeException("AI đang bị giới hạn request, vui lòng thử lại sau vài giây", e);
            }

            throw new RuntimeException("Lỗi khi parse giao dịch bằng AI: " + message, e);
        }
    }

    private boolean isValidAiItem(AiParseResponse item) {
        if (item == null) {
            return false;
        }

        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        if (item.getType() == null || item.getType().trim().isEmpty()) {
            return false;
        }

        try {
            TransactionType.valueOf(item.getType().trim().toUpperCase());
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private Category mapCategory(String aiCategory, String type) {
        String userId = budgetService.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserId(userId);

        if (categories == null || categories.isEmpty()) {
            throw new RuntimeException("Người dùng chưa có category để map dữ liệu AI");
        }

        String normalizedCategory = normalize(aiCategory);
        String normalizedType = type == null ? "" : type.trim().toUpperCase();

        // 1. exact match theo category của user
        for (Category category : categories) {
            if (!sameType(category, normalizedType)) {
                continue;
            }

            String dbName = normalize(category.getName());
            if (!dbName.isEmpty() && dbName.equals(normalizedCategory)) {
                return category;
            }
        }

        // 2. contains match nhẹ theo category của user
        for (Category category : categories) {
            if (!sameType(category, normalizedType)) {
                continue;
            }

            String dbName = normalize(category.getName());
            if (!dbName.isEmpty()
                    && !normalizedCategory.isEmpty()
                    && (dbName.contains(normalizedCategory) || normalizedCategory.contains(dbName))) {
                return category;
            }
        }

        // 3. fallback về category "Khác" của chính user
        Category otherCategory = findOtherCategory(categories, normalizedType);
        if (otherCategory != null) {
            return otherCategory;
        }

        throw new RuntimeException("Không map được category AI và user chưa có category 'Khác' cho type: " + type);
    }

    private boolean sameType(Category category, String normalizedType) {
        return category.getType() != null
                && category.getType().name().equalsIgnoreCase(normalizedType);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private Category findOtherCategory(List<Category> categories, String type) {
        for (Category category : categories) {
            if (!sameType(category, type)) {
                continue;
            }

            String name = normalize(category.getName());
            if (name.equals("khác")
                    || name.equals("other")
                    || name.equals("other expense")
                    || name.equals("other income")
                    || name.contains("khác")) {
                return category;
            }
        }
        return null;
    }


    private String cleanJson(String rawText) {
        if (rawText == null) {
            return "[]";
        }

        return rawText
                .replace("```json", "")
                .replace("```", "")
                .trim();
    }
}