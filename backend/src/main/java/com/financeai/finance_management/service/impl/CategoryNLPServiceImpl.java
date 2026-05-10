package com.financeai.finance_management.service.impl;

import com.financeai.finance_management.service.ICategoryNLPService;
import com.financeai.finance_management.utils.OCRParser;
import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.doccat.*;
import opennlp.tools.namefind.*;
import opennlp.tools.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CategoryNLPServiceImpl implements ICategoryNLPService {
  private DoccatModel catModel;
  private static final String CAT_MODEL_PATH = "src/main/resources/nlp-models/category-model.bin";

  @PostConstruct
  public void init() {
    new File("src/main/resources/nlp-models").mkdirs();
    File modelFile = new File(CAT_MODEL_PATH);

    if (modelFile.exists()) {
      log.info(" Loading AI model ");
      loadModel();
    } else {
      forceRetrain();
    }
  }

  private void loadModel() {
    try (InputStream is = new FileInputStream(CAT_MODEL_PATH)) {
      this.catModel = new DoccatModel(is);
    } catch (Exception e) {
      log.error("❌ Load Cat Model Fail");
    }
  }

  @Override
  public void trainMoneyModel() {
    try {
      Resource res = new ClassPathResource("nlp-models/money.train");
      ObjectStream<NameSample> sampleStream =
          new NameSampleDataStream(
              new PlainTextByLineStream(
                  new MarkableFileInputStreamFactory(res.getFile()), StandardCharsets.UTF_8));

      TrainingParameters params = new TrainingParameters();
      params.put(TrainingParameters.ITERATIONS_PARAM, 200);

      TokenNameFinderModel moneyModel =
          NameFinderME.train("vi", "money", sampleStream, params, new TokenNameFinderFactory());
      try (OutputStream out =
          new FileOutputStream("src/main/resources/nlp-models/money-model.bin")) {
        moneyModel.serialize(out);
      }
      OCRParser.reloadModel();
      log.info("✅ Trained Money Model");
    } catch (Exception e) {
      log.error("❌ Train Money Fail: {}", e.getMessage());
    }
  }

  @Override
  public String predictCategory(String text) {
    if (catModel == null) return "Chi_phi_khac";
    String[] tokens = OCRParser.tokenizeForNLP(text); // Dùng chung bộ tokenizer
    double[] outcomes = new DocumentCategorizerME(catModel).categorize(tokens);
    return new DocumentCategorizerME(catModel).getBestCategory(outcomes);
  }

  @Override
  public void forceRetrain() {
    trainMoneyModel();
    log.info(" Toàn bộ AI đã được huấn luyện lại");
  }
}
