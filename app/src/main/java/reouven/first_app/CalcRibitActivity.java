package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails;
    private ImageView btnInfoFees, btnCurrency;
    private View mainLayout;
    private String currentCurrency = "ILS";
    private String currencySymbol = "₪";
    private double usdRate = 3.75;
    private double eurRate = 4.05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mainLayout = findViewById(R.id.main_layout);

        initViews();
        setupClickListeners();
        setupTopBar();
        setupBottomNavigation();
        fetchExchangeRates();
        checkIntentExtras();

        applyCustomColorMode();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        if (mainLayout != null) {
            if (isDarkMode) {
                mainLayout.setBackgroundColor(Color.BLACK);
                updateTextColors(true);
            } else {
                mainLayout.setBackgroundColor(Color.WHITE);
                updateTextColors(false);
            }
        }
    }

    private void updateTextColors(boolean isDark) {
        int color = isDark ? Color.WHITE : Color.BLACK;
        tvResult.setTextColor(color);
        tvCurrencySymbol.setTextColor(color);
        // עדכון צבעי תוויות ה-EditText אם קיימות ב-XML כ-TextViews נפרדים
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean currentMode = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !currentMode).apply();
        recreate();
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
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
            });
        }
    }

    private void initViews() {
        etInitial = findViewById(R.id.etInitial);
        etMonths = findViewById(R.id.etMonths);
        etMonthly = findViewById(R.id.etMonthly);
        etRate = findViewById(R.id.etRate);
        etYears = findViewById(R.id.etYears);
        etFees = findViewById(R.id.etFees);
        tvResult = findViewById(R.id.tvResult);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDetails = findViewById(R.id.btnDetails);
        btnInfoFees = findViewById(R.id.btnInfoFees);
        btnCurrency = findViewById(R.id.btnCurrency);
        if (tvResult != null) tvResult.setVisibility(View.GONE);
        setDetailsButtonEnabled(false);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    // תיקון: חזרה לדף הבית הראשי
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    startActivity(new Intent(this, ChatActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_tips) {
                    startActivity(new Intent(this, TipsActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupClickListeners() {
        if (btnCurrency != null) btnCurrency.setOnClickListener(v -> showCurrencyDialog());
        if (btnCalculate != null) btnCalculate.setOnClickListener(v -> calculateInvestment());
        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> {
                Intent intent = new Intent(CalcRibitActivity.this, DetailsActivity.class);
                intent.putExtra("initial", getDouble(etInitial));
                intent.putExtra("monthly", getDouble(etMonthly));
                intent.putExtra("rate", getDouble(etRate));
                intent.putExtra("years", (int) getDouble(etYears));
                intent.putExtra("months", (int) getDouble(etMonths));
                intent.putExtra("fees", getDouble(etFees));
                intent.putExtra("currency", currencySymbol);
                startActivity(intent);
            });
        }
    }

    private void calculateInvestment() {
        try {
            double principal = getDouble(etInitial);
            double monthlyDeposit = getDouble(etMonthly);
            double annualRate = getDouble(etRate);
            double annualFees = getDouble(etFees);
            int totalYears = (int) getDouble(etYears);
            int extraMonths = (int) getDouble(etMonths);
            int totalMonths = (totalYears * 12) + extraMonths;
            if (totalMonths <= 0) return;
            double r = ((annualRate - annualFees) / 100) / 12;
            double total = (r != 0) ? principal * Math.pow(1 + r, totalMonths) + monthlyDeposit * (Math.pow(1 + r, totalMonths) - 1) / r : principal + (monthlyDeposit * totalMonths);
            if (currentCurrency.equals("USD")) total /= usdRate;
            else if (currentCurrency.equals("EUR")) total /= eurRate;
            tvResult.setText("סכום צפוי: " + currencySymbol + String.format(Locale.US, "%,.2f", total));
            tvResult.setVisibility(View.VISIBLE);
            setDetailsButtonEnabled(true);
        } catch (Exception e) { Toast.makeText(this, "שגיאה בחישוב", Toast.LENGTH_SHORT).show(); }
    }

    private void setDetailsButtonEnabled(boolean enabled) {
        if (btnDetails != null) {
            btnDetails.setEnabled(enabled);
            btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(enabled ? "#4CAF50" : "#E8F5E9")));
        }
    }

    private void showCurrencyDialog() {
        String[] currencies = {"שקל (₪)", "דולר ($)", "אירו (€)"};
        new AlertDialog.Builder(this).setTitle("בחר מטבע").setItems(currencies, (dialog, which) -> {
            switch (which) {
                case 0: currentCurrency = "ILS"; currencySymbol = "₪"; break;
                case 1: currentCurrency = "USD"; currencySymbol = "$"; break;
                case 2: currentCurrency = "EUR"; currencySymbol = "€"; break;
            }
            tvCurrencySymbol.setText(currencySymbol);
            if (tvResult.getVisibility() == View.VISIBLE) calculateInvestment();
        }).show();
    }

    private void fetchExchangeRates() {
        new Thread(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/ILS");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                Scanner s = new Scanner(conn.getInputStream()).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject json = new JSONObject(response);
                usdRate = 1 / json.getJSONObject("rates").getDouble("USD");
                eurRate = 1 / json.getJSONObject("rates").getDouble("EUR");
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void checkIntentExtras() {
        Intent intent = getIntent();
        if (intent == null) return;
        boolean isEdit = intent.hasExtra("edit_initial");
        if (isEdit || intent.hasExtra("initial")) {
            String p = isEdit ? "edit_" : "";
            updateField(etInitial, intent.getDoubleExtra(p + "initial", 0));
            updateField(etMonthly, intent.getDoubleExtra(p + "monthly", 0));
            updateField(etRate, intent.getDoubleExtra(p + "rate", 0));
            updateField(etYears, intent.getIntExtra(p + "years", 0));
            updateField(etMonths, intent.getIntExtra(p + "months", 0));
            updateField(etFees, intent.getDoubleExtra(p + "fees", 0));
            if (intent.hasExtra("currency")) {
                currencySymbol = intent.getStringExtra("currency");
                tvCurrencySymbol.setText(currencySymbol);
                currentCurrency = currencySymbol.equals("$") ? "USD" : currencySymbol.equals("€") ? "EUR" : "ILS";
            }
            calculateInvestment();
        }
    }

    private void updateField(EditText et, double value) {
        if (et == null || value == 0) return;
        et.setText(value == (long) value ? String.valueOf((long) value) : String.valueOf(value));
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}