package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינה: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        planList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> data = doc.getData();
                            data.put("docId", doc.getId());
                            planList.add(data);
                        }

                        // מיון לפי זמן - החדש ביותר למעלה
                        planList.sort((a, b) -> {
                            long t1 = getLong(a.get("timestamp"));
                            long t2 = getLong(b.get("timestamp"));
                            return Long.compare(t2, t1);
                        });

                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onPlanClick(Map<String, Object> plan) {
        String type = (String) plan.getOrDefault("type", "investment");

        if ("mortgage".equals(type)) {
            // מעבר למחשבון משכנתא עם כל הנתונים המורחבים
            Intent intent = new Intent(this, MortgageActivity.class);
            intent.putExtra("isFromHistory", true);
            intent.putExtra("loanAmount", getDouble(plan.get("loanAmount")));
            intent.putExtra("interest", getDouble(plan.get("interest")));
            intent.putExtra("years", getInt(plan.get("years")));
            intent.putExtra("fullPrice", getDouble(plan.get("fullPrice")));
            intent.putExtra("propertySize", getDouble(plan.get("propertySize")));
            intent.putExtra("cityAvgPrice", getDouble(plan.get("cityAvgPrice")));
            intent.putExtra("city", (String) plan.get("city"));
            startActivity(intent);
        } else {
            // מעבר לדף פרטי השקעה (DetailsActivity)
            // הערה: אם אתה רוצה שזה יחזור למסך העריכה (CalcRibitActivity), שנה את המחלקה כאן
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", getDouble(plan.get("initial")));
            intent.putExtra("monthly", getDouble(plan.get("monthly")));
            intent.putExtra("rate", getDouble(plan.get("rate")));
            intent.putExtra("years", getInt(plan.get("years")));
            intent.putExtra("months", getInt(plan.get("months")));
            intent.putExtra("fees", getDouble(plan.get("fees")));
            intent.putExtra("currency", (String) plan.getOrDefault("currency", "₪"));
            startActivity(intent);
        }
    }

    @Override
    public void onDeleteClick(Map<String, Object> plan, int position) {
        String docId = (String) plan.get("docId");
        if (docId == null) return;

        new AlertDialog.Builder(this)
                .setTitle("מחיקה")
                .setMessage("למחוק את החישוב '" + plan.get("planName") + "' לצמיתות?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    db.collection("saved_plans").document(docId).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "החישוב נמחק", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
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
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_history);
        // מוודא שהכיתוב תמיד מופיע
        nav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_ai_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                return true; // כבר כאן
            }
            return false;
        });
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (tvTitle != null) tvTitle.setTextColor(Color.WHITE);
        }
    }

    // פונקציות עזר להמרה בטוחה של נתונים מ-Firebase
    private double getDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0.0; }
    }

    private int getInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    private long getLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }
}