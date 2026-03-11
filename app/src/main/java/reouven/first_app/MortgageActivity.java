package reouven.first_app;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;

public class MortgageActivity extends AppCompatActivity {

    private EditText etLoanAmount, etInterestRate, etYears, etPropertySize, etCityAvgPrice;
    private AutoCompleteTextView actvCity; // החלפנו לתיבת בחירת עיר
    private Button btnCalculate;
    private TextView tvResult, tvDealStatus;
    private BottomNavigationView bottomNavigationView;
    private ImageView btnBackHeader;

    // מאגר נתונים למחירי דירות ממוצעים למטר לפי עיר (נתונים לדוגמה)
    private HashMap<String, Integer> cityPrices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mortgage);

        initData(); // טעינת מחירי הערים
        initViews();
        setupNavigation(); // חיבור תפריט עליון ותחתון
        setupCityAutocomplete(); // הגדרת השלמה אוטומטית לעיר

        btnCalculate.setOnClickListener(v -> calculateMortgageLogic());
    }

    private void initData() {
        cityPrices.put("תל אביב", 60000);
        cityPrices.put("ירושלים", 35000);
        cityPrices.put("חיפה", 22000);
        cityPrices.put("ראשון לציון", 28000);
        cityPrices.put("פתח תקווה", 26000);
        cityPrices.put("נתניה", 25000);
        cityPrices.put("באר שבע", 16000);
    }

    private void initViews() {
        etLoanAmount = findViewById(R.id.etLoanAmount);
        etInterestRate = findViewById(R.id.etInterestRate);
        etYears = findViewById(R.id.etYears);
        etPropertySize = findViewById(R.id.etPropertySize);
        etCityAvgPrice = findViewById(R.id.etCityAvgPrice);
        actvCity = findViewById(R.id.actvCity); // וודא שה-ID ב-XML הוא actvCity
        btnCalculate = findViewById(R.id.btnCalculate);
        tvResult = findViewById(R.id.tvResult);
        tvDealStatus = findViewById(R.id.tvDealStatus);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        btnBackHeader = findViewById(R.id.btnBackHeader);
    }

    private void setupCityAutocomplete() {
        String[] cities = cityPrices.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        actvCity.setAdapter(adapter);

        // כשבוחרים עיר, המחיר למטר מתעדכן אוטומטית
        actvCity.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = (String) parent.getItemAtPosition(position);
            if (cityPrices.containsKey(selectedCity)) {
                etCityAvgPrice.setText(String.valueOf(cityPrices.get(selectedCity)));
            }
        });
    }

    private void setupNavigation() {
        // 1. חיבור כפתור חזור בתפריט העליון
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> finish());
        }

        // 2. חיבור תפריט תחתון - ניווט מלא
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_ai_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (id == R.id.nav_tips) {
                startActivity(new Intent(this, TipsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void calculateMortgageLogic() {
        try {
            String loanStr = etLoanAmount.getText().toString();
            String interestStr = etInterestRate.getText().toString();
            String yearsStr = etYears.getText().toString();

            if (loanStr.isEmpty() || interestStr.isEmpty() || yearsStr.isEmpty()) {
                Toast.makeText(this, "נא למלא נתוני משכנתא", Toast.LENGTH_SHORT).show();
                return;
            }

            double p = Double.parseDouble(loanStr);
            double annualRate = Double.parseDouble(interestStr);
            double r = (annualRate / 100) / 12;
            int n = Integer.parseInt(yearsStr) * 12;

            double monthlyPayment = (r > 0) ? (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1) : p / n;
            tvResult.setText(String.format("החזר חודשי: %.2f ₪", monthlyPayment));

            analyzePropertyDeal(p);

        } catch (Exception e) {
            tvResult.setText("שגיאה בחישוב");
        }
    }

    private void analyzePropertyDeal(double totalPrice) {
        String sizeStr = etPropertySize.getText().toString();
        String cityAvgStr = etCityAvgPrice.getText().toString();

        if (!sizeStr.isEmpty() && !cityAvgStr.isEmpty()) {
            double size = Double.parseDouble(sizeStr);
            double cityAvg = Double.parseDouble(cityAvgStr);
            double currentPricePerMeter = totalPrice / size;
            double diffPercent = ((currentPricePerMeter - cityAvg) / cityAvg) * 100;

            if (diffPercent > 5) {
                tvDealStatus.setText(String.format("הנכס יקר ב-%.1f%% מהממוצע", diffPercent));
                tvDealStatus.setTextColor(Color.RED);
            } else if (diffPercent < -5) {
                tvDealStatus.setText(String.format("הנכס זול ב-%.1f%% מהממוצע!", Math.abs(diffPercent)));
                tvDealStatus.setTextColor(Color.parseColor("#2E7D32"));
            } else {
                tvDealStatus.setText("מחיר הנכס תואם לממוצע");
                tvDealStatus.setTextColor(Color.BLUE);
            }
        }
    }
}