package com.financeai.finance_management.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financeai.finance_management.dto.request.AiChatRequest;
import com.financeai.finance_management.dto.request.AiParseRequest;
import com.financeai.finance_management.dto.request.AiQueryRequest;
import com.financeai.finance_management.dto.request.UpsertTransactionRequest;
import com.financeai.finance_management.dto.response.AiChatIntentResult;
import com.financeai.finance_management.dto.response.AiChatResponse;
import com.financeai.finance_management.dto.response.AiIntentResult;
import com.financeai.finance_management.dto.response.AiParseResponse;
import com.financeai.finance_management.dto.response.AiQueryResponse;
import com.financeai.finance_management.dto.response.BaseResponse;
import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.enums.CategoryType;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.repository.CategoryRepository;
import com.financeai.finance_management.repository.TransactionRepository;
import com.financeai.finance_management.service.IAiService;
import com.financeai.finance_management.service.IBudgetService;
import com.financeai.finance_management.service.ITransactionService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements IAiService {

    private final ITransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final IBudgetService budgetService;
    private final ObjectMapper objectMapper;
    private final AiChatMemoryService aiChatMemoryService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.model}")
    private String geminiModel;

    @Override
    public BaseResponse<List<TransactionResponse>> parseAndSaveTransaction(AiParseRequest request) {
        validateParseRequest(request);

        List<AiParseResponse> parsedList = parseTransactionsInternal(request.getText());
        List<TransactionResponse> result = new ArrayList<>();

        for (AiParseResponse item : parsedList) {
            if (!isValidAiItem(item)) {
                continue;
            }

            Category mappedCategory = mapCategory(item.getCategory(), item.getType());

            UpsertTransactionRequest txRequest = new UpsertTransactionRequest();
            txRequest.setAmount(item.getAmount());
            txRequest.setType(TransactionType.valueOf(item.getType().trim().toUpperCase()));
            txRequest.setCategoryId(mappedCategory.getId());
            txRequest.setNote(item.getDescription());
            txRequest.setImageUrl(null);
            txRequest.setIsAuto(true);

            BaseResponse<TransactionResponse> savedResponse = transactionService.createTransaction(txRequest);

            if (savedResponse != null && savedResponse.getData() != null) {
                result.add(savedResponse.getData());
            }
        }

        return BaseResponse.ok(result);
    }

    @Override
    public BaseResponse<AiQueryResponse> query(AiQueryRequest request) {
        validateQueryRequest(request);

        try {
            AiIntentResult intentResult = extractIntent(request.getQuestion());
            Object data = handleIntent(intentResult);

            AiQueryResponse response = new AiQueryResponse();
            response.setIntent(intentResult.getIntent());
            response.setData(data);
            response.setAnswer(buildAnswer(intentResult, data));

            return BaseResponse.ok(response);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý AI query: " + e.getMessage(), e);
        }
    }

    @Override
    public BaseResponse<AiChatResponse> chat(AiChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            throw new RuntimeException("Message không được để trống");
        }

        try {
            String userId = budgetService.getCurrentUserId();
            List<String> history = aiChatMemoryService.getHistory(userId);

            AiChatIntentResult chatIntent = analyzeChatMessage(history, request.getMessage());

            AiChatResponse response = new AiChatResponse();

            if ("PARSE_TRANSACTION".equalsIgnoreCase(chatIntent.getMode())) {
                AiParseRequest parseRequest = new AiParseRequest();
                parseRequest.setText(request.getMessage());

                BaseResponse<List<TransactionResponse>> parseResponse = parseAndSaveTransaction(parseRequest);
                List<TransactionResponse> savedTransactions = parseResponse.getData();

                response.setActionType("PARSE_TRANSACTION");
                response.setIntent("PARSE_TRANSACTION");
                response.setData(savedTransactions);
                response.setAnswer(buildParseAnswer(savedTransactions));

                aiChatMemoryService.addUserMessage(userId, request.getMessage());
                aiChatMemoryService.addBotMessage(userId, response.getAnswer());

                return BaseResponse.ok(response);
            }

            if ("QUERY".equalsIgnoreCase(chatIntent.getMode())) {
                AiIntentResult intentResult = new AiIntentResult();
                intentResult.setIntent(chatIntent.getIntent());
                intentResult.setCategory(chatIntent.getCategory());
                intentResult.setType(chatIntent.getType());
                intentResult.setTimeRange(chatIntent.getTimeRange());
                intentResult.setKeyword(chatIntent.getKeyword());

                Object data = handleIntent(intentResult);

                response.setActionType("QUERY");
                response.setIntent(intentResult.getIntent());
                response.setData(data);
                response.setAnswer(buildAnswer(intentResult, data));

                aiChatMemoryService.addUserMessage(userId, request.getMessage());
                aiChatMemoryService.addBotMessage(userId, response.getAnswer());

                return BaseResponse.ok(response);
            }

            throw new RuntimeException("Không xác định được mode xử lý");
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý chatbot: " + e.getMessage(), e);
        }
    }

    private void validateParseRequest(AiParseRequest request) {
        if (request == null || request.getText() == null || request.getText().trim().isEmpty()) {
            throw new RuntimeException("Text không được để trống");
        }
    }

    private void validateQueryRequest(AiQueryRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new RuntimeException("Question không được để trống");
        }
    }

    private List<AiParseResponse> parseTransactionsInternal(String text) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            AiParseRequest requestBody = new AiParseRequest();
            requestBody.setText(text);

            HttpEntity<AiParseRequest> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List<AiParseResponse>> response = restTemplate.exchange(
                    aiServiceUrl + "/parse-transaction",
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {}
            );

            List<AiParseResponse> result = response.getBody();
            return result != null ? result : new ArrayList<>();

        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi gọi AI parse service: " + e.getMessage(), e);
        }
    }

    private AiIntentResult extractIntent(String question) throws Exception {
        Client client = Client.builder()
                .apiKey(geminiApiKey)
                .build();

        String prompt = """
                Bạn là AI phân tích truy vấn tài chính cá nhân.

                Nhiệm vụ:
                - Đọc câu hỏi tiếng Việt của người dùng
                - Trả về DUY NHẤT 1 JSON hợp lệ
                - Không thêm markdown
                - Không thêm giải thích
                - Không thêm ký tự nào ngoài JSON

                JSON có format:
                {
                  "intent": "",
                  "category": "",
                  "type": "",
                  "timeRange": "",
                  "keyword": ""
                }

                intent chỉ được thuộc 1 trong các giá trị sau:
                - TOTAL_EXPENSE
                - TOTAL_INCOME
                - TOTAL_EXPENSE_BY_CATEGORY
                - TOTAL_INCOME_BY_CATEGORY
                - TOTAL_EXPENSE_BY_KEYWORD
                - TOTAL_INCOME_BY_KEYWORD
                - BALANCE
                - TOP_EXPENSE_CATEGORY

                Quy ước:
                - category: tên danh mục nếu có, không có thì để chuỗi rỗng
                - type: EXPENSE hoặc INCOME, không có thì để chuỗi rỗng
                - timeRange: THIS_MONTH, LAST_MONTH, THIS_YEAR hoặc rỗng
                - keyword: dùng cho các truy vấn theo từ khóa note/mô tả như "mẹ cho", "nhặt được", "sen cho"; nếu không có thì để chuỗi rỗng

                Ví dụ:
                Câu hỏi: "Tháng này tôi đã chi bao nhiêu?"
                JSON:
                {"intent":"TOTAL_EXPENSE","category":"","type":"EXPENSE","timeRange":"THIS_MONTH","keyword":""}

                Câu hỏi: "Tôi đã chi bao nhiêu cho ăn uống?"
                JSON:
                {"intent":"TOTAL_EXPENSE_BY_CATEGORY","category":"ăn uống","type":"EXPENSE","timeRange":"","keyword":""}

                Câu hỏi: "Danh mục nào tôi chi nhiều nhất?"
                JSON:
                {"intent":"TOP_EXPENSE_CATEGORY","category":"","type":"EXPENSE","timeRange":"","keyword":""}

                Câu hỏi: "Mẹ cho tôi bao nhiêu?"
                JSON:
                {"intent":"TOTAL_INCOME_BY_KEYWORD","category":"","type":"INCOME","timeRange":"","keyword":"mẹ cho"}

                Câu hỏi: "Tôi nhặt được bao nhiêu?"
                JSON:
                {"intent":"TOTAL_INCOME_BY_KEYWORD","category":"","type":"INCOME","timeRange":"","keyword":"nhặt được"}

                Câu hỏi cần phân tích:
                "%s"
                """.formatted(question.replace("\"", "\\\""));

        GenerateContentResponse response = client.models.generateContent(
                geminiModel,
                prompt,
                null
        );

        String text = response.text();
        String cleanedJson = cleanJson(text);

        return objectMapper.readValue(cleanedJson, AiIntentResult.class);
    }

    private String cleanJson(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "{}";
        }

        String result = raw.trim();

        if (result.startsWith("```json")) {
            result = result.substring(7).trim();
        }
        if (result.startsWith("```")) {
            result = result.substring(3).trim();
        }
        if (result.endsWith("```")) {
            result = result.substring(0, result.length() - 3).trim();
        }

        return result;
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

        for (Category category : categories) {
            if (!sameType(category, normalizedType)) {
                continue;
            }

            String dbName = normalize(category.getName());
            if (!dbName.isEmpty() && dbName.equals(normalizedCategory)) {
                return category;
            }
        }

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
            if (name.contains("khác")
                    || name.equals("other")
                    || name.equals("other expense")
                    || name.equals("other income")) {
                return category;
            }
        }
        return null;
    }

    private Object handleIntent(AiIntentResult intentResult) {
        if (intentResult == null || intentResult.getIntent() == null || intentResult.getIntent().trim().isEmpty()) {
            throw new RuntimeException("AI không phân tích được intent");
        }

        String userId = budgetService.getCurrentUserId();
        String intent = intentResult.getIntent().trim().toUpperCase();
        LocalDateRange range = resolveTimeRange(intentResult.getTimeRange());

        return switch (intent) {
            case "TOTAL_EXPENSE" -> nvl(
                    transactionRepository.sumByUserIdAndTypeAndDateRange(
                            userId,
                            TransactionType.EXPENSE,
                            range.getStartEpochMilli(),
                            range.getEndEpochMilli()
                    )
            );

            case "TOTAL_INCOME" -> nvl(
                    transactionRepository.sumByUserIdAndTypeAndDateRange(
                            userId,
                            TransactionType.INCOME,
                            range.getStartEpochMilli(),
                            range.getEndEpochMilli()
                    )
            );

            case "BALANCE" -> {
                BigDecimal income = nvl(
                        transactionRepository.sumByUserIdAndTypeAndDateRange(
                                userId,
                                TransactionType.INCOME,
                                range.getStartEpochMilli(),
                                range.getEndEpochMilli()
                        )
                );

                BigDecimal expense = nvl(
                        transactionRepository.sumByUserIdAndTypeAndDateRange(
                                userId,
                                TransactionType.EXPENSE,
                                range.getStartEpochMilli(),
                                range.getEndEpochMilli()
                        )
                );

                yield income.subtract(expense);
            }

            case "TOTAL_EXPENSE_BY_CATEGORY" -> {
                Category category = findMatchedCategory(userId, intentResult.getCategory(), CategoryType.EXPENSE);

                if (category != null) {
                    BigDecimal total = transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                            userId,
                            TransactionType.EXPENSE,
                            category.getId(),
                            range.getStartEpochMilli(),
                            range.getEndEpochMilli()
                    );
                    yield nvl(total);
                }

                BigDecimal total = transactionRepository.sumByKeyword(
                        userId,
                        TransactionType.EXPENSE,
                        intentResult.getCategory()
                );
                yield nvl(total);
            }

            case "TOTAL_INCOME_BY_CATEGORY" -> {
                Category category = findMatchedCategory(userId, intentResult.getCategory(), CategoryType.INCOME);

                if (category != null) {
                    BigDecimal total = transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                            userId,
                            TransactionType.INCOME,
                            category.getId(),
                            range.getStartEpochMilli(),
                            range.getEndEpochMilli()
                    );
                    yield nvl(total);
                }

                BigDecimal total = transactionRepository.sumByKeyword(
                        userId,
                        TransactionType.INCOME,
                        intentResult.getCategory()
                );
                yield nvl(total);
            }

            case "TOP_EXPENSE_CATEGORY" -> {
                List<Category> categories = categoryRepository.findByUserId(userId)
                        .stream()
                        .filter(c -> c.getType() == CategoryType.EXPENSE)
                        .toList();

                String topCategoryName = null;
                BigDecimal max = BigDecimal.ZERO;

                for (Category category : categories) {
                    BigDecimal total = nvl(
                            transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                                    userId,
                                    TransactionType.EXPENSE,
                                    category.getId(),
                                    range.getStartEpochMilli(),
                                    range.getEndEpochMilli()
                            )
                    );

                    if (total.compareTo(max) > 0) {
                        max = total;
                        topCategoryName = category.getName();
                    }
                }

                yield topCategoryName == null ? "Chưa có dữ liệu chi tiêu" : topCategoryName;
            }

            case "TOTAL_INCOME_BY_KEYWORD" -> {
                String keyword = intentResult.getKeyword();

                if (keyword == null || keyword.trim().isEmpty()) {
                    throw new RuntimeException("Thiếu keyword cho intent TOTAL_INCOME_BY_KEYWORD");
                }

                BigDecimal total = transactionRepository.sumByKeyword(
                        userId,
                        TransactionType.INCOME,
                        keyword.trim()
                );

                yield nvl(total);
            }

            case "TOTAL_EXPENSE_BY_KEYWORD" -> {
                String keyword = intentResult.getKeyword();

                if (keyword == null || keyword.trim().isEmpty()) {
                    throw new RuntimeException("Thiếu keyword cho intent TOTAL_EXPENSE_BY_KEYWORD");
                }

                BigDecimal total = transactionRepository.sumByKeyword(
                        userId,
                        TransactionType.EXPENSE,
                        keyword.trim()
                );

                yield nvl(total);
            }

            default -> throw new RuntimeException("Intent chưa hỗ trợ: " + intent);
        };
    }

    private Category findMatchedCategory(String userId, String categoryName, CategoryType categoryType) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return null;
        }

        String input = normalize(categoryName);

        return categoryRepository.findByUserId(userId)
                .stream()
                .filter(c -> c.getType() == categoryType)
                .filter(c -> {
                    String db = normalize(c.getName());
                    return !db.isEmpty() && (db.equals(input) || db.contains(input) || input.contains(db));
                })
                .findFirst()
                .orElse(null);
    }

    private String buildAnswer(AiIntentResult intentResult, Object data) {
        String intent = intentResult.getIntent() == null ? "" : intentResult.getIntent().trim().toUpperCase();

        return switch (intent) {
            case "TOTAL_EXPENSE" ->
                    "Tổng chi của bạn là " + data + " VNĐ.";
            case "TOTAL_INCOME" ->
                    "Tổng thu của bạn là " + data + " VNĐ.";
            case "BALANCE" ->
                    "Số dư hiện tại của bạn là " + data + " VNĐ.";
            case "TOTAL_EXPENSE_BY_CATEGORY" ->
                    "Bạn đã chi " + data + " VNĐ cho danh mục " + intentResult.getCategory() + ".";
            case "TOTAL_INCOME_BY_CATEGORY" ->
                    "Bạn đã thu " + data + " VNĐ từ danh mục " + intentResult.getCategory() + ".";
            case "TOTAL_EXPENSE_BY_KEYWORD" ->
                    "Bạn đã chi " + data + " VNĐ cho từ khóa " + intentResult.getKeyword() + ".";
            case "TOTAL_INCOME_BY_KEYWORD" ->
                    "Bạn đã nhận " + data + " VNĐ từ " + intentResult.getKeyword() + ".";
            case "TOP_EXPENSE_CATEGORY" ->
                    "Danh mục chi nhiều nhất của bạn là " + data + ".";
            default ->
                    "Đã xử lý truy vấn thành công.";
        };
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDateRange resolveTimeRange(String timeRange) {
        if (timeRange == null || timeRange.trim().isEmpty()) {
            return LocalDateRange.allTime();
        }

        String normalized = timeRange.trim().toUpperCase();
        LocalDate today = LocalDate.now();

        return switch (normalized) {
            case "THIS_MONTH" -> {
                LocalDate start = today.withDayOfMonth(1);
                LocalDate end = today.withDayOfMonth(today.lengthOfMonth());
                yield LocalDateRange.of(start, end);
            }
            case "LAST_MONTH" -> {
                LocalDate lastMonth = today.minusMonths(1);
                LocalDate start = lastMonth.withDayOfMonth(1);
                LocalDate end = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth());
                yield LocalDateRange.of(start, end);
            }
            case "THIS_YEAR" -> {
                LocalDate start = today.withDayOfYear(1);
                LocalDate end = today.withDayOfYear(today.lengthOfYear());
                yield LocalDateRange.of(start, end);
            }
            default -> LocalDateRange.allTime();
        };
    }

    private AiChatIntentResult analyzeChatMessage(List<String> history, String message) throws Exception {
        Client client = Client.builder()
                .apiKey(geminiApiKey)
                .build();

        String historyText = (history == null || history.isEmpty())
                ? "Không có lịch sử hội thoại."
                : String.join("\n", history);

        String prompt = """
                Bạn là AI phân loại message trong chatbot tài chính cá nhân.

                Nhiệm vụ:
                - Dựa vào lịch sử hội thoại và câu mới nhất của user
                - Xác định user đang:
                  1. nhập giao dịch
                  2. hỏi truy vấn tài chính
                - Trả về DUY NHẤT 1 JSON hợp lệ
                - Không thêm markdown
                - Không thêm giải thích
                - Không thêm ký tự ngoài JSON

                JSON format:
                {
                  "mode": "",
                  "intent": "",
                  "category": "",
                  "type": "",
                  "timeRange": "",
                  "keyword": ""
                }

                mode chỉ được là:
                - PARSE_TRANSACTION
                - QUERY

                intent chỉ dùng khi mode = QUERY:
                - TOTAL_EXPENSE
                - TOTAL_INCOME
                - TOTAL_EXPENSE_BY_CATEGORY
                - TOTAL_INCOME_BY_CATEGORY
                - TOTAL_EXPENSE_BY_KEYWORD
                - TOTAL_INCOME_BY_KEYWORD
                - BALANCE
                - TOP_EXPENSE_CATEGORY

                Quy ước:
                - Nếu user đang nhập giao dịch kiểu "hôm nay ăn sáng 50k", "mẹ cho 300k", chọn mode = PARSE_TRANSACTION
                - Nếu user đang hỏi kiểu "tháng này tôi chi bao nhiêu", "còn ăn uống thì sao", chọn mode = QUERY
                - category: dùng cho danh mục như ăn uống, mua sắm, lương...
                - keyword: dùng cho note/mô tả như mẹ cho, nhặt được, sen cho...
                - type: EXPENSE / INCOME / ""
                - timeRange: THIS_MONTH / LAST_MONTH / THIS_YEAR / ""

                Ví dụ:
                "mẹ cho tôi bao nhiêu?"
                {"mode":"QUERY","intent":"TOTAL_INCOME_BY_KEYWORD","category":"","type":"INCOME","timeRange":"","keyword":"mẹ cho"}

                "trong đó mẹ đã cho bao nhiêu?"
                {"mode":"QUERY","intent":"TOTAL_INCOME_BY_KEYWORD","category":"","type":"INCOME","timeRange":"","keyword":"mẹ"}

                "còn ăn uống thì sao?"
                {"mode":"QUERY","intent":"TOTAL_EXPENSE_BY_CATEGORY","category":"ăn uống","type":"EXPENSE","timeRange":"","keyword":""}

                Lịch sử hội thoại:
                %s

                Câu mới nhất:
                "%s"
                """.formatted(historyText, message.replace("\"", "\\\""));

        GenerateContentResponse response = client.models.generateContent(
                geminiModel,
                prompt,
                null
        );

        String text = response.text();
        String cleanedJson = cleanJson(text);

        return objectMapper.readValue(cleanedJson, AiChatIntentResult.class);
    }

    private String buildParseAnswer(List<TransactionResponse> savedTransactions) {
        if (savedTransactions == null || savedTransactions.isEmpty()) {
            return "Mình chưa lưu được giao dịch nào từ câu bạn nhập.";
        }

        if (savedTransactions.size() == 1) {
            TransactionResponse tx = savedTransactions.get(0);
            return "Đã lưu 1 giao dịch với số tiền " + tx.getAmount() + " VNĐ.";
        }

        return "Đã lưu " + savedTransactions.size() + " giao dịch thành công.";
    }

    private static class LocalDateRange {
        private final Long startEpochMilli;
        private final Long endEpochMilli;

        private LocalDateRange(Long startEpochMilli, Long endEpochMilli) {
            this.startEpochMilli = startEpochMilli;
            this.endEpochMilli = endEpochMilli;
        }

        public static LocalDateRange of(LocalDate start, LocalDate end) {
            long startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMillis = end.plusDays(1)
                    .atStartOfDay(ZoneId.systemDefault())
                    .minusNanos(1)
                    .toInstant()
                    .toEpochMilli();

            return new LocalDateRange(startMillis, endMillis);
        }

        public static LocalDateRange allTime() {
            return new LocalDateRange(null, null);
        }

        public Long getStartEpochMilli() {
            return startEpochMilli;
        }

        public Long getEndEpochMilli() {
            return endEpochMilli;
        }
    }
}