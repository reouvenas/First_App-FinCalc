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

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword, etConfirm;
    private Spinner spPrefix;
    private CheckBox cbTerms;
    private Button btnRegister;
    private TextView tvGoToLogin;
    private ImageButton ibBackArrow;
    private ImageView ivShowPassword;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupSpinner();
        setupClickListeners();
    }

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
    }

    private void setupSpinner() {
        String[] prefixes = {"קידומת", "050", "052", "053", "054", "055", "058"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, prefixes);
        spPrefix.setAdapter(adapter);
    }

    private void setupClickListeners() {
        ibBackArrow.setOnClickListener(v -> navigateToLogin());
        tvGoToLogin.setOnClickListener(v -> navigateToLogin());
        ivShowPassword.setOnClickListener(v -> togglePasswordVisibility());
        btnRegister.setOnClickListener(v -> handleRegister());
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivShowPassword.setImageResource(android.R.drawable.ic_menu_view);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivShowPassword.setImageResource(android.R.drawable.btn_star_big_off);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.length());
    }

    private void navigateToLogin() {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void handleRegister() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirm.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String prefix = spPrefix.getSelectedItem().toString();

        // בדיקות תקינות
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

        // יצירת המשתמש ב-Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String uid = user.getUid(); // הגדרת ה-UID
                            String fullPhone = prefix + "-" + phone;

                            // 1. עדכון השם ב-Firebase Auth (בשביל תצוגה בסיסית)
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();
                            user.updateProfile(profileUpdates);

                            // 2. שמירת כל הנתונים ב-Realtime Database (כולל הטלפון!)
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("phone", fullPhone);
                            userData.put("uid", uid);

                            // שימוש בקישור הישיר שמצאנו ב-JSON שלך כדי להיות בטוחים
                            FirebaseDatabase.getInstance("https://androidproject-91b41-default-rtdb.firebaseio.com")
                                    .getReference("Users")
                                    .child(uid)
                                    .setValue(userData)
                                    .addOnCompleteListener(dbTask -> {
                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(RegisterActivity.this, "נרשמת בהצלחה!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(RegisterActivity.this, "שגיאה בשמירת נתונים: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "שגיאה בהרשמה: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }}