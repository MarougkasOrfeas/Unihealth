package gr.uniwa.unihealth.backend.utils.email;

public class EmailNotificationTextConstants {

  public static final String USER_CREATED_SUBJECT_EN =
      "Login Credentials for the Unihealth Application";
  public static final String USER_CREATED_SUBJECT_GR =
      "Στοιχεία σύνδεσης για την εφαρμογή Unihealth";

  public static final String EMAIL_CHANGED_SUBJECT_EN = "Previous email address has changed";
  public static final String EMAIL_CHANGED_SUBJECT_GR = "Η προηγούμενη διεύθυνση email άλλαξε";

  public static final String IMMINENT_DEACTIVATION_SUBJECT_EN =
      "Your account will be deactivated soon";
  public static final String IMMINENT_DEACTIVATION_SUBJECT_GR =
      "Ο λογαριασμός σας θα απενεργοποιηθεί σύντομα";

  public static final String DATED_ADMIN_ACCOUNT_SUBJECT =
      "Σημαντικό μήνυμα/Important Notice: Απενεργοποίηση του τελευταίου λογαριασμού διαχειριστή λόγω προγραμματισμένης ημερομηνίας λήξης/Last Admin Account Pending Deactivation Due to Schedule";

  public static final String USER_CREATED_BODY_EN = """
        <html>
        <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
        <p>Dear User Administrator,</p>
        <p>A user account has been created in the UNIHEALTH application for the newly added user.</p>
        <p>Please forward the following login information to the new user:</p>
        <p>Username: {0}</p>
        <p>The user is asked to open the application (<a href="{1}">{1}</a>) and follow the “Forgot password” process in order to verify their account. Once the verification has been completed, they can access the application via the following link: <a href="{1}">{1}</a></p>
        <p>If you have any questions or encounter any problems, please contact the appropriate department.</p>
        </body>
        </html>
      """;

  public static final String USER_CREATED_BODY_GR = """
        <html>
        <body style="font-family: 'Times New Roman', Times, serif; font-size: 16px; line-height: 1.6;">
        <p>Αγαπητέ Διαχειριστή Χρηστών,</p>
        <p>Δημιουργήθηκε λογαριασμός χρήστη στην εφαρμογή UNIHEALTH για τον νέο χρήστη που προστέθηκε.</p>
        <p>Παρακαλούμε προωθήστε τα ακόλουθα στοιχεία σύνδεσης στον νέο χρήστη:</p>
        <p>Όνομα χρήστη: {0}</p>
        <p>Ο χρήστης καλείται να ανοίξει την εφαρμογή (<a href="{1}">{1}</a>) και να ακολουθήσει τη διαδικασία «Ξέχασα τον κωδικό πρόσβασης» για να επαληθεύσει τον λογαριασμό του. Μόλις ολοκληρωθεί η επαλήθευση, θα μπορεί να αποκτήσει πρόσβαση στην εφαρμογή μέσω του ακόλουθου συνδέσμου: <a href="{1}">{1}</a></p>
        <p>Εάν έχετε ερωτήσεις ή αντιμετωπίσετε προβλήματα, παρακαλούμε επικοινωνήστε με το αρμόδιο τμήμα.</p>
        </body>
        </html>
      """;

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

  public static final String NEWSLETTER_UNSUBSCRIBED_SUBJECT_EN =
      "You have unsubscribed from the UNIHEALTH health news";
  public static final String NEWSLETTER_UNSUBSCRIBED_SUBJECT_GR =
      "Καταργήσατε την εγγραφή σας στα νέα υγείας του UNIHEALTH";

  public static final String OPTIONAL_FORM_REMINDER_SUBJECT_EN =
      "Complete your health profile to get better suggestions";
  public static final String OPTIONAL_FORM_REMINDER_SUBJECT_GR =
      "Συμπληρώστε το προφίλ υγείας σας για καλύτερες προτάσεις";

  public static final String NEWS_DIGEST_SUBJECT_EN = "Your UNIHEALTH health news";
  public static final String NEWS_DIGEST_SUBJECT_GR = "Τα νέα υγείας σας από το UNIHEALTH";

  /*
   * No apostrophes or single quotes in the bodies below: MessageFormat treats ' as a quoting
   * character, strips it, and stops substituting inside a quoted run. The existing templates get
   * away with 'Times New Roman' only because the quotes happen to balance.
   */

