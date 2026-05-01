package com.financeai.finance_management.controller;

import com.financeai.finance_management.service.ICategoryNLPService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/nlp")
@RequiredArgsConstructor
public class NLPAdminController {

    private final ICategoryNLPService nlpService;

    @PostMapping("/retrain")
    public String retrain() {
        long start = System.currentTimeMillis();

        nlpService.forceRetrain();

        long time = System.currentTimeMillis() - start;

        return "✅ Model đã retrain xong trong " + time + " ms";
    }
}