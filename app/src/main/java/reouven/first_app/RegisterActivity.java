package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

/**
 * מחלקה: RegisterActivity
 * תפקיד: ניהול תהליך ההרשמה של משתמשים חדשים למערכת.
 * תכונות מרכזיות:
 * 1. יצירת משתמש מבוסס אימייל וסיסמה ב-Firebase Auth.
 * 2. שמירת נתונים מורחבים (שם, טלפון עם קידומת) ב-Realtime Database.
 * 3. אפשרות כניסה כאורח (Anonymous Login).
 * 4. הצגה/הסתרה של הסיסמה בזמן ההקלדה.
 */
public class RegisterActivity extends AppCompatActivity {

    // רכיבי קלט וטפסים
    private EditText etName, etEmail, etPhone, etPassword, etConfirm;
    private Spinner spPrefix; // בחירת קידומת טלפון
    private CheckBox cbTerms; // הסכמה לתנאי שימוש
    private Button btnRegister;
    private TextView tvGoToLogin, tvGuestMode;
    private ImageButton ibBackArrow;
    private ImageView ivShowPassword; // כפתור עין להצגת סיסמה

    // ניהול נתונים ואימות
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // קריאה לפעולות האתחול
        initViews();
        setupSpinner();
        setupClickListeners();
    }

    /**
     * פעולה: initViews
     * תפקיד: קישור רכיבי ה-XML למשתני ה-Java.
     */
    private void initViews() {
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
        ivShowPassword = findViewById(R.id.ivShowPassword);
        tvGuestMode = findViewById(R.id.tvGuestMode);
    }

    /**
     * פעולה: setupClickListeners
     * תפקיד: הגדרת לוגיקה ללחיצות על כפתורי הניווט, ההרשמה וכניסת האורח.
     */
    private void setupClickListeners() {
        ibBackArrow.setOnClickListener(v -> navigateToLogin());
        tvGoToLogin.setOnClickListener(v -> navigateToLogin());
        ivShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        btnRegister.setOnClickListener(v -> handleRegister());

        // לוגיקה לכניסה במצב אורח (ללא צורך בחשבון)
        if (tvGuestMode != null) {
            tvGuestMode.setOnClickListener(v -> loginAsGuest());
        }
    }

    /**
     * פעולה: loginAsGuest
     * תפקיד: ביצוע כניסה אנונימית ל-Firebase.
     */
    private void loginAsGuest() {
        mAuth.signInAnonymously().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this, "נכנסת במצב אורח", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                finish();
            } else {
                Toast.makeText(RegisterActivity.this, "שגיאה בכניסת אורח", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * פעולה: setupSpinner
     * תפקיד: הגדרת רשימת הקידומות של מספרי הטלפון בישראל.
     */
    private void setupSpinner() {
        String[] prefixes = {"קידומת", "050", "052", "053", "054", "055", "058"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, prefixes);
        spPrefix.setAdapter(adapter);
    }

    /**
     * פעולה: togglePasswordVisibility
     * תפקיד: שינוי סוג שדה הסיסמה בין כוכביות לטקסט גלוי.
     */
    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivShowPassword.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivShowPassword.setImageResource(android.R.drawable.btn_star_big_off);
        }
        isPasswordVisible = !isPasswordVisible;
        // החזרת הסמן לסוף הטקסט לאחר שינוי ה-InputType
        etPassword.setSelection(etPassword.length());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    /**
     * פעולה: handleRegister
     * תפקיד: ולידציה של השדות וביצוע הרשמה מול Firebase.
     * שלבי התהליך:
     * 1. בדיקת תקינות שדות.
     * 2. יצירת חשבון ב-Auth.
     * 3. עדכון שם המשתמש ב-Profile של ה-User.
     * 4. שמירת נתונים נוספים ב-Realtime Database.
     */
    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirm.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String prefix = spPrefix.getSelectedItem().toString();

        // בדיקות בסיסיות
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || prefix.equals("קידומת")) {
            Toast.makeText(this, "נא למלא את כל השדות", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "הסיסמאות לא תואמות", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "יש לאשר את תנאי השימוש", Toast.LENGTH_SHORT).show();
            return;
        }

        // ביצוע ההרשמה ב-Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid();
                            String fullPhone = prefix + "-" + phone;

                            // עדכון שם התצוגה של המשתמש ב-Firebase Auth
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name).build();
                            user.updateProfile(profileUpdates);

                            // הכנת אובייקט נתונים לשמירה ב-Database
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("phone", fullPhone);
                            userData.put("uid", uid);

                            // שמירה ב-Realtime Database תחת ענף "Users"
                            FirebaseDatabase.getInstance("https://androidproject-91b41-default-rtdb.firebaseio.com")
                                    .getReference("Users").child(uid).setValue(userData)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                            finish();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "הרשמה נכשלה: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}