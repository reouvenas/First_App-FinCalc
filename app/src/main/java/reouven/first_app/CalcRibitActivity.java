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

        // 2. הגדרת מאזינים (לחיצות על כפתורים) - זה מה שהיה חסר!
        setupClickListeners();

        // 3. הגדרת הטול-בר העליון (חץ חזור ותפריט)
        setupTopBar();

        // 4. חיבור התפריט התחתון
        setupBottomNavigation();

        // 5. עדכון שערי חליפין מהאינטרנט
        fetchExchangeRates();
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

        // כפתור המטבע
        btnCurrency = findViewById(R.id.btnCurrency);
        // כפתור הגרף (שמירה)
        btnSavePlan = findViewById(R.id.btnSavePlan);

        if (tvResult != null) tvResult.setVisibility(View.GONE);

        if (btnDetails != null) {
            btnDetails.setEnabled(false);
            btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E8F5E9")));
            btnDetails.setTextColor(Color.parseColor("#A5D6A7"));
        }
    }

    private void setupTopBar() {
        // חץ חזור בטול-בר
        View btnBackHeader = findViewById(R.id.btnBackHeader);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> finish());
        }

        // כפתור תפריט הגדרות בטול-בר
        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                // כאן אפשר להוסיף PopupMenu עם "יצירת קשר", "אודות" וכו'
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
        // כפתור בחירת מטבע
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> showCurrencyDialog());
        }

        // כפתור חשב
        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> calculateInvestment());
        }

        // כפתור פירוט תוצאה (Details)
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

        // כפתור ה-i ליד דמי ניהול
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("הסבר דמי ניהול")
                        .setMessage("דמי הניהול מופחתים מהריבית השנתית.\nלדוגמה: ריבית של 10% עם דמי ניהול של 1% תחושב כריבית של 9%.")
                        .setPositiveButton("הבנתי", null).show();
            });
        }

        // כפתור שמירה (הגרף)
        if (btnSavePlan != null) {
            btnSavePlan.setOnClickListener(v -> {
                if (tvResult.getVisibility() == View.GONE) {
                    Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
                } else {
                    // כאן יופעל הדיאלוג שכבר כתבנו בדפים הקודמים
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
                    // אם כבר נעשה חישוב, נעדכן אותו למטבע החדש
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

            if (totalMonths <= 0) {
                Toast.makeText(this, "הזן תקופת זמן", Toast.LENGTH_SHORT).show();
                return;
            }

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

            // הפעלת כפתור הפירוט
            if (btnDetails != null) {
                btnDetails.setEnabled(true);
                btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                btnDetails.setTextColor(Color.WHITE);
            }
        } catch (Exception e) {
            Toast.makeText(this, "מלא את כל השדות בצורה תקינה", Toast.LENGTH_SHORT).show();
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