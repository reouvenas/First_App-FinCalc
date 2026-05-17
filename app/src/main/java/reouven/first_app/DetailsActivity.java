package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * מחלקה: DetailsActivity
 * תפקיד: הצגת פירוט מלא של תוצאות ההשקעה, כולל רווחים, סכום סופי ואפשרות לשמירה או ייצוא ל-PDF.
 */
public class DetailsActivity extends AppCompatActivity {

    // משתנים לאחסון נתוני החישוב הפיננסי
    private double initial, monthly, rate, fees, finalBalance, totalInvested, totalProfit;
    private int years, extraMonths;
    private String currencySymbol;

    // רכיבי מערכת ועיצוב
    private FirebaseAuth mAuth;
    private View mainLayout;
    private boolean isDarkMode;

    // משתנה עזר לזיהוי מקור הניווט (האם המשתמש הגיע למסך זה דרך דף ההיסטוריה)
    private boolean isFromHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * פעולת onCreate: טעינת הגדרות תצוגה, קבלת נתונים מהמסך הקודם והפעלת הממשק.
         */
        checkAndApplyDarkMode(); // החלת מצב כהה/בהיר
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mAuth = FirebaseAuth.getInstance();
        mainLayout = findViewById(R.id.main_layout);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // קבלת נתונים שנשלחו דרך ה-Intent מהמסך הקודם (CalcRibitActivity או HistoryActivity)
        Intent intent = getIntent();
        initial = intent.getDoubleExtra("initial", 0);
        monthly = intent.getDoubleExtra("monthly", 0);
        rate = intent.getDoubleExtra("rate", 0);
        years = intent.getIntExtra("years", 0);
        extraMonths = intent.getIntExtra("months", 0);
        fees = intent.getDoubleExtra("fees", 0);
        currencySymbol = intent.getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        // שלב 2: קליטת הפרמטר המזהה האם הגענו ממסך ההיסטוריה
        isFromHistory = intent.getBooleanExtra("isFromHistory", false);

        // ביצוע החישובים והצגתם על המסך
        calculateResults((years * 12) + extraMonths);
        displayData();

