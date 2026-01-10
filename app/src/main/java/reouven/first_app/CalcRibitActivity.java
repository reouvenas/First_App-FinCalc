package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore; // הוספנו לספרייה
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CalcRibitActivity extends AppCompatActivity {

    private EditText etInitial, etMonths, etMonthly, etRate, etYears, etFees;
    private TextView tvResult;
    private Button btnCalculate;
    private ImageButton btnInfoFees, btnSavePlan; // הוספנו את btnSavePlan
    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // משתנה למסד הנתונים

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calc_ribit);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // אתחול Firebase

        initViews();

        // כפתור מידע על דמי ניהול
        btnInfoFees.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("מהם דמי ניהול?")
                    .setMessage("דמי ניהול הם עמלה שנתית המשולמת לגוף המנהל את ההשקעה. אחוז זה יורד מהרווח הכולל שלך.")
                    .setPositiveButton("הבנתי", null)
                    .show();
        });

        // כפתור פלוס לשמירת התוכנית
        btnSavePlan.setOnClickListener(v -> {
            // בדיקה אם המשתמש מחובר
            if (mAuth.getCurrentUser() == null) {
                showGuestDialog();
            }
            // בדיקה אם כבר בוצע חישוב (שלא ישמור דף ריק)
            else if (tvResult.getText().toString().equals("לתוצאה") || tvResult.getText().toString().isEmpty()) {
                Toast.makeText(this, "בצע חישוב לפני השמירה", Toast.LENGTH_SHORT).show();
            } else {
                showSavePlanDialog();
            }
        });

        // כפתור החישוב
        btnCalculate.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) {
                showGuestDialog();
            } else {
                calculateInvestment();
            }
        });
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
        btnInfoFees = findViewById(R.id.btnInfoFees);
        btnSavePlan = findViewById(R.id.btnSavePlan);

        // חיבור התפריט התחתון (חשוב כדי שיופיע)
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
    }

    // חלונית לבקשת שם התוכנית מהמשתמש
    private void showSavePlanDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("שמירת תוכנית השקעה");

        final EditText input = new EditText(this);
        input.setHint("הכנס שם לתוכנית (למשל: חיסכון לילד)");
        input.setPadding(50, 40, 50, 40);
        builder.setView(input);

        builder.setPositiveButton("שמור", (dialog, which) -> {
            String planName = input.getText().toString().trim();
            if (!planName.isEmpty()) {
                saveDataToFirebase(planName);
            } else {
                Toast.makeText(this, "חובה להזין שם", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    // שמירת הנתונים בפועל ל-Firebase
    private void saveDataToFirebase(String planName) {
        android.util.Log.d("SAVE_DEBUG", "נסיו שמירה ל-Firebase...");
        String uid = mAuth.getCurrentUser().getUid();
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        // יצירת מבנה הנתונים לשמירה
        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("planName", planName);
        historyItem.put("calculatorName", "מחשבון השקעות");
        historyItem.put("date", date);
        historyItem.put("result", tvResult.getText().toString());
        // שומרים גם את הפרמטרים למקרה שנרצה להציג פירוט
        historyItem.put("initial", etInitial.getText().toString());
        historyItem.put("years", etYears.getText().toString());
        historyItem.put("monthly", etMonthly.getText().toString());

        db.collection("users").document(uid).collection("history")
                .add(historyItem)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "התוכנית '" + planName + "' נשמרה בהיסטוריה", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "שגיאה בשמירה: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showGuestDialog() {
        new AlertDialog.Builder(this)
                .setTitle("פעולה לביצוע לרשומים בלבד")
                .setMessage("כדי לשמור תוכניות ולראות תוצאות עליך להירשם.")
                .setPositiveButton("להרשמה", (dialog, which) -> {
                    Intent intent = new Intent(CalcRibitActivity.this, RegisterActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void calculateInvestment() {
        try {
            double principal = getDouble(etInitial);
            int extraMonths = (int) getDouble(etMonths);
            double monthlyDeposit = getDouble(etMonthly);
            double annualRate = getDouble(etRate) / 100;
            int years = (int) getDouble(etYears);
            double annualFees = getDouble(etFees) / 100;

            int totalMonths = (years * 12) + extraMonths;
            if (totalMonths <= 0) {
                Toast.makeText(this, "נא להזין תקופת זמן תקינה", Toast.LENGTH_SHORT).show();
                return;
            }

            double netAnnualRate = annualRate - annualFees;
            double monthlyRate = netAnnualRate / 12;

            double total;
            if (monthlyRate != 0) {
                double principalGrowth = principal * Math.pow(1 + monthlyRate, totalMonths);
                double depositsGrowth = monthlyDeposit * (Math.pow(1 + monthlyRate, totalMonths) - 1) / monthlyRate;
                total = principalGrowth + depositsGrowth;
            } else {
                total = principal + (monthlyDeposit * totalMonths);
            }

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("iw", "IL"));
            tvResult.setText("הסכום הצפוי: " + format.format(total));

        } catch (Exception e) {
            tvResult.setText("שגיאה בנתונים");
        }
    }

    private double getDouble(EditText et) {
        if (et == null) return 0;
        String text = et.getText().toString().trim();
        return text.isEmpty() ? 0 : Double.parseDouble(text);
    }
}