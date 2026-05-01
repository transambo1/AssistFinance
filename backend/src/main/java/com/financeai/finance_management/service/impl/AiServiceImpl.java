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
import com.financeai.finance_management.entity.Budget;
import com.financeai.finance_management.entity.Category;
import com.financeai.finance_management.entity.Transaction;
import com.financeai.finance_management.enums.CategoryType;
import com.financeai.finance_management.enums.TransactionType;
import com.financeai.finance_management.repository.BudgetRepository;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AiServiceImpl implements IAiService {

    private final ITransactionService transactionService;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetRepository budgetRepository;
    private final IBudgetService budgetService;
    private final ObjectMapper objectMapper;
    private final AiChatMemoryService aiChatMemoryService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String geminiModel;

    private static final Pattern MONEY_PATTERN =
            Pattern.compile(".*\\d+\\s*(k|nghìn|ngàn|triệu|m|vnd|đ).*|.*\\d{4,}.*", Pattern.CASE_INSENSITIVE);

    private static final Set<String> QUERY_HINTS = Set.of(
            "bao nhiêu", "tổng", "số dư", "còn", "nhiều nhất", "so với",
            "tháng này", "tháng trước", "hôm nay", "tuần này", "tuần trước",
            "gần đây", "mới nhất", "lớn nhất", "vượt ngân sách", "tiết kiệm",
            "bất thường", "khuyên", "gợi ý", "ngân sách"
    );

    private static final Set<String> TRANSACTION_HINTS = Set.of(
            "ăn", "uống", "mua", "đóng", "trả", "đổ xăng", "lương", "thưởng",
            "mẹ cho", "nhặt được", "nhận", "chuyển khoản", "thu", "chi"
    );

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
            txRequest.setTransactionDate(
                    item.getTransactionDate() != null ? item.getTransactionDate() : System.currentTimeMillis()
            );
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
            AiIntentResult intentResult = extractIntentWithFallback(request.getQuestion());
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

            AiChatIntentResult chatIntent = analyzeChatMessageWithFallback(history, request.getMessage());
            chatIntent = enrichChatIntentFromHistory(history, chatIntent, request.getMessage());

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

                normalizeIntentResult(intentResult);

                Object data = handleIntent(intentResult);

                response.setActionType("QUERY");
                response.setIntent(intentResult.getIntent());
                response.setData(data);
                response.setAnswer(buildAnswer(intentResult, data));

                aiChatMemoryService.addUserMessage(userId, request.getMessage());
                aiChatMemoryService.addBotMessage(userId, response.getAnswer());

                return BaseResponse.ok(response);
            }

            response.setActionType("UNKNOWN");
            response.setIntent("UNKNOWN");
            response.setData(null);
            response.setAnswer("""
                    Mình chưa hiểu rõ ý bạn. Bạn có thể thử:
                    - "Tháng này tôi chi bao nhiêu?"
                    - "Ăn uống tháng này hết bao nhiêu?"
                    - "Ăn sáng 35k"
                    - "Ngân sách ăn uống còn bao nhiêu?"
                    """);

            aiChatMemoryService.addUserMessage(userId, request.getMessage());
            aiChatMemoryService.addBotMessage(userId, response.getAnswer());

            return BaseResponse.ok(response);

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

    private AiIntentResult extractIntentWithFallback(String question) {
        try {
            AiIntentResult result = extractIntent(question);
            normalizeIntentResult(result);
            return result;
        } catch (Exception e) {
            return buildRuleBasedIntent(question);
        }
    }

    private AiChatIntentResult analyzeChatMessageWithFallback(List<String> history, String message) {
        try {
            AiChatIntentResult result = analyzeChatMessage(history, message);
            normalizeChatIntentResult(result);
            return result;
        } catch (Exception e) {
            return buildRuleBasedChatIntent(history, message);
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
                - COMPARE_EXPENSE_THIS_MONTH_LAST_MONTH
                - COMPARE_INCOME_THIS_MONTH_LAST_MONTH
                - BUDGET_REMAINING
                - BUDGET_WARNING
                - ABNORMAL_EXPENSE_CHECK
                - SPENDING_ADVICE
                - SAVING_SUGGESTION
                - RECENT_TRANSACTIONS

                Quy ước:
                - category: tên danh mục nếu có, không có thì để chuỗi rỗng
                - type: EXPENSE hoặc INCOME, không có thì để chuỗi rỗng
                - timeRange: TODAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR hoặc rỗng
                - keyword: dùng cho note/mô tả như "mẹ cho", "nhặt được", "trà sữa"; nếu không có thì để chuỗi rỗng

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
                - COMPARE_EXPENSE_THIS_MONTH_LAST_MONTH
                - COMPARE_INCOME_THIS_MONTH_LAST_MONTH
                - BUDGET_REMAINING
                - BUDGET_WARNING
                - ABNORMAL_EXPENSE_CHECK
                - SPENDING_ADVICE
                - SAVING_SUGGESTION
                - RECENT_TRANSACTIONS

                Quy ước:
                - Nếu user đang nhập giao dịch kiểu "hôm nay ăn sáng 50k", "mẹ cho 300k", chọn mode = PARSE_TRANSACTION
                - Nếu user đang hỏi kiểu "tháng này tôi chi bao nhiêu", "còn ăn uống thì sao", chọn mode = QUERY
                - category: dùng cho danh mục như ăn uống, mua sắm, lương...
                - keyword: dùng cho note/mô tả như mẹ cho, nhặt được, trà sữa...
                - type: EXPENSE / INCOME / ""
                - timeRange: TODAY / THIS_WEEK / LAST_WEEK / THIS_MONTH / LAST_MONTH / THIS_YEAR / ""

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

    private AiIntentResult buildRuleBasedIntent(String question) {
        String text = normalize(question);
        AiIntentResult result = new AiIntentResult();

        if (containsAny(text, "giao dịch gần đây", "giao dịch mới nhất", "5 khoản chi gần nhất", "gần đây", "mới nhất")) {
            result.setIntent("RECENT_TRANSACTIONS");
            if (containsAny(text, "chi")) {
                result.setType("EXPENSE");
            } else if (containsAny(text, "thu", "nhận")) {
                result.setType("INCOME");
            }
            return result;
        }

        if (containsAny(text, "vượt ngân sách", "còn bao nhiêu ngân sách", "ngân sách")) {
            if (containsAny(text, "còn bao nhiêu", "còn lại")) {
                result.setIntent("BUDGET_REMAINING");
            } else {
                result.setIntent("BUDGET_WARNING");
            }
            result.setCategory(extractCategoryKeyword(text));
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "bất thường", "khác thường")) {
            result.setIntent("ABNORMAL_EXPENSE_CHECK");
            result.setType("EXPENSE");
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "nên tiết kiệm", "giảm chi", "cắt giảm", "tiết kiệm thế nào")) {
            result.setIntent("SAVING_SUGGESTION");
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "nên làm gì", "khuyên", "gợi ý", "tư vấn")) {
            result.setIntent("SPENDING_ADVICE");
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "so với tháng trước", "tháng này so với tháng trước", "tuần này so với tuần trước")) {
            if (containsAny(text, "thu", "nhận", "lương")) {
                result.setIntent("COMPARE_INCOME_THIS_MONTH_LAST_MONTH");
                result.setType("INCOME");
            } else {
                result.setIntent("COMPARE_EXPENSE_THIS_MONTH_LAST_MONTH");
                result.setType("EXPENSE");
            }
            return result;
        }

        if (containsAny(text, "danh mục nào tốn tiền nhất", "chi nhiều nhất", "danh mục nhiều nhất")) {
            result.setIntent("TOP_EXPENSE_CATEGORY");
            result.setType("EXPENSE");
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "số dư", "còn lại bao nhiêu")) {
            result.setIntent("BALANCE");
            applyTimeRange(text, result);
            return result;
        }

        String category = extractCategoryKeyword(text);
        String keyword = extractKeyword(text);

        if (!category.isBlank()) {
            if (containsAny(text, "thu", "nhận", "lương", "thưởng")) {
                result.setIntent("TOTAL_INCOME_BY_CATEGORY");
                result.setType("INCOME");
            } else {
                result.setIntent("TOTAL_EXPENSE_BY_CATEGORY");
                result.setType("EXPENSE");
            }
            result.setCategory(category);
            applyTimeRange(text, result);
            return result;
        }

        if (!keyword.isBlank()) {
            if (containsAny(text, "thu", "nhận", "cho", "được")) {
                result.setIntent("TOTAL_INCOME_BY_KEYWORD");
                result.setType("INCOME");
            } else {
                result.setIntent("TOTAL_EXPENSE_BY_KEYWORD");
                result.setType("EXPENSE");
            }
            result.setKeyword(keyword);
            applyTimeRange(text, result);
            return result;
        }

        if (containsAny(text, "thu", "nhận", "lương", "thưởng")) {
            result.setIntent("TOTAL_INCOME");
            result.setType("INCOME");
        } else {
            result.setIntent("TOTAL_EXPENSE");
            result.setType("EXPENSE");
        }

        applyTimeRange(text, result);
        return result;
    }

    private AiChatIntentResult buildRuleBasedChatIntent(List<String> history, String message) {
        String text = normalize(message);
        AiChatIntentResult result = new AiChatIntentResult();

        boolean looksLikeQuery = QUERY_HINTS.stream().anyMatch(text::contains);
        boolean looksLikeTransaction = containsMoney(text) || TRANSACTION_HINTS.stream().anyMatch(text::contains);

        if (looksLikeQuery && !looksLikePureTransaction(text)) {
            AiIntentResult queryIntent = buildRuleBasedIntent(message);
            result.setMode("QUERY");
            result.setIntent(queryIntent.getIntent());
            result.setCategory(queryIntent.getCategory());
            result.setType(queryIntent.getType());
            result.setTimeRange(queryIntent.getTimeRange());
            result.setKeyword(queryIntent.getKeyword());
            return result;
        }

        if (looksLikeTransaction) {
            result.setMode("PARSE_TRANSACTION");
            result.setIntent("PARSE_TRANSACTION");
            return result;
        }

        String merged = history == null ? "" : normalize(String.join(" ", history));
        if (containsAny(merged, "tháng này", "tháng trước", "ăn uống", "chi", "thu")) {
            result.setMode("QUERY");
            result.setIntent("BALANCE");
            return result;
        }

        result.setMode("QUERY");
        result.setIntent("BALANCE");
        return result;
    }

    private AiChatIntentResult enrichChatIntentFromHistory(List<String> history, AiChatIntentResult current, String message) {
        if (current == null) {
            return null;
        }

        String text = normalize(message);
        String mergedHistory = history == null ? "" : normalize(String.join(" ", history));

        if (isBlank(current.getTimeRange())) {
            if (containsAny(text, "hôm nay")) {
                current.setTimeRange("TODAY");
            } else if (containsAny(text, "tuần này")) {
                current.setTimeRange("THIS_WEEK");
            } else if (containsAny(text, "tuần trước")) {
                current.setTimeRange("LAST_WEEK");
            } else if (containsAny(text, "tháng này")) {
                current.setTimeRange("THIS_MONTH");
            } else if (containsAny(text, "tháng trước")) {
                current.setTimeRange("LAST_MONTH");
            } else if (containsAny(text, "năm nay")) {
                current.setTimeRange("THIS_YEAR");
            } else if (containsAny(mergedHistory, "hôm nay")) {
                current.setTimeRange("TODAY");
            } else if (containsAny(mergedHistory, "tuần này")) {
                current.setTimeRange("THIS_WEEK");
            } else if (containsAny(mergedHistory, "tuần trước")) {
                current.setTimeRange("LAST_WEEK");
            } else if (containsAny(mergedHistory, "tháng này")) {
                current.setTimeRange("THIS_MONTH");
            } else if (containsAny(mergedHistory, "tháng trước")) {
                current.setTimeRange("LAST_MONTH");
            } else if (containsAny(mergedHistory, "năm nay")) {
                current.setTimeRange("THIS_YEAR");
            }
        }

        if (isBlank(current.getCategory())) {
            String category = extractCategoryKeyword(text);
            if (category.isBlank()) {
                category = extractCategoryKeyword(mergedHistory);
            }
            if (!category.isBlank()) {
                current.setCategory(category);
            }
        }

        if (isBlank(current.getType())) {
            if (containsAny(text, "thu", "nhận", "lương", "thưởng")) {
                current.setType("INCOME");
            } else if (containsAny(text, "chi", "tiêu", "mất", "trả", "mua", "ăn", "uống")) {
                current.setType("EXPENSE");
            }
        }

        return current;
    }

    private void normalizeIntentResult(AiIntentResult result) {
        if (result == null) return;

        result.setIntent(safeUpper(result.getIntent()));
        result.setType(safeUpper(result.getType()));
        result.setTimeRange(safeUpper(result.getTimeRange()));

        if (result.getCategory() != null) result.setCategory(result.getCategory().trim());
        if (result.getKeyword() != null) result.setKeyword(result.getKeyword().trim());
    }

    private void normalizeChatIntentResult(AiChatIntentResult result) {
        if (result == null) return;

        result.setMode(safeUpper(result.getMode()));
        result.setIntent(safeUpper(result.getIntent()));
        result.setType(safeUpper(result.getType()));
        result.setTimeRange(safeUpper(result.getTimeRange()));

        if (result.getCategory() != null) result.setCategory(result.getCategory().trim());
        if (result.getKeyword() != null) result.setKeyword(result.getKeyword().trim());
    }

    private Object handleIntent(AiIntentResult intentResult) {
        if (intentResult == null || isBlank(intentResult.getIntent())) {
            throw new RuntimeException("AI không phân tích được intent");
        }

        String userId = budgetService.getCurrentUserId();
        String intent = safeUpper(intentResult.getIntent());
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

            case "TOTAL_INCOME_BY_KEYWORD" -> {
                String keyword = intentResult.getKeyword();

                if (isBlank(keyword)) {
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

                if (isBlank(keyword)) {
                    throw new RuntimeException("Thiếu keyword cho intent TOTAL_EXPENSE_BY_KEYWORD");
                }

                BigDecimal total = transactionRepository.sumByKeyword(
                        userId,
                        TransactionType.EXPENSE,
                        keyword.trim()
                );

                yield nvl(total);
            }

            case "TOP_EXPENSE_CATEGORY" -> getTopExpenseCategory(userId, range);

            case "COMPARE_EXPENSE_THIS_MONTH_LAST_MONTH" -> buildCompareResult(userId, TransactionType.EXPENSE);

            case "COMPARE_INCOME_THIS_MONTH_LAST_MONTH" -> buildCompareResult(userId, TransactionType.INCOME);

            case "ABNORMAL_EXPENSE_CHECK" -> checkAbnormalExpense(userId);

            case "SPENDING_ADVICE" -> buildSpendingAdvice(userId);

            case "SAVING_SUGGESTION" -> buildSavingSuggestion(userId);

            case "BUDGET_REMAINING" -> buildBudgetRemainingResult(userId, intentResult);

            case "BUDGET_WARNING" -> buildBudgetWarningResult(userId, intentResult);

            case "RECENT_TRANSACTIONS" -> buildRecentTransactionsResult(userId, intentResult);

            default -> throw new RuntimeException("Intent chưa hỗ trợ: " + intent);
        };
    }

    private TopCategoryResult getTopExpenseCategory(String userId, LocalDateRange range) {
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

        TopCategoryResult result = new TopCategoryResult();
        result.setCategoryName(topCategoryName == null ? "Chưa có dữ liệu chi tiêu" : topCategoryName);
        result.setAmount(max);
        return result;
    }

    private CompareResult buildCompareResult(String userId, TransactionType type) {
        LocalDateRange thisMonth = resolveTimeRange("THIS_MONTH");
        LocalDateRange lastMonth = resolveTimeRange("LAST_MONTH");

        BigDecimal current = nvl(
                transactionRepository.sumByUserIdAndTypeAndDateRange(
                        userId,
                        type,
                        thisMonth.getStartEpochMilli(),
                        thisMonth.getEndEpochMilli()
                )
        );

        BigDecimal previous = nvl(
                transactionRepository.sumByUserIdAndTypeAndDateRange(
                        userId,
                        type,
                        lastMonth.getStartEpochMilli(),
                        lastMonth.getEndEpochMilli()
                )
        );

        CompareResult result = new CompareResult();
        result.setCurrent(current);
        result.setPrevious(previous);
        result.setDifference(current.subtract(previous));
        result.setLabel(type == TransactionType.INCOME ? "thu nhập" : "chi tiêu");
        return result;
    }

    private AbnormalResult checkAbnormalExpense(String userId) {
        LocalDateRange thisMonth = resolveTimeRange("THIS_MONTH");
        LocalDateRange lastMonth = resolveTimeRange("LAST_MONTH");

        List<Category> expenseCategories = categoryRepository.findByUserId(userId)
                .stream()
                .filter(c -> c.getType() == CategoryType.EXPENSE)
                .toList();

        String abnormalCategory = null;
        BigDecimal abnormalDiff = BigDecimal.ZERO;
        BigDecimal abnormalCurrent = BigDecimal.ZERO;
        BigDecimal abnormalPrevious = BigDecimal.ZERO;

        for (Category category : expenseCategories) {
            BigDecimal current = nvl(
                    transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                            userId,
                            TransactionType.EXPENSE,
                            category.getId(),
                            thisMonth.getStartEpochMilli(),
                            thisMonth.getEndEpochMilli()
                    )
            );

            BigDecimal previous = nvl(
                    transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                            userId,
                            TransactionType.EXPENSE,
                            category.getId(),
                            lastMonth.getStartEpochMilli(),
                            lastMonth.getEndEpochMilli()
                    )
            );

            BigDecimal diff = current.subtract(previous);
            if (diff.compareTo(abnormalDiff) > 0) {
                abnormalDiff = diff;
                abnormalCategory = category.getName();
                abnormalCurrent = current;
                abnormalPrevious = previous;
            }
        }

        AbnormalResult result = new AbnormalResult();
        result.setDetected(abnormalCategory != null && abnormalDiff.compareTo(BigDecimal.ZERO) > 0);
        result.setCategory(abnormalCategory);
        result.setCurrent(abnormalCurrent);
        result.setPrevious(abnormalPrevious);
        result.setDifference(abnormalDiff);
        return result;
    }

    private AdviceResult buildSpendingAdvice(String userId) {
        LocalDateRange thisMonth = resolveTimeRange("THIS_MONTH");
        TopCategoryResult top = getTopExpenseCategory(userId, thisMonth);

        AdviceResult result = new AdviceResult();

        if (top.getAmount() == null || top.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.setTitle("Chưa đủ dữ liệu");
            result.setMessage("Hiện tại mình chưa thấy dữ liệu chi tiêu đủ để đưa ra gợi ý cụ thể.");
            return result;
        }

        result.setTitle("Gợi ý chi tiêu");
        result.setMessage("Danh mục chi lớn nhất hiện tại là " + top.getCategoryName()
                + " với " + formatMoney(top.getAmount())
                + ". Bạn nên kiểm tra và ưu tiên tối ưu danh mục này trước.");
        return result;
    }

    private AdviceResult buildSavingSuggestion(String userId) {
        LocalDateRange thisMonth = resolveTimeRange("THIS_MONTH");
        TopCategoryResult top = getTopExpenseCategory(userId, thisMonth);

        AdviceResult result = new AdviceResult();

        if (top.getAmount() == null || top.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            result.setTitle("Gợi ý tiết kiệm");
            result.setMessage("Mình chưa có đủ dữ liệu để đưa ra đề xuất tiết kiệm rõ ràng.");
            return result;
        }

        result.setTitle("Gợi ý tiết kiệm");
        result.setMessage("Nếu muốn tiết kiệm nhiều hơn trong tháng này, bạn nên cân nhắc giảm chi ở nhóm "
                + top.getCategoryName() + " vì đây là danh mục đang tốn nhiều nhất.");
        return result;
    }

    private BudgetInsightResult buildBudgetRemainingResult(String userId, AiIntentResult intentResult) {
        BudgetInsightResult result = new BudgetInsightResult();
        result.setTitle("Ngân sách còn lại");

        String categoryId = null;
        Category matched = null;

        if (!isBlank(intentResult.getCategory())) {
            matched = findMatchedCategory(userId, intentResult.getCategory(), CategoryType.EXPENSE);
            if (matched != null) {
                categoryId = matched.getId();
                result.setCategory(matched.getName());
            }
        }

        Long now = System.currentTimeMillis();
        List<Budget> budgets = budgetRepository.findBudgets(userId, categoryId, now);

        if (budgets == null || budgets.isEmpty()) {
            result.setStatus("NOT_FOUND");
            result.setMessage("Bạn chưa có ngân sách phù hợp.");
            return result;
        }

        Budget budget = budgets.get(0);
        BigDecimal target = nvl(budget.getTargetAmount());

        BigDecimal spent;
        if (!isBlank(budget.getCategoryId())) {
            spent = nvl(transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                    userId,
                    TransactionType.EXPENSE,
                    budget.getCategoryId(),
                    budget.getStartDate(),
                    budget.getEndDate()
            ));
        } else {
            spent = nvl(transactionRepository.sumByUserIdAndTypeAndDateRange(
                    userId,
                    TransactionType.EXPENSE,
                    budget.getStartDate(),
                    budget.getEndDate()
            ));
        }

        BigDecimal remaining = target.subtract(spent);
        BigDecimal usedPercent = BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP);
        }

        result.setStatus("OK");
        result.setTargetAmount(target);
        result.setSpentAmount(spent);
        result.setRemainingAmount(remaining);
        result.setUsedPercent(usedPercent);

        result.setMessage("Bạn đã dùng " + formatMoney(spent)
                + " / " + formatMoney(target)
                + ", còn lại " + formatMoney(remaining)
                + " (" + usedPercent.stripTrailingZeros().toPlainString() + "% đã sử dụng).");

        return result;
    }

    private BudgetInsightResult buildBudgetWarningResult(String userId, AiIntentResult intentResult) {
        BudgetInsightResult result = new BudgetInsightResult();
        result.setTitle("Cảnh báo ngân sách");

        String categoryId = null;
        Category matched = null;

        if (!isBlank(intentResult.getCategory())) {
            matched = findMatchedCategory(userId, intentResult.getCategory(), CategoryType.EXPENSE);
            if (matched != null) {
                categoryId = matched.getId();
                result.setCategory(matched.getName());
            }
        }

        Long now = System.currentTimeMillis();
        List<Budget> budgets = budgetRepository.findBudgets(userId, categoryId, now);

        if (budgets == null || budgets.isEmpty()) {
            result.setStatus("NOT_FOUND");
            result.setMessage("Không có ngân sách để kiểm tra.");
            return result;
        }

        Budget budget = budgets.get(0);
        BigDecimal target = nvl(budget.getTargetAmount());

        BigDecimal spent;
        if (!isBlank(budget.getCategoryId())) {
            spent = nvl(transactionRepository.sumByUserIdAndTypeAndCategoryIdAndDateRange(
                    userId,
                    TransactionType.EXPENSE,
                    budget.getCategoryId(),
                    budget.getStartDate(),
                    budget.getEndDate()
            ));
        } else {
            spent = nvl(transactionRepository.sumByUserIdAndTypeAndDateRange(
                    userId,
                    TransactionType.EXPENSE,
                    budget.getStartDate(),
                    budget.getEndDate()
            ));
        }

        BigDecimal remaining = target.subtract(spent);
        BigDecimal usedPercent = BigDecimal.ZERO;

        if (target.compareTo(BigDecimal.ZERO) > 0) {
            usedPercent = spent.multiply(BigDecimal.valueOf(100))
                    .divide(target, 2, RoundingMode.HALF_UP);
        }

        result.setTargetAmount(target);
        result.setSpentAmount(spent);
        result.setRemainingAmount(remaining);
        result.setUsedPercent(usedPercent);

        if (spent.compareTo(target) > 0) {
            result.setStatus("EXCEEDED");
            result.setMessage("Bạn đã vượt ngân sách "
                    + formatMoney(spent.subtract(target))
                    + ". Tổng chi hiện tại là " + formatMoney(spent)
                    + " trên ngân sách " + formatMoney(target) + ".");
            return result;
        }

        if (usedPercent.compareTo(BigDecimal.valueOf(80)) >= 0) {
            result.setStatus("WARNING");
            result.setMessage("Bạn đã dùng "
                    + usedPercent.stripTrailingZeros().toPlainString()
                    + "% ngân sách. Còn lại " + formatMoney(remaining) + ".");
            return result;
        }

        result.setStatus("SAFE");
        result.setMessage("Ngân sách vẫn an toàn. Hiện mới dùng "
                + usedPercent.stripTrailingZeros().toPlainString() + "%.");
        return result;
    }

    private RecentTransactionsResult buildRecentTransactionsResult(String userId, AiIntentResult intentResult) {
        RecentTransactionsResult result = new RecentTransactionsResult();
        result.setTitle("Giao dịch gần đây");

        List<Transaction> list;

        if ("EXPENSE".equalsIgnoreCase(intentResult.getType())) {
            list = transactionRepository.findRecentTransactionsByType(userId, TransactionType.EXPENSE);
        } else if ("INCOME".equalsIgnoreCase(intentResult.getType())) {
            list = transactionRepository.findRecentTransactionsByType(userId, TransactionType.INCOME);
        } else {
            list = transactionRepository.findRecentTransactions(userId);
        }

        if (list == null || list.isEmpty()) {
            result.setMessage("Chưa có giao dịch nào.");
            result.setItems(Collections.emptyList());
            return result;
        }

        List<Object> items = new ArrayList<>();

        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Transaction t = list.get(i);

            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", t.getId());
            map.put("amount", t.getAmount());
            map.put("formattedAmount", formatMoney(t.getAmount()));
            map.put("type", t.getType() != null ? t.getType().name() : null);
            map.put("categoryId", t.getCategory() != null ? t.getCategory().getId() : null);
            map.put("categoryName", t.getCategory() != null ? t.getCategory().getName() : null);
            map.put("note", t.getNote());
            map.put("createdAt", t.getCreatedAt());

            items.add(map);
        }

        result.setItems(items);

        @SuppressWarnings("unchecked")
        Map<String, Object> first = (Map<String, Object>) items.get(0);
        result.setMessage("Có " + items.size() + " giao dịch gần nhất. "
                + "Gần nhất là " + first.get("formattedAmount")
                + (first.get("note") != null ? " - " + first.get("note") : "") + ".");

        return result;
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

    private Category mapCategory(String aiCategory, String type) {
        String userId = budgetService.getCurrentUserId();
        List<Category> categories = categoryRepository.findByUserId(userId);

        if (categories == null || categories.isEmpty()) {
            throw new RuntimeException("Người dùng chưa có category để map dữ liệu AI");
        }

        String normalizedCategory = normalize(aiCategory);
        String normalizedType = safeUpper(type);

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

    private boolean isValidAiItem(AiParseResponse item) {
        if (item == null) return false;
        if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) return false;
        if (item.getType() == null || item.getType().trim().isEmpty()) return false;

        try {
            TransactionType.valueOf(item.getType().trim().toUpperCase());
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean sameType(Category category, String normalizedType) {
        return category.getType() != null
                && category.getType().name().equalsIgnoreCase(normalizedType);
    }

    private Category findOtherCategory(List<Category> categories, String type) {
        for (Category category : categories) {
            if (!sameType(category, type)) continue;

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

    private String buildAnswer(AiIntentResult intentResult, Object data) {
        String intent = safeUpper(intentResult.getIntent());
        String timeText = humanizeTimeRange(intentResult.getTimeRange());

        return switch (intent) {
            case "TOTAL_EXPENSE" ->
                    (timeText.isBlank() ? "Tổng chi của bạn là " : "Tổng chi " + timeText + " của bạn là ")
                            + formatMoney((BigDecimal) data) + ".";

            case "TOTAL_INCOME" ->
                    (timeText.isBlank() ? "Tổng thu của bạn là " : "Tổng thu " + timeText + " của bạn là ")
                            + formatMoney((BigDecimal) data) + ".";

            case "BALANCE" ->
                    (timeText.isBlank() ? "Số dư hiện tại của bạn là " : "Số dư " + timeText + " của bạn là ")
                            + formatMoney((BigDecimal) data) + ".";

            case "TOTAL_EXPENSE_BY_CATEGORY" ->
                    "Bạn đã chi " + formatMoney((BigDecimal) data)
                            + (timeText.isBlank() ? "" : " trong " + timeText)
                            + " cho danh mục " + safeText(intentResult.getCategory()) + ".";

            case "TOTAL_INCOME_BY_CATEGORY" ->
                    "Bạn đã thu " + formatMoney((BigDecimal) data)
                            + (timeText.isBlank() ? "" : " trong " + timeText)
                            + " từ danh mục " + safeText(intentResult.getCategory()) + ".";

            case "TOTAL_EXPENSE_BY_KEYWORD" ->
                    "Bạn đã chi " + formatMoney((BigDecimal) data)
                            + " cho nội dung \"" + safeText(intentResult.getKeyword()) + "\".";

            case "TOTAL_INCOME_BY_KEYWORD" ->
                    "Bạn đã nhận " + formatMoney((BigDecimal) data)
                            + " từ nội dung \"" + safeText(intentResult.getKeyword()) + "\".";

            case "TOP_EXPENSE_CATEGORY" -> {
                TopCategoryResult top = (TopCategoryResult) data;
                yield "Danh mục chi nhiều nhất"
                        + (timeText.isBlank() ? "" : " trong " + timeText)
                        + " là " + top.getCategoryName()
                        + " với " + formatMoney(top.getAmount()) + ".";
            }

            case "COMPARE_EXPENSE_THIS_MONTH_LAST_MONTH",
                 "COMPARE_INCOME_THIS_MONTH_LAST_MONTH" ->
                    buildCompareAnswer((CompareResult) data);

            case "ABNORMAL_EXPENSE_CHECK" ->
                    buildAbnormalAnswer((AbnormalResult) data);

            case "SPENDING_ADVICE",
                 "SAVING_SUGGESTION" -> {
                AdviceResult advice = (AdviceResult) data;
                yield advice.getTitle() + ": " + advice.getMessage();
            }

            case "BUDGET_REMAINING",
                 "BUDGET_WARNING" -> {
                BudgetInsightResult budget = (BudgetInsightResult) data;
                yield budget.getTitle() + ": " + budget.getMessage();
            }

            case "RECENT_TRANSACTIONS" -> {
                RecentTransactionsResult recent = (RecentTransactionsResult) data;
                yield recent.getTitle() + ": " + recent.getMessage();
            }

            default ->
                    "Đã xử lý truy vấn thành công.";
        };
    }

    private String buildParseAnswer(List<TransactionResponse> savedTransactions) {
        if (savedTransactions == null || savedTransactions.isEmpty()) {
            return "Mình chưa lưu được giao dịch nào từ câu bạn nhập.";
        }

        if (savedTransactions.size() == 1) {
            TransactionResponse tx = savedTransactions.get(0);
            return "Đã lưu 1 giao dịch với số tiền " + formatMoney(tx.getAmount()) + ".";
        }

        BigDecimal total = savedTransactions.stream()
                .map(TransactionResponse::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return "Đã lưu " + savedTransactions.size()
                + " giao dịch thành công, tổng số tiền là " + formatMoney(total) + ".";
    }

    private String buildCompareAnswer(CompareResult compare) {
        BigDecimal current = nvl(compare.getCurrent());
        BigDecimal previous = nvl(compare.getPrevious());
        BigDecimal diff = nvl(compare.getDifference());

        if (diff.compareTo(BigDecimal.ZERO) > 0) {
            return "Tháng này " + compare.getLabel() + " của bạn là " + formatMoney(current)
                    + ", tăng " + formatMoney(diff)
                    + " so với tháng trước (" + formatMoney(previous) + ").";
        }

        if (diff.compareTo(BigDecimal.ZERO) < 0) {
            return "Tháng này " + compare.getLabel() + " của bạn là " + formatMoney(current)
                    + ", giảm " + formatMoney(diff.abs())
                    + " so với tháng trước (" + formatMoney(previous) + ").";
        }

        return "Tháng này và tháng trước, " + compare.getLabel()
                + " của bạn đều là " + formatMoney(current) + ".";
    }

    private String buildAbnormalAnswer(AbnormalResult abnormal) {
        if (!abnormal.isDetected()) {
            return "Hiện tại mình chưa phát hiện khoản chi nào tăng bất thường so với tháng trước.";
        }

        return "Danh mục có dấu hiệu tăng mạnh nhất là " + abnormal.getCategory()
                + ": tháng này " + formatMoney(abnormal.getCurrent())
                + ", tháng trước " + formatMoney(abnormal.getPrevious())
                + ", chênh lệch " + formatMoney(abnormal.getDifference()) + ".";
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String safeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsMoney(String text) {
        return text != null && MONEY_PATTERN.matcher(text).matches();
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) return false;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikePureTransaction(String text) {
        return containsMoney(text)
                && !containsAny(text, "bao nhiêu", "tổng", "số dư", "nhiều nhất", "so với", "gợi ý", "khuyên");
    }

    private void applyTimeRange(String text, AiIntentResult result) {
        if (containsAny(text, "hôm nay")) {
            result.setTimeRange("TODAY");
        } else if (containsAny(text, "tuần này")) {
            result.setTimeRange("THIS_WEEK");
        } else if (containsAny(text, "tuần trước")) {
            result.setTimeRange("LAST_WEEK");
        } else if (containsAny(text, "tháng này")) {
            result.setTimeRange("THIS_MONTH");
        } else if (containsAny(text, "tháng trước")) {
            result.setTimeRange("LAST_MONTH");
        } else if (containsAny(text, "năm nay")) {
            result.setTimeRange("THIS_YEAR");
        }
    }

    private String extractCategoryKeyword(String text) {
        if (text == null) return "";

        Map<String, String> categoryMap = new LinkedHashMap<>();
        categoryMap.put("ăn uống", "ăn uống");
        categoryMap.put("ăn sáng", "ăn uống");
        categoryMap.put("trà sữa", "ăn uống");
        categoryMap.put("cafe", "ăn uống");
        categoryMap.put("cà phê", "ăn uống");
        categoryMap.put("đi lại", "di chuyển");
        categoryMap.put("di chuyển", "di chuyển");
        categoryMap.put("xăng", "di chuyển");
        categoryMap.put("mua sắm", "mua sắm");
        categoryMap.put("shopping", "mua sắm");
        categoryMap.put("lương", "lương");
        categoryMap.put("thưởng", "thưởng");
        categoryMap.put("học tập", "học tập");
        categoryMap.put("giải trí", "giải trí");
        categoryMap.put("sức khỏe", "sức khỏe");
        categoryMap.put("nhà ở", "nhà ở");

        for (Map.Entry<String, String> entry : categoryMap.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "";
    }

    private String extractKeyword(String text) {
        if (text == null) return "";

        if (containsAny(text, "mẹ cho")) return "mẹ cho";
        if (containsAny(text, "nhặt được")) return "nhặt được";
        if (containsAny(text, "trà sữa")) return "trà sữa";
        if (containsAny(text, "việc làm thêm")) return "việc làm thêm";
        if (containsAny(text, "quà")) return "quà";

        return "";
    }

    private String humanizeTimeRange(String timeRange) {
        if (isBlank(timeRange)) return "";

        return switch (safeUpper(timeRange)) {
            case "TODAY" -> "hôm nay";
            case "THIS_WEEK" -> "tuần này";
            case "LAST_WEEK" -> "tuần trước";
            case "THIS_MONTH" -> "tháng này";
            case "LAST_MONTH" -> "tháng trước";
            case "THIS_YEAR" -> "năm nay";
            default -> "";
        };
    }

    private String formatMoney(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        DecimalFormat format = new DecimalFormat("#,###");
        return format.format(safeAmount.longValue()) + "đ";
    }

    private LocalDateRange resolveTimeRange(String timeRange) {
        if (isBlank(timeRange)) {
            return LocalDateRange.allTime();
        }

        String normalized = safeUpper(timeRange);
        LocalDate today = LocalDate.now();

        return switch (normalized) {
            case "TODAY" -> LocalDateRange.of(today, today);

            case "THIS_WEEK" -> {
                LocalDate start = today.with(DayOfWeek.MONDAY);
                LocalDate end = today.with(DayOfWeek.SUNDAY);
                yield LocalDateRange.of(start, end);
            }

            case "LAST_WEEK" -> {
                LocalDate lastWeekToday = today.minusWeeks(1);
                LocalDate start = lastWeekToday.with(DayOfWeek.MONDAY);
                LocalDate end = lastWeekToday.with(DayOfWeek.SUNDAY);
                yield LocalDateRange.of(start, end);
            }

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

    private static class CompareResult {
        private BigDecimal current;
        private BigDecimal previous;
        private BigDecimal difference;
        private String label;

        public BigDecimal getCurrent() {
            return current;
        }

        public void setCurrent(BigDecimal current) {
            this.current = current;
        }

        public BigDecimal getPrevious() {
            return previous;
        }

        public void setPrevious(BigDecimal previous) {
            this.previous = previous;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    private static class TopCategoryResult {
        private String categoryName;
        private BigDecimal amount;

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }
    }

    private static class AbnormalResult {
        private boolean detected;
        private String category;
        private BigDecimal current;
        private BigDecimal previous;
        private BigDecimal difference;

        public boolean isDetected() {
            return detected;
        }

        public void setDetected(boolean detected) {
            this.detected = detected;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getCurrent() {
            return current;
        }

        public void setCurrent(BigDecimal current) {
            this.current = current;
        }

        public BigDecimal getPrevious() {
            return previous;
        }

        public void setPrevious(BigDecimal previous) {
            this.previous = previous;
        }

        public BigDecimal getDifference() {
            return difference;
        }

        public void setDifference(BigDecimal difference) {
            this.difference = difference;
        }
    }

    private static class AdviceResult {
        private String title;
        private String message;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    private static class BudgetInsightResult {
        private String title;
        private String status;
        private String category;
        private String message;
        private BigDecimal targetAmount;
        private BigDecimal spentAmount;
        private BigDecimal remainingAmount;
        private BigDecimal usedPercent;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public BigDecimal getTargetAmount() {
            return targetAmount;
        }

        public void setTargetAmount(BigDecimal targetAmount) {
            this.targetAmount = targetAmount;
        }

        public BigDecimal getSpentAmount() {
            return spentAmount;
        }

        public void setSpentAmount(BigDecimal spentAmount) {
            this.spentAmount = spentAmount;
        }

        public BigDecimal getRemainingAmount() {
            return remainingAmount;
        }

        public void setRemainingAmount(BigDecimal remainingAmount) {
            this.remainingAmount = remainingAmount;
        }

        public BigDecimal getUsedPercent() {
            return usedPercent;
        }

        public void setUsedPercent(BigDecimal usedPercent) {
            this.usedPercent = usedPercent;
        }
    }

    private static class RecentTransactionsResult {
        private String title;
        private String message;
        private List<Object> items;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public List<Object> getItems() {
            return items;
        }

        public void setItems(List<Object> items) {
            this.items = items;
        }
    }
}