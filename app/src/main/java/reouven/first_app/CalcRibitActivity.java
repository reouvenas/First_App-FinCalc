package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Locale;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails;
    private FirebaseAuth mAuth;
    private String currencySymbol = "₪";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBarLogic();
        setupBottomNavigation();
        checkIfEditing();

        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> calculateInvestment());
        }

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> navigateToDetails());
        }
    }

    private void initViews() {
        etInitial = findViewById(R.id.etInitial);
        etMonthly = findViewById(R.id.etMonthly);
        etRate = findViewById(R.id.etRate);
        etYears = findViewById(R.id.etYears);
        etMonths = findViewById(R.id.etMonths);
        etFees = findViewById(R.id.etFees);
        tvResult = findViewById(R.id.tvResult);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDetails = findViewById(R.id.btnDetails);

        ImageView btnCurrency = findViewById(R.id.btnCurrency);
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> {
                currencySymbol = currencySymbol.equals("₪") ? "$" : (currencySymbol.equals("$") ? "€" : "₪");
                if (tvCurrencySymbol != null) tvCurrencySymbol.setText(currencySymbol);
            });
        }

        ImageView btnInfoFees = findViewById(R.id.btnInfoFees);
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> showInfoDialog("דמי ניהול", "אחוז דמי הניהול השנתיים המופחתים מהתשואה הכוללת."));
        }
    }

    private void setupTopBarLogic() {
        View header = findViewById(R.id.header);
        if (header != null) {
            View btnBack = header.findViewById(R.id.btnBackHeader);
            if (btnBack != null) btnBack.setOnClickListener(v -> finish());

            View btnMenu = header.findViewById(R.id.btnMenuHeader);
            if (btnMenu != null) {
                btnMenu.setOnClickListener(this::showPopupMenu);
            }
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            } else if (id == R.id.menu_about) {
                showInfoDialog("אודות", "InvestCalc - אפליקציה לחישובים פיננסיים מתקדמים.\nגרסה 1.0");
                return true;
            } else if (id == R.id.menu_contact) {
                showContactDialog();
                return true;
            } else if (id == R.id.menu_logout) {
                mAuth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("צור קשר")
                .setMessage("נתקלת בבעיה? נשמח לשמוע ממך במייל.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:support@investcalc.com")); // שנה למייל שלך
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc");
                    try {
                        startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("הבנתי", null)
                .show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) startActivity(new Intent(this, HomeActivity.class));
                else if (id == R.id.nav_history) startActivity(new Intent(this, HistoryActivity.class));
                else if (id == R.id.nav_tips) startActivity(new Intent(this, TipsActivity.class));
                else if (id == R.id.nav_ai_chat) startActivity(new Intent(this, ChatActivity.class));
                finish();
                return true;
            });
        }
    }

    private void navigateToDetails() {
        try {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", getDouble(etInitial));
            intent.putExtra("monthly", getDouble(etMonthly));
            intent.putExtra("rate", getDouble(etRate));
            intent.putExtra("years", (int) getDouble(etYears));
            intent.putExtra("months", (int) getDouble(etMonths));
            intent.putExtra("fees", getDouble(etFees));
            intent.putExtra("currency", currencySymbol);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "נא להזין נתונים תקינים", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateInvestment() {
        try {
            double p = getDouble(etInitial);
            double m = getDouble(etMonthly);
            double r = (getDouble(etRate) - getDouble(etFees)) / 100 / 12;
            int t = ((int) getDouble(etYears) * 12) + (int) getDouble(etMonths);
            if (t <= 0) {
                Toast.makeText(this, "הזן תקופת זמן", Toast.LENGTH_SHORT).show();
                return;
            }
            double total = (r != 0) ? p * Math.pow(1 + r, t) + m * (Math.pow(1 + r, t) - 1) / r : p + (m * t);
            if (tvResult != null) {
                tvResult.setText(currencySymbol + String.format(Locale.US, "%,.2f", total));
                tvResult.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בחישוב", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !isDark).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void checkIfEditing() {
        if (getIntent().hasExtra("edit_initial")) {
            etInitial.setText(String.valueOf(getIntent().getDoubleExtra("edit_initial", 0)));
        }
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}