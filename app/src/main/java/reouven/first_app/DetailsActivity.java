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

        // קבלת הנתונים מה-Intent
        Intent intent = getIntent();
        initial = intent.getDoubleExtra("initial", 0);
        monthly = intent.getDoubleExtra("monthly", 0);
        rate = intent.getDoubleExtra("rate", 0);
        years = intent.getIntExtra("years", 0);
        extraMonths = intent.getIntExtra("months", 0);
        fees = intent.getDoubleExtra("fees", 0);
        currencySymbol = intent.getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        // חישוב והצגה
        int totalMonths = (years * 12) + extraMonths;
        calculateResults(totalMonths);
        displayData();

        // הגדרות רכיבים
        setupTopBar();
        setupBottomNavigation();
        setupActionButtons();

        // החלת צבעים ידנית (מצב לילה/יום)
        applyCustomColorMode();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        CardView card1 = findViewById(R.id.cardInputSummary);
        CardView card2 = findViewById(R.id.cardResultSummary);
        TextView title = findViewById(R.id.tvDetailsTitle);
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            title.setTextColor(Color.WHITE);
            card1.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            card2.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            nav.setBackgroundColor(Color.BLACK);
            updateTextInCards(Color.WHITE);
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            title.setTextColor(Color.parseColor("#1A237E"));
            card1.setCardBackgroundColor(Color.WHITE);
            card2.setCardBackgroundColor(Color.WHITE);
            updateTextInCards(Color.BLACK);
        }
    }

    private void updateTextInCards(int color) {
        ((TextView)findViewById(R.id.tvSumInitial)).setTextColor(color);
        ((TextView)findViewById(R.id.tvSumMonthly)).setTextColor(color);
        ((TextView)findViewById(R.id.tvSumPeriod)).setTextColor(color);
        ((TextView)findViewById(R.id.tvSumRate)).setTextColor(color);
        ((TextView)findViewById(R.id.tvFinalInvested)).setTextColor(color);
    }

    private void setupTopBar() {
        View btnBackHeader = findViewById(R.id.btnBackHeader);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> onBackPressed());
        }

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                        return true;
                    } else if (id == R.id.menu_contact) {
                        NavigationHelper.showContactDialog(this);
                        return true;
                    } else if (id == R.id.menu_logout) {
                        FirebaseAuth.getInstance().signOut();
                        Intent logoutIntent = new Intent(this, LoginActivity.class);
                        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(logoutIntent);
                        finish();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent nextIntent = null;

                if (id == R.id.nav_home) {
                    nextIntent = new Intent(this, HomeActivity.class);
                } else if (id == R.id.nav_ai_chat) {
                    nextIntent = new Intent(this, ChatActivity.class);
                } else if (id == R.id.nav_tips) {
                    nextIntent = new Intent(this, TipsActivity.class);
                }

                if (nextIntent != null) {
                    nextIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(nextIntent);
                    finish();
                    return true;
                }
                return true;
            });
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
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

    private void displayData() {
        String format = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, format, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, format, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " שנים ו-" + extraMonths + " חודשים");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "% (ניהול: " + fees + "%)");
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח צפוי: " + currencySymbol + String.format(Locale.US, format, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, format, totalInvested));
    }

    private void setupActionButtons() {
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent intent = new Intent(this, CalcRibitActivity.class);
            intent.putExtra("edit_initial", initial);
            intent.putExtra("edit_monthly", monthly);
            intent.putExtra("edit_rate", rate);
            intent.putExtra("edit_years", years);
            intent.putExtra("edit_months", extraMonths);
            intent.putExtra("edit_fees", fees);
            intent.putExtra("currency", currencySymbol);

            // תיקון: לא סוגרים את הדף ולא מנקים את ה-Stack כדי שנוכל לחזור לכאן
            startActivity(intent);
        });

        findViewById(R.id.btnShare).setOnClickListener(v -> createPdfAndShare());
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> saveToFirebaseWithDialog());
        findViewById(R.id.btnViewChart).setOnClickListener(v -> {
            Intent intent = new Intent(this, GraphActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        });
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("לדוגמה: חיסכון לילד");
        input.setGravity(android.view.Gravity.CENTER);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = params.rightMargin = 60;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("שמירת תוכנית")
                .setMessage("בחר שם לתוכנית:")
                .setView(container)
                .setPositiveButton("שמור", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    performActualSave(name.isEmpty() ? "תוכנית ללא שם" : name);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void performActualSave(String planName) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> plan = new HashMap<>();
        plan.put("planName", planName);
        plan.put("initial", initial);
        plan.put("monthly", monthly);
        plan.put("rate", rate);
        plan.put("years", years);
        plan.put("months", extraMonths);
        plan.put("fees", fees);
        plan.put("currency", currencySymbol);
        plan.put("timestamp", System.currentTimeMillis());
        db.collection("saved_plans").add(plan)
                .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
    }

    private void createPdfAndShare() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 450, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Paint paint = new Paint();
        paint.setTextSize(12f);
        page.getCanvas().drawText("דו''ח השקעה - InvestCalc", 20, 40, paint);
        document.finishPage(page);
        File file = new File(getExternalFilesDir(null), "Report.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "שתף דו''ח"));
        } catch (IOException e) { e.printStackTrace(); }
    }
}