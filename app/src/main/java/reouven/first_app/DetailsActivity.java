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
        currencySymbol = intent.getStringExtra("currency");
        if (currencySymbol == null) currencySymbol = "₪";

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
                // כאן וודא שיש לך GraphActivity בפרויקט
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
        View btnShare = findViewById(R.id.btnShare);
        if (btnShare != null) btnShare.setOnClickListener(v -> createAndSharePDF());

        // כפתור עריכה - מחזיר למחשבון עם הנתונים
        View btnEdit = findViewById(R.id.btnEdit);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                Intent editIntent = new Intent(this, CalcRibitActivity.class);
                editIntent.putExtra("initial", initial);
                editIntent.putExtra("monthly", monthly);
                editIntent.putExtra("rate", rate);
                editIntent.putExtra("years", years);
                editIntent.putExtra("months", extraMonths);
                editIntent.putExtra("fees", fees);
                editIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(editIntent);
                finish();
            });
        }

        // כפתור שמירה
        View btnSave = findViewById(R.id.btnSaveTable);
        if (btnSave != null) {
            btnSave.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null || user.isAnonymous()) {
                    showGuestRestrictionDialog("שמירת תוכניות השקעה זמינה למשתמשים רשומים בלבד.");
                } else {
                    saveToFirebaseWithDialog();
                }
            });
        }
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        try {
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_logout) {
                    showLogoutDialog();
                    return true;
                }
                // הוסף פה שאר פריטים במידת הצורך
                return false;
            });
            popup.show();
        } catch (Exception e) {
            Toast.makeText(this, "התפריט לא הוגדר עדיין", Toast.LENGTH_SHORT).show();
        }
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
        String f = "%,.0f";
        ((TextView)findViewById(R.id.tvSumInitial)).setText("סכום התחלתי: " + currencySymbol + String.format(Locale.US, f, initial));
        ((TextView)findViewById(R.id.tvSumMonthly)).setText("הפקדה חודשית: " + currencySymbol + String.format(Locale.US, f, monthly));
        ((TextView)findViewById(R.id.tvSumPeriod)).setText("תקופה: " + years + " ש' ו-" + extraMonths + " ח'");
        ((TextView)findViewById(R.id.tvSumRate)).setText("תשואה שנתית: " + rate + "%");

        ((TextView)findViewById(R.id.tvFinalInvested)).setText("סך השקעה: " + currencySymbol + String.format(Locale.US, f, totalInvested));
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח: " + currencySymbol + String.format(Locale.US, f, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, f, finalBalance));
    }

    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("פעולה חסומה")
                .setMessage(message)
                .setPositiveButton("להרשמה", (d, w) -> startActivity(new Intent(this, RegisterActivity.class)))
                .setNegativeButton("ביטול", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("בטוח שרוצה לצאת?")
                .setPositiveButton("כן", (d, w) -> {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }).setNegativeButton("לא", null).show();
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("שם התוכנית")
                .setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    String name = input.getText().toString();
                    Map<String, Object> data = new HashMap<>();
                    data.put("planName", name.isEmpty() ? "חישוב שלי" : name);
                    data.put("finalBalance", finalBalance);
                    data.put("date", System.currentTimeMillis());
                    FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                            .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
                }).show();
    }

    private void createAndSharePDF() {
        // ... (הקוד של ה-PDF שלך מצוין, השארתי אותו אותו דבר)
        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 450, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(12f);
        canvas.drawText("דו\"ח השקעה - InvestCalc", 80, 40, paint);
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

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        if (prefs.getBoolean("dark_mode", false)) {
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