        // אתחול תפריטים וכפתורים
        setupTopBar();
        setupBottomNavigation();
        setupActionButtons();
        applyCustomColorMode();
    }

    /**
     * פעולה מעודכנת: setupActionButtons
     * תפקיד: הגדרת מאזיני לחיצה לכפתורי הפעולה (גרף, עריכה, שמירה ושיתוף).
     * עדכון זרימת ניווט: כפתור העריכה בודק כעת את מקור ההגעה ומנתב את המשתמש בצורה נכונה חזרה לעריכה.
     */
    private void setupActionButtons() {
        // מעבר למסך הגרף להצגה ויזואלית של הנתונים
        findViewById(R.id.btnViewChart).setOnClickListener(v -> {
            Intent gIntent = new Intent(this, GraphActivity.class);
            gIntent.putExtra("initial", initial);
            gIntent.putExtra("monthly", monthly);
            gIntent.putExtra("rate", rate);
            gIntent.putExtra("years", years);
            gIntent.putExtra("months", extraMonths);
            gIntent.putExtra("fees", fees);
            gIntent.putExtra("currency", currencySymbol);
            startActivity(gIntent);
        });

        // לוגיקת כפתור העריכה המתוקנת (פתרון באג החזרה להיסטוריה)
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            if (isFromHistory) {
                // אם המשתמש הגיע מההיסטוריה, נפתח לו מפורשות אקטיביטי חדש של המחשבון
                Intent calcIntent = new Intent(this, CalcRibitActivity.class);

                // העברת הנתונים הקיימים חזרה למחשבון כדי שיטענו ישירות בתוך שדות הקלט לעריכה
                calcIntent.putExtra("initial", initial);
                calcIntent.putExtra("monthly", monthly);
                calcIntent.putExtra("rate", rate);
                calcIntent.putExtra("years", years);
                calcIntent.putExtra("months", extraMonths);
                calcIntent.putExtra("fees", fees);

                startActivity(calcIntent);
                finish(); // סגירת דף הפירוט הנוכחי
            } else {
                // אם המשתמש הגיע ישירות לאחר חישוב במחשבון, פשוט נסגור את הדף ונחזור לאותו אקטיביטי חי בזיכרון
                finish();
            }
        });

        // לחיצה על שמירה - מפעילה בדיקה מול Firebase
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> handleSaveRequest());

        // הפקת PDF ושיתוף
        findViewById(R.id.btnShare).setOnClickListener(v -> exportToPDF());
    }

    /**
     * פעולה: handleSaveRequest
     * תפקיד: בדיקה האם המשתמש מחובר (לא אורח) לפני אישור שמירת הנתונים לענן.
     */
    private void handleSaveRequest() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            showGuestRestrictionDialog("שמירת תוכניות השקעה זמינה למשתמשים רשומים בלבד.");
        } else {
            saveToFirebaseWithDialog();
        }
    }

    /**
     * פעולה: showGuestRestrictionDialog
     * תפקיד: הצגת הודעה לאורח המציעה לו להירשם כדי לפתוח אפשרויות שמירה.
     */
    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("פעולה חסומה")
                .setMessage(message + "\nרוצה להירשם עכשיו כדי לשמור את התוכניות שלך?")
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * פעולה מעודכנת: saveToFirebaseWithDialog
     * תפקיד: פתיחת תיבת קלט לקבלת שם לתוכנית ושמירת כל הנתונים ל-Cloud Firestore.
     * עדכון ארכיטקטורה: הנתונים נשמרים במבנה היררכי פנימי מבוסס UID: users -> [UID] -> history.
     */
    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("למשל: חיסכון לדירה");

        new AlertDialog.Builder(this)
                .setTitle("תן שם לתוכנית")
                .setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if(name.isEmpty()) name = "תוכנית ריבית דריבית";

                    String uid = mAuth.getUid();
                    if (uid == null) return;

                    // יצירת אובייקט המידע לשמירה
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", uid);
                    data.put("planName", name);
                    data.put("type", "compound_interest");
                    data.put("finalBalance", finalBalance);
                    data.put("initial", initial);
                    data.put("monthly", monthly);
                    data.put("rate", rate);
                    data.put("years", years);
                    data.put("months", extraMonths);
                    data.put("fees", fees); // שמירת דמי הניהול כחלק בלתי נפרד מההיסטוריה
                    data.put("timestamp", System.currentTimeMillis());

                    // שמירה מאובטחת בתוך תת-האוסף הפנימי history של המשתמש
                    FirebaseFirestore.getInstance().collection("users").document(uid).collection("history").add(data)
                            .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר בהיסטוריה!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null).show();
    }

    /**
     * פעולה מעודכנת: setupTopBar
     * תפקיד: הגדרת סרגל הכלים העליון, כפתור חזור ותפריט ה-Popup.
     * שינוי ארכיטקטוני: הדיאלוגים של "אודות" ו"יצירת קשר" נקראים כעת בצורה נקייה ויעילה דרך ה-NavigationHelper.
     */
    private void setupTopBar() {
        View topBar = findViewById(R.id.included_top_bar);
        if (topBar != null) {
            topBar.findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
            topBar.findViewById(R.id.btnMenuHeader).setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    FirebaseUser user = mAuth.getCurrentUser();

                    if (id == R.id.menu_dark_mode) { toggleDarkMode(); }
                    else if (id == R.id.menu_profile) {
                        if (user == null || user.isAnonymous()) showGuestRestrictionDialog("פרופיל זמין לרשומים בלבד.");
                        else startActivity(new Intent(this, ProfileActivity.class));
                    }
                    else if (id == R.id.menu_contact) { NavigationHelper.showContactDialog(this); } // שימוש ב-Helper
                    else if (id == R.id.menu_about) { NavigationHelper.showAboutDialog(this); }     // שימוש ב-Helper
                    else if (id == R.id.menu_logout) { showLogoutDialog(); }
                    return true;
                });
                popup.show();
            });
        }
    }

    /**
     * פעולה: calculateResults
     * תפקיד: חישוב מתמטי של הריבית דריבית, סך ההשקעה והרווח הנקי.
     */
    private void calculateResults(int totalMonths) {
        double r = ((rate - fees) / 100) / 12; // ריבית חודשית נטו
        if (r != 0) finalBalance = initial * Math.pow(1 + r, totalMonths) + monthly * (Math.pow(1 + r, totalMonths) - 1) / r;
        else finalBalance = initial + (monthly * totalMonths);

        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;
    }

    /**
     * פעולה: displayData
     * תפקיד: הצגת הנתונים המחושבים בתוך רכיבי ה-TextView במסך.
     */
    private void displayData() {
        String f = "%,.0f"; // פורמט להצגת מספרים עם פסיקים
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, f, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, f, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " ש' ו-" + extraMonths + " ח'");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, f, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח צפוי: " + currencySymbol + String.format(Locale.US, f, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, f, finalBalance));
    }

    /**
     * פעולה: exportToPDF
     * תפקיד: יצירת קובץ PDF עם סיכום הנתונים ושמירתו בזיכרון המכשיר.
     */
    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Paint paint = new Paint();
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        page.getCanvas().drawText("סיכום השקעה - InvestCalc", 40, 50, paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(12);
        int y = 100;
        page.getCanvas().drawText("סכום התחלתי: " + currencySymbol + String.format("%.0f", initial), 40, y, paint);
        y += 30;
        page.getCanvas().drawText("הפקדה חודשית: " + currencySymbol + String.format("%.0f", monthly), 40, y, paint);
        y += 30;
        page.getCanvas().drawText("סה\"כ ברוטו צפוי: " + currencySymbol + String.format("%.0f", finalBalance), 40, y, paint);

        document.finishPage(page);
        File file = new File(getExternalFilesDir(null), "InvestmentSummary.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            shareFile(file); // שיתוף הקובץ לאחר היצירה
        } catch (IOException e) { e.printStackTrace(); }
    }

    /**
     * פעולה: shareFile
     * תפקיד: פתיחת ממשק השיתוף של אנדרואיד לשליחת קובץ ה-PDF.
     */
    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "שתף סיכום כקובץ PDF"));
    }

    /**
     * פעולה: showLogoutDialog
     * תפקיד: ניתוק המשתמש מהחשבון וחזרה למסך ההתחברות.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם ברצונך להתנתק?")
                .setPositiveButton("כן", (dialog, which) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    /**
     * פעולה: applyCustomColorMode
     * תפקיד: התאמת צבעי הכרטיסיות והטקסט למצב כהה.
     */
    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            findViewById(R.id.cardSummary).setBackgroundColor(Color.parseColor("#1E1E1E"));
            findViewById(R.id.cardFinal).setBackgroundColor(Color.parseColor("#1E1E1E"));
            ((TextView)findViewById(R.id.textViewTitle)).setTextColor(Color.WHITE);
        }
    }

    /**
     * פעולה: toggleDarkMode
     * תפקיד: החלפת מצב התצוגה ושמירתו.
     */
    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    /**
     * פעולה: checkAndApplyDarkMode
     * תפקיד: החלת הגדרת מצב הלילה של המערכת.
     */
    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * פעולה: setupBottomNavigation
     * תפקיד: הגדרת לוגיקת המעבר בתפריט התחתון.
     */
    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                FirebaseUser user = mAuth.getCurrentUser();
                if (id == R.id.nav_home) { finish(); return true; }
                else if (id == R.id.nav_history) {
                    if (user == null || user.isAnonymous()) { showGuestRestrictionDialog("היסטוריה זמינה למשתמשים רשומים בלבד."); return false; }
                    startActivity(new Intent(this, HistoryActivity.class)); finish(); return true;
                }
                return false;
            });
        }
    }
}