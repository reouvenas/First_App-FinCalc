package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
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

        // הסתרת סרגל המערכת
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. הגדרת הטול-בר העליון (include layout)
        setupTopBar();

        // 2. הגדרת התפריט התחתון
        setupBottomNavigation();

        // 3. הגדרת הרשימה
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        planList = new ArrayList<>();

        // שליחת 'this' כמאזין ללחיצות
        adapter = new HistoryAdapter(planList, this);
        rvHistory.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadHistoryFromFirebase();
    }

    private void setupTopBar() {
        // חץ חזור (נמצא בתוך top_bar.xml שהכללנו ב-XML)
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
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

    // פונקציה שמופעלת כשלוחצים על כרטיס בהיסטוריה
    @Override
    public void onPlanClick(Map<String, Object> plan) {
        Intent intent = new Intent(this, DetailsActivity.class);

        // העברת הנתונים מהמפה ל-Intent כדי שדף הפירוט יציג אותם
        intent.putExtra("initial", Double.parseDouble(String.valueOf(plan.get("initial"))));
        intent.putExtra("monthly", Double.parseDouble(String.valueOf(plan.get("monthly"))));
        intent.putExtra("rate", Double.parseDouble(String.valueOf(plan.get("rate"))));
        intent.putExtra("years", Integer.parseInt(String.valueOf(plan.get("years"))));
        intent.putExtra("months", Integer.parseInt(String.valueOf(plan.get("months"))));
        intent.putExtra("fees", Double.parseDouble(String.valueOf(plan.get("fees"))));
        intent.putExtra("currency", String.valueOf(plan.get("currency")));
        intent.putExtra("isFromHistory", true); // סימון כדי שנוכל להציג כפתור "עריכה"

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
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בטעינה", Toast.LENGTH_SHORT).show());
    }
}