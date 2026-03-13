package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(this::showPopupMenu);
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
            } else if (id == R.id.menu_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.menu_contact) {
                NavigationHelper.showContactDialog(this);
            } else if (id == R.id.menu_about) {
                showAboutDialog();
            } else if (id == R.id.menu_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });
        popup.show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("InvestCalc - המחשבון הפיננסי שלך.\nגרסה 2.0\n\nכאן תוכל לראות את פירוט הרווחים וההפקדות שלך לאורך זמן.\n\nפותח על ידי: ראובן")
                .setPositiveButton("סגור", null).show();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            TextView[] tvs = {findViewById(R.id.tvSumInitial), findViewById(R.id.tvSumMonthly),
                    findViewById(R.id.tvSumPeriod), findViewById(R.id.tvSumRate),
                    findViewById(R.id.tvFinalInvested), findViewById(R.id.tvFinalProfit),
                    findViewById(R.id.tvFinalTotal)};
            for(TextView tv : tvs) if(tv != null) tv.setTextColor(Color.WHITE);
        }
    }

    // ... שאר פונקציות החישוב והשמירה (calculateResults, displayData, setupActionButtons, saveToFirebaseWithDialog, setupBottomNavigation) נשארות כפי שהיו ...
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
            String msg = "סיכום השקעה:\nסכום סופי: " + currencySymbol + String.format("%.0f", finalBalance);
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

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if(nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
            nav.setOnItemSelectedListener(item -> {
                if (item.getItemId() == R.id.nav_home) { finish(); return true; }
                return false;
            });
        }
    }
}