package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonthly, etRate, etYears, etMonths, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails, btnConvert;
    private LinearLayout resultArea;
    private FirebaseAuth mAuth;

    private String currencySymbol = "₪";
    private double lastCalculatedValue = 0;
    private double USD_TO_ILS = 3.65;
    private double EUR_TO_ILS = 3.95;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupNavigation();
        fetchLiveRates();
    }

    private void initViews() {
        etInitial = findViewById(R.id.etInitial);
        etMonthly = findViewById(R.id.etMonthly);
        etRate = findViewById(R.id.etRate);
        etYears = findViewById(R.id.etYears);
        etMonths = findViewById(R.id.etMonths);
        etFees = findViewById(R.id.etFees);
        tvResult = findViewById(R.id.tvResult);
        tvCurrencySymbol = findViewById(R.id.tvCurrencySymbol);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDetails = findViewById(R.id.btnDetails);
        btnConvert = findViewById(R.id.btnConvert);
        resultArea = findViewById(R.id.resultArea);

        btnDetails.setEnabled(false);
        btnDetails.setAlpha(0.5f);

        ImageView btnInfoFees = findViewById(R.id.btnInfoFees);
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("מה זה דמי ניהול?")
                    .setMessage("אחוז שנתי המופחת מהרווחים שלך (למשל בקרן השתלמות או פוליסת חיסכון).")
                    .setPositiveButton("הבנתי", null).show());
        }

        ImageView btnCurrency = findViewById(R.id.btnCurrency);
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> {
                if (currencySymbol.equals("₪")) currencySymbol = "$";
                else if (currencySymbol.equals("$")) currencySymbol = "€";
                else currencySymbol = "₪";
                tvCurrencySymbol.setText(currencySymbol);
            });
        }

        btnCalculate.setOnClickListener(v -> calculateInvestment());

        if (btnConvert != null) {
            btnConvert.setOnClickListener(v -> showConversionDialog());
        }

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", parseDouble(etInitial));
            intent.putExtra("monthly", parseDouble(etMonthly));
            intent.putExtra("rate", parseDouble(etRate));
            intent.putExtra("years", (int) parseDouble(etYears));
            intent.putExtra("months", (int) parseDouble(etMonths));
            intent.putExtra("fees", parseDouble(etFees));
            intent.putExtra("currency", currencySymbol);
            startActivity(intent);
        });
    }

    private void calculateInvestment() {
        try {
            double p = parseDouble(etInitial);
            double m = parseDouble(etMonthly);
            double r = (parseDouble(etRate) - parseDouble(etFees)) / 100 / 12;
            int t = ((int) parseDouble(etYears) * 12) + (int) parseDouble(etMonths);

            if (t <= 0) {
                Toast.makeText(this, "נא להזין זמן תקין", Toast.LENGTH_SHORT).show();
                return;
            }

            if (r != 0) lastCalculatedValue = p * Math.pow(1 + r, t) + m * (Math.pow(1 + r, t) - 1) / r;
            else lastCalculatedValue = p + (m * t);

            tvResult.setText(currencySymbol + String.format(Locale.US, "%,.2f", lastCalculatedValue));
            resultArea.setVisibility(View.VISIBLE);

            btnDetails.setEnabled(true);
            btnDetails.setAlpha(1.0f);
            btnDetails.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

        } catch (Exception e) {
            Toast.makeText(this, "שגיאה בחישוב, בדוק את הנתונים", Toast.LENGTH_SHORT).show();
        }
    }

    private void showConversionDialog() {
        String[] options = {"שקלים (₪)", "דולרים ($)", "אירו (€)"};
        new AlertDialog.Builder(this)
                .setTitle("המר תוצאה ל:")
                .setItems(options, (dialog, which) -> {
                    double converted = lastCalculatedValue;
                    String newSym = "";
                    if (currencySymbol.equals("₪")) {
                        if (which == 1) { converted /= USD_TO_ILS; newSym = "$"; }
                        else if (which == 2) { converted /= EUR_TO_ILS; newSym = "€"; }
                        else newSym = "₪";
                    } else if (currencySymbol.equals("$")) {
                        if (which == 0) { converted *= USD_TO_ILS; newSym = "₪"; }
                        else if (which == 2) { converted = (converted * USD_TO_ILS) / EUR_TO_ILS; newSym = "€"; }
                        else newSym = "$";
                    } else if (currencySymbol.equals("€")) {
                        if (which == 0) { converted *= EUR_TO_ILS; newSym = "₪"; }
                        else if (which == 1) { converted = (converted * EUR_TO_ILS) / USD_TO_ILS; newSym = "$"; }
                        else newSym = "€";
                    }
                    tvResult.setText(newSym + String.format(Locale.US, "%,.2f", converted));
                }).show();
    }

    private void setupNavigation() {
        // מציאת ה-Header מתוך ה-Include
        View topBar = findViewById(R.id.included_top_bar);
        if (topBar != null) {
            View btnMenu = topBar.findViewById(R.id.btnMenuHeader);
            if (btnMenu != null) {
                btnMenu.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(this, v);
                    popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                    popup.setOnMenuItemClickListener(item -> {
                        int id = item.getItemId();
                        if (id == R.id.menu_profile) {
                            startActivity(new Intent(this, ProfileActivity.class));
                            return true;
                        } else if (id == R.id.menu_dark_mode) {
                            toggleDarkMode();
                            return true;
                        } else if (id == R.id.menu_about) {
                            showAboutDialog();
                            return true;
                        } else if (id == R.id.menu_contact) {
                            showContactDialog();
                            return true;
                        } else if (id == R.id.menu_logout) {
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
        }

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setSelectedItemId(R.id.nav_home);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_ai_chat) {
                startActivity(new Intent(this, ChatActivity.class));
                return true;
            } else if (id == R.id.nav_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (id == R.id.nav_tips) {
                startActivity(new Intent(this, TipsActivity.class));
                return true;
            }
            return id == R.id.nav_home;
        });
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית, החזרי משכנתא ותחזיות בצורה הכי מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";
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

    private double parseDouble(EditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private void fetchLiveRates() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url("https://open.er-api.com/v6/latest/ILS").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject rates = json.getJSONObject("rates");
                        USD_TO_ILS = 1 / rates.getDouble("USD");
                        EUR_TO_ILS = 1 / rates.getDouble("EUR");
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }
}