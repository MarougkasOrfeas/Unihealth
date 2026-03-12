package gr.uniwa.unihealth.backend.service.bootstrap;

import gr.uniwa.unihealth.backend.model.SymptomItem;
import gr.uniwa.unihealth.backend.repository.SymptomItemRepository;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SymptomItemInitData {



  public static void addSymptomsData(SymptomItemRepository repository) {
    int order = 1;

    repository.save(createSymptom("Acid reflux in babies", "acid-reflux-in-babies", "A",
        "Reflux is when a baby brings up milk, or is sick, during or shortly after feeding.",
        "Reflux is when a baby brings up milk, or is sick, during or shortly after feeding. It’s very common and usually gets better on its own.",
        "Symptoms of reflux in babies can include:\n" + "- bringing up milk or being sick during or shortly after feeding\n" + "- coughing or hiccuping when feeding\n" + "- being unsettled during feeding\n" + "- swallowing or gulping after burping or feeding\n" + "- crying and not settling\n" + "- not gaining weight because they’re not keeping enough food down",
        "- ask a health visitor for advice and support\n" + "- get advice about your baby’s breastfeeding position or how to bottle feed your baby\n" + "- hold your baby upright during feeding and for as long as possible after feeding\n" + "- burp your baby regularly during feeds\n" + "- make sure your baby sleeps flat on their back",
        "- do not raise the head of their cot or moses basket",
        "See a doctor if your baby:\n" + "- is not improving after trying feeding advice for at least 2 weeks\n" + "- starts reflux after the age of 6 months\n" + "- is older than 1 and still has reflux\n" + "- is losing weight or not gaining weight",
        "Treatment is not usually needed unless reflux is severe or caused by another problem. Sometimes special formula or medicines may be recommended by a doctor.",
        "Reflux usually happens because the ring of muscle at the bottom of a baby’s food pipe is not fully developed yet.",
        order++));

    repository.save(createSymptom("Acid reflux", "acid-reflux", "A",
        "Acid reflux is when stomach acid travels up towards the throat and causes discomfort.",
        "Acid reflux happens when stomach acid travels up from the stomach towards the throat. It can cause a burning feeling in the chest, an unpleasant taste in the mouth and discomfort after eating.",
        "Symptoms of acid reflux can include:\n" + "- heartburn\n" + "- a sour or bitter taste in the mouth\n" + "- bad breath\n" + "- bloating\n" + "- feeling sick\n" + "- difficulty swallowing in some cases",
        "- eat smaller meals more often\n" + "- avoid food or drink that triggers your symptoms\n" + "- try to keep to a healthy weight\n" + "- raise the head of your bed slightly\n" + "- stop smoking if you smoke",
        "- do not eat large meals late at night\n" + "- do not lie down straight after eating\n" + "- do not wear tight clothes around your waist\n" + "- do not overuse alcohol",
        "See a doctor if:\n" + "- symptoms keep coming back\n" + "- medicines from the pharmacy are not helping\n" + "- you have difficulty swallowing\n" + "- you are losing weight without trying",
        "Treatment may include lifestyle changes, avoiding trigger foods, antacids or other medicines that reduce stomach acid.",
        "Acid reflux can be caused by overeating, obesity, pregnancy, certain foods, smoking, alcohol or a weakened muscle between the stomach and food pipe.",
        order++));

    repository.save(createSymptom("Amnesia", "amnesia", "A",
        "Amnesia means memory loss that can affect recent events, past events or the ability to learn new information.",
        "Amnesia is a problem with memory. It can affect the ability to remember past events or make new memories. It can happen suddenly or gradually depending on the cause.",
        "Symptoms can include:\n" + "- memory loss\n" + "- confusion\n" + "- difficulty learning new information\n" + "- forgetting people, places or recent events",
        "- keep a note of when symptoms started\n" + "- ask someone you trust to stay with you if needed\n" + "- seek medical advice for assessment",
        "- do not ignore sudden memory loss\n" + "- do not drive if you are confused\n" + "- do not stay alone if symptoms are severe",
        "See a doctor if:\n" + "- memory loss is persistent\n" + "- symptoms are affecting daily life\n" + "- there are other neurological symptoms",
        "Treatment depends on the cause and may include treatment of underlying illness, neurological assessment and supportive care.",
        "Possible causes include head injury, stroke, dementia, alcohol misuse, infections, seizures or emotional trauma.",
        order++));

    repository.save(createSymptom("Anal pain", "anal-pain", "A",
        "Anal pain can happen for many reasons, including piles, constipation or small tears in the skin.",
        "Anal pain is pain in or around the anus. It can range from mild discomfort to severe pain and may happen during or after going to the toilet.",
        "Symptoms can include:\n" + "- pain during bowel movements\n" + "- itching or irritation\n" + "- bleeding\n" + "- swelling or a lump near the anus",
        "- drink plenty of water\n" + "- eat more fibre\n" + "- keep the area clean and dry\n" + "- use pain relief if appropriate",
        "- do not strain too much when passing stools\n" + "- do not ignore bleeding that continues\n" + "- do not delay seeing a doctor if pain is severe",
        "See a doctor if:\n" + "- pain lasts more than a few days\n" + "- there is repeated bleeding\n" + "- there is a lump or swelling",
        "Treatment depends on the cause and may include creams, pain relief, laxatives or treatment for piles or fissures.",
        "Common causes include piles, anal fissures, constipation, infections or inflammation.",
        order++));

    repository.save(createSymptom("Anger", "anger", "A",
        "Anger is a normal emotion, but if it becomes overwhelming or hard to control it may need support.",
        "Anger is a natural emotional response, but frequent or intense anger can affect relationships, work and mental wellbeing.",
        "Symptoms or signs can include:\n" + "- irritability\n" + "- feeling out of control\n" + "- shouting or aggression\n" + "- physical tension\n" + "- difficulty calming down",
        "- try to identify what triggers your anger\n" + "- take time out before reacting\n" + "- practice slow breathing\n" + "- talk to someone you trust\n" + "- seek professional support if needed",
        "- do not bottle feelings up for too long\n" + "- do not react violently\n" + "- do not ignore patterns that are affecting your life",
        "See a doctor if anger is affecting daily life, work, relationships or your mental health.",
        "Treatment can include talking therapies, anger management strategies and support for underlying mental health issues.",
        "Anger can be linked to stress, anxiety, depression, trauma, relationship difficulties or substance misuse.",
        order++));

    repository.save(createSymptom("Anxiety, fear and panic", "anxiety-fear-and-panic", "A",
        "Anxiety can cause worry, fear and physical symptoms such as a racing heart or shortness of breath.",
        "Anxiety is a feeling of worry, fear or unease. It can be mild or severe and sometimes happens as panic attacks.",
        "Symptoms can include:\n" + "- feeling nervous or on edge\n" + "- racing heartbeat\n" + "- sweating\n" + "- shaking\n" + "- shortness of breath\n" + "- trouble sleeping\n" + "- difficulty concentrating",
        "- try breathing slowly and deeply\n" + "- keep a routine\n" + "- stay physically active\n" + "- reduce caffeine if it makes symptoms worse\n" + "- talk to someone you trust",
        "- do not ignore persistent symptoms\n" + "- do not rely on alcohol or drugs to cope\n" + "- do not isolate yourself completely",
        "See a doctor if anxiety is affecting your daily life, studies, sleep or relationships.",
        "Treatment may include self-help techniques, talking therapies and sometimes medicine.",
        "Causes can include stress, trauma, personality factors, long-term pressure, physical illness or other mental health conditions.",
        order));
  }


  private SymptomItem createSymptom(String title, String slug, String startingLetter, String brief,
      String overviewText, String symptomsText, String doText, String dontText,
      String seeDoctorIfText, String treatmentText, String causesText, int displayOrder) {

    SymptomItem item = new SymptomItem();
    item.setTitle(title);
    item.setSlug(slug);
    item.setStartingLetter(startingLetter);
    item.setBrief(brief);
    item.setOverviewText(overviewText);
    item.setSymptomsText(symptomsText);
    item.setDoText(doText);
    item.setDontText(dontText);
    item.setSeeDoctorIfText(seeDoctorIfText);
    item.setTreatmentText(treatmentText);
    item.setCausesText(causesText);
    item.setDisplayOrder(displayOrder);
    item.setActive(true);
    return item;
  }
}
