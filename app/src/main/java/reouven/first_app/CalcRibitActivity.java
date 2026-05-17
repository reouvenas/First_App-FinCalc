package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * מחלקה מרכזית: CalcRibitActivity
 * תפקיד: ניהול מחשבון השקעות חכם הכולל חישובי ריבית דריבית, המרת מטבעות ותצוגה מותאמת אישית.
 */
public class CalcRibitActivity extends AppCompatActivity {

    // הגדרת משתנים לרכיבי הממשק (UI) - שדות קלט, טקסט וכפתורים
    private EditText etInitial, etMonthly, etRate, etYears, etMonths, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails, btnConvert;
    private LinearLayout resultArea;
    private View mainLayout;
    private FirebaseAuth mAuth;
    private boolean isDarkMode;

    // משתנים לניהול לוגיקה פיננסית ושערי חליפין
    private String currencySymbol = "₪";
    private double lastCalculatedValue = 0;
    private double USD_TO_ILS = 3.65; // ברירת מחדל שתתעדכן מה-API
    private double EUR_TO_ILS = 3.95; // ברירת מחדל שתתעדכן מה-API

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // טעינת הגדרות מצב לילה לפני שהמסך עולה
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();

        // הסתרת שורת הכותרת המובנית של אנדרואיד
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();            // חיבור הרכיבים מה-XML לקוד הג'אווה
        setupNavigation();      // הגדרת כפתורי התפריט העליון והתחתון
        applyCustomColorMode();   // החלת צבעים מותאמים (Dark/Light)
        fetchLiveRates();       // עדכון שערי מטבע מהאינטרנט

