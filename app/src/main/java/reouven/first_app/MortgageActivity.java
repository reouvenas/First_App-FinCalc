package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MortgageActivity extends AppCompatActivity {

    private EditText etLoanAmount, etInterestRate, etYears, etFullPropertyPrice, etPropertySize, etCityAvgPrice;
    private AutoCompleteTextView actvCity;
    private Button btnCalculate, btnSavePlan;
    private TextView tvResult, tvDealStatus, tvMortgageTitle, tvMortgageSubTitle;
    private BottomNavigationView bottomNav;
    private View mainLayout;
    private HashMap<String, Integer> cityPrices = new HashMap<>();
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mortgage);

        mAuth = FirebaseAuth.getInstance();
        initViews();
        initData();
        setupCitySelectionListener(); // הפונקציה שמעדכנת מחיר למטר
        applyCustomColorMode();
        setupTopBar();
        setupBottomNav();

        btnCalculate.setOnClickListener(v -> calculateLogic());
        btnSavePlan.setOnClickListener(v -> handleSaveButtonClick());

        if (getIntent().getBooleanExtra("isFromHistory", false)) {
            loadDataFromHistory();
        }
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        etLoanAmount = findViewById(R.id.etLoanAmount);
        etInterestRate = findViewById(R.id.etInterestRate);
        etYears = findViewById(R.id.etYears);
        etFullPropertyPrice = findViewById(R.id.etFullPropertyPrice);
        etPropertySize = findViewById(R.id.etPropertySize);
        etCityAvgPrice = findViewById(R.id.etCityAvgPrice);
        actvCity = findViewById(R.id.actvCity);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        tvResult = findViewById(R.id.tvResult);
        tvDealStatus = findViewById(R.id.tvDealStatus);
        tvMortgageTitle = findViewById(R.id.tvMortgageTitle);
        tvMortgageSubTitle = findViewById(R.id.tvMortgageSubTitle);
        bottomNav = findViewById(R.id.bottom_navigation);

        String[] cities = {"תל אביב", "ירושלים", "חיפה", "ראשון לציון", "נתניה", "באר שבע"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        actvCity.setAdapter(adapter);
    }

    // מאזין לבחירת עיר שממלא אוטומטית את המחיר הממוצע למטר
    private void setupCitySelectionListener() {
        actvCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            if (cityPrices.containsKey(selectedCity)) {
                etCityAvgPrice.setText(String.valueOf(cityPrices.get(selectedCity)));
                Toast.makeText(this, "עודכן מחיר ממוצע ל" + selectedCity, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleSaveButtonClick() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null || user.isAnonymous()) {
            showGuestRestrictionDialog("שמירת תוכניות משכנתא זמינה למשתמשים רשומים בלבד.");
        } else {
            showSaveDialog();
        }
    }

    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("פעולה חסומה")
                .setMessage(message + "\nרוצה להירשם עכשיו כדי לשמור?")
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showSaveDialog() {
        if (tvResult.getText().toString().isEmpty()) {
            Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שמירת תוכנית");
        final EditText input = new EditText(this);
        input.setHint("תן שם לתוכנית...");
        builder.setView(input);
        builder.setPositiveButton("שמור", (dialog, which) -> savePlan(input.getText().toString()));
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void savePlan(String name) {
        String uid = mAuth.getUid();
        if (uid == null) return;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("planName", name.isEmpty() ? "חישוב משכנתא" : name);
            data.put("type", "mortgage");
            data.put("timestamp", System.currentTimeMillis());
            data.put("loanAmount", Double.parseDouble(etLoanAmount.getText().toString()));
            data.put("interest", Double.parseDouble(etInterestRate.getText().toString()));
            data.put("years", Integer.parseInt(etYears.getText().toString()));
            data.put("fullPrice", Double.parseDouble(etFullPropertyPrice.getText().toString()));
            data.put("propertySize", Double.parseDouble(etPropertySize.getText().toString()));
            data.put("cityAvgPrice", Double.parseDouble(etCityAvgPrice.getText().toString()));
            data.put("city", actvCity.getText().toString());

            FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                    .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר בהיסטוריה!", Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateLogic() {
        try {
            if (etLoanAmount.getText().toString().isEmpty() || etYears.getText().toString().isEmpty()) return;

            double p = Double.parseDouble(etLoanAmount.getText().toString());
            double annualRate = etInterestRate.getText().toString().isEmpty() ? 0 : Double.parseDouble(etInterestRate.getText().toString());
            double r = (annualRate / 100) / 12;
            int n = Integer.parseInt(etYears.getText().toString()) * 12;

            double monthly = (r > 0) ? (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1) : p / n;
            tvResult.setText(String.format("החזר חודשי: %.0f ₪", monthly));

            if (!etFullPropertyPrice.getText().toString().isEmpty() && !etPropertySize.getText().toString().isEmpty() && !etCityAvgPrice.getText().toString().isEmpty()) {
                double fullPrice = Double.parseDouble(etFullPropertyPrice.getText().toString());
                double size = Double.parseDouble(etPropertySize.getText().toString());
                double avgPrice = Double.parseDouble(etCityAvgPrice.getText().toString());

                double currentMeterPrice = fullPrice / size;
                double diff = ((currentMeterPrice - avgPrice) / avgPrice) * 100;

                if (diff > 5) {
                    tvDealStatus.setText(String.format("הנכס יקר ב-%.1f%% מהממוצע", diff));
                    tvDealStatus.setTextColor(Color.RED);
                } else if (diff < -5) {
                    tvDealStatus.setText(String.format("עסקה מעולה! זול ב-%.1f%% מהממוצע", Math.abs(diff)));
                    tvDealStatus.setTextColor(Color.parseColor("#2E7D32"));
                } else {
                    tvDealStatus.setText("מחיר תואם לממוצע השוק");
                    tvDealStatus.setTextColor(Color.BLUE);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "בדוק את תקינות הנתונים", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuHeader).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                FirebaseUser user = mAuth.getCurrentUser();

                if (id == R.id.menu_dark_mode) { toggleDarkMode(); }
                else if (id == R.id.menu_profile) {
                    if (user == null || user.isAnonymous()) showGuestRestrictionDialog("פרופיל זמין לרשומים בלבד.");
                    else startActivity(new Intent(this, ProfileActivity.class));
                }
                else if (id == R.id.menu_contact) { NavigationHelper.showContactDialog(this); }
                else if (id == R.id.menu_about) { showAboutDialog(); }
                else if (id == R.id.menu_logout) { showLogoutDialog(); }
                return true;
            });
            popup.show();
        });
    }

    private void initData() {
        cityPrices.put("תל אביב", 60000);
        cityPrices.put("ירושלים", 35000);
        cityPrices.put("חיפה", 22000);
        cityPrices.put("נתניה", 25000);
        cityPrices.put("ראשון לציון", 28000);
        cityPrices.put("באר שבע", 15000);
    }

    private void setupBottomNav() {
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            FirebaseUser user = mAuth.getCurrentUser();
            if (id == R.id.nav_history) {
                if (user == null || user.isAnonymous()) {
                    showGuestRestrictionDialog("היסטוריה זמינה למשתמשים רשומים בלבד.");
                    return false;
                }
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_home) {
                finish();
                return true;
            }
            return false;
        });
    }

    // פונקציות עיצוב וכלליות (נשארות כמעט ללא שינוי)
    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        CardView card1 = findViewById(R.id.cardLoanDetails);
        CardView card2 = findViewById(R.id.cardPropertyDetails);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            tvMortgageTitle.setTextColor(Color.WHITE);
            tvResult.setTextColor(Color.WHITE);
            if (card1 != null) card1.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            if (card2 != null) card2.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            bottomNav.setBackgroundColor(Color.BLACK);
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

    private void showAboutDialog() {
        new AlertDialog.Builder(this).setTitle("אודות").setMessage("מחשבון משכנתא InvestCalc\nפותח ע\"י ראובן").setPositiveButton("סגור", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("להתנתק?").setPositiveButton("כן", (d, w) -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
        }).setNegativeButton("ביטול", null).show();
    }

    private void loadDataFromHistory() {
        Intent intent = getIntent();
        etLoanAmount.setText(String.valueOf(intent.getDoubleExtra("loanAmount", 0)));
        etInterestRate.setText(String.valueOf(intent.getDoubleExtra("interest", 0)));
        etYears.setText(String.valueOf(intent.getIntExtra("years", 0)));
        etFullPropertyPrice.setText(String.valueOf(intent.getDoubleExtra("fullPrice", 0)));
        etPropertySize.setText(String.valueOf(intent.getDoubleExtra("propertySize", 0)));
        etCityAvgPrice.setText(String.valueOf(intent.getDoubleExtra("cityAvgPrice", 0)));
        actvCity.setText(intent.getStringExtra("city"));
        calculateLogic();
    }
}