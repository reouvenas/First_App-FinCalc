package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult;
    private Button btnCalculate, btnDetails;
    private ImageButton btnInfoFees, btnSavePlan, ibBackArrow;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        etInitial = findViewById(R.id.etInitial);
        etMonths = findViewById(R.id.etMonths);
        etMonthly = findViewById(R.id.etMonthly);
        etRate = findViewById(R.id.etRate);
        etYears = findViewById(R.id.etYears);
        etFees = findViewById(R.id.etFees);
        tvResult = findViewById(R.id.tvResult);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnDetails = findViewById(R.id.btnDetails);
        btnInfoFees = findViewById(R.id.btnInfoFees);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        ibBackArrow = findViewById(R.id.ibBackArrow);

        // הסתרת התוצאה בהתחלה
        if (tvResult != null) {
            tvResult.setVisibility(View.GONE);
        }

        // כפתור פירוט לא פעיל בהתחלה
        if (btnDetails != null) {
            btnDetails.setEnabled(false);
            btnDetails.setAlpha(0.5f);
        }

        // --- הוספת הטיפול בתפריט התחתון ---
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_home); // אנחנו בדף הבית/חישוב
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true; // כבר פה, אל תעשה כלום
                } else {
                    // אם לוחצים על היסטוריה או טיפים, פשוט סוגרים את הדף הזה
                    // זה יחשוף את דף הבית שנמצא מתחת וימנע כפילות דפים
                    finish();
                    return true;
                }
            });
        }
    }

    private void setupClickListeners() {
        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> finish());
        }

        btnCalculate.setOnClickListener(v -> calculateInvestment());

        btnSavePlan.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                showGuestDialog();
            } else if (btnDetails != null && !btnDetails.isEnabled()) {
                Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
            } else {
                showSavePlanDialog();
            }
        });

        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> {
                Toast.makeText(this, "עובר לדף פירוט...", Toast.LENGTH_SHORT).show();
            });
        }

        btnInfoFees.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("מידע")
                    .setMessage("דמי ניהול מחושבים בקיזוז מהריבית השנתית.")
                    .setPositiveButton("הבנתי", null).show();
        });
    }

    private void calculateInvestment() {
        try {
            double principal = getDouble(etInitial);
            double monthlyDeposit = getDouble(etMonthly);
            double annualRate = getDouble(etRate) / 100;
            int totalMonths = ((int)getDouble(etYears) * 12) + (int)getDouble(etMonths);
            double annualFees = getDouble(etFees) / 100;

            if (totalMonths <= 0) return;

            double netMonthlyRate = (annualRate - annualFees) / 12;
            double total;

            if (netMonthlyRate != 0) {
                double principalGrowth = principal * Math.pow(1 + netMonthlyRate, totalMonths);
                double depositsGrowth = monthlyDeposit * (Math.pow(1 + netMonthlyRate, totalMonths) - 1) / netMonthlyRate;
                total = principalGrowth + depositsGrowth;
            } else {
                total = principal + (monthlyDeposit * totalMonths);
            }

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("iw", "IL"));
            tvResult.setText("הסכום הצפוי: " + format.format(total));

            // הצגת התוצאה רק לאחר החישוב
            tvResult.setVisibility(View.VISIBLE);

            if (btnDetails != null) {
                btnDetails.setEnabled(true);
                btnDetails.setAlpha(1.0f);
            }

        } catch (Exception e) {
            Toast.makeText(this, "נתונים לא תקינים", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSavePlanDialog() {
        final EditText input = new EditText(this);
        input.setHint("שם התוכנית");
        new AlertDialog.Builder(this)
                .setTitle("שמירה")
                .setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) saveDataToFirebase(name);
                })
                .setNegativeButton("ביטול", null).show();
    }

    private void saveDataToFirebase(String planName) {
        if (mAuth.getCurrentUser() == null) return;

        String uid = mAuth.getCurrentUser().getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("planName", planName);
        data.put("result", tvResult.getText().toString());
        data.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        db.collection("users").document(uid).collection("history")
                .add(data)
                .addOnSuccessListener(ref -> Toast.makeText(this, "התוכנית נשמרה בהצלחה!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "שגיאה: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    private double getDouble(EditText et) {
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }

    private void showGuestDialog() {
        new AlertDialog.Builder(this).setTitle("אורח").setMessage("הירשם לשמירה").show();
    }
}