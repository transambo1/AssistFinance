package com.financeai.finance_management.service;

import com.financeai.finance_management.dto.response.TransactionResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IOCRService {
  TransactionResponse processReceipt(MultipartFile file);
}