        /**
         * עדכון שלב בונוס: קליטת נתונים לעריכה מדף הפירוט
         * תפקיד: אם המשתמש הגיע למסך זה בלחיצה על כפתור "עריכה" מדף הפירוט (DetailsActivity),
         * הנתונים הישנים יישלפו מה-Intent ויוזנו אוטומטית לשדות הקלט כדי לחסוך הקלדה מחדש.
         */
        Intent intent = getIntent();
        if (intent.hasExtra("initial")) {
            etInitial.setText(String.valueOf(intent.getDoubleExtra("initial", 0)));
            etMonthly.setText(String.valueOf(intent.getDoubleExtra("monthly", 0)));
            etRate.setText(String.valueOf(intent.getDoubleExtra("rate", 0)));
            etYears.setText(String.valueOf(intent.getIntExtra("years", 0)));
            etMonths.setText(String.valueOf(intent.getIntExtra("months", 0)));
            etFees.setText(String.valueOf(intent.getDoubleExtra("fees", 0)));

            // הרצת החישוב באופן אוטומטי כדי להציג את התוצאה מיד עם טעינת הנתונים לעריכה
            calculateInvestment();
        }
    }

    /**
     * פונקציה: initViews
     * תפקיד: מציאת רכיבי ה-UI והגדרת לוגיקה בסיסית לכפתורים.
     */
    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        etInitial = findViewById(R.id.etInitial);
        etMonthly = findViewById(R.id.etMonthly);
        etRate = findViewById(R.id.etRate);
        etYears = findViewById(R.id.etYears);
        etMonths = findViewById(R.id.etMonths);
        etFees = findViewById(R.id.etFees);
        tvResult = findViewById(R.id.tvResult);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDetails = findViewById(R.id.btnDetails);
        btnConvert = findViewById(R.id.btnConvert);
        resultArea = findViewById(R.id.resultArea);

        // כפתור הפירוט יהיה כבוי (חצי שקוף) עד שהמשתמש יבצע חישוב
        btnDetails.setEnabled(false);
        btnDetails.setAlpha(0.5f);

        // כפתור עזרה ליד שדה דמי הניהול
        ImageView btnInfoFees = findViewById(R.id.btnInfoFees);
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("מה זה דמי ניהול?")
                    .setMessage("אחוז מהרווח הנלקח לטובת הגוף המנהל(ברוקר,בנק). בחישוב שלנו הוא מהרווח הכולל.")
                    .setPositiveButton("הבנתי", null).show());
        }

        // שינוי סמל המטבע בלחיצה מהירה (שקל -> דולר -> אירו)
        ImageView btnCurrency = findViewById(R.id.btnCurrency);
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> {
                if (currencySymbol.equals("₪")) currencySymbol = "$";
                else if (currencySymbol.equals("$")) currencySymbol = "€";
                else currencySymbol = "₪";
                tvCurrencySymbol.setText(currencySymbol);
            });
        }

        // הגדרת פעולת החישוב
        btnCalculate.setOnClickListener(v -> calculateInvestment());

        // הצגת דיאלוג המרת מטבעות
        if (btnConvert != null) {
            btnConvert.setOnClickListener(v -> showConversionDialog());
        }

        // מעבר למסך הפירוט (גרפים וטבלאות) עם שליחת הנתונים
        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", parseDouble(etInitial));
            intent.putExtra("monthly", parseDouble(etMonthly));
            intent.putExtra("rate", parseDouble(etRate));
            intent.putExtra("years", (int) parseDouble(etYears));
            intent.putExtra("months", (int) parseDouble(etMonths));
            intent.putExtra("fees", parseDouble(etFees));
            intent.putExtra("currency", currencySymbol);
            startActivity(intent);
        });
    }

    /**
     * פונקציה: setupNavigation
     * תפקיד: הגדרת התפריט העליון (מדרג) והתפריט התחתון.
     * עדכון ארכיטקטוני: קריאה ישירה ומרוכזת ל-NavigationHelper עבור דיאלוגי "אודות" ו"יצירת קשר".
     */
    private void setupNavigation() {
        // כפתור חזור בראש המסך
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // כפתור תפריט (שלוש נקודות)
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_profile) {
                        if (isUserGuest()) showGuestRestrictionDialog("הפרופיל זמין לרשומים בלבד.");
                        else startActivity(new Intent(this, ProfileActivity.class));
                        return true;
                    } else if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                        return true;
                    } else if (id == R.id.menu_about) {
                        NavigationHelper.showAboutDialog(this); // שימוש ב-NavigationHelper המרכזי
                        return true;
                    } else if (id == R.id.menu_contact) {
                        NavigationHelper.showContactDialog(this); // שימוש ב-NavigationHelper המרכזי
                        return true;
                    } else if (id == R.id.menu_logout) {
                        mAuth.signOut();
                        startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }

        // ניווט תחתון
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                else if (id == R.id.nav_history) {
                    if (isUserGuest()) {
                        showGuestRestrictionDialog("ההיסטוריה זמינה למשתמשים רשומים בלבד.");
                        return false;
                    }
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    startActivity(new Intent(this, ChatActivity.class));
                    return true;
                } else if (id == R.id.nav_tips) {
                    if (isUserGuest()) {
                        showGuestRestrictionDialog("הטיפים זמינים למשתמשים רשומים בלבד.");
                        return false;
                    }
                    startActivity(new Intent(this, TipsActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * פונקציה: calculateInvestment
     * תפקיד: ביצוע חישוב ריבית דריבית מורכב המבוסס על קלטי המשתמש.
     */
    private void calculateInvestment() {
        try {
            double p = parseDouble(etInitial); // קרן ראשונית
            double m = parseDouble(etMonthly); // הפקדה חודשית
            // חישוב ריבית חודשית נטו
            double r = (parseDouble(etRate) - parseDouble(etFees)) / 100 / 12;
            int t = ((int) parseDouble(etYears) * 12) + (int) parseDouble(etMonths); // סה"כ חודשים

            if (t <= 0) {
                Toast.makeText(this, "נא להזין תקופת זמן", Toast.LENGTH_SHORT).show();
                return;
            }

            // נוסחת ריבית דריבית מצטברת
            if (r != 0) {
                lastCalculatedValue = p * Math.pow(1 + r, t) + m * (Math.pow(1 + r, t) - 1) / r;
            } else {
                lastCalculatedValue = p + (m * t);
            }

            // הצגת התוצאה
            tvResult.setText(currencySymbol + String.format(Locale.US, "%,.2f", lastCalculatedValue));
            resultArea.setVisibility(View.VISIBLE);

            // הפעלת כפתור הפירוט
            btnDetails.setEnabled(true);
            btnDetails.setAlpha(1.0f);
            btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));


        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בנתונים", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * פונקציה: savePlanSilently
     * תפקיד: שמירה ברקע של חישוב הריבית לתוך תת-האוסף הפנימי history של המשתמש ב-Firestore.
     */
    private void savePlanSilently(String planName) {
        String uid = mAuth.getUid();
        if (uid == null) return;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("planName", planName);
            data.put("type", "investment");
            data.put("timestamp", System.currentTimeMillis());
            data.put("initial", parseDouble(etInitial));
            data.put("monthly", parseDouble(etMonthly));
            data.put("rate", parseDouble(etRate));
            data.put("years", (int) parseDouble(etYears));
            data.put("months", (int) parseDouble(etMonths));
            data.put("fees", parseDouble(etFees));

            // שמירה היררכית: users -> UID -> history
            FirebaseFirestore.getInstance().collection("users").document(uid).collection("history").add(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * פונקציה מעודכנת: showConversionDialog
     * תפקיד: המרת התוצאה המחושבת לכל אחד משלושת המטבעות (₪, $, €) לכל הכיוונים בצורה דינמית ומלאה.
     */
    private void showConversionDialog() {
        String[] options = {"שקלים (₪)", "דולרים ($)", "אירו (€)"};
        new AlertDialog.Builder(this)
                .setTitle("המר תוצאה ל:")
                .setItems(options, (dialog, which) -> {
                    double converted = lastCalculatedValue;
                    String newSym = "";

                    // מקרה 1: החישוב המקורי התחיל בשקלים
                    if (currencySymbol.equals("₪")) {
                        if (which == 1) { converted /= USD_TO_ILS; newSym = "$"; }
                        else if (which == 2) { converted /= EUR_TO_ILS; newSym = "€"; }
                        else newSym = "₪";
                    }
                    // מקרה 2: החישוב המקורי התחיל בדולרים
                    else if (currencySymbol.equals("$")) {
                        if (which == 0) { converted *= USD_TO_ILS; newSym = "₪"; }
                        else if (which == 2) { converted = (converted * USD_TO_ILS) / EUR_TO_ILS; newSym = "€"; }
                        else newSym = "$";
                    }
                    // מקרה 3: החישוב המקורי התחיל באירו
                    else if (currencySymbol.equals("€")) {
                        if (which == 0) { converted *= EUR_TO_ILS; newSym = "₪"; }
                        else if (which == 1) { converted = (converted * EUR_TO_ILS) / USD_TO_ILS; newSym = "$"; }
                        else newSym = "€";
                    }

                    tvResult.setText(newSym + String.format(Locale.US, "%,.2f", converted));
                }).show();
    }

    // --- פונקציות עזר כלליות ---

    /**
     * פונקציה: isUserGuest
     * תפקיד: בודקת האם המשתמש הנוכחי מחובר כאורח אנונימי.
     */
    private boolean isUserGuest() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user == null || user.isAnonymous();
    }

    /**
     * פונקציה: showGuestRestrictionDialog
     * תפקיד: הצגת דיאלוג חסימה המונע מאורחים גישה לאזורים רשומים ומציע מעבר להרשמה.
     */
    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this).setTitle("פעולה חסומה").setMessage(message)
                .setPositiveButton("להרשמה", (d, w) -> startActivity(new Intent(this, RegisterActivity.class)))
                .setNegativeButton("ביטול", null).show();
    }

    /**
     * פונקציה: toggleDarkMode
     * תפקיד: שינוי הגדרת השמירה של מצב התצוגה (Light/Dark) ורענון הדף.
     */
    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !isDarkMode).apply();
        recreate();
    }

    /**
     * פונקציה: parseDouble
     * תפקיד: המרת הטקסט משדות הקלט לערך מסוג double בצורה בטוחה.
     */
    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    /**
     * פונקציה: applyCustomColorMode
     * תפקיד: צביעת שדות הקלט והרכיבים למניעת באגים ויזואליים של טקסט שחור על רקע שחור במצב כהה.
     */
    private void applyCustomColorMode() {
        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvCurrencySymbol.setTextColor(Color.WHITE);
            tvResult.setTextColor(Color.WHITE);

            int white = Color.WHITE;
            int gray = Color.GRAY;

            etInitial.setTextColor(white); etInitial.setHintTextColor(gray);
            etMonthly.setTextColor(white); etMonthly.setHintTextColor(gray);
            etRate.setTextColor(white); etRate.setHintTextColor(gray);
            etYears.setTextColor(white); etYears.setHintTextColor(gray);
            etMonths.setTextColor(white); etMonths.setHintTextColor(gray);
            etFees.setTextColor(white); etFees.setHintTextColor(gray);

            findViewById(R.id.bottom_navigation).setBackgroundColor(Color.parseColor("#121212"));
        }
    }

    /**
     * פונקציה: fetchLiveRates
     * תפקיד: פנייה אסינכרונית ל-API חיצוני למשיכת שערי חליפין מעודכנים עבור המרות מטבע בזמן אמת.
     */
    private void fetchLiveRates() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://open.er-api.com/v6/latest/ILS").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject rates = json.getJSONObject("rates");
                        USD_TO_ILS = 1 / rates.getDouble("USD");
                        EUR_TO_ILS = 1 / rates.getDouble("EUR");
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }
}