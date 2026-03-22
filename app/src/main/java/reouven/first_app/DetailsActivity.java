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
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

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
    }

    private void setupTopBar() {
        // כפתור חזרה - פשוט סוגר את הדף וחוזר למחשבון
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // תפריט עליון
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_logout) {
                    mAuth.signOut();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        if(nav != null) {
            nav.setSelectedItemId(R.id.nav_home);
            nav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    finish(); // חזרה למחשבון
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    startActivity(new Intent(this, ChatActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void setupActionButtons() {
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

        findViewById(R.id.btnShare).setOnClickListener(v -> createAndSharePDF());
        findViewById(R.id.btnEdit).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveTable).setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) Toast.makeText(this, "התחבר לשמירה", Toast.LENGTH_SHORT).show();
            else saveToFirebaseWithDialog();
        });
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
        ((TextView)findViewById(R.id.tvFinalProfit)).setText("סך רווח: " + currencySymbol + String.format(Locale.US, f, totalProfit));
        ((TextView)findViewById(R.id.tvFinalTotal)).setText("סה''כ ברוטו: " + currencySymbol + String.format(Locale.US, f, finalBalance));
    }

    private void saveToFirebaseWithDialog() {
        final EditText input = new EditText(this);
        new AlertDialog.Builder(this).setTitle("שם התוכנית").setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("planName", input.getText().toString());
                    data.put("finalBalance", finalBalance);
                    FirebaseFirestore.getInstance().collection("saved_plans").add(data)
                            .addOnSuccessListener(doc -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
                }).show();
    }

    private void createAndSharePDF() {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pi = new PdfDocument.PageInfo.Builder(300, 400, 1).create();
        PdfDocument.Page page = doc.startPage(pi);
        page.getCanvas().drawText("דו\"ח השקעה - " + finalBalance, 50, 50, new Paint());
        doc.finishPage(page);
        File file = new File(getExternalFilesDir(null), "Report.pdf");
        try {
            doc.writeTo(new FileOutputStream(file));
            doc.close();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent it = new Intent(Intent.ACTION_SEND);
            it.setType("application/pdf");
            it.putExtra(Intent.EXTRA_STREAM, uri);
            it.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(it, "שתף"));
        } catch (IOException e) { e.printStackTrace(); }
    }
}