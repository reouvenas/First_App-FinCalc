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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
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
    private FirebaseAuth mAuth;
    private View mainLayout;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mAuth = FirebaseAuth.getInstance();
        mainLayout = findViewById(R.id.main_layout);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // קבלת נתונים מה-Intent
        Intent intent = getIntent();
        initial = intent.getDoubleExtra("initial", 0);
        monthly = intent.getDoubleExtra("monthly", 0);
        rate = intent.getDoubleExtra("rate", 0);
        years = intent.getIntExtra("years", 0);
        extraMonths = intent.getIntExtra("months", 0);
        fees = intent.getDoubleExtra("fees", 0);
        currencySymbol = intent.getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

        calculateResults((years * 12) + extraMonths);
        displayData();

        setupTopBar();
        setupBottomNavigation();
        setupActionButtons();
        applyCustomColorMode();
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        TextView tvTitle = findViewById(R.id.textViewTitle);
        TextView tv1 = findViewById(R.id.tvSumInitial);
        TextView tv2 = findViewById(R.id.tvSumMonthly);
        TextView tv3 = findViewById(R.id.tvSumPeriod);
        TextView tv4 = findViewById(R.id.tvSumRate);
        TextView tv5 = findViewById(R.id.tvFinalInvested);
        TextView tv6 = findViewById(R.id.tvFinalProfit);
        TextView tv7 = findViewById(R.id.tvFinalTotal);

        CardView card1 = findViewById(R.id.cardSummary);
        CardView card2 = findViewById(R.id.cardFinal);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (card1 != null) card1.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            if (card2 != null) card2.setCardBackgroundColor(Color.parseColor("#1E1E1E"));

            setTextColorIfNotNull(tvTitle, Color.WHITE);
            setTextColorIfNotNull(tv1, Color.WHITE);
            setTextColorIfNotNull(tv2, Color.WHITE);
            setTextColorIfNotNull(tv3, Color.WHITE);
            setTextColorIfNotNull(tv4, Color.WHITE);
            setTextColorIfNotNull(tv5, Color.WHITE);
            setTextColorIfNotNull(tv6, Color.parseColor("#4CAF50"));
            setTextColorIfNotNull(tv7, Color.WHITE);

            if (bottomNav != null) bottomNav.setBackgroundColor(Color.parseColor("#121212"));
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F5F5"));
            if (card1 != null) card1.setCardBackgroundColor(Color.WHITE);
            if (card2 != null) card2.setCardBackgroundColor(Color.WHITE);

            setTextColorIfNotNull(tvTitle, Color.parseColor("#1A237E"));
            setTextColorIfNotNull(tv1, Color.BLACK);
            setTextColorIfNotNull(tv2, Color.BLACK);
            setTextColorIfNotNull(tv3, Color.BLACK);
            setTextColorIfNotNull(tv4, Color.BLACK);
            setTextColorIfNotNull(tv5, Color.BLACK);
            setTextColorIfNotNull(tv6, Color.parseColor("#2E7D32"));
            setTextColorIfNotNull(tv7, Color.parseColor("#1A237E"));
        }
    }

    private void setTextColorIfNotNull(TextView tv, int color) {
        if (tv != null) tv.setTextColor(color);
    }

    private void setupActionButtons() {
        // צפייה בגרף
        findViewById(R.id.btnViewChart).setOnClickListener(v -> {
            Intent gIntent = new Intent(this, GraphActivity.class);
            gIntent.putExtra("initial", initial);
            gIntent.putExtra("monthly", monthly);
            gIntent.putExtra("rate", rate);
            gIntent.putExtra("years", years);
            gIntent.putExtra("months", extraMonths);
            gIntent.putExtra("fees", fees);
            gIntent.putExtra("currency", currencySymbol);
            startActivity(gIntent);
        });

        // כפתור עריכה - מחזיר למחשבון
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent calcIntent = new Intent(this, CalcRibitActivity.class);
            calcIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(calcIntent);
            finish();
        });

        // כפתור שמירה
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> saveToFirebaseWithDialog());

        // כפתור שיתוף PDF
        findViewById(R.id.btnShare).setOnClickListener(v -> exportToPDF());
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("למשל: חיסכון לדירה");

        new AlertDialog.Builder(this)
                .setTitle("תן שם לתוכנית")
                .setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if(name.isEmpty()) name = "תוכנית ללא שם";

                    Map<String, Object> data = new HashMap<>();
                    data.put("planName", name);
                    data.put("finalBalance", finalBalance);
                    data.put("timestamp", System.currentTimeMillis());

                    FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                            .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר בהיסטוריה!", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("ביטול", null).show();
    }

    private void exportToPDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);

        Paint paint = new Paint();
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        page.getCanvas().drawText("סיכום השקעה - InvestCalc", 40, 50, paint);

        paint.setFakeBoldText(false);
        paint.setTextSize(12);
        int y = 100;
        page.getCanvas().drawText("סכום התחלתי: " + currencySymbol + String.format("%.0f", initial), 40, y, paint);
        y += 30;
        page.getCanvas().drawText("הפקדה חודשית: " + currencySymbol + String.format("%.0f", monthly), 40, y, paint);
        y += 30;
        page.getCanvas().drawText("סה\"כ ברוטו צפוי: " + currencySymbol + String.format("%.0f", finalBalance), 40, y, paint);

        document.finishPage(page);

        File file = new File(getExternalFilesDir(null), "InvestmentSummary.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            shareFile(file);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "שגיאה ביצירת PDF", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "שתף סיכום כקובץ PDF"));
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_dark_mode) { toggleDarkMode(); return true; }
                    else if (id == R.id.menu_contact) { showContactDialog(); return true; }
                    else if (id == R.id.menu_about) { showAboutDialog(); return true; }
                    else if (id == R.id.menu_logout) { showLogoutDialog(); return true; }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\nפותח ע\"י ראובן\nגרסה: 1.0";
        new AlertDialog.Builder(this).setTitle("אודות InvestCalc").setMessage(aboutMessage).setPositiveButton("סגור", null).show();
    }

    private void showContactDialog() {
        new AlertDialog.Builder(this).setTitle("יצירת קשר").setMessage("צריכים עזרה? אנחנו כאן בשבילכם.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc");
                    try { startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:")); } catch (Exception e) { Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show(); }
                }).setNegativeButton("סגור", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("האם ברצונך להתנתק מהחשבון?")
                .setPositiveButton("כן, צא", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }).setNegativeButton("ביטול", null).show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if (nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    Intent calcIntent = new Intent(this, CalcRibitActivity.class);
                    calcIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(calcIntent);
                    finish();
                    return true;
                }
                else if (id == R.id.nav_ai_chat) { startActivity(new Intent(this, ChatActivity.class)); finish(); return true; }
                else if (id == R.id.nav_history) { startActivity(new Intent(this, HistoryActivity.class)); finish(); return true; }
                return false;
            });
        }
    }

    private void calculateResults(int totalMonths) {
        double r = ((rate - fees) / 100) / 12;
        if (r != 0) finalBalance = initial * Math.pow(1 + r, totalMonths) + monthly * (Math.pow(1 + r, totalMonths) - 1) / r;
        else finalBalance = initial + (monthly * totalMonths);
        totalInvested = initial + (monthly * totalMonths);
        totalProfit = finalBalance - totalInvested;
    }

    private void displayData() {
        String f = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, f, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, f, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " ש' ו-" + extraMonths + " ח'");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, f, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח צפוי: " + currencySymbol + String.format(Locale.US, f, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, f, finalBalance));
    }
}