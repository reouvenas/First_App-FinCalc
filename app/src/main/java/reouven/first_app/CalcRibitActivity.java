package reouven.first_app;

import android.content.Intent;
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
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView; // ייבוא חשוב
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Scanner;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails;
    private ImageView btnInfoFees, btnCurrency;
    private String currentCurrency = "ILS";
    private String currencySymbol = "₪";
    private double usdRate = 3.75;
    private double eurRate = 4.05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupClickListeners();
        setupTopBar();
        setupBottomNavigation();
        fetchExchangeRates();

        checkIntentExtras();
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

    private void setupTopBar() {
        View btnBackHeader = findViewById(R.id.btnBackHeader);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> finish());
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
                    } else if (id == R.id.menu_profile) {
                        Toast.makeText(this, "פרופיל אישי (בקרוב)", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (id == R.id.menu_about) {
                        showAboutDialog();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void toggleDarkMode() {
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("מחשבון ריבית דריבית חכם.\nגרסה 1.0")
                .setPositiveButton("סגור", null)
                .show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            // תיקון 1: גורם לשמות להופיע תמיד בכל הלחצנים
            bottomNav.setLabelVisibilityMode(NavigationBarView.LABEL_VISIBILITY_LABELED);

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    // תיקון 2: פתיחת הצ'אט
                    startActivity(new Intent(this, ChatActivity.class));
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

            if (totalMonths <= 0) {
                Toast.makeText(this, "נא להזין תקופת זמן", Toast.LENGTH_SHORT).show();
                return;
            }

            double r = ((annualRate - annualFees) / 100) / 12;
            double total;

            if (r != 0) {
                total = principal * Math.pow(1 + r, totalMonths) +
                        monthlyDeposit * (Math.pow(1 + r, totalMonths) - 1) / r;
            } else {
                total = principal + (monthlyDeposit * totalMonths);
            }

            double finalAmount = total;
            if (currentCurrency.equals("USD")) finalAmount /= usdRate;
            else if (currentCurrency.equals("EUR")) finalAmount /= eurRate;

            tvResult.setText("סכום צפוי: " + currencySymbol + String.format(Locale.US, "%,.2f", finalAmount));
            tvResult.setVisibility(View.VISIBLE);
            setDetailsButtonEnabled(true);
        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בחישוב", Toast.LENGTH_SHORT).show();
        }
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void checkIntentExtras() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("initial")) {
            etInitial.setText(String.valueOf(intent.getDoubleExtra("initial", 0)));
            etMonthly.setText(String.valueOf(intent.getDoubleExtra("monthly", 0)));
            etRate.setText(String.valueOf(intent.getDoubleExtra("rate", 0)));
            etYears.setText(String.valueOf(intent.getIntExtra("years", 0)));
            etMonths.setText(String.valueOf(intent.getIntExtra("months", 0)));
            etFees.setText(String.valueOf(intent.getDoubleExtra("fees", 0)));
            currencySymbol = intent.getStringExtra("currency");
            if (tvCurrencySymbol != null) tvCurrencySymbol.setText(currencySymbol);
            calculateInvestment();
        }
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}