package reouven.first_app;

import android.content.Intent;
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
    private CardView cardCompoundInterest;
    private View btnMenuHeader;
    private View btnBackHeader;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        btnBackHeader = findViewById(R.id.btnBackHeader);
        btnMenuHeader = findViewById(R.id.btnMenuHeader);

        if (btnBackHeader != null) {
            btnBackHeader.setVisibility(View.GONE);
        }

        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> showPopupMenu(v));
        }

        displayUserInfo();

        cardCompoundInterest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalcRibitActivity.class);
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;

                Intent intent = null;
                if (id == R.id.nav_history) {
                    intent = new Intent(HomeActivity.this, HistoryActivity.class);
                } else if (id == R.id.nav_tips) {
                    intent = new Intent(HomeActivity.this, TipsActivity.class);
                } else if (id == R.id.nav_ai_chat) {
                    intent = new Intent(HomeActivity.this, ChatActivity.class);
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
        }
    }

    private void showPopupMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_profile) {
                Toast.makeText(this, "פרופיל אישי (בקרוב)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.menu_contact) { // הוספתי את הלוגיקה של יצירת קשר
                Toast.makeText(this, "ליצירת קשר שלחו מייל ל-support@investcalc.com", Toast.LENGTH_LONG).show();
                return true;
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void toggleDarkMode() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, "מצב בהיר הופעל", Toast.LENGTH_SHORT).show();
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            Toast.makeText(this, "מצב כהה הופעל", Toast.LENGTH_SHORT).show();
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
                if (name != null && name.contains("@")) {
                    name = name.split("@")[0];
                }
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
                    Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null)
                .show();
    }
}