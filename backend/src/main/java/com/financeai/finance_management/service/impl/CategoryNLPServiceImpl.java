package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.service.ICategoryNLPService;
import opennlp.tools.doccat.*;
import opennlp.tools.util.*;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.springframework.core.io.ClassPathResource;

@Service
public class CategoryNLPServiceImpl implements ICategoryNLPService {

    private DoccatModel model;

    @PostConstruct
    public void trainModel() {
        try {
            // Đọc file train từ resources
            InputStream inputStream = new ClassPathResource("categorizer.train").getInputStream();
            ObjectStream<String> lineStream = new PlainTextByLineStream(new InputStreamFactory() {
                @Override public InputStream createInputStream() { return inputStream; }
            }, StandardCharsets.UTF_8);

            ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(lineStream);

            TrainingParameters params = new TrainingParameters();
            params.put(TrainingParameters.ITERATIONS_PARAM, 100);
            params.put(TrainingParameters.CUTOFF_PARAM, 0);

            this.model = DocumentCategorizerME.train("vie", sampleStream, params, new DoccatFactory());
            System.out.println("✅ [NLP] Model đã được huấn luyện thành công!");
        } catch (Exception e) {
            System.err.println("❌ [NLP] Lỗi huấn luyện: " + e.getMessage());
        }
    }


    @Override
    public String predictCategory(String text) {
        if (model == null || text == null) return "Chi_phi_khac";

        DocumentCategorizerME categorizer = new DocumentCategorizerME(model);

        // 1. Chuyển về chữ thường + BỎ DẤU hoàn toàn (Normalize)
        String cleanText = removeAccent(text.toLowerCase());

        // 2. Chỉ giữ lại chữ cái a-z (Xóa sạch số 18:18, dấu #, @, /, :)
        cleanText = cleanText.replaceAll("[^a-z\\s]", " ");

        // 3. Tách từ và lọc từ rác (Chỉ lấy từ >= 3 ký tự để bỏ SL, sI, BI, K)
        String[] tokens = java.util.Arrays.stream(cleanText.split("\\s+"))
                .filter(word -> word.length() >= 3)
                .toArray(String[]::new);

        if (tokens.length == 0) return "Chi_phi_khac";

        double[] outcomes = categorizer.categorize(tokens);
        return categorizer.getBestCategory(outcomes);
    }

    // Hàm hỗ trợ bỏ dấu tiếng Việt (Quan trọng nhất)
    private String removeAccent(String s) {
        String temp = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
   /* public void loadModel() {
        try {
            // 1. Lấy file từ Resources
            ClassPathResource modelRes = new ClassPathResource("nlp-models/finance.model");
            ClassPathResource structRes = new ClassPathResource("nlp-models/finance.structure");

            // 2. Mẹo nhỏ: Vì Model nằm trong file JAR không có "đường dẫn thực",
            // ta nên copy nó ra một file tạm để thư mục NLP đọc được
            Path tempModel = Files.createTempFile("finance_", ".model");
            Path tempStruct = Files.createTempFile("finance_", ".structure");

            Files.copy(modelRes.getInputStream(), tempModel, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(structRes.getInputStream(), tempStruct, StandardCopyOption.REPLACE_EXISTING);

            // 3. Load model từ file tạm này
            // Giả sử hàm load của bạn cần đường dẫn String:
            this.nlpModel = YourNLPLibrary.load(tempModel.toAbsolutePath().toString());

            log.info("Đã load model thành công từ Resources!");
        } catch (Exception e) {
            log.error("Không thể load model sẵn có: {}", e.getMessage());
        }*/
    //}
}