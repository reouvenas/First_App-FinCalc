package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // 1. קבלת נתונים מה-Intent
        initial = getIntent().getDoubleExtra("initial", 0);
        monthly = getIntent().getDoubleExtra("monthly", 0);
        rate = getIntent().getDoubleExtra("rate", 0);
        years = getIntent().getIntExtra("years", 0);
        extraMonths = getIntent().getIntExtra("months", 0);
        fees = getIntent().getDoubleExtra("fees", 0);
        currencySymbol = getIntent().getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        int totalMonths = (years * 12) + extraMonths;

        // 2. חישובים מתמטיים
        double netAnnualRate = (rate - fees) / 100;
        double monthlyRate = netAnnualRate / 12;

        if (monthlyRate != 0) {
            double principalGrowth = initial * Math.pow(1 + monthlyRate, totalMonths);
            double depositsGrowth = monthly * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
            finalBalance = principalGrowth + depositsGrowth;
        } else {
            finalBalance = initial + (monthly * totalMonths);
        }

        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;

        // 3. הצגת נתונים ב-UI
        displayData(totalMonths);

        // 4. אתחול מערכות ניווט וכפתורים
        setupTopBar();
        setupBottomNavigation();
        setupButtons();
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                Toast.makeText(this, "פרופיל אישי", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.menu_about) {
                new AlertDialog.Builder(this).setTitle("אודות").setMessage("מחשבון השקעות חכם\nפותח על ידי ראובן").show();
            } else if (id == R.id.menu_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
            return true;
        });
        popup.show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            if (getIntent().getBooleanExtra("isFromHistory", false)) {
                bottomNav.setSelectedItemId(R.id.nav_history);
            } else {
                bottomNav.setSelectedItemId(0); // לא מסומן כבית כי אנחנו בפירוט
            }

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, CalcRibitActivity.class);
                else if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupButtons() {
        // כפתור גרף
        findViewById(R.id.btnViewChart).setOnClickListener(v -> {
            Intent intent = new Intent(this, GraphActivity.class);
            intent.putExtra("initial", initial);
            intent.putExtra("monthly", monthly);
            intent.putExtra("rate", rate);
            intent.putExtra("years", years);
            intent.putExtra("months", extraMonths);
            intent.putExtra("fees", fees);
            startActivity(intent);
        });

        // כפתור עריכה
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
            finish();
        });

        // כפתור שיתוף - הוספתי לוגיקה
        findViewById(R.id.btnShare).setOnClickListener(v -> shareResults());

        // כפתור שמירה - חיבור ל-Firestore
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> showSaveDialog());
    }

    private void shareResults() {
        String shareBody = String.format(Locale.getDefault(),
                "סיכום השקעה מ-InvestCalc:\nסכום התחלתי: %s%.0f\nצפי עתידי: %s%.0f\nרווח צפוי: %s%.0f",
                currencySymbol, initial, currencySymbol, finalBalance, currencySymbol, totalProfit);

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        startActivity(Intent.createChooser(sharingIntent, "שתף באמצעות"));
    }

    private void showSaveDialog() {
        EditText input = new EditText(this);
        input.setHint("שם התוכנית (למשל: חיסכון לילדים)");
        new AlertDialog.Builder(this)
                .setTitle("שמירת תוכנית")
                .setView(input)
                .setPositiveButton("שמור", (dialog, which) -> saveToFirestore(input.getText().toString()))
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void saveToFirestore(String name) {
        if (name.isEmpty()) {
            Toast.makeText(this, "נא להזין שם", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> plan = new HashMap<>();
        plan.put("planName", name);
        plan.put("initial", initial);
        plan.put("monthly", monthly);
        plan.put("rate", rate);
        plan.put("years", years);
        plan.put("months", extraMonths);
        plan.put("fees", fees);
        plan.put("currency", currencySymbol);
        plan.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("saved_plans").add(plan)
                .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר בהצלחה!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בשמירה", Toast.LENGTH_SHORT).show());
    }

    private void displayData(int totalMonths) {
        String format = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, format, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, format, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + (totalMonths / 12) + " שנים ו-" + (totalMonths % 12) + " חודשים");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך ההשקעה בפועל: " + currencySymbol + String.format(Locale.US, format, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך הרווח הצפוי: " + currencySymbol + String.format(Locale.US, format, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("ערך עתידי ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
    }
}