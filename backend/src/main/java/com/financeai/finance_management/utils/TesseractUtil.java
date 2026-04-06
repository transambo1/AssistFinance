package com.financeai.finance_management.utils;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

public class TesseractUtil {
    public static String extractText(String imagePath) {
        Tesseract tesseract = new Tesseract();
        try {
            // Cách lấy đường dẫn folder tessdata an toàn nhất trong Spring Boot
            File tessDataFolder = new ClassPathResource("tessdata").getFile();
            tesseract.setDatapath(tessDataFolder.getAbsolutePath());

            tesseract.setLanguage("vie");
            return tesseract.doOCR(new File(imagePath));
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi: " + e.getMessage();
        }
    }
}