  public static final String NEWSLETTER_UNSUBSCRIBED_BODY_EN = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Hi,</p>
      <p>We are sorry to see you go. You will no longer receive health news emails from UNIHEALTH.</p>
      <p>Important account emails, such as a change of email address or a warning before your
         account is deactivated, will still reach you.</p>
      <p>If you change your mind, you can subscribe again at any time: open the application, go to
         <strong>Profile &gt; Preferences</strong> and switch off
         <strong>Unsubscribe from News Feeds</strong>.</p>
      <p>Access the application here: {0}</p>
      </body>
      </html>
      """;

  public static final String NEWSLETTER_UNSUBSCRIBED_BODY_GR = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Γεια σας,</p>
      <p>Λυπούμαστε που φεύγετε. Δεν θα λαμβάνετε πλέον email με νέα υγείας από το UNIHEALTH.</p>
      <p>Τα σημαντικά email του λογαριασμού σας, όπως η αλλαγή διεύθυνσης email ή η προειδοποίηση
         πριν την απενεργοποίηση του λογαριασμού, θα συνεχίσουν να σας φτάνουν.</p>
      <p>Αν αλλάξετε γνώμη, μπορείτε να εγγραφείτε ξανά όποτε θέλετε: ανοίξτε την εφαρμογή, πηγαίνετε
         στο <strong>Προφίλ &gt; Προτιμήσεις</strong> και απενεργοποιήστε την επιλογή
         <strong>Κατάργηση εγγραφής από τα Νέα Υγείας</strong>.</p>
      <p>Μπείτε στην εφαρμογή εδώ: {0}</p>
      </body>
      </html>
      """;

  public static final String OPTIONAL_FORM_REMINDER_BODY_EN = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Hi,</p>
      <p>Your optional health profile is still mostly empty. It takes a couple of minutes and it is
         what lets UNIHEALTH tailor its health content to you.</p>
      <p>You can fill it in here: {0}</p>
      <p>You will get at most one more reminder about this. To stop these emails entirely, go to
         <strong>Profile &gt; Preferences</strong> and switch off
         <strong>Enable Notifications</strong>.</p>
      </body>
      </html>
      """;

  public static final String OPTIONAL_FORM_REMINDER_BODY_GR = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Γεια σας,</p>
      <p>Το προαιρετικό προφίλ υγείας σας είναι ακόμη σχεδόν κενό. Χρειάζεται μόνο δύο λεπτά και
         είναι αυτό που επιτρέπει στο UNIHEALTH να προσαρμόσει το περιεχόμενο υγείας σε εσάς.</p>
      <p>Μπορείτε να το συμπληρώσετε εδώ: {0}</p>
      <p>Θα λάβετε το πολύ μία ακόμη υπενθύμιση. Για να σταματήσετε εντελώς αυτά τα email, πηγαίνετε
         στο <strong>Προφίλ &gt; Προτιμήσεις</strong> και απενεργοποιήστε την επιλογή
         <strong>Ενεργοποίηση Ειδοποιήσεων</strong>.</p>
      </body>
      </html>
      """;

  public static final String NEWS_DIGEST_BODY_EN = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Hi,</p>
      <p>Here is a selection of recent health news.</p>
      {0}
      <p>Read more in the application: {1}</p>
      <p>To stop receiving these, go to <strong>Profile &gt; Preferences</strong> and switch on
         <strong>Unsubscribe from News Feeds</strong>.</p>
      </body>
      </html>
      """;

  public static final String NEWS_DIGEST_BODY_GR = """
      <html>
      <body style="font-family: Times New Roman, Times, serif; font-size: 16px; line-height: 1.6;">
      <p>Γεια σας,</p>
      <p>Ακολουθεί μια επιλογή από πρόσφατα νέα υγείας.</p>
      {0}
      <p>Διαβάστε περισσότερα στην εφαρμογή: {1}</p>
      <p>Για να σταματήσετε να τα λαμβάνετε, πηγαίνετε στο <strong>Προφίλ &gt; Προτιμήσεις</strong>
         και ενεργοποιήστε την επιλογή <strong>Κατάργηση εγγραφής από τα Νέα Υγείας</strong>.</p>
      </body>
      </html>
      """;

}
