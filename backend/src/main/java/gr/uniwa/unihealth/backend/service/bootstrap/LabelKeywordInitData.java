package gr.uniwa.unihealth.backend.service.bootstrap;

import gr.uniwa.unihealth.backend.config.context.TenantContext;
import gr.uniwa.unihealth.backend.model.LabelKeywordMapping;
import gr.uniwa.unihealth.backend.repository.LabelKeywordMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LabelKeywordInitData implements ApplicationRunner {

  private final LabelKeywordMappingRepository repository;
  private final TenantContext tenantContext;

  @Override
  public void run(ApplicationArguments args) {
    tenantContext.runForEachTenant(this::initialize, "LabelKeywordInitData.run");
  }

  public void initialize(String tenantId) {
    if (repository.count() > 0)
      return;
    log.info("Initializing keyword mappings for tenant [{}]", tenantId);

    List<LabelKeywordMapping> keywords = new ArrayList<>();

    // =========================
    // Allergies
    // =========================
    keywords.add(kw("peanut", "ALLERGY_PEANUT", "ALLERGY"));
    keywords.add(kw("peanuts", "ALLERGY_PEANUT", "ALLERGY"));
    keywords.add(kw("groundnut", "ALLERGY_PEANUT", "ALLERGY"));
    keywords.add(kw("groundnuts", "ALLERGY_PEANUT", "ALLERGY"));
    keywords.add(kw("tree nut", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("tree nuts", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("almond", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("almonds", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("cashew", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("cashews", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("walnut", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("walnuts", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("hazelnut", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("hazelnuts", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("pistachio", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("pistachios", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("pecan", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("pecans", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("macadamia", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("macadamias", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("brazil nut", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("brazil nuts", "ALLERGY_TREE_NUT", "ALLERGY"));
    keywords.add(kw("dairy", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("milk", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("cow milk", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("milk protein", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("casein", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("whey", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("lactose", "ALLERGY_DAIRY", "ALLERGY"));
    keywords.add(kw("gluten", "ALLERGY_GLUTEN", "ALLERGY"));
    keywords.add(kw("wheat", "ALLERGY_GLUTEN", "ALLERGY"));
    keywords.add(kw("barley", "ALLERGY_GLUTEN", "ALLERGY"));
    keywords.add(kw("rye", "ALLERGY_GLUTEN", "ALLERGY"));
    keywords.add(kw("egg", "ALLERGY_EGG", "ALLERGY"));
    keywords.add(kw("eggs", "ALLERGY_EGG", "ALLERGY"));
    keywords.add(kw("egg white", "ALLERGY_EGG", "ALLERGY"));
    keywords.add(kw("egg yolk", "ALLERGY_EGG", "ALLERGY"));
    keywords.add(kw("shellfish", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("shrimp", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("prawn", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("prawns", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("crab", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("lobster", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("crayfish", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("mussel", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("mussels", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("clam", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("clams", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("oyster", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("oysters", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("scallop", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("scallops", "ALLERGY_SHELLFISH", "ALLERGY"));
    keywords.add(kw("soy", "ALLERGY_SOY", "ALLERGY"));
    keywords.add(kw("soya", "ALLERGY_SOY", "ALLERGY"));
    keywords.add(kw("soybean", "ALLERGY_SOY", "ALLERGY"));
    keywords.add(kw("soybeans", "ALLERGY_SOY", "ALLERGY"));
    keywords.add(kw("onion", "ALLERGY_ONION", "ALLERGY"));
    keywords.add(kw("onions", "ALLERGY_ONION", "ALLERGY"));
    keywords.add(kw("fish", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("salmon", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("tuna", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("cod", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("sardine", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("sardines", "ALLERGY_FISH", "ALLERGY"));
    keywords.add(kw("sesame", "ALLERGY_SESAME", "ALLERGY"));
    keywords.add(kw("sesame seed", "ALLERGY_SESAME", "ALLERGY"));
    keywords.add(kw("sesame seeds", "ALLERGY_SESAME", "ALLERGY"));
    keywords.add(kw("tahini", "ALLERGY_SESAME", "ALLERGY"));
    keywords.add(kw("mustard", "ALLERGY_MUSTARD", "ALLERGY"));
    keywords.add(kw("celery", "ALLERGY_CELERY", "ALLERGY"));
    keywords.add(kw("garlic", "ALLERGY_GARLIC", "ALLERGY"));
    keywords.add(kw("corn", "ALLERGY_CORN", "ALLERGY"));
    keywords.add(kw("maize", "ALLERGY_CORN", "ALLERGY"));
    keywords.add(kw("tomato", "ALLERGY_TOMATO", "ALLERGY"));
    keywords.add(kw("tomatoes", "ALLERGY_TOMATO", "ALLERGY"));
    keywords.add(kw("chocolate", "ALLERGY_CHOCOLATE", "ALLERGY"));
    keywords.add(kw("cocoa", "ALLERGY_CHOCOLATE", "ALLERGY"));
    keywords.add(kw("strawberry", "ALLERGY_STRAWBERRY", "ALLERGY"));
    keywords.add(kw("strawberries", "ALLERGY_STRAWBERRY", "ALLERGY"));
    keywords.add(kw("kiwi", "ALLERGY_KIWI", "ALLERGY"));
    keywords.add(kw("banana", "ALLERGY_BANANA", "ALLERGY"));
    keywords.add(kw("bananas", "ALLERGY_BANANA", "ALLERGY"));
    keywords.add(kw("avocado", "ALLERGY_AVOCADO", "ALLERGY"));
    keywords.add(kw("avocados", "ALLERGY_AVOCADO", "ALLERGY"));
    keywords.add(kw("mushroom", "ALLERGY_MUSHROOM", "ALLERGY"));
    keywords.add(kw("mushrooms", "ALLERGY_MUSHROOM", "ALLERGY"));
    keywords.add(kw("sulfite", "ALLERGY_SULFITE", "ALLERGY"));
    keywords.add(kw("sulfites", "ALLERGY_SULFITE", "ALLERGY"));
    keywords.add(kw("sulphite", "ALLERGY_SULFITE", "ALLERGY"));
    keywords.add(kw("sulphites", "ALLERGY_SULFITE", "ALLERGY"));

    // =========================
    // Chronic conditions
    // =========================
    keywords.add(kw("asthma", "CHRONIC_ASTHMA", "CHRONIC"));
    keywords.add(kw("asthmatic", "CHRONIC_ASTHMA", "CHRONIC"));
    keywords.add(kw("diabetes", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("diabetic", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("type 1 diabetes", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("type 2 diabetes", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("t1d", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("t2d", "CHRONIC_DIABETES", "CHRONIC"));
    keywords.add(kw("hypertension", "CHRONIC_HYPERTENSION", "CHRONIC"));
    keywords.add(kw("high blood pressure", "CHRONIC_HYPERTENSION", "CHRONIC"));
    keywords.add(kw("blood pressure", "CHRONIC_HYPERTENSION", "CHRONIC"));
    keywords.add(kw("htn", "CHRONIC_HYPERTENSION", "CHRONIC"));
    keywords.add(kw("heart disease", "CHRONIC_HEART_DISEASE", "CHRONIC"));
    keywords.add(kw("cardiovascular disease", "CHRONIC_HEART_DISEASE", "CHRONIC"));
    keywords.add(kw("coronary disease", "CHRONIC_HEART_DISEASE", "CHRONIC"));
    keywords.add(kw("heart condition", "CHRONIC_HEART_DISEASE", "CHRONIC"));
    keywords.add(kw("cardiac condition", "CHRONIC_HEART_DISEASE", "CHRONIC"));
    keywords.add(kw("arthritis", "CHRONIC_ARTHRITIS", "CHRONIC"));
    keywords.add(kw("osteoarthritis", "CHRONIC_ARTHRITIS", "CHRONIC"));
    keywords.add(kw("rheumatoid arthritis", "CHRONIC_ARTHRITIS", "CHRONIC"));
    keywords.add(kw("thyroid", "CHRONIC_THYROID", "CHRONIC"));
    keywords.add(kw("hypothyroidism", "CHRONIC_THYROID", "CHRONIC"));
    keywords.add(kw("hyperthyroidism", "CHRONIC_THYROID", "CHRONIC"));
    keywords.add(kw("hashimoto", "CHRONIC_THYROID", "CHRONIC"));
    keywords.add(kw("graves disease", "CHRONIC_THYROID", "CHRONIC"));
    keywords.add(kw("celiac", "CHRONIC_CELIAC", "CHRONIC"));
    keywords.add(kw("coeliac", "CHRONIC_CELIAC", "CHRONIC"));
    keywords.add(kw("celiac disease", "CHRONIC_CELIAC", "CHRONIC"));
    keywords.add(kw("coeliac disease", "CHRONIC_CELIAC", "CHRONIC"));
    keywords.add(kw("ibs", "CHRONIC_IBS", "CHRONIC"));
    keywords.add(kw("irritable bowel syndrome", "CHRONIC_IBS", "CHRONIC"));
    keywords.add(kw("crohn", "CHRONIC_CROHNS", "CHRONIC"));
    keywords.add(kw("crohns", "CHRONIC_CROHNS", "CHRONIC"));
    keywords.add(kw("crohn disease", "CHRONIC_CROHNS", "CHRONIC"));
    keywords.add(kw("crohn s disease", "CHRONIC_CROHNS", "CHRONIC"));
    keywords.add(kw("depression", "CHRONIC_DEPRESSION", "CHRONIC"));
    keywords.add(kw("depressive disorder", "CHRONIC_DEPRESSION", "CHRONIC"));
    keywords.add(kw("major depression", "CHRONIC_DEPRESSION", "CHRONIC"));
    keywords.add(kw("anxiety", "CHRONIC_ANXIETY", "CHRONIC"));
    keywords.add(kw("anxiety disorder", "CHRONIC_ANXIETY", "CHRONIC"));
    keywords.add(kw("panic disorder", "CHRONIC_ANXIETY", "CHRONIC"));
    keywords.add(kw("copd", "CHRONIC_COPD", "CHRONIC"));
    keywords.add(kw("chronic obstructive pulmonary disease", "CHRONIC_COPD", "CHRONIC"));
    keywords.add(kw("gerd", "CHRONIC_GERD", "CHRONIC"));
    keywords.add(kw("acid reflux", "CHRONIC_GERD", "CHRONIC"));
    keywords.add(kw("reflux", "CHRONIC_GERD", "CHRONIC"));
    keywords.add(kw("gastroesophageal reflux", "CHRONIC_GERD", "CHRONIC"));
    keywords.add(kw("ulcerative colitis", "CHRONIC_ULCERATIVE_COLITIS", "CHRONIC"));
    keywords.add(kw("colitis", "CHRONIC_ULCERATIVE_COLITIS", "CHRONIC"));
    keywords.add(kw("chronic kidney disease", "CHRONIC_KIDNEY_DISEASE", "CHRONIC"));
    keywords.add(kw("kidney disease", "CHRONIC_KIDNEY_DISEASE", "CHRONIC"));
    keywords.add(kw("ckd", "CHRONIC_KIDNEY_DISEASE", "CHRONIC"));
    keywords.add(kw("fatty liver", "CHRONIC_LIVER_DISEASE", "CHRONIC"));
    keywords.add(kw("liver disease", "CHRONIC_LIVER_DISEASE", "CHRONIC"));
    keywords.add(kw("nafld", "CHRONIC_LIVER_DISEASE", "CHRONIC"));
    keywords.add(kw("epilepsy", "CHRONIC_EPILEPSY", "CHRONIC"));
    keywords.add(kw("seizure disorder", "CHRONIC_EPILEPSY", "CHRONIC"));
    keywords.add(kw("migraine", "CHRONIC_MIGRAINE", "CHRONIC"));
    keywords.add(kw("migraines", "CHRONIC_MIGRAINE", "CHRONIC"));
    keywords.add(kw("osteoporosis", "CHRONIC_OSTEOPOROSIS", "CHRONIC"));
    keywords.add(kw("pcos", "CHRONIC_PCOS", "CHRONIC"));
    keywords.add(kw("polycystic ovary syndrome", "CHRONIC_PCOS", "CHRONIC"));
    keywords.add(kw("lupus", "CHRONIC_LUPUS", "CHRONIC"));
    keywords.add(kw("psoriasis", "CHRONIC_PSORIASIS", "CHRONIC"));
    keywords.add(kw("fibromyalgia", "CHRONIC_FIBROMYALGIA", "CHRONIC"));
    keywords.add(kw("sleep apnea", "CHRONIC_SLEEP_APNEA", "CHRONIC"));
    keywords.add(kw("sleep apnoea", "CHRONIC_SLEEP_APNEA", "CHRONIC"));
    keywords.add(kw("anemia", "CHRONIC_ANEMIA", "CHRONIC"));
    keywords.add(kw("anaemia", "CHRONIC_ANEMIA", "CHRONIC"));
    
    repository.saveAll(keywords);
  }

  private LabelKeywordMapping kw(String keyword, String labelCode, String type) {
    LabelKeywordMapping m = new LabelKeywordMapping();
    m.setKeyword(keyword);
    m.setLabelCode(labelCode);
    m.setKeywordType(type);
    m.setActive(true);
    return m;
  }
}
