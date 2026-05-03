package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * מחלקה: HistoryActivity
 * תפקיד: הצגת רשימת החישובים (תוכניות השקעה ומשכנתאות) שהמשתמש שמר בענן.
 * המחלקה מאזינה לשינויים ב-Firestore ומעדכנת את הרשימה באופן דינמי.
 */
public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnPlanClickListener {

    // רכיבי ממשק המשתמש (UI)
    private RecyclerView rvHistory;      // רכיב להצגת רשימה ארוכה ונגללת
    private HistoryAdapter adapter;      // המתאם שמקשר בין הנתונים לתצוגה ברשימה
    private List<Map<String, Object>> planList; // רשימת הנתונים (התוכניות)
    private FirebaseFirestore db;        // אובייקט הגישה למסד הנתונים של Firebase
    private View mainLayout;             // הרקע הראשי של המסך
    private TextView tvTitle;            // כותרת הדף

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * פעולת onCreate: הגדרת מצב תצוגה, אתחול הרכיבים וטעינת נתונים מהענן.
         */
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();               // אתחול רכיבי ה-UI וה-RecyclerView
        setupTopBar();             // הגדרת סרגל הכלים העליון
        setupBottomNavigation();   // הגדרת תפריט הניווט התחתון
        applyCustomColorMode();    // התאמת צבעים אישית למצב כהה
        loadHistoryFromFirebase(); // משיכת הנתונים מהענן
    }

    /**
     * פעולה: initViews
     * תפקיד: חיבור משתני הג'אווה ל-XML והגדרת ה-Adapter עבור ה-RecyclerView.
     */
    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvTitle = findViewById(R.id.tvTitle);
        rvHistory = findViewById(R.id.rvHistory);

        planList = new ArrayList<>();
        // יצירת האדפטר והגדרת ה-Activity הנוכחי כמאזין ללחיצות (this)
        adapter = new HistoryAdapter(planList, this);

        // הגדרת ניהול פריסה ליניארי (אחד מתחת לשני)
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
    }

    /**
     * פעולה: loadHistoryFromFirebase
     * תפקיד: משיכת כל המסמכים השייכים למשתמש הנוכחי מאוסף ה-"saved_plans".
     * משתמש ב-addSnapshotListener כדי לעדכן את המסך אוטומטית בכל שינוי בענן.
     */
    private void loadHistoryFromFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("saved_plans")
                .whereEqualTo("userId", uid) // סינון: רק תוכניות של המשתמש המחובר
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        planList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> data = doc.getData();
                            data.put("docId", doc.getId()); // שמירת מזהה המסמך לצורך מחיקה עתידית
                            planList.add(data);
                        }
                        // מיון הרשימה לפי זמן (מהחדש לישן)
                        planList.sort((a, b) -> Long.compare(getLong(b.get("timestamp")), getLong(a.get("timestamp"))));
                        adapter.notifyDataSetChanged(); // עדכון ה-RecyclerView
                    }
                });
    }

    /**
     * פעולה: onPlanClick (מימוש ממשק ה-Adapter)
     * תפקיד: זיהוי סוג התוכנית (משכנתא או השקעה) והעברת המשתמש למסך הפירוט המתאים עם הנתונים.
     */
    @Override
    public void onPlanClick(Map<String, Object> plan) {
        String type = (String) plan.getOrDefault("type", "investment");
        Intent intent;

        if ("mortgage".equals(type)) {
            // אם זו משכנתא, נשלח את הנתונים ל-MortgageActivity
            intent = new Intent(this, MortgageActivity.class);
            intent.putExtra("isFromHistory", true);
            intent.putExtra("loanAmount", getDouble(plan.get("loanAmount")));
            intent.putExtra("interest", getDouble(plan.get("interest")));
            intent.putExtra("years", getInt(plan.get("years")));
            intent.putExtra("fullPrice", getDouble(plan.get("fullPrice")));
            intent.putExtra("propertySize", getDouble(plan.get("propertySize")));
            intent.putExtra("cityAvgPrice", getDouble(plan.get("cityAvgPrice")));
            intent.putExtra("city", (String) plan.get("city"));
        } else {
            // אם זו ריבית דריבית, נשלח ל-DetailsActivity
            intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", getDouble(plan.get("initial")));
            intent.putExtra("monthly", getDouble(plan.get("monthly")));
            intent.putExtra("rate", getDouble(plan.get("rate")));
            intent.putExtra("years", getInt(plan.get("years")));
            intent.putExtra("months", getInt(plan.get("months")));
            intent.putExtra("fees", getDouble(plan.get("fees")));
        }
        startActivity(intent);
    }

    /**
     * פעולה: onDeleteClick (מימוש ממשק ה-Adapter)
     * תפקיד: הצגת דיאלוג אישור לפני מחיקת המסמך מהענן.
     */
    @Override
    public void onDeleteClick(Map<String, Object> plan, int position) {
        String docId = (String) plan.get("docId");
        new AlertDialog.Builder(this)
                .setTitle("מחיקה")
                .setMessage("למחוק את החישוב מההיסטוריה?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    db.collection("saved_plans").document(docId).delete();
                }).setNegativeButton("ביטול", null).show();
    }

    /**
     * פעולה: setupTopBar
     * תפקיד: הגדרת כפתורי החזור, התפריט וכפתור המידע (Help).
     */
    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuHeader).setOnClickListener(this::showPopupMenu);

        View btnInfo = findViewById(R.id.btnHelpInfoHistory);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showHistoryInfoDialog());
        }
    }

    /**
     * תפריט Popup בראש המסך.
     */
    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) toggleDarkMode();
            else if (id == R.id.menu_profile) startActivity(new Intent(this, ProfileActivity.class));
            else if (id == R.id.menu_contact) showContactDialog();
            else if (id == R.id.menu_about) showAboutDialog();
            else if (id == R.id.menu_logout) showLogoutDialog();
            return true;
        });
        popup.show();
    }

    // --- דיאלוגים והודעות ---

    private void showContactDialog() {
        new AlertDialog.Builder(this).setTitle("יצירת קשר").setMessage("צריכים עזרה?")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    try { startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:")); } catch (Exception e) {}
                }).setNegativeButton("סגור", null).show();
    }

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
    private void showHistoryInfoDialog() {
        new AlertDialog.Builder(this).setTitle("היסטוריית תוכניות")
                .setMessage("כאן תוכל לראות את כל החישובים ששמרת.\n\nלחיצה על כרטיס תפתח את פרטי החישוב, ולחיצה על הפח תמחק אותו.")
                .setPositiveButton("הבנתי", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("להתנתק מהחשבון?")
                .setPositiveButton("כן", (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                }).setNegativeButton("לא", null).show();
    }

    // --- מצב כהה ועיצוב ---

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvTitle.setTextColor(Color.WHITE);
        }
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    /**
     * הגדרת הניוט התחתון (Bottom Navigation) למעבר בין דפי האפליקציה.
     */
    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_history);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) { startActivity(new Intent(this, HomeActivity.class)); finish(); return true; }
            if (id == R.id.nav_ai_chat) { startActivity(new Intent(this, ChatActivity.class)); finish(); return true; }
            if (id == R.id.nav_tips) { startActivity(new Intent(this, TipsActivity.class)); finish(); return true; }
            return id == R.id.nav_history;
        });
    }

    // --- פונקציות עזר להמרת טיפוסים (Casting) בטוחה מ-Firestore ---
    private double getDouble(Object o) { return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0; }
    private int getInt(Object o) { return (o instanceof Number) ? ((Number) o).intValue() : 0; }
    private long getLong(Object o) { return (o instanceof Number) ? ((Number) o).longValue() : 0L; }
}