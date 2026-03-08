package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.FileProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnMenuHeader).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                } else if (id == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
                else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                return true;
            });
        }
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) mainLayout.setBackgroundColor(Color.BLACK);
    }

    private void calculateResults(int totalMonths) {
        double monthlyRate = ((rate - fees) / 100) / 12;
        if (monthlyRate != 0) {
            finalBalance = initial * Math.pow(1 + monthlyRate, totalMonths) + monthly * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
        } else finalBalance = initial + (monthly * totalMonths);
        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;
    }

    private void displayData() {
        String format = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, format, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, format, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " שנים ו-" + extraMonths + " חודשים");
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
    }

    private void setupActionButtons() {
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalcRibitActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        });
        findViewById(R.id.btnViewChart).setOnClickListener(v -> {
            Intent intent = new Intent(this, GraphActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        });
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> saveToFirebaseWithDialog());
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("שם לתוכנית");
        new AlertDialog.Builder(this).setTitle("שמירה").setView(input).setPositiveButton("שמור", (d, w) -> {
            FirebaseFirestore.getInstance().collection("saved_plans").add(new HashMap<String, Object>(){{
                put("planName", input.getText().toString());
                put("initial", initial);
                put("timestamp", System.currentTimeMillis());
            }}).addOnSuccessListener(doc -> Toast.makeText(DetailsActivity.this, "נשמר!", Toast.LENGTH_SHORT).show());
        }).show();
    }
}