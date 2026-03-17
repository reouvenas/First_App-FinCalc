package reouven.first_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import java.util.Locale;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult, tvCurrencySymbol;
    private Button btnCalculate, btnDetails;
    private FirebaseAuth mAuth;
    private String currencySymbol = "₪";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBar();
        checkIfEditing();

        btnCalculate.setOnClickListener(v -> calculateInvestment());

        btnDetails.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("initial", getDouble(etInitial));
            intent.putExtra("monthly", getDouble(etMonthly));
            intent.putExtra("rate", getDouble(etRate));
            intent.putExtra("years", (int) getDouble(etYears));
            intent.putExtra("months", (int) getDouble(etMonths));
            intent.putExtra("fees", getDouble(etFees));
            intent.putExtra("currency", currencySymbol);
            startActivity(intent);
        });
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

        // לוגיקה להחלפת מטבע (₪, $, €)
        ImageView btnCurrency = findViewById(R.id.btnCurrency);
        if (btnCurrency != null) {
            btnCurrency.setOnClickListener(v -> {
                if (currencySymbol.equals("₪")) currencySymbol = "$";
                else if (currencySymbol.equals("$")) currencySymbol = "€";
                else currencySymbol = "₪";
                tvCurrencySymbol.setText(currencySymbol);
                if (tvResult.getVisibility() == View.VISIBLE) calculateInvestment();
            });
        }

        // לוגיקה לכפתור המידע של דמי ניהול
        ImageView btnInfoFees = findViewById(R.id.btnInfoFees);
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("דמי ניהול")
                        .setMessage("דמי הניהול השנתיים מופחתים מהתשואה הכוללת של ההשקעה.")
                        .setPositiveButton("הבנתי", null).show();
            });
        }
    }

    private void setupTopBar() {
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_about) {
                        new AlertDialog.Builder(this).setTitle("אודות").setMessage("InvestCalc v2.0\nפותח ע\"י ראובן").show();
                    } else if (id == R.id.menu_contact) {
                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@invest.com"));
                        startActivity(i);
                    } else if (id == R.id.menu_logout) {
                        mAuth.signOut();
                        finish();
                    }
                    return true;
                });
                popup.show();
            });
        }
    }

    private void checkIfEditing() {
        Intent intent = getIntent();
        if (intent.hasExtra("edit_initial")) {
            etInitial.setText(String.valueOf(intent.getDoubleExtra("edit_initial", 0)));
            etMonthly.setText(String.valueOf(intent.getDoubleExtra("edit_monthly", 0)));
            etRate.setText(String.valueOf(intent.getDoubleExtra("edit_rate", 0)));
            etYears.setText(String.valueOf(intent.getIntExtra("edit_years", 0)));
            etMonths.setText(String.valueOf(intent.getIntExtra("edit_months", 0)));
            etFees.setText(String.valueOf(intent.getDoubleExtra("edit_fees", 0)));
            calculateInvestment();
        }
    }

    private void calculateInvestment() {
        double p = getDouble(etInitial);
        double m = getDouble(etMonthly);
        double r = (getDouble(etRate) - getDouble(etFees)) / 100 / 12;
        int t = ((int) getDouble(etYears) * 12) + (int) getDouble(etMonths);
        if (t <= 0) return;
        double total = (r != 0) ? p * Math.pow(1 + r, t) + m * (Math.pow(1 + r, t) - 1) / r : p + (m * t);
        tvResult.setText(currencySymbol + String.format(Locale.US, "%,.2f", total));
        tvResult.setVisibility(View.VISIBLE);
        btnDetails.setEnabled(true);
    }

    private double getDouble(EditText et) {
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}