package com.financeai.finance_management.common;

import java.security.SecureRandom;

public class IdGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() {
        // private constructor để ngăn khởi tạo
    }

    public static long generateRandomId() {
        long id;
        do {
            id = RANDOM.nextLong();
        } while (id <= 0); // tránh số âm hoặc 0 (nếu cần)
        return id;
    }
}

