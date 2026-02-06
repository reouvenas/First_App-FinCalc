package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;

public class TipsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // הגדרת כל מערכות הניווט בדף
        setupNavigation();
    }

    private void setupNavigation() {
        // --- 1. הגדרת התפריט התחתון ---
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_tips);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, CalcRibitActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return id == R.id.nav_tips;
        });

        // --- 2. הגדרת הטופ-בר (חץ חזור ותפריט עליון) ---

        // כפתור חזור
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // כפתור תפריט עליון (האייקון של ה-3 שורות/נקודות)
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(menuItem -> {
                    int id = menuItem.getItemId();
                    if (id == R.id.menu_profile) {
                        Toast.makeText(this, "פרופיל אישי (בקרוב)", Toast.LENGTH_SHORT).show();
                    } else if (id == R.id.menu_about) {
                        Toast.makeText(this, "אודות: מחשבון השקעות חכם v1.0", Toast.LENGTH_LONG).show();
                    } else if (id == R.id.menu_contact) {
                        Toast.makeText(this, "יוצר קשר עם התמיכה...", Toast.LENGTH_SHORT).show();
                    } else if (id == R.id.menu_logout) {
                        Toast.makeText(this, "מתנתק מהמערכת...", Toast.LENGTH_SHORT).show();
                        // כאן תוסיף בעתיד את ה-SignOut של Firebase
                        finish();
                    }
                    return true;
                });
                popup.show();
            });
        }
    }
}