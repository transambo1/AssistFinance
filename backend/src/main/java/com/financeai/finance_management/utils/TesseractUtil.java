package com.financeai.finance_management.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.core.io.ClassPathResource;

@Slf4j
public class TesseractUtil {
  public static String extractText(String imagePath) {
    Tesseract tesseract = new Tesseract();
    try {
      // 1. Cấu hình đường dẫn tessdata
      File tessDataFolder = new ClassPathResource("tessdata").getFile();
      tesseract.setDatapath(tessDataFolder.getAbsolutePath());
      tesseract.setLanguage("vie+eng"); // Kết hợp cả tiếng Việt và Anh để tăng độ chính xác

      // 2. Đọc ảnh thông qua ImageIO (Sẽ được JAI Image I/O hỗ trợ giải mã)
      File imageFile = new File(imagePath);
      BufferedImage bufferedImage = ImageIO.read(imageFile);

      if (bufferedImage == null) {
        log.error("❌ Không thể đọc file ảnh: {}", imagePath);
        return "Lỗi: Định dạng ảnh không được hỗ trợ hoặc file hỏng.";
      }

      // 3. Thực hiện OCR trên đối tượng BufferedImage
      log.info("📸 Đang bắt đầu OCR cho file: {}", imageFile.getName());
      return tesseract.doOCR(bufferedImage);

    } catch (Exception e) {
      log.error("❌ Lỗi Tesseract: {}", e.getMessage());
      return "Lỗi: " + e.getMessage();
    }
  }
}
