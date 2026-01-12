package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private FirebaseAuth mAuth;
    private CardView cardCompoundInterest;
    private ImageButton btnProfile;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // אתחול רכיבים
        mAuth = FirebaseAuth.getInstance();
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        btnProfile = findViewById(R.id.btnProfile);
        bottomNavigationView = findViewById(R.id.bottom_navigation); // אתחול ה-BottomNavigationView

        // הצגת שם המשתמש
        displayUserInfo();

        // לחיצה על המחשבון - מעבר לדף המחשבון
        cardCompoundInterest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalcRibitActivity.class);
            startActivity(intent);
        });

        // לחיצה על תפריט שלוש הנקודות ב-Header
        btnProfile.setOnClickListener(v -> {
            showPopupMenu(v);
        });

        // --- טיפול בתפריט התחתון ---
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // הגדרת פריט הבית כנבחר
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // אם כבר נמצאים בדף הבית, אל תעשה כלום
                    return true;
                } else if (id == R.id.nav_history) {
                    // פותח את HistoryActivity
                    Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
                    // דגלים אלה עוזרים לנהל את מחסנית הפעילויות
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish(); // סוגר את HomeActivity כדי לאפשר מעבר חלק
                    return true;
                } else if (id == R.id.nav_tips) {
                    // פותח את TipsActivity
                    Intent intent = new Intent(HomeActivity.this, TipsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish(); // סוגר את HomeActivity כדי לאפשר מעבר חלק
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
                if (name != null && name.contains("@")) {
                    name = name.split("@")[0];
                }
            }
            tvWelcomeName.setText("שלום, " + name);
        }
    }

    private void showPopupMenu(android.view.View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                Toast.makeText(this, "פרופיל אישי (בקרוב)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.menu_contact) {
                Toast.makeText(this, "יצירת קשר (בקרוב)", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
                return true;
            }
            return false;
        });
        popup.show();
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