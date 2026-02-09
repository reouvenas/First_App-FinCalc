package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Calendar;

public class TipsActivity extends AppCompatActivity {

    private TextView tvDailyTipTitle, tvDailyTipContent, tvPrinciplesTitle;
    private View mainLayout;
    private BottomNavigationView bottomNav;

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
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tips);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        applyCustomColorMode();
        setDailyTip();
        setupNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvDailyTipTitle = findViewById(R.id.tvDailyTipTitle);
        tvDailyTipContent = findViewById(R.id.tvDailyTipContent);
        tvPrinciplesTitle = findViewById(R.id.tvPrinciplesTitle);
        bottomNav = findViewById(R.id.bottom_navigation);
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

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        CardView dailyCard = findViewById(R.id.cardDailyTip);
        TextView dailyLabel = findViewById(R.id.tvDailyTipLabel);

        CardView[] cards = {
                findViewById(R.id.card1), findViewById(R.id.card2),
                findViewById(R.id.card3), findViewById(R.id.card4),
                findViewById(R.id.card5)
        };

        TextView[] contents = {
                findViewById(R.id.tvCard1Content), findViewById(R.id.tvCard2Content),
                findViewById(R.id.tvCard3Content), findViewById(R.id.tvCard4Content),
                findViewById(R.id.tvCard5Content)
        };

        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (tvPrinciplesTitle != null) tvPrinciplesTitle.setTextColor(Color.WHITE);
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.BLACK);

            if (dailyCard != null) dailyCard.setCardBackgroundColor(Color.parseColor("#1A237E"));
            if (dailyLabel != null) dailyLabel.setTextColor(Color.parseColor("#9FA8DA"));
            tvDailyTipTitle.setTextColor(Color.WHITE);
            tvDailyTipContent.setTextColor(Color.LTGRAY);

            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) cards[i].setCardBackgroundColor(Color.parseColor("#1E1E1E"));
                if (contents[i] != null) contents[i].setTextColor(Color.parseColor("#B0B0B0"));
            }
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F7FA"));
            if (tvPrinciplesTitle != null) tvPrinciplesTitle.setTextColor(Color.parseColor("#455A64"));
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.WHITE);

            if (dailyCard != null) dailyCard.setCardBackgroundColor(Color.parseColor("#E8EAF6"));
            if (dailyLabel != null) dailyLabel.setTextColor(Color.parseColor("#3F51B5"));
            tvDailyTipTitle.setTextColor(Color.parseColor("#1A237E"));
            tvDailyTipContent.setTextColor(Color.parseColor("#333333"));

            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) cards[i].setCardBackgroundColor(Color.WHITE);
                if (contents[i] != null) contents[i].setTextColor(Color.parseColor("#666666"));
            }
        }
    }

    private void setDailyTip() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int index = dayOfYear % dailyTitles.length;
        tvDailyTipTitle.setText(dailyTitles[index]);
        tvDailyTipContent.setText(dailyContents[index]);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isCurrentlyDark = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !isCurrentlyDark).apply();
        recreate();
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tips);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // תיקון: חזרה לדף הבית הראשי
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    startActivity(new Intent(this, ChatActivity.class));
                    finish();
                    return true;
                }
                return id == R.id.nav_tips;
            });
        }

        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            // תיקון: שימוש ב-onBackPressed לחזרה טבעית
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> showPopupMenu(v));
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
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
}