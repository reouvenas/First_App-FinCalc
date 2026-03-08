package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
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

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
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

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (tvTitle != null) tvTitle.setTextColor(Color.WHITE);
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.BLACK);
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            if (tvTitle != null) tvTitle.setTextColor(Color.parseColor("#1A237E"));
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.WHITE);
        }
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showPopupMenu);
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_profile) { // החיבור לפרופיל
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.menu_contact) {
                NavigationHelper.showContactDialog(this);
                return true;
            } else if (id == R.id.menu_logout) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void loadHistoryFromFirebase() {
        db.collection("saved_plans")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (value != null) {
                        planList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            planList.add(doc.getData());
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;

                if (id == R.id.nav_home) {
                    intent = new Intent(this, HomeActivity.class);
                } else if (id == R.id.nav_ai_chat) {
                    intent = new Intent(this, ChatActivity.class);
                } else if (id == R.id.nav_tips) {
                    intent = new Intent(this, TipsActivity.class);
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return id == R.id.nav_history;
            });
        }
    }

    @Override
    public void onPlanClick(Map<String, Object> plan) {
        Intent intent = new Intent(this, DetailsActivity.class);
        // העברת כל הנתונים של התוכנית שנבחרה חזרה לדף הפירוט
        intent.putExtra("initial", getDouble(plan.get("initial")));
        intent.putExtra("monthly", getDouble(plan.get("monthly")));
        intent.putExtra("rate", getDouble(plan.get("rate")));
        intent.putExtra("years", getInt(plan.get("years")));
        intent.putExtra("months", getInt(plan.get("months")));
        intent.putExtra("fees", getDouble(plan.get("fees")));
        intent.putExtra("currency", (String) plan.get("currency"));
        startActivity(intent);
    }

    @Override
    public void onDeleteClick(Map<String, Object> plan, int position) {

    }

    // פונקציות עזר להמרה בטוחה של נתונים מ-Firebase
    private double getDouble(Object o) {
        if (o instanceof Number) return ((Number) o).doubleValue();
        return 0.0;
    }

    private int getInt(Object o) {
        if (o instanceof Number) return ((Number) o).intValue();
        return 0;
    }
}