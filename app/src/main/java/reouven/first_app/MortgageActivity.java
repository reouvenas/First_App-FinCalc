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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MortgageActivity extends AppCompatActivity {

    private EditText etLoanAmount, etInterestRate, etYears, etPropertySize, etFullPropertyPrice, etCityAvgPrice;
    private AutoCompleteTextView actvCity;
    private Button btnCalculate, btnSavePlan;
    private TextView tvResult, tvDealStatus;
    private BottomNavigationView bottomNav;
    private HashMap<String, Integer> cityPrices = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // קריטי: בודק אם המצב כהה *לפני* שה-Layout נטען
        checkAndApplyDarkMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mortgage);

        initData();
        initViews();
        setupTopBar();
        setupBottomNav();

        btnCalculate.setOnClickListener(v -> calculateLogic());
        btnSavePlan.setOnClickListener(v -> showSaveDialog());

        if (getIntent().getBooleanExtra("isFromHistory", false)) {
            loadDataFromHistory();
        }
    }

    private void initViews() {
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
        bottomNav = findViewById(R.id.bottom_navigation);

        String[] cities = cityPrices.keySet().toArray(new String[0]);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        actvCity.setAdapter(adapter);
    }

    private void setupTopBar() {
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());

                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();

                    if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                        return true;
                    } else if (id == R.id.menu_logout) {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                        return true;
                    } else if (id == R.id.menu_about) {
                        showAboutDialog();
                        return true;
                    } else if (id == R.id.menu_contact) {
                        Toast.makeText(this, "פתיחת יצירת קשר...", Toast.LENGTH_SHORT).show();
                        // כאן אפשר להוסיף Intent למייל אם תרצה
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        if (isDark) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();

        // זה מה שיגרום לדף להתחלף *עכשיו* ולא רק אחרי הפעלה מחדש
        recreate();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("גרסה 1.0\nמחשבון פיננסי מתקדם.\nכל הזכויות שמורות לראובן.")
                .setPositiveButton("הבנתי", null)
                .show();
    }

    private void initData() {
        cityPrices.put("תל אביב", 60000);
        cityPrices.put("ירושלים", 35000);
        cityPrices.put("חיפה", 22000);
        cityPrices.put("ראשון לציון", 28000);
    }

    private void setupBottomNav() {
        bottomNav.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_home) {
                finish();
                return true;
            }
            return false;
        });
    }

    private void calculateLogic() {
        try {
            double p = Double.parseDouble(etLoanAmount.getText().toString());
            double r = (Double.parseDouble(etInterestRate.getText().toString()) / 100) / 12;
            int n = Integer.parseInt(etYears.getText().toString()) * 12;
            if (r == 0) {
                tvResult.setText(String.format("החזר חודשי: %.0f ₪", p/n));
            } else {
                double monthly = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
                tvResult.setText(String.format("החזר חודשי: %.0f ₪", monthly));
            }
        } catch (Exception e) {
            Toast.makeText(this, "נא למלא את נתוני ההלוואה", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSaveDialog() {
        if (tvResult.getText().toString().isEmpty()) return;
        EditText input = new EditText(this);
        input.setHint("שם לתוכנית");
        new AlertDialog.Builder(this).setTitle("שמירה").setView(input)
                .setPositiveButton("שמור", (d, w) -> savePlan(input.getText().toString())).show();
    }

    private void savePlan(String name) {
        String uid = FirebaseAuth.getInstance().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("userId", uid);
        data.put("planName", name.isEmpty() ? "חישוב משכנתא" : name);
        data.put("type", "mortgage");
        data.put("loanAmount", Double.parseDouble(etLoanAmount.getText().toString()));
        data.put("interest", Double.parseDouble(etInterestRate.getText().toString()));
        data.put("years", Integer.parseInt(etYears.getText().toString()));
        data.put("fullPrice", Double.parseDouble(etFullPropertyPrice.getText().toString()));
        data.put("propertySize", Double.parseDouble(etPropertySize.getText().toString()));
        data.put("cityAvgPrice", Double.parseDouble(etCityAvgPrice.getText().toString()));
        data.put("city", actvCity.getText().toString());
        data.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר בהיסטוריה!", Toast.LENGTH_SHORT).show());
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