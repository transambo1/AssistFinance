package com.financeai.finance_management.controller;

import com.financeai.finance_management.dto.response.TransactionResponse;
import com.financeai.finance_management.service.IOCRService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/ocr")
public class OCRController {

    @Autowired
    private IOCRService ocrService;

    @PostMapping("/upload")
    public ResponseEntity<TransactionResponse> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ocrService.processReceipt(file));
    }
}