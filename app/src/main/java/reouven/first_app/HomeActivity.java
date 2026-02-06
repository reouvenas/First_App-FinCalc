package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View; // הוספתי את זה
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
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
    // נשתמש ב-ID מה-top_bar.xml
    private View btnMenuHeader;
    private View btnBackHeader;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // אתחול רכיבים
        mAuth = FirebaseAuth.getInstance();
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // --- טיפול ב-Top Bar (הסתרת חץ וחיבור תפריט) ---
        btnBackHeader = findViewById(R.id.btnBackHeader);
        btnMenuHeader = findViewById(R.id.btnMenuHeader);

        // הסתרת החץ כי זה דף הבית
        if (btnBackHeader != null) {
            btnBackHeader.setVisibility(View.GONE);
        }

        // לחיצה על תפריט שלוש השורות ב-Top Bar
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                showPopupMenu(v);
            });
        }

        // הצגת שם המשתמש
        displayUserInfo();

        // לחיצה על המחשבון - מעבר לדף המחשבון
        cardCompoundInterest.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, CalcRibitActivity.class);
            startActivity(intent);
        });

        // --- טיפול בתפריט התחתון ---
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
                }

                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    // הערה: בדרך כלל בדף הבית לא עושים finish() כדי שהמשתמש יוכל לחזור אליו
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
        // וודא שקובץ המניו העליון שלך נקרא top_app_menu או שנה כאן את השם
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