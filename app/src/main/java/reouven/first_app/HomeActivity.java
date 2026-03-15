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
    private CardView cardCompoundInterest, cardMortgage, cardThirdCalc;
    private View mainLayout;
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

        // חיבור למחשבון השלישי החדש
        cardThirdCalc.setOnClickListener(v -> {
            startActivity(new Intent(this, FreelanceVsEmployeeActivity.class));
        });

        setupBottomNavigation();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        cardMortgage = findViewById(R.id.cardMortgage);
        cardThirdCalc = findViewById(R.id.cardThirdCalc);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        findViewById(R.id.btnMenuHeader).setOnClickListener(this::showPopupMenu);
    }

    private void displayUserInfo() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            tvWelcomeName.setText("שלום, אורח");
        } else {
            String name = user.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = (user.getEmail() != null) ? user.getEmail().split("@")[0] : "משתמש";
            }
            tvWelcomeName.setText("שלום, " + name);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;

            FirebaseUser user = mAuth.getCurrentUser();
            if ((id == R.id.nav_tips || id == R.id.nav_history) && (user == null || user.isAnonymous())) {
                showGuestBlockedDialog();
                return false;
            }

            if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
            else if (id == R.id.nav_tips) startActivity(new Intent(this, TipsActivity.class));
            else if (id == R.id.nav_ai_chat) startActivity(new Intent(this, ChatActivity.class));
            return true;
        });
    }

    private void showGuestBlockedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("גישה מוגבלת")
                .setMessage("דף זה זמין למשתמשים רשומים בלבד. רוצה להירשם?")
                .setPositiveButton("להרשמה", (d, w) -> startActivity(new Intent(this, RegisterActivity.class)))
                .setNegativeButton("ביטול", null).show();
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) toggleDarkMode();
            else if (id == R.id.menu_about) showAboutDialog();
            else if (id == R.id.menu_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });
        popup.show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (tvWelcomeName != null) tvWelcomeName.setTextColor(Color.WHITE);
        }
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this).setTitle("אודות").setMessage("InvestCalc v2.0\nמחשבון פיננסי חכם.\nפותח ע\"י ראובן").setPositiveButton("סגור", null).show();
    }
}