package gr.uniwa.unihealth.backend.utils.email;

public class EmailNotificationTextConstants {

  public static final String EMAIL_CHANGED_SUBJECT_EN = "Previous email address has changed";
  public static final String EMAIL_CHANGED_SUBJECT_GR = "Η προηγούμενη διεύθυνση email άλλαξε";

  public static final String IMMINENT_DEACTIVATION_SUBJECT_EN = "Your account will be deactivated soon";
  public static final String IMMINENT_DEACTIVATION_SUBJECT_GR = "Ο λογαριασμός σας θα απενεργοποιηθεί σύντομα";

  public static final String DATED_ADMIN_ACCOUNT_SUBJECT = "Σημαντικό μήνυμα/Important Notice: Απενεργοποίηση του τελευταίου λογαριασμού διαχειριστή λόγω προγραμματισμένης ημερομηνίας λήξης/Last Admin Account Pending Deactivation Due to Schedule";

  public static final String EMAIL_CHANGED_BODY_EN = """
    <html>
    <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
    <p>The email address associated with your UniHealth account was changed before the verification process was completed. As a result, any previous links are no longer valid.</p>
    <p>If this change should not happen, please contact an administrator.</p>
    </body>
    </html>""";
  public static final String EMAIL_CHANGED_BODY_GR = """
    <html>
    <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
    <p>Η διεύθυνση email που είναι συνδεδεμένη με τον λογαριασμό σας στο UniHealth άλλαξε πριν ολοκληρωθεί η διαδικασία επαλήθευσης.</p>
    <p>Ως αποτέλεσμα, οποιοσδήποτε προηγούμενος σύνδεσμος επαλήθευσης δεν είναι πλέον έγκυρος.</p>
    <p>Εάν αυτή η αλλαγή δεν θα έπρεπε να έχει συμβεί, παρακαλούμε επικοινωνήστε με έναν διαχειριστή.</p>
    </body>
    </html>""";

  public static final String IMMINENT_DEACTIVATION_BODY_EN = """
    <html>
    <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
    <p>Hi</p>
    <p>This message is to inform you that your account has been inactive for the past 12 months.User accounts that remain inactive for 13 consecutive months are automatically deactivated.</p>
    <p>Unless activity is recorded, your account is scheduled for deactivation on {0}.</p>
    <p><strong>Required Action</strong></p>
    <p>To prevent the deactivation of your account, please log in to the application within the next 30 days.</p>
    <p>Access the application here: {1}</p>
    </body>
    </html>
    """;

  public static final String IMMINENT_DEACTIVATION_BODY_GR = """
    <html>
    <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
    <p>Γεια σας</p>
    <p>Με το παρόν μήνυμα σας ενημερώνουμε ότι ο λογαριασμός σας είναι ανενεργός τους τελευταίους 12 μήνες. Οι λογαριασμοί χρηστών που παραμένουν ανενεργοί για 13 συνεχόμενους μήνες απενεργοποιούνται αυτόματα.</p>
    <p>Εάν δεν καταγραφεί κάποια δραστηριότητα, ο λογαριασμός σας έχει προγραμματιστεί να απενεργοποιηθεί στις {0}.</p>
    <p><strong>Απαιτούμενη ενέργεια</strong></p>
    <p>Για να αποτρέψετε την απενεργοποίηση του λογαριασμού σας, παρακαλούμε συνδεθείτε στην εφαρμογή μέσα στις επόμενες 30 ημέρες.</p>
    <p>Πρόσβαση στην εφαρμογή εδώ: {1}</p>
    </body>
    </html>
    """;

  public static final String DATED_ADMIN_ACCOUNT_BODY = """
    <html>
    <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
    <p>Γεια σας</p>
    <p>Με το παρόν μήνυμα σας ενημερώνουμε ότι ο τελευταίος χρήστης με ρόλο διαχειριστή στον οργανισμό σας έχει προγραμματιστεί να απενεργοποιηθεί λόγω καθορισμένης ημερομηνίας λήξης του λογαριασμού.</p>
    <p><strong>Απαιτούμενη ενέργεια</strong></p>
    <p>Παρακαλούμε ορίστε έναν νέο διαχειριστή ή τροποποιήστε την ημερομηνία απενεργοποίησης για τον συγκεκριμένο χρήστη.</p>
    <p>Μεταβείτε στην εφαρμογή εδώ: {0}</p>
    <br />
    <p>Hi</p>
    <p>This message is to inform you that the last user assigned with the Admin role is scheduled for deactivation based on an existing schedule.</p>
    <p><strong>Required Action</strong></p>
    <p>Please assign another admin or change the deactivation schedule of the specific user.</p>
    <p>Access the application here: {0}</p>
    </body>
    </html>
    """;
}
