package gr.uniwa.unihealth.backend.service.bootstrap;

import gr.uniwa.unihealth.backend.model.UnihealthAssistantItem;
import gr.uniwa.unihealth.backend.model.enums.AssistantSection;
import gr.uniwa.unihealth.backend.repository.UnihealthAssistantItemRepository;
import lombok.experimental.UtilityClass;

@UtilityClass
public class UnihealthAssistantInitData {

  public static void seedInitialData(UnihealthAssistantItemRepository repository) {
    repository.save(
        createItem(AssistantSection.FAQ, "Πώς μπορώ να βρω υπηρεσίες υγείας του Πανεπιστημίου;",
            "Δείτε τις διαθέσιμες πανεπιστημιακές υπηρεσίες υγείας.",
            "Μπορείτε να δείτε τις διαθέσιμες πανεπιστημιακές υπηρεσίες υγείας από την ενότητα \"Υπηρεσίες Υγείας\".",
            null, null, 1));

    repository.save(
        createItem(AssistantSection.FAQ, "Τι να κάνω σε περίπτωση επείγοντος περιστατικού;",
            "Οδηγίες για επείγουσα ανάγκη.",
            "Καλέστε άμεσα το 166 ή το 112. Αν βρίσκεστε εντός πανεπιστημιακού χώρου, ενημερώστε και τη γραμματεία ή την αρμόδια υπηρεσία φύλαξης.",
            null, null, 2));

    repository.save(createItem(AssistantSection.FAQ, "Πού μπορώ να βρω κοντινές κλινικές;",
        "Βρείτε δομές υγείας κοντά στις εγκαταστάσεις του ΠΑΔΑ.",
        "Από την ενότητα \"Κοντινές Κλινικές\" μπορείτε να δείτε δομές υγείας κοντά στις εγκαταστάσεις του ΠΑΔΑ.",
        null, null, 3));

    repository.save(createItem(AssistantSection.FAQ, "Υπάρχει υποστήριξη για ψυχική υγεία;",
        "Διαθεσιμότητα συμβουλευτικής υποστήριξης.",
        "Ναι, μπορείτε να συμπεριλάβετε συμβουλευτικές και υποστηρικτικές υπηρεσίες σε επόμενη έκδοση του βοηθού.",
        null, null, 4));

    repository.save(createItem(AssistantSection.SERVICES, "Υπηρεσίες Υγείας Πανεπιστημίου",
        "Πληροφορίες για διαθέσιμες δομές υποστήριξης φοιτητών.",
        "Πληροφορίες για διαθέσιμες δομές υποστήριξης φοιτητών.", null, null, 1));

    repository.save(createItem(AssistantSection.SERVICES, "Συμβουλευτική Υποστήριξη",
        "Υπηρεσίες ενημέρωσης και καθοδήγησης για θέματα υγείας.",
        "Υπηρεσίες ενημέρωσης και καθοδήγησης για θέματα υγείας.", null, null, 2));

    repository.save(createItem(AssistantSection.EMERGENCY, "ΕΚΑΒ", "Άμεση ιατρική βοήθεια.",
        "Καλέστε το 166 για επείγουσα ιατρική βοήθεια.", "Καλέστε", "tel:166", 1));

    repository.save(createItem(AssistantSection.EMERGENCY, "Ευρωπαϊκός Αριθμός Έκτακτης Ανάγκης",
        "Ευρωπαϊκή γραμμή έκτακτης ανάγκης.", "Καλέστε το 112 για κάθε έκτακτη ανάγκη.", "Καλέστε",
        "tel:112", 2));

    repository.save(
        createItem(AssistantSection.EMERGENCY, "Άμεση Βοήθεια", "Άμεση αστυνομική συνδρομή.",
            "Καλέστε το 100 για άμεση βοήθεια.", "Καλέστε", "tel:100", 3));

    repository.save(createItem(AssistantSection.CLINICS, "Γενικό Νοσοκομείο Αττικής",
        "Ενδεικτική κοντινή μονάδα υγείας.", "Ενδεικτική κοντινή μονάδα υγείας.", null, null, 1));

    repository.save(createItem(AssistantSection.CLINICS, "Κέντρο Υγείας περιοχής Πανεπιστημίου",
        "Ενδεικτική δομή για εξωτερική περίθαλψη.", "Ενδεικτική δομή για εξωτερική περίθαλψη.",
        null, null, 2));

    repository.save(createItem(AssistantSection.CLINICS, "Εφημερεύον Νοσοκομείο",
        "Μπορεί να προστεθεί δυναμικά σε επόμενη έκδοση.",
        "Μπορεί να προστεθεί δυναμικά σε επόμενη έκδοση.", null, null, 3));
  }

  private UnihealthAssistantItem createItem(AssistantSection section, String title, String brief,
      String details, String actionLabel, String actionValue, int displayOrder) {
    UnihealthAssistantItem item = new UnihealthAssistantItem();
    item.setSection(section);
    item.setTitle(title);
    item.setBrief(brief);
    item.setContent(details);
    item.setActionLabel(actionLabel);
    item.setActionValue(actionValue);
    item.setDisplayOrder(displayOrder);
    item.setActive(true);
    return item;
  }
}
