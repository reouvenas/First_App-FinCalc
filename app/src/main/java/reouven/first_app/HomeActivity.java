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
        // בדיקת מצב לילה בזיכרון
        checkAndApplyDarkMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        initViews();

        // החלת צבעים ידנית
        applyCustomColorMode();

        displayUserInfo();

        cardCompoundInterest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalcRibitActivity.class);
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        cardMortgage = findViewById(R.id.cardMortgage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnBackHeader = findViewById(R.id.btnBackHeader);
        btnMenuHeader = findViewById(R.id.btnMenuHeader);

        if (btnBackHeader != null) btnBackHeader.setVisibility(View.GONE);
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

        TextView tvLabel = findViewById(R.id.tvSelectCalcLabel);
        TextView tvCardTitle = findViewById(R.id.tvCardTitle);
        TextView tvCardDesc = findViewById(R.id.tvCardDesc);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvWelcomeName.setTextColor(Color.WHITE);
            tvLabel.setTextColor(Color.LTGRAY);
            cardCompoundInterest.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            tvCardTitle.setTextColor(Color.parseColor("#9FA8DA")); // כחול בהיר שקריא על שחור
            tvCardDesc.setTextColor(Color.LTGRAY);
            bottomNavigationView.setBackgroundColor(Color.BLACK);
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            tvWelcomeName.setTextColor(Color.parseColor("#333333"));
            tvLabel.setTextColor(Color.parseColor("#757575"));
            cardCompoundInterest.setCardBackgroundColor(Color.WHITE);
            tvCardTitle.setTextColor(Color.parseColor("#1A237E"));
            tvCardDesc.setTextColor(Color.parseColor("#757575"));
            bottomNavigationView.setBackgroundColor(Color.WHITE);
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isCurrentlyDark = prefs.getBoolean("dark_mode", false);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("dark_mode", !isCurrentlyDark);
        editor.apply();

        if (!isCurrentlyDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        recreate();
    }

    private void showPopupMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_contact) {
                // כאן העדכון: קריאה לדיאלוג האחיד של האפליקציה
                NavigationHelper.showContactDialog(this);
                return true;
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.menu_profile) {
                Toast.makeText(this, "פרופיל אישי (בקרוב)", Toast.LENGTH_SHORT).show();
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
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);
                else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);

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
                .setMessage("InvestCalc היא אפליקציה לניהול וחישוב השקעות חכם.\n\nפותח על ידי: ראובן\nגרסה: 1.0")
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