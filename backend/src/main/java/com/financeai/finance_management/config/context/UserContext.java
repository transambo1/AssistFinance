package com.financeai.finance_management.config.context;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder(access = AccessLevel.PACKAGE)
public class UserContext {
  private Long userId;
  private String userEmail;
}
