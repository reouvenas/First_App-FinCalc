package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
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
import androidx.core.content.FileProvider;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mAuth = FirebaseAuth.getInstance();

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        mainLayout = findViewById(R.id.main_layout);

        // קבלת נתונים מה-Intent
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

    private void setupActionButtons() {
        // כפתור גרף
        View btnChart = findViewById(R.id.btnViewChart);
        if (btnChart != null) {
            btnChart.setOnClickListener(v -> {
                Intent gIntent = new Intent(this, GraphActivity.class);
                gIntent.putExtra("initial", initial);
                gIntent.putExtra("monthly", monthly);
                gIntent.putExtra("rate", rate);
                gIntent.putExtra("years", years);
                gIntent.putExtra("months", extraMonths);
                gIntent.putExtra("fees", fees);
                startActivity(gIntent);
            });
        }

        // כפתור שיתוף PDF
        findViewById(R.id.btnShare).setOnClickListener(v -> createAndSharePDF());

        // כפתור עריכה
        findViewById(R.id.btnEdit).setOnClickListener(v -> {
            Intent editIntent = new Intent(this, CalcRibitActivity.class);
            editIntent.putExtra("edit_initial", initial);
            editIntent.putExtra("edit_monthly", monthly);
            editIntent.putExtra("edit_rate", rate);
            editIntent.putExtra("edit_years", years);
            editIntent.putExtra("edit_months", extraMonths);
            editIntent.putExtra("edit_fees", fees);
            editIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(editIntent);
            finish();
        });

        // כפתור שמירה - חסימת אורח הרמטית
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null || user.isAnonymous()) {
                showGuestRestrictionDialog("שמירת תוכניות השקעה זמינה למשתמשים רשומים בלבד.");
            } else {
                saveToFirebaseWithDialog();
            }
        });
    }

    // הודעה קופצת לחסימת אורח עם קישור להרשמה
    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("פעולה חסומה")
                .setMessage(message + "\nרוצה להירשם עכשיו?")
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                })
                .setNegativeButton("אולי אחר כך", null)
                .show();
    }

    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());
        findViewById(R.id.btnMenuHeader).setOnClickListener(this::showPopupMenu);
    }

    // תפריט שלוש נקודות מתוקן (כולל פרופיל, אודות והתנתקות)
    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            FirebaseUser user = mAuth.getCurrentUser();

            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
            } else if (id == R.id.menu_profile) {
                if (user == null || user.isAnonymous()) {
                    showGuestRestrictionDialog("צפייה בפרופיל זמינה לרשומים בלבד.");
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
            } else if (id == R.id.menu_contact) {
                NavigationHelper.showContactDialog(this);
            } else if (id == R.id.menu_about) {
                showAboutDialog();
            } else if (id == R.id.menu_logout) {
                showLogoutDialog();
            }
            return true;
        });
        popup.show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("InvestCalc v2.0\nמחשבון פיננסי חכם לניהול השקעות.\nפותח על ידי: ראובן")
                .setPositiveButton("סגור", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם אתה בטוח שברצונך לצאת?")
                .setPositiveButton("כן", (d, w) -> {
                    mAuth.signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null).show();
    }

    // --- שאר הפונקציות המקוריות (PDF, חישוב, צבעים) ---

    private void createAndSharePDF() {
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 450, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);

        paint.setTextSize(14f);
        canvas.drawText("דו\"ח השקעה - InvestCalc", 80, 40, paint);
        paint.setTextSize(10f);
        canvas.drawText("סכום התחלתי: " + currencySymbol + String.format("%.0f", initial), 20, 80, paint);
        canvas.drawText("הפקדה חודשית: " + currencySymbol + String.format("%.0f", monthly), 20, 105, paint);
        canvas.drawText("תקופה: " + years + " שנים ו-" + extraMonths + " חודשים", 20, 130, paint);
        canvas.drawText("סה\"כ ברוטו צפוי: " + currencySymbol + String.format("%,.0f", finalBalance), 20, 170, paint);

        paint.setTextSize(8f);
        canvas.drawText("פותח על ידי ראובן", 110, 400, paint);

        document.finishPage(page);
        File file = new File(getExternalFilesDir(null), "InvestCalc_Report.pdf");
        try {
            document.writeTo(new FileOutputStream(file));
            document.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "שתף PDF"));
        } catch (IOException e) { e.printStackTrace(); }
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
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " ש' ו-" + extraMonths + " ח'");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");
        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, format, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח: " + currencySymbol + String.format(Locale.US, format, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, format, finalBalance));
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        input.setHint("שם לתוכנית");
        new AlertDialog.Builder(this).setTitle("שמירה").setView(input).setPositiveButton("שמור", (d, w) -> {
            String name = input.getText().toString();
            Map<String, Object> data = new HashMap<>();
            data.put("planName", name.isEmpty() ? "חישוב שלי" : name);
            data.put("finalBalance", finalBalance);
            data.put("userId", mAuth.getUid());
            FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                    .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
        }).show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    private void applyCustomColorMode() {
        if (getSharedPreferences("AppConfig", MODE_PRIVATE).getBoolean("dark_mode", false)) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
        }
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