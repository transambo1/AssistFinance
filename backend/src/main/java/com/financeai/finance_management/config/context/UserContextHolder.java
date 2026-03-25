package com.financeai.finance_management.config.context;


import java.util.List;

public final class UserContextHolder {
  private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

  private UserContextHolder() {
    // Prevent instantiation
  }

  public static void startOperation(
      Long userId, String userEmail) {
    set(
        UserContext.builder()
            .userId(userId)
            .userEmail(userEmail)
            .build());
  }

  public static UserContext get() {
    return CONTEXT.get();
  }

  public static void set(UserContext context) {
    CONTEXT.set(context);
  }

  public static void clear() {
    CONTEXT.remove();
  }
}
