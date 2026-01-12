package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;

public class DetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        // 1. קבלת הנתונים מהמחשבון (CalcRibitActivity)
        double initial = getIntent().getDoubleExtra("INITIAL_SUM", 0);
        double monthly = getIntent().getDoubleExtra("MONTHLY_SUM", 0);
        double rate = getIntent().getDoubleExtra("RATE", 0);
        int years = getIntent().getIntExtra("YEARS", 0);
        double fees = getIntent().getDoubleExtra("FEES", 0);

        // 2. חישוב ריבית דריבית מדויק (לפי נוסחת צבירה חודשית)
        double netAnnualRate = (rate - fees) / 100;
        double monthlyRate = netAnnualRate / 12;
        int totalMonths = years * 12;

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

        // 3. הצגת הנתונים במסך (לפי ה-IDs ב-XML שלך)
        displayData(initial, monthly, years, rate, totalInvested, totalProfit, finalBalance);

        // 4. הגדרת כפתור "לצפייה בגרף"
        Button btnViewChart = findViewById(R.id.btnViewChart);
        if (btnViewChart != null) {
            btnViewChart.setOnClickListener(v -> {
                Intent intent = new Intent(DetailsActivity.this, GraphActivity.class);
                intent.putExtra("INITIAL_SUM", initial);
                intent.putExtra("MONTHLY_SUM", monthly);
                intent.putExtra("RATE", rate);
                intent.putExtra("YEARS", years);
                intent.putExtra("FEES", fees);
                startActivity(intent);
            });
        }

        // כפתור עריכה/חזרה
        if (findViewById(R.id.btnEdit) != null) {
            findViewById(R.id.btnEdit).setOnClickListener(v -> finish());
        }
    }

    private void displayData(double initial, double monthly, int years, double rate,
                             double invested, double profit, double totalFinal) {

        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("iw", "IL"));

        // חלק סיכום השקעה (הנתונים שהוזנו)
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + nf.format(initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + nf.format(monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " שנים");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");

        // חלק תוצאה סופית (החישובים)
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך ההשקעה בפועל: " + nf.format(invested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך הרווח הצפוי: " + nf.format(profit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("ערך עתידי ברוטו: " + nf.format(totalFinal));
    }
}