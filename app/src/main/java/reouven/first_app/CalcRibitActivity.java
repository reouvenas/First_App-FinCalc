package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
    private ImageView btnInfoFees, btnSavePlan, btnBack;

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

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
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

        // תיקון: חיבור כפתור הפירוט ששכחנו מקודם
        btnDetails = findViewById(R.id.btnDetails);

        btnInfoFees = findViewById(R.id.btnInfoFees);
        btnSavePlan = findViewById(R.id.btnSavePlan);
        btnBack = findViewById(R.id.btnBack);

        // הסתרת התוצאה והפיכת כפתור הפירוט ללא פעיל בהתחלה
        if (tvResult != null) tvResult.setVisibility(View.GONE);
        if (btnDetails != null) {
            btnDetails.setEnabled(false);
            btnDetails.setAlpha(0.5f);
        }
    }

    private void setupClickListeners() {
        // 1. חץ חזרה
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 2. כפתור שמירה
        if (btnSavePlan != null) {
            btnSavePlan.setOnClickListener(v -> {
                if (mAuth.getCurrentUser() == null) {
                    showGuestDialog();
                } else if (tvResult.getVisibility() == View.GONE) {
                    Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
                } else {
                    showSavePlanDialog();
                }
            });
        }

        // 3. סימן קריאה למידע על דמי ניהול
        if (btnInfoFees != null) {
            btnInfoFees.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("הסבר דמי ניהול")
                        .setMessage("דמי הניהול מופחתים מהריבית השנתית.\nלדוגמה: ריבית של 10% עם דמי ניהול של 1% תחושב כריבית של 9%.")
                        .setPositiveButton("הבנתי", null).show();
            });
        }

        // 4. כפתור חישוב
        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> calculateInvestment());
        }

        // 5. כפתור פירוט התוצאה
        if (btnDetails != null) {
            btnDetails.setOnClickListener(v -> {
                Toast.makeText(this, "מעבר לדף פירוט נתונים...", Toast.LENGTH_SHORT).show();
                // כאן נפתח בהמשך את הטבלה עם Intent
            });
        }
    }

    private void calculateInvestment() {
        try {
            double principal = getDouble(etInitial);
            double monthlyDeposit = getDouble(etMonthly);
            double annualRate = getDouble(etRate) / 100;
            double annualFees = getDouble(etFees) / 100;
            int years = (int) getDouble(etYears);
            int extraMonths = (int) getDouble(etMonths);
            int totalMonths = (years * 12) + extraMonths;

            if (totalMonths <= 0) {
                Toast.makeText(this, "הזן תקופת זמן תקינה", Toast.LENGTH_SHORT).show();
                return;
            }

            // חישוב ריבית דריבית חודשית (נטו לאחר דמי ניהול)
            double netMonthlyRate = (annualRate - annualFees) / 12;
            double total;

            if (netMonthlyRate != 0) {
                double pGrowth = principal * Math.pow(1 + netMonthlyRate, totalMonths);
                double dGrowth = monthlyDeposit * (Math.pow(1 + netMonthlyRate, totalMonths) - 1) / netMonthlyRate;
                total = pGrowth + dGrowth;
            } else {
                total = principal + (monthlyDeposit * totalMonths);
            }

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("iw", "IL"));
            tvResult.setText("הסכום הצפוי: " + format.format(total));
            tvResult.setVisibility(View.VISIBLE);

            // הפעלת כפתור הפירוט אחרי שיש תוצאה
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
                .setTitle("שמירת תוכנית")
                .setView(input)
                .setPositiveButton("שמור", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (!name.isEmpty()) saveDataToFirebase(name);
                })
                .setNegativeButton("ביטול", null).show();
    }

    private void saveDataToFirebase(String planName) {
        if (mAuth.getCurrentUser() == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("planName", planName);
        data.put("result", tvResult.getText().toString());
        data.put("date", new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));

        db.collection("users").document(mAuth.getCurrentUser().getUid())
                .collection("history").add(data)
                .addOnSuccessListener(ref -> Toast.makeText(this, "נשמר!", Toast.LENGTH_SHORT).show());
    }

    private void showGuestDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אורח")
                .setMessage("יש להתחבר כדי לשמור")
                .setPositiveButton("התחברות", null)
                .setNegativeButton("ביטול", null).show();
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String s = et.getText().toString();
        return s.isEmpty() ? 0 : Double.parseDouble(s);
    }
}