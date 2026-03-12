package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DetailsActivity extends AppCompatActivity {

    private double initial, monthly, rate, fees, finalBalance, totalInvested, totalProfit;
    private int years, extraMonths;
    private String currencySymbol;
    private View mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mainLayout = findViewById(R.id.main_layout);

        Intent intent = getIntent();
        initial = intent.getDoubleExtra("initial", 0);
        monthly = intent.getDoubleExtra("monthly", 0);
        rate = intent.getDoubleExtra("rate", 0);
        years = intent.getIntExtra("years", 0);
        extraMonths = intent.getIntExtra("months", 0);
        fees = intent.getDoubleExtra("fees", 0);
        currencySymbol = intent.getStringExtra("currency") != null ? intent.getStringExtra("currency") : "₪";

        calculateResults((years * 12) + extraMonths);
        displayData();
        setupTopBar();
        setupBottomNavigation();
        setupActionButtons();
        applyCustomColorMode();
    }

    private void calculateResults(int totalMonths) {
        double monthlyRate = ((rate - fees) / 100) / 12;
        if (monthlyRate != 0) {
            finalBalance = initial * Math.pow(1 + monthlyRate, totalMonths) +
                    monthly * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
        } else {
            finalBalance = initial + (monthly * totalMonths);
        }
        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;
    }

    private void displayData() {
        String format = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, format, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, format, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " שנים ו-" + extraMonths + " חודשים");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");

        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, format, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח צפוי: " + currencySymbol + String.format(Locale.US, format, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
    }

    private void setupActionButtons() {
        findViewById(R.id.btnEdit).setOnClickListener(v -> finish());

        findViewById(R.id.btnShare).setOnClickListener(v -> {
            String msg = "סיכום השקעה:\nסכום סופי: " + currencySymbol + String.format("%.0f", finalBalance) + "\nרווח צפוי: " + currencySymbol + String.format("%.0f", totalProfit);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            startActivity(Intent.createChooser(intent, "שתף תוצאות"));
        });

        findViewById(R.id.btnSaveTable).setOnClickListener(v -> saveToFirebaseWithDialog());
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("שם לתוכנית");
        new AlertDialog.Builder(this).setTitle("שמירה").setView(input).setPositiveButton("שמור", (d, w) -> {
            String name = input.getText().toString();
            String uid = FirebaseAuth.getInstance().getUid();
            Map<String, Object> data = new HashMap<>();
            data.put("userId", uid);
            data.put("planName", name.isEmpty() ? "חישוב השקעה" : name);
            data.put("type", "investment");
            data.put("initial", initial);
            data.put("monthly", monthly);
            data.put("rate", rate);
            data.put("years", years);
            data.put("months", extraMonths);
            data.put("fees", fees);
            data.put("currency", currencySymbol);
            data.put("timestamp", System.currentTimeMillis());

            FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                    .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
        }).show();
    }

    private void setupTopBar() { findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish()); }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_history);
        nav.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) { finish(); return true; }
            return false;
        });
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) mainLayout.setBackgroundColor(Color.BLACK);
    }
}