package reouven.first_app;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

        // קבלת נתונים
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

        // אנימציה קטנה של כניסה לטקסט הרווח
        animateResult();
    }

    private void animateResult() {
        TextView tvProfit = findViewById(R.id.tvFinalProfit);
        Animation anim = new AlphaAnimation(0.0f, 1.0f);
        anim.setDuration(1000);
        tvProfit.startAnimation(anim);
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
        findViewById(R.id.btnEdit).setOnClickListener(v -> finish());

        findViewById(R.id.btnShare).setOnClickListener(v -> createPdfAndShare());
    }

    private void createPdfAndShare() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(14);

        canvas.drawText("דו''ח סיכום השקעה", 80, 50, paint);
        canvas.drawText("סכום סופי: " + String.format("%.2f", finalBalance) + currencySymbol, 20, 100, paint);
        canvas.drawText("סך רווח: " + String.format("%.2f", totalProfit) + currencySymbol, 20, 130, paint);
        canvas.drawText("תקופה: " + years + " שנים", 20, 160, paint);

        document.finishPage(page);

        File file = new File(getExternalFilesDir(null), "InvestmentReport.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF נוצר בהצלחה!", Toast.LENGTH_LONG).show();
            // כאן אפשר להוסיף קוד לשיתוף הקובץ
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה ביצירת PDF", Toast.LENGTH_SHORT).show();
        }
        document.close();
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
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    Toast.makeText(this, "צ'אט AI בקרוב!", Toast.LENGTH_SHORT).show();
                }
                return false;
            });
        }
    }
}