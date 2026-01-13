package reouven.first_app;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private HistoryAdapter adapter;
    private List<Map<String, Object>> planList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // 1. חיבור ה-RecyclerView מה-XML (תוודא שב-activity_history.xml ה-ID הוא rvHistory)
        rvHistory = findViewById(R.id.rvHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // 2. הכנת הרשימה והאדפטר
        planList = new ArrayList<>();
        adapter = new HistoryAdapter(planList);
        rvHistory.setAdapter(adapter);

        // 3. חיבור ל-Firebase
        db = FirebaseFirestore.getInstance();

        // 4. קריאה לנתונים
        loadHistoryFromFirebase();
    }

    private void loadHistoryFromFirebase() {
        // אנחנו פונים לאוסף saved_plans שבו שמרנו את הנתונים ב-DetailsActivity
        db.collection("saved_plans")
                .orderBy("timestamp", Query.Direction.DESCENDING) // מסדר מהכי חדש להכי ישן
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    planList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        // מוסיף כל תוכנית שמצאנו לרשימה
                        planList.add(document.getData());
                    }
                    // אומר לאדפטר: "יש נתונים חדשים, תציג אותם על המסך!"
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בטעינה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}