package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;

public class TipsActivity extends AppCompatActivity {

    private TextView tvDailyTipTitle, tvDailyTipContent;

    // מאגר הטיפים למנגנון ה"טיפ היומי" בלבד
    private final String[] dailyTitles = {
            "חוק ה-72", "הכוח של 100 ש''ח", "אינפלציה שוחקת", "הפסיכולוגיה של ההפסד", "מדד ה-S&P 500"
    };

    private final String[] dailyContents = {
            "חלקו 72 בריבית השנתית ותדעו תוך כמה שנים הכסף שלכם יכפיל את עצמו!",
            "אפילו 100 ש''ח בחודש לאורך 30 שנה יכולים להפוך לעשרות אלפי שקלים בזכות הריבית דריבית.",
            "כסף בעו''ש מאבד ערך בגלל עליית המחירים. השקעה היא הדרך להגן עליו.",
            "הפחד מהפסד גורם לאנשים למכור כשהשוק יורד - זה לרוב הזמן הכי גרוע למכור.",
            "זהו מדד של 500 החברות הגדולות בארה''ב. היסטורית, הוא הניב תשואה יפה לטווח ארוך."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        tvDailyTipTitle = findViewById(R.id.tvDailyTipTitle);
        tvDailyTipContent = findViewById(R.id.tvDailyTipContent);

        // הפעלת המנגנון האוטומטי לפי תאריך
        setDailyTip();

        setupNavigation();
    }

    private void setDailyTip() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int index = dayOfYear % dailyTitles.length;

        tvDailyTipTitle.setText(dailyTitles[index]);
        tvDailyTipContent.setText(dailyContents[index]);
    }

    private void setupNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.nav_tips);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                finish();
                return true;
            }
            return id == R.id.nav_tips;
        });

        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(menuItem -> {
                if (menuItem.getItemId() == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
                return true;
            });
            popup.show();
        });
    }
}