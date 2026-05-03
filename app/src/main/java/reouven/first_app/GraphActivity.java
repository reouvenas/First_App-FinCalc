package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

/**
 * מחלקה: GraphActivity
 * תפקיד: הצגת גרף ויזואלי של תחזית צמיחת ההון לאורך תקופת ההשקעה.
 * המחלקה מחשבת את הנתונים חודש בחודש ומציגה אותם בנקודות זמן שנתיות על גבי הגרף.
 */
public class GraphActivity extends AppCompatActivity {

    // רכיבי הממשק (UI)
    private LineChart lineChart;    // אובייקט הגרף מסוג LineChart
    private View mainLayout;       // ה-Layout הראשי להתאמת צבעים

    // משתני הנתונים הפיננסיים שהתקבלו מה-Activity הקודם
    private double initial, monthly, rate, fees;
    private int years, extraMonths;
    private boolean isDarkMode;    // בדיקה האם מצב כהה פעיל

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * פעולת onCreate: שליפת הגדרות, קבלת נתונים מה-Intent ואתחול הגרף.
         */
        // טעינת הגדרת מצב כהה מה-SharedPreferences לפני טעינת ה-View
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // הסתרת סרגל הפעולות המובנה
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mainLayout = findViewById(R.id.main_layout);
        lineChart = findViewById(R.id.lineChart);

        // שליפת הנתונים שנשלחו ב-Bundle
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            initial = extras.getDouble("initial", 0);
            monthly = extras.getDouble("monthly", 0);
            rate = extras.getDouble("rate", 0);
            fees = extras.getDouble("fees", 0);
            years = extras.getInt("years", 0);
            extraMonths = extras.getInt("months", 0);
        }

        // הפעלת פונקציות העזר לבניית המסך
        setupTopBar();
        setupBottomNavigation();
        applyCustomColorMode();
        setupGraph(); // יצירת והצגת הנתונים בגרף
    }

    /**
     * פעולה: setupTopBar
     * תפקיד: ניהול כפתור החזור ותפריט האפשרויות העליון.
     */
    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuHeader).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                } else if (id == R.id.menu_about) {
                    showAboutDialog();
                } else if (id == R.id.menu_contact) {
                    showContactDialog();
                } else if (id == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
                return true;
            });
            popup.show();
        });
    }

    /**
     * פעולה: showContactDialog
     * תפקיד: יצירת קשר עם המפתחים דרך אימייל.
     */
    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("יצירת קשר")
                .setMessage("צריכים עזרה או יש לכם הצעה לשיפור? אנחנו כאן בשבילכם.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc (דף הגרף)");
                    try {
                        startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null).show();
    }

    /**
     * פעולה: showAboutDialog
     * תפקיד: הצגת פרטי האפליקציה בתיבת דיאלוג.
     */
    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית ותחזיות בצורה מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";

        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage(aboutMessage)
                .setPositiveButton("סגור", null)
                .show();
    }

    /**
     * פעולה: setupGraph
     * תפקיד: הלוגיקה המתמטית של הגרף. חישוב ריבית דריבית מצטברת לאורך השנים וציור הקו.
     */
    private void setupGraph() {
        List<Entry> entries = new ArrayList<>(); // רשימת הנקודות בגרף (X, Y)
        int totalMonths = (years * 12) + extraMonths;
        double monthlyRate = ((rate - fees) / 100) / 12; // ריבית חודשית נטו
        double currentBalance = initial;

        // הוספת נקודת ההתחלה (זמן 0)
        entries.add(new Entry(0, (float) currentBalance));

        // חישוב היתרה בסוף כל חודש
        for (int i = 1; i <= totalMonths; i++) {
            currentBalance = currentBalance * (1 + monthlyRate) + monthly;

            // הוספת נקודה לגרף רק בכל סוף שנה (או בסוף התקופה) כדי לא להעמיס ויזואלית
            if (i % 12 == 0 || i == totalMonths) {
                entries.add(new Entry(i / 12f, (float) currentBalance));
            }
        }

        // הגדרת סדרת הנתונים (הקו של הגרף)
        LineDataSet dataSet = new LineDataSet(entries, "צמיחת הון");
        int mainColor = isDarkMode ? Color.CYAN : Color.parseColor("#1A237E");

        // התאמת צבעי הטקסט והצירים למצב כהה במידת הצורך
        if (isDarkMode) {
            dataSet.setValueTextColor(Color.WHITE);
            lineChart.getAxisLeft().setTextColor(Color.WHITE);
            lineChart.getAxisRight().setTextColor(Color.WHITE);
            lineChart.getXAxis().setTextColor(Color.WHITE);
            lineChart.getLegend().setTextColor(Color.WHITE);
            lineChart.getDescription().setTextColor(Color.WHITE);
        }

        // עיצוב הקו והנקודות
        dataSet.setColor(mainColor);
        dataSet.setCircleColor(mainColor);
        dataSet.setLineWidth(3f);        // עובי הקו
        dataSet.setCircleRadius(5f);     // גודל הנקודות
        dataSet.setDrawValues(false);    // הסתרת הערכים המספריים מעל כל נקודה למראה נקי

        // הזנת הנתונים לגרף ורענון התצוגה
        lineChart.setData(new LineData(dataSet));
        lineChart.getDescription().setText("שנים");
        lineChart.invalidate(); // פקודה המרעננת את הגרף ומציירת אותו מחדש
    }

    /**
     * פעולה: applyCustomColorMode
     * תפקיד: שינוי צבע הרקע של ה-Activity והגרף בהתאם להעדפות המשתמש.
     */
    private void applyCustomColorMode() {
        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            lineChart.setBackgroundColor(Color.BLACK);
            View toolbar = findViewById(R.id.toolbar_graph);
            if (toolbar != null) toolbar.setBackgroundColor(Color.parseColor("#121212"));
        }
    }

    /**
     * פעולה: setupBottomNavigation
     * תפקיד: ניהול הניווט בתפריט התחתון.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // הגרף הוא חלק מהמחשבון, לכן נסמן את ה-Home כפעיל
            bottomNav.setSelectedItemId(R.id.nav_home);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    finish(); // חזרה למחשבון
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_tips) {
                    startActivity(new Intent(this, TipsActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    startActivity(new Intent(this, ChatActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                return false;
            });
        }
    }
}