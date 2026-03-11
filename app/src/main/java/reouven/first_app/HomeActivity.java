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
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private FirebaseAuth mAuth;
    private CardView cardCompoundInterest, cardMortgage;
    private View btnMenuHeader, btnBackHeader, mainLayout;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // בדיקת מצב לילה בזיכרון - חייב לקרות לפני super.onCreate
        checkAndApplyDarkMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        // 1. חיבור הרכיבים מה-XML
        initViews();

        // 2. החלת צבעים (מצב לילה/יום) על כל הכרטיסים
        applyCustomColorMode();

        // 3. הצגת שם המשתמש
        displayUserInfo();

        // 4. הגדרת לחיצה למחשבון השקעות
        cardCompoundInterest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalcRibitActivity.class);
            startActivity(intent);
        });

        // 5. הגדרת לחיצה למחשבון משכנתא
        cardMortgage.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, MortgageActivity.class);
            startActivity(intent);
        });

        // 6. הגדרת התפריט התחתון
        setupBottomNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        cardMortgage = findViewById(R.id.cardMortgage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnBackHeader = findViewById(R.id.btnBackHeader); // נמצא ב-top_bar.xml
        btnMenuHeader = findViewById(R.id.btnMenuHeader); // נמצא ב-top_bar.xml

        if (btnBackHeader != null) btnBackHeader.setVisibility(View.GONE); // בבית אין כפתור "חזור"
        if (btnMenuHeader != null) btnMenuHeader.setOnClickListener(this::showPopupMenu);
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

        // רכיבי טקסט כלליים
        TextView tvLabel = findViewById(R.id.tvSelectCalcLabel);

        // רכיבי כרטיס השקעות
        TextView tvCardTitle = findViewById(R.id.tvCardTitle);
        TextView tvCardDesc = findViewById(R.id.tvCardDesc);

        // רכיבי כרטיס משכנתא
        TextView tvMortgageTitle = findViewById(R.id.tvMortgageTitle);
        TextView tvMortgageDesc = findViewById(R.id.tvMortgageDesc);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvWelcomeName.setTextColor(Color.WHITE);
            if (tvLabel != null) tvLabel.setTextColor(Color.LTGRAY);

            // עיצוב כרטיסים למצב לילה
            cardCompoundInterest.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            cardMortgage.setCardBackgroundColor(Color.parseColor("#1E1E1E"));

            if (tvCardTitle != null) tvCardTitle.setTextColor(Color.parseColor("#9FA8DA"));
            if (tvCardDesc != null) tvCardDesc.setTextColor(Color.LTGRAY);
            if (tvMortgageTitle != null) tvMortgageTitle.setTextColor(Color.parseColor("#9FA8DA"));
            if (tvMortgageDesc != null) tvMortgageDesc.setTextColor(Color.LTGRAY);

            bottomNavigationView.setBackgroundColor(Color.BLACK);
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tvWelcomeName.setTextColor(Color.parseColor("#333333"));
            if (tvLabel != null) tvLabel.setTextColor(Color.parseColor("#757575"));

            // עיצוב כרטיסים למצב יום
            cardCompoundInterest.setCardBackgroundColor(Color.WHITE);
            cardMortgage.setCardBackgroundColor(Color.WHITE);

            if (tvCardTitle != null) tvCardTitle.setTextColor(Color.parseColor("#1A237E"));
            if (tvCardDesc != null) tvCardDesc.setTextColor(Color.parseColor("#757575"));
            if (tvMortgageTitle != null) tvMortgageTitle.setTextColor(Color.parseColor("#1A237E"));
            if (tvMortgageDesc != null) tvMortgageDesc.setTextColor(Color.parseColor("#757575"));

            bottomNavigationView.setBackgroundColor(Color.WHITE);
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isCurrentlyDark = prefs.getBoolean("dark_mode", false);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("dark_mode", !isCurrentlyDark);
        editor.apply();

        // הפעלה מחדש של ה-Activity כדי להחיל את השינוי
        recreate();
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class));
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true; // כבר בבית

                Intent intent = null;
                if (id == R.id.nav_history) {
                    intent = new Intent(this, HistoryActivity.class);
                } else if (id == R.id.nav_tips) {
                    intent = new Intent(this, TipsActivity.class);
                } else if (id == R.id.nav_ai_chat) {
                    intent = new Intent(this, ChatActivity.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void displayUserInfo() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userType = getIntent().getStringExtra("USER_TYPE");

        if ("guest".equals(userType)) {
            tvWelcomeName.setText("שלום, אורח");
        } else if (currentUser != null) {
            String name = currentUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = currentUser.getEmail();
                if (name != null && name.contains("@")) name = name.split("@")[0];
            }
            tvWelcomeName.setText("שלום, " + name);
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("InvestCalc היא אפליקציה לניהול וחישוב השקעות חכם.\n\nפותח על ידי: ראובן\nגרסה: 1.1")
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך לצאת מהחשבון?")
                .setPositiveButton("כן, התנתק", (dialog, which) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}