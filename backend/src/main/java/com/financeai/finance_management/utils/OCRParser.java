package com.financeai.finance_management.utils;

import lombok.extern.slf4j.Slf4j;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class OCRParser {

    private static TokenNameFinderModel model;
    private static final String MODEL_PATH = "src/main/resources/nlp-models/money-model.bin";

    static { reloadModel(); }

    public static void reloadModel() {
        try (InputStream is = new FileInputStream(MODEL_PATH)) {
            model = new TokenNameFinderModel(is);
            log.info(" Money model reloaded");
        } catch (Exception e) { log.error("❌ Reload model fail", e); }
    }

    /**
     * Hàm chính để trích xuất số tiền từ văn bản OCR
     */
    public static Long extractAmount(String text) {
        if (text == null || text.isBlank() || model == null) return 0L;

        try {
            // Bước 1: Tiền xử lý và chia nhỏ văn bản thành mảng từ (tokens)
            String[] tokens = tokenizeForNLP(text);
            NameFinderME nameFinder = new NameFinderME(model);
            Span[] spans = nameFinder.find(tokens);

            // Duyệt ngược từ dưới lên
            for (int i = spans.length - 1; i >= 0; i--) {
                String detected = buildString(tokens, spans[i]);

                //  bỏ các số không phải tiền (SĐT, Pin, Mã đơn)
                if (isInvalidMoneyFormat(detected)) continue;

                //  ngữ cảnh tài chính
                if (hasFinancialContext(tokens, spans[i].getStart())) {
                    return cleanToLong(detected);
                }
            }
        } catch (Exception e) {
            log.error("❌ AI Error: {}", e.getMessage());
        }

        // (Fallback)
        return extractByRegex(text);
    }


    public static String extractDate(String text) {
        Matcher m = Pattern.compile("(\\d{1,2}[/|-]\\d{1,2}[/|-]\\d{4})").matcher(text);
        return m.find() ? m.group().replace("/", "-") : null;
    }


    private static String buildString(String[] tokens, Span span) {
        StringBuilder sb = new StringBuilder();
        for (int i = span.getStart(); i < span.getEnd(); i++) {
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    public static String[] tokenizeForNLP(String text) {
        String cleaned = text.replaceAll("\\b\\d{1,2}[:/-]\\d{2,4}\\b", " ")
                .replaceAll("(?i)(\\d+)([đ|VND|₫])", "$1 $2")
                .replaceAll("[^a-zA-Z0-9àáạảãâầấnậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđĐ.,\\s-]", " ");
        return cleaned.trim().split("\\s+");
    }

    private static boolean isInvalidMoneyFormat(String detected) {
        String digits = detected.replaceAll("[^0-9]", "");
        // Chặn SĐT: Dài >= 10
        if (digits.length() >= 10 && !detected.contains(".") && !detected.contains(",")) return true;

        if (digits.length() < 3 && !detected.toLowerCase().contains("k")) return true;
        return false;
    }

    private static boolean hasFinancialContext(String[] tokens, int startIdx) {
        int lookBack = Math.max(0, startIdx - 3);
        for (int i = startIdx - 1; i >= lookBack; i--) {
            String t = tokens[i].toLowerCase();
            if (t.matches(".*(tổng|toán|tiền|giá|đơn|phí).*")) return true;
        }
        return false;
    }

    private static Long cleanToLong(String raw) {
        String numeric = raw.replaceAll("[^0-9]", "");
        if (numeric.isEmpty()) return 0L;
        long val = Long.parseLong(numeric);
        return raw.toLowerCase().contains("k") ? val * 1000 : val;
    }

    private static Long extractByRegex(String text) {
        Pattern p = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,})");
        Matcher m = p.matcher(text);
        long lastValidValue = 0;
        while (m.find()) {
            long val = Long.parseLong(m.group().replaceAll("[.,]", ""));
            // Lấy con số cuối cùng nằm trong khoảng giá trị hợp lý
            if (val >= 1000 && val < 10000000) lastValidValue = val;
        }
        return lastValidValue;
    }
}