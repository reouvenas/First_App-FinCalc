package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 1. קישור הרכיבים מה-XML לקוד ה-Java
        etName = findViewById(R.id.etRegisterName);
        etEmail = findViewById(R.id.etRegisterEmail);
        etPhone = findViewById(R.id.etRegisterPhone);
        etPassword = findViewById(R.id.etRegisterPassword);
        etConfirm = findViewById(R.id.etRegisterConfirmPassword);
        spPrefix = findViewById(R.id.spPhonePrefix);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegisterSubmit);
        tvGoToLogin = findViewById(R.id.tvGoToLoginFromRegister);

        // 2. תיקון סופי לספינר - שלא יהיה רשום "אייטם 1"
        String[] prefixes = {"קידומת", "050", "052", "053", "054", "055", "058"};

// השתמשתי כאן ב-simple_list_item_1 - הוא תמיד מציג את הטקסט האמיתי
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, prefixes);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPrefix.setAdapter(adapter);

// מוודא שהבחירה הראשונה היא המילה "קידומת"
        spPrefix.setSelection(0);

        // 3. הגדרת פעולה לכפתור ההרשמה
        btnRegister.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String confirmPass = etConfirm.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String prefix = spPrefix.getSelectedItem().toString();

            // בדיקת אישור תנאי שימוש
            if (!cbTerms.isChecked()) {
                Toast.makeText(this, "חובה לאשר את תנאי השימוש *", Toast.LENGTH_SHORT).show();
                return;
            }

            // בדיקה שכל השדות מלאים
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty() || phone.isEmpty() || prefix.equals("קידומת")) {
                Toast.makeText(this, "נא למלא את כל השדות ולבחור קידומת", Toast.LENGTH_SHORT).show();
                return;
            }

            // בדיקה שהסיסמאות זהות
            if (!password.equals(confirmPass)) {
                Toast.makeText(this, "הסיסמאות אינן תואמות", Toast.LENGTH_SHORT).show();
                return;
            }

            // יצירת המשתמש ב-Firebase
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RegisterActivity.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                            // מעבר למסך הראשי
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(RegisterActivity.this, "שגיאה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // 4. הפיכת "התחברות" לכפתור לחיץ (חוזר למסך הקודם)
        tvGoToLogin.setOnClickListener(v -> {
            finish();
        });
    }
}