package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.Locale;

public class DetailsActivity extends AppCompatActivity {

    private double initial, monthly, rate, fees, finalBalance, totalInvested, totalProfit;
    private int years, extraMonths;
    private String currencySymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initial = getIntent().getDoubleExtra("initial", 0);
        monthly = getIntent().getDoubleExtra("monthly", 0);
        rate = getIntent().getDoubleExtra("rate", 0);
        years = getIntent().getIntExtra("years", 0);
        extraMonths = getIntent().getIntExtra("months", 0);
        fees = getIntent().getDoubleExtra("fees", 0);
        currencySymbol = getIntent().getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        int totalMonths = (years * 12) + extraMonths;
        calculateResults(totalMonths);
        displayData(totalMonths);
        setupTopBar();
        setupBottomNavigation();
        setupActionButtons();
    }

    private void calculateResults(int totalMonths) {
        double netAnnualRate = (rate - fees) / 100;
        double monthlyRate = netAnnualRate / 12;
        if (monthlyRate != 0) {
            finalBalance = initial * Math.pow(1 + monthlyRate, totalMonths) +
                    monthly * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
        } else {
            finalBalance = initial + (monthly * totalMonths);
        }
        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;
    }

    private void setupActionButtons() {
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalcRibitActivity.class);
            intent.putExtra("initial", initial);
            intent.putExtra("monthly", monthly);
            intent.putExtra("rate", rate);
            intent.putExtra("years", years);
            intent.putExtra("months", extraMonths);
            intent.putExtra("fees", fees);
            intent.putExtra("currency", currencySymbol);
            startActivity(intent);
            // לא עושים finish() כאן כדי שהדף יישאר בזיכרון והחזור מהמחשבון יגיע אליו
        });

        findViewById(R.id.btnShare).setOnClickListener(v -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "הרווח הצפוי שלי: " + currencySymbol + String.format("%.0f", totalProfit));
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, "שתף"));
        });
    }

    private void displayData(int totalMonths) {
        String format = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, format, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, format, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + (totalMonths / 12) + " שנים ו-" + (totalMonths % 12) + " חודשים");
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח צפוי: " + currencySymbol + String.format(Locale.US, format, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}