package reouven.first_app;

import android.content.Intent;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
    private ImageView btnInfoFees, btnBack, btnCurrency, btnSavePlan;

    // משתני המטבע
    private String currentCurrency = "ILS";
    private String currencySymbol = "₪";
    private double usdRate = 3.75;
    private double eurRate = 4.05;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // 1. אתחול הרכיבים (חיבור ל-XML)
        initViews();

        // 2. הגדרת מאזינים (לחיצות על כפתורים)
        setupClickListeners();

        // 3. הגדרת הטול-בר העליון (חץ חזור ותפריט)
        setupTopBar();

        // 4. חיבור התפריט התחתון
        setupBottomNavigation();

        // 5. עדכון שערי חליפין מהאינטרנט
        fetchExchangeRates();

        // 6. בדיקה אם הגענו במצב "עריכה" ומילוי שדות אוטומטי
        checkIntentExtras();
    }

    // --- הפונקציה החדשה למילוי שדות בעריכה ---
    private void checkIntentExtras() {
        Intent intent = getIntent();
        // בודקים אם הגיע נתון אחד (למשל סכום התחלתי) כדי לדעת אם אנחנו במצב עריכה
        if (intent != null && intent.hasExtra("initial")) {

            // הצבת הנתונים בתיבות הטקסט
            etInitial.setText(String.valueOf(intent.getDoubleExtra("initial", 0)));
            etMonthly.setText(String.valueOf(intent.getDoubleExtra("monthly", 0)));
            etRate.setText(String.valueOf(intent.getDoubleExtra("rate", 0)));
            etYears.setText(String.valueOf(intent.getIntExtra("years", 0)));
            etMonths.setText(String.valueOf(intent.getIntExtra("months", 0)));
            etFees.setText(String.valueOf(intent.getDoubleExtra("fees", 0)));

            // עדכון המטבע אם נשלח
            String savedSymbol = intent.getStringExtra("currency");
            if (savedSymbol != null) {
                currencySymbol = savedSymbol;
                if (tvCurrencySymbol != null) tvCurrencySymbol.setText(currencySymbol);
            }

            // הרצת חישוב אוטומטי כדי שהתוצאה תופיע מיד
            calculateInvestment();

            Toast.makeText(this, "הנתונים נטענו לעריכה", Toast.LENGTH_SHORT).show();
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
        btnSavePlan = findViewById(R.id.btnSavePlan);

        if (tvResult != null) tvResult.setVisibility(View.GONE);

        if (btnDetails != null) {
            btnDetails.setEnabled(false);
            btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            btnDetails.setTextColor(Color.parseColor("#A5D6A7"));
        }
    }

    private void setupTopBar() {
        View btnBackHeader = findViewById(R.id.btnBackHeader);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> finish());
        }

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                Toast.makeText(this, "תפריט הגדרות", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) return true;
                if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                }
                if (id == R.id.nav_tips) {
                    startActivity(new Intent(this, TipsActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    private void setupClickListeners() {
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> showCurrencyDialog());
        }

        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> calculateInvestment());
        }

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

        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("הסבר דמי ניהול")
                        .setMessage("דמי הניהול מופחתים מהריבית השנתית.\nלדוגמה: ריבית של 10% עם דמי ניהול של 1% תחושב כריבית של 9%.")
                        .setPositiveButton("הבנתי", null).show();
            });
        }

        if (btnSavePlan != null) {
            btnSavePlan.setOnClickListener(v -> {
                if (tvResult.getVisibility() == View.GONE) {
                    Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "פתיחת ממשק שמירה...", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showCurrencyDialog() {
        String[] currencies = {"שקל (₪)", "דולר ($)", "אירו (€)"};
        new AlertDialog.Builder(this)
                .setTitle("בחר מטבע לחישוב")
                .setItems(currencies, (dialog, which) -> {
                    switch (which) {
                        case 0: currentCurrency = "ILS"; currencySymbol = "₪"; break;
                        case 1: currentCurrency = "USD"; currencySymbol = "$"; break;
                        case 2: currentCurrency = "EUR"; currencySymbol = "€"; break;
                    }
                    if (tvCurrencySymbol != null) tvCurrencySymbol.setText(currencySymbol);
                    if(tvResult.getVisibility() == View.VISIBLE) calculateInvestment();
                }).show();
    }

    private void calculateInvestment() {
        try {
            double principal = getDouble(etInitial);
            double monthlyDeposit = getDouble(etMonthly);
            double annualRate = getDouble(etRate);
            double annualFees = getDouble(etFees);
            int totalMonths = ((int) getDouble(etYears) * 12) + (int) getDouble(etMonths);

            if (totalMonths <= 0) return;

            double r = ((annualRate - annualFees) / 100) / 12;
            double totalInILS;
            if (r != 0) {
                totalInILS = principal * Math.pow(1 + r, totalMonths) +
                        monthlyDeposit * (Math.pow(1 + r, totalMonths) - 1) / r;
            } else {
                totalInILS = principal + (monthlyDeposit * totalMonths);
            }

            double finalDisplayAmount = totalInILS;
            if (currentCurrency.equals("USD")) finalDisplayAmount = totalInILS / usdRate;
            if (currentCurrency.equals("EUR")) finalDisplayAmount = totalInILS / eurRate;

            tvResult.setText("סכום צפוי: " + currencySymbol + String.format(Locale.US, "%,.2f", finalDisplayAmount));
            tvResult.setVisibility(View.VISIBLE);

            if (btnDetails != null) {
                btnDetails.setEnabled(true);
                btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                btnDetails.setTextColor(Color.WHITE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "מלא את השדות בצורה תקינה", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchExchangeRates() {
        new Thread(() -> {
            try {
                URL url = new URL("https://open.er-api.com/v6/latest/ILS");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                InputStream is = conn.getInputStream();
                Scanner s = new Scanner(is).useDelimiter("\\A");
                String result = s.hasNext() ? s.next() : "";
                JSONObject json = new JSONObject(result);
                JSONObject rates = json.getJSONObject("rates");
                usdRate = 1 / rates.getDouble("USD");
                eurRate = 1 / rates.getDouble("EUR");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}