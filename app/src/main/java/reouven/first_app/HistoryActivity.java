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

public class HistoryActivity extends AppCompatActivity implements HistoryAdapter.OnPlanClickListener {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> planList;
    private FirebaseFirestore db;
    private View mainLayout;
    private TextView tvTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBar();
        setupBottomNavigation();
        applyCustomColorMode();
        loadHistoryFromFirebase();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvTitle = findViewById(R.id.tvTitle);
        rvHistory = findViewById(R.id.rvHistory);

        planList = new ArrayList<>();
        adapter = new HistoryAdapter(planList, this);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
        db = FirebaseFirestore.getInstance();
    }

    private void loadHistoryFromFirebase() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("saved_plans")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        planList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> data = doc.getData();
                            data.put("docId", doc.getId());
                            planList.add(data);
                        }
                        planList.sort((a, b) -> Long.compare(getLong(b.get("timestamp")), getLong(a.get("timestamp"))));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onPlanClick(Map<String, Object> plan) {
        String type = (String) plan.getOrDefault("type", "investment");
        Intent intent;
        if ("mortgage".equals(type)) {
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

    @Override
    public void onDeleteClick(Map<String, Object> plan, int position) {
        String docId = (String) plan.get("docId");
        new AlertDialog.Builder(this)
                .setTitle("מחיקה")
                .setMessage("למחוק את החישוב?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    db.collection("saved_plans").document(docId).delete();
                }).setNegativeButton("ביטול", null).show();
    }

    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuHeader).setOnClickListener(this::showPopupMenu);

        // כפתור המידע החדש של דף ההיסטוריה
        View btnInfo = findViewById(R.id.btnHelpInfoHistory);
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> showHistoryInfoDialog());
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.menu_contact) {
                showContactDialog();
            } else if (id == R.id.menu_about) {
                showAboutDialog();
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
            }
            return true;
        });
        popup.show();
    }

    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("יצירת קשר")
                .setMessage("צריכים עזרה או יש לכם הצעה לשיפור? אנחנו כאן בשבילכם.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc - היסטוריה");
                    try {
                        startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית, החזרי משכנתא ותחזיות בצורה הכי מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";

        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage(aboutMessage)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showHistoryInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("היסטוריית תוכניות")
                .setMessage("כאן תוכל לראות את כל החישובים ששמרת בעבר.\n\nלחיצה על כרטיס תפתח את פרטי החישוב המלאים, ולחיצה על האייקון של הפח תמחק אותו לצמיתות.")
                .setPositiveButton("הבנתי", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("להתנתק מהחשבון?").setPositiveButton("כן", (d, w) -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }).setNegativeButton("לא", null).show();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvTitle.setTextColor(Color.WHITE);
        }
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

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

    private double getDouble(Object o) { return (o instanceof Number) ? ((Number) o).doubleValue() : 0.0; }
    private int getInt(Object o) { return (o instanceof Number) ? ((Number) o).intValue() : 0; }
    private long getLong(Object o) { return (o instanceof Number) ? ((Number) o).longValue() : 0L; }
}