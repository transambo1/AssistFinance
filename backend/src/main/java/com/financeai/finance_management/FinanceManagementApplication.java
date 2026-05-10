package com.financeai.finance_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching // Kích hoạt bộ quản lý Cache
@EnableAsync
public class FinanceManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(FinanceManagementApplication.class, args);
  }

  // Cấu hình bảo mật trực tiếp tại đây để ép Spring Boot phải nhận

}
