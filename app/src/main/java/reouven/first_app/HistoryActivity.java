package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // אתחול מערכות
        setupTopBar();
        setupBottomNavigation();

        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        planList = new ArrayList<>();
        adapter = new HistoryAdapter(planList, this);
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadHistoryFromFirebase();
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
            if (id == R.id.menu_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
            return true;
        });
        popup.show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // טיפ זהב: מוודא שהתפריט נמצא בשכבה העליונה ביותר
            bottomNav.bringToFront();

            bottomNav.setSelectedItemId(R.id.nav_history);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, CalcRibitActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                } else if (id == R.id.nav_tips) {
                    startActivity(new Intent(this, TipsActivity.class));
                    overridePendingTransition(0, 0);
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
        intent.putExtra("initial", Double.parseDouble(String.valueOf(plan.getOrDefault("initial", 0))));
        intent.putExtra("monthly", Double.parseDouble(String.valueOf(plan.getOrDefault("monthly", 0))));
        intent.putExtra("rate", Double.parseDouble(String.valueOf(plan.getOrDefault("rate", 0))));
        intent.putExtra("years", Integer.parseInt(String.valueOf(plan.getOrDefault("years", 0))));
        intent.putExtra("months", Integer.parseInt(String.valueOf(plan.getOrDefault("months", 0))));
        intent.putExtra("fees", Double.parseDouble(String.valueOf(plan.getOrDefault("fees", 0))));
        intent.putExtra("currency", String.valueOf(plan.getOrDefault("currency", "₪")));
        intent.putExtra("isFromHistory", true);
        startActivity(intent);
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
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}