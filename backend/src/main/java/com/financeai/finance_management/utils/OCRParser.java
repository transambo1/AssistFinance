package com.financeai.finance_management.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRParser {

    public static Long extractAmount(String text) {
        if (text == null) return 0L;

        // Tìm tất cả các cụm số có dấu chấm hoặc phẩy (định dạng tiền tệ)
        // Ví dụ: 33.000, 852.000, 1.200.000
        Pattern p = Pattern.compile("\\d{1,3}([\\.,]\\d{3})+");
        Matcher m = p.matcher(text);

        List<Long> candidates = new ArrayList<>();
        while (m.find()) {
            candidates.add(cleanAmountString(m.group()));
        }

        // Lấy số lớn nhất nhưng phải < 10.000.000 (để tránh lấy số điện thoại)
        return candidates.stream()
                .filter(n -> n < 10000000)
                .max(Long::compare)
                .orElse(0L);
    }

    private static Long cleanAmountString(String raw) {
        // Xóa TẤT CẢ những gì không phải là số (0-9)
        String cleaned = raw.replaceAll("[^0-9]", "");
        if (cleaned.isEmpty()) return 0L;
        try {
            return Long.parseLong(cleaned);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static Long findLargestNumber(String text) {
        // Chỉ lấy chuỗi số từ 4 đến 8 chữ số (Tiền từ 1.000 đến 99.000.000)
        Pattern pattern = Pattern.compile("\\b\\d{4,8}\\b");
        Matcher matcher = pattern.matcher(text.replaceAll("[\\.,]", ""));

        List<Long> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Long.parseLong(matcher.group()));
        }

        // Lấy số lớn nhất trong khoảng thực tế (dưới 10 triệu cho hóa đơn lẻ)
        return numbers.stream()
                .filter(n -> n < 10000000)
                .max(Long::compare)
                .orElse(0L);
    }

    public static String extractDate(String text) {
        if (text == null || text.isEmpty()) return null;

        // Regex tìm ngày định dạng dd/MM/yyyy hoặc yyyy-MM-dd
        String dateRegex = "(\\d{1,2}[/|-]\\d{1,2}[/|-]\\d{4})|(\\d{4}[/|-]\\d{1,2}[/|-]\\d{1,2})";
        Pattern pattern = Pattern.compile(dateRegex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String dateStr = matcher.group();
            return dateStr.replace("/", "-");
        }
        return null;
    }


}