package reouven.first_app;

import android.content.Intent;
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
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // הסתרת ה-ActionBar המובנה (כי יש לנו טול-בר מעוצב ב-XML)
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. קבלת הנתונים מהאינטנט
        double initial = getIntent().getDoubleExtra("initial", 0);
        double monthly = getIntent().getDoubleExtra("monthly", 0);
        double rate = getIntent().getDoubleExtra("rate", 0);
        int years = getIntent().getIntExtra("years", 0);
        int extraMonths = getIntent().getIntExtra("months", 0);
        double fees = getIntent().getDoubleExtra("fees", 0);
        String currencySymbol = getIntent().getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        int totalMonths = (years * 12) + extraMonths;

        // 2. חישוב ריבית דריבית
        double netAnnualRate = (rate - fees) / 100;
        double monthlyRate = netAnnualRate / 12;

        double finalBalance;
        if (monthlyRate != 0) {
            double principalGrowth = initial * Math.pow(1 + monthlyRate, totalMonths);
            double depositsGrowth = monthly * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
            finalBalance = principalGrowth + depositsGrowth;
        } else {
            finalBalance = initial + (monthly * totalMonths);
        }

        double totalInvested = initial + (monthly * totalMonths);
        double totalProfit = finalBalance - totalInvested;

        // 3. הצגת הנתונים בטקסטים
        displayData(initial, monthly, totalMonths, rate, totalInvested, totalProfit, finalBalance, currencySymbol);

        // 4. הגדרת כפתורים, טול-בר ותפריט תחתון
        setupButtons(initial, monthly, rate, years, extraMonths, fees, currencySymbol);
        setupTopBar();
        setupBottomNavigation();
    }

    private void setupTopBar() {
        // חיבור חץ החזור מה-include layout של ה-top_bar
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, CalcRibitActivity.class));
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

    private void setupButtons(double initial, double monthly, double rate, int years, int months, double fees, String currencySymbol) {
        // כפתור לצפייה בגרף
        Button btnViewChart = findViewById(R.id.btnViewChart);
        if (btnViewChart != null) {
            btnViewChart.setOnClickListener(v -> {
                Intent intent = new Intent(DetailsActivity.this, GraphActivity.class);
                intent.putExtra("initial", initial);
                intent.putExtra("monthly", monthly);
                intent.putExtra("rate", rate);
                intent.putExtra("years", years);
                intent.putExtra("months", months);
                intent.putExtra("fees", fees);
                startActivity(intent);
            });
        }

        // כפתור עריכה - מחזיר למחשבון עם הנתונים כדי שיוכל לשנות
        Button btnEdit = findViewById(R.id.btnEdit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(DetailsActivity.this, CalcRibitActivity.class);
                intent.putExtra("initial", initial);
                intent.putExtra("monthly", monthly);
                intent.putExtra("rate", rate);
                intent.putExtra("years", years);
                intent.putExtra("months", months);
                intent.putExtra("fees", fees);
                intent.putExtra("currency", currencySymbol);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // חוזר למחשבון ומנקה את הדרך
                startActivity(intent);
            });
        }

        // כפתור שמירה
        Button btnSaveTable = findViewById(R.id.btnSaveTable);
        if (btnSaveTable != null) {
            btnSaveTable.setOnClickListener(v -> showSaveDialog());
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שמירת תוכנית השקעה");

        final EditText input = new EditText(this);
        input.setHint("תן שם לתוכנית (למשל: יעד לדירה)");
        builder.setView(input);

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String planName = input.getText().toString().trim();
            if (!planName.isEmpty()) {
                savePlanToFirestore(planName);
            } else {
                Toast.makeText(this, "אנא הזן שם לתוכנית", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void savePlanToFirestore(String planName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> plan = new HashMap<>();
        plan.put("planName", planName);
        plan.put("initial", getIntent().getDoubleExtra("initial", 0));
        plan.put("monthly", getIntent().getDoubleExtra("monthly", 0));
        plan.put("rate", getIntent().getDoubleExtra("rate", 0));
        plan.put("years", getIntent().getIntExtra("years", 0));
        plan.put("months", getIntent().getIntExtra("months", 0));
        plan.put("fees", getIntent().getDoubleExtra("fees", 0));
        plan.put("currency", getIntent().getStringExtra("currency"));
        plan.put("timestamp", System.currentTimeMillis());

        db.collection("saved_plans")
                .add(plan)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "התוכנית '" + planName + "' נשמרה בהיסטוריה!", Toast.LENGTH_LONG).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void displayData(double initial, double monthly, int totalMonths, double rate,
                             double invested, double profit, double totalFinal, String symbol) {

        String formattedInitial = symbol + String.format(Locale.US, "%,.0f", initial);
        String formattedMonthly = symbol + String.format(Locale.US, "%,.0f", monthly);
        String formattedInvested = symbol + String.format(Locale.US, "%,.0f", invested);
        String formattedProfit = symbol + String.format(Locale.US, "%,.0f", profit);
        String formattedTotal = symbol + String.format(Locale.US, "%,.0f", totalFinal);

        if (findViewById(R.id.tvSumInitial) != null)
            ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + formattedInitial);

        if (findViewById(R.id.tvSumMonthly) != null)
            ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + formattedMonthly);

        if (findViewById(R.id.tvSumPeriod) != null)
            ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + (totalMonths / 12) + " שנים ו-" + (totalMonths % 12) + " חודשים");

        if (findViewById(R.id.tvSumRate) != null)
            ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");

        if (findViewById(R.id.tvFinalInvested) != null)
            ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך ההשקעה בפועל: " + formattedInvested);

        if (findViewById(R.id.tvFinalProfit) != null)
            ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך הרווח הצפוי: " + formattedProfit);

        if (findViewById(R.id.tvFinalTotal) != null)
            ((TextView)findViewById(R.id.tvFinalTotal)).setText("ערך עתידי ברוטו: " + formattedTotal);
    }
}