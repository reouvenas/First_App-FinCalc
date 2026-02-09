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
        boolean currentMode = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !currentMode).apply();
        recreate();
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
                } else if (id == R.id.nav_tips) {
                    intent = new Intent(this, TipsActivity.class);
                } else if (id == R.id.nav_ai_chat) {
                    intent = new Intent(this, ChatActivity.class);
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
        try {
            intent.putExtra("initial", getDoubleValue(plan.get("initial")));
            intent.putExtra("monthly", getDoubleValue(plan.get("monthly")));
            intent.putExtra("rate", getDoubleValue(plan.get("rate")));
            intent.putExtra("years", getIntValue(plan.get("years")));
            intent.putExtra("months", getIntValue(plan.get("months")));
            intent.putExtra("fees", getDoubleValue(plan.get("fees")));
            intent.putExtra("currency", String.valueOf(plan.getOrDefault("currency", "₪")));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בטעינת נתוני התוכנית", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDeleteClick(Map<String, Object> plan, int position) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת חישוב")
                .setMessage("האם אתה בטוח שברצונך למחוק את '" + plan.getOrDefault("planName", "תוכנית זו") + "'?")
                .setPositiveButton("מחק", (dialog, which) -> deletePlanFromFirebase(plan, position))
                .setNegativeButton("ביטול", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePlanFromFirebase(Map<String, Object> plan, int position) {
        Object timestamp = plan.get("timestamp");
        if (timestamp == null) return;
        db.collection("saved_plans")
                .whereEqualTo("timestamp", timestamp)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        doc.getReference().delete().addOnSuccessListener(aVoid -> {
                            planList.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(this, "החישוב נמחק בהצלחה", Toast.LENGTH_SHORT).show();
                        });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה במחיקה", Toast.LENGTH_SHORT).show());
    }

    private double getDoubleValue(Object obj) {
        if (obj == null) return 0.0;
        try { return Double.parseDouble(String.valueOf(obj)); } catch (Exception e) { return 0.0; }
    }

    private int getIntValue(Object obj) {
        if (obj == null) return 0;
        try { return Integer.parseInt(String.valueOf(obj)); } catch (Exception e) { return 0; }
    }

    private void loadHistoryFromFirebase() {
        db.collection("saved_plans")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    planList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        planList.add(document.getData());
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינת הנתונים", Toast.LENGTH_SHORT).show());
    }
}