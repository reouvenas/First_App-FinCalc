package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etConfirm;
    private Spinner spPrefix;
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ImageButton ibBackArrow;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();

        // קישור רכיבים
        ibBackArrow = findViewById(R.id.ibBackArrow);
        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPhone = findViewById(R.id.etRegisterPhone);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirm = findViewById(R.id.etRegisterConfirmPassword);
        spPrefix = findViewById(R.id.spPhonePrefix);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        tvGoToLogin = findViewById(R.id.tvGoToLoginFromRegister);

        // תיקון חץ חזרה - מעבר מפורש ללוגין
        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> {
                navigateToLogin();
            });
        }

        // הגדרת ספינר קידומות
        String[] prefixes = {"קידומת", "050", "052", "053", "054", "055", "058"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, prefixes);
        spPrefix.setAdapter(adapter);

        // כפתור הרשמה
        btnRegister.setOnClickListener(v -> {
            handleRegister();
        });

        // תיקון טקסט חזרה ללוגין - מעבר מפורש ללוגין
        tvGoToLogin.setOnClickListener(v -> {
            navigateToLogin();
        });
    }

    // פונקציית עזר למעבר בטוח למסך הלוגין
    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        // השורה הזו היא הקסם - היא מנקה את הדרך חזרה ללוגין
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish(); // סוגר את דף ההרשמה
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                        // אחרי הרשמה מוצלחת עוברים לדף הבית
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}