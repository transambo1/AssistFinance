package com.financeai.finance_management.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import opennlp.tools.namefind.*;
import opennlp.tools.util.*;
import org.junit.jupiter.api.Test;

public class ModelTrainingTest {

  @Test
  public void trainMoneyModel() throws Exception {
    // 1. Đường dẫn file đầu vào (.train) và đầu ra (.bin)
    File trainingData = new File("src/main/resources/nlp-models/money.train");
    File modelOutput = new File("src/main/resources/nlp-models/money-model.bin");

    // 2. Thiết lập dòng dữ liệu để đọc file .train
    InputStreamFactory in = new MarkableFileInputStreamFactory(trainingData);
    ObjectStream<String> lineStream = new PlainTextByLineStream(in, StandardCharsets.UTF_8);
    ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);

    // 3. Cấu hình tham số huấn luyện (Số lần lặp và ngưỡng cắt giảm)
    TrainingParameters params = TrainingParameters.defaultParams();
    params.put(TrainingParameters.ITERATIONS_PARAM, 100);
    params.put(TrainingParameters.CUTOFF_PARAM, 5);

    try {
      // 4. Bắt đầu quá trình huấn luyện
      TokenNameFinderModel model =
          NameFinderME.train(
              "vi", // Ngôn ngữ
              "money", // Loại nhãn (Entity Type)
              sampleStream,
              params,
              new TokenNameFinderFactory());

      // 5. Lưu kết quả ra file .bin
      try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream(modelOutput))) {
        model.serialize(modelOut);
      }

      System.out.println(
          "✅ Huấn luyện thành công! File đã được lưu tại: " + modelOutput.getAbsolutePath());

    } finally {
      sampleStream.close();
    }
  }
}
