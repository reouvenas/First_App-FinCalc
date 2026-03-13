package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
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
    private View btnMenuHeader, mainLayout;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        applyCustomColorMode();
        displayUserInfo();

        cardCompoundInterest.setOnClickListener(v -> startActivity(new Intent(this, CalcRibitActivity.class)));
        cardMortgage.setOnClickListener(v -> startActivity(new Intent(this, MortgageActivity.class)));

        setupBottomNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        cardMortgage = findViewById(R.id.cardMortgage);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnMenuHeader = findViewById(R.id.btnMenuHeader);

        findViewById(R.id.btnBackHeader).setVisibility(View.GONE);
        if (btnMenuHeader != null) btnMenuHeader.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) { toggleDarkMode(); }
            else if (id == R.id.menu_profile) { startActivity(new Intent(this, ProfileActivity.class)); }
            else if (id == R.id.menu_contact) { NavigationHelper.showContactDialog(this); }
            else if (id == R.id.menu_about) { showAboutDialog(); }
            else if (id == R.id.menu_logout) { showLogoutDialog(); }
            return true;
        });
        popup.show();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvWelcomeName.setTextColor(Color.WHITE);
            cardCompoundInterest.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            cardMortgage.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            bottomNavigationView.setBackgroundColor(Color.BLACK);
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            cardCompoundInterest.setCardBackgroundColor(Color.WHITE);
            cardMortgage.setCardBackgroundColor(Color.WHITE);
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            Intent intent = null;
            if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
            else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);
            else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);
            if (intent != null) { startActivity(intent); return true; }
            return false;
        });
    }

    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) name = user.getEmail().split("@")[0];
            tvWelcomeName.setText("שלום, " + name);
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this).setTitle("אודות InvestCalc").setMessage("InvestCalc v1.0\nיועץ מחשבון חכם בשילוב AI אסטרטגי ופיננסי.\nפותח על ידי: ראובן").setPositiveButton("סגור", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("לצאת מהחשבון?").setPositiveButton("כן", (d, w) -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }).setNegativeButton("ביטול", null).show();
    }
}