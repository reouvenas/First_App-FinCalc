package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;

public class TipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        // הסתרת סרגל המערכת למראה נקי
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // הגדרת התפריט התחתון
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // סימון "טיפים" כנבחר בתפריט
        bottomNavigationView.setSelectedItemId(R.id.nav_tips);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                // חזרה למחשבון
                startActivity(new Intent(this, CalcRibitActivity.class));
                overridePendingTransition(0, 0);
                finish(); // סוגר את דף הטיפים
                return true;
            } else if (id == R.id.nav_history) {
                // מעבר להיסטוריה
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_tips) {
                return true; // כבר נמצא בטיפים
            }
            return false;
        });
    }
}