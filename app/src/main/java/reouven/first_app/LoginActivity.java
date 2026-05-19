package reouven.first_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * מחלקה: LoginActivity
 * תפקיד: ניהול מסך ההתחברות לאפליקציה.
 * עדכון ארכיטקטורה: אימות ישיר ומאובטח מול Firebase Auth באמצעות אימייל וסיסמה.
 * פתרון באג חסימה: עקיפת הפנייה המוקדמת ל-Firestore שנחסמה על ידי חוקי האבטחה לפני ביצוע ההתחברות,
 * ותמיכה באפשרות "זכור אותי" מבודדת, מבוססת אימייל, למניעת בלבול נתונים.
 */
public class LoginActivity extends AppCompatActivity {

    // רכיבי הממשק
    private EditText etEmail, etPassword; // שונה מ-etUsername ל-etEmail
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ImageButton ibBackArrow;
    private CheckBox cbRememberMe;

    // אובייקטים לניהול נתונים
    private FirebaseAuth mAuth;            // אימות מול Firebase Auth
    private SharedPreferences sharedPreferences; // זיכרון מקומי לשמירת פרטי התחברות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול שירותי Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // אתחול זיכרון פנימי תחת השם "LoginPrefs"
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // חיבור הרכיבים מה-XML למשתני הג'אווה
        etEmail = findViewById(R.id.etLoginEmail); // קישור ל-ID המעודכן ב-XML
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLoginSubmit);
        tvGoToRegister = findViewById(R.id.tvGoToRegisterFromLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ibBackArrow = findViewById(R.id.ibBackArrow);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // טעינה אוטומטית של פרטים אם המשתמש בחר "זכור אותי" בעבר
        loadRememberedDetails();

        /**
         * תיקון זרימת ניווט: מאזין לחץ חזור (ibBackArrow)
         * פתרון באג ה-Auto-Login: במקום סתם finish() שמחזיר למסך פתיחה פגום שמחבר אוטומטית,
         * כאן אנחנו יוצרים כוונה נקייה שמנקה את ה-Stack ופותחת את ה-MainActivity מאופס לחלוטין.
         */
        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }

        // הגדרת קישור לדף ההרשמה (כולל קו תחתי מעוצב)
        if (tvGoToRegister != null) {
            tvGoToRegister.setPaintFlags(tvGoToRegister.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvGoToRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        }

        // מעבר לדף שחזור סיסמה
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        }

        // לחיצה על כפתור ההתחברות
        btnLogin.setOnClickListener(v -> loginUser());
    }

    /**
     * פעולה: loadRememberedDetails
     * תפקיד: בודקת ב-SharedPreferences האם קיימים פרטים שמורים ומציגה אותם בשדות.
     */
    private void loadRememberedDetails() {
        String savedEmail = sharedPreferences.getString("email", ""); // שינוי מ-username ל-email
        String savedPass = sharedPreferences.getString("password", "");
        boolean isRemembered = sharedPreferences.getBoolean("remember", false);

        if (isRemembered) {
            etEmail.setText(savedEmail);
            etPassword.setText(savedPass);
            cbRememberMe.setChecked(true);
        }
    }

    /**
     * פעולה: saveDetails
     * תפקיד: שמירה או מחיקה של פרטי המשתמש מהזיכרון המקומי בהתאם למצב ה-CheckBox.
     */
    private void saveDetails(String email, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (cbRememberMe.isChecked()) {
            editor.putString("email", email); // שמירת המייל כמזהה כניסה
            editor.putString("password", password);
            editor.putBoolean("remember", true);
        } else {
            editor.clear(); // מחיקת הנתונים הישנים באופן מוחלט כדי למנוע בלבול שמות בין משתמשים
        }
        editor.apply(); // שמירה אסינכרונית מאובטחת
    }

    /**
     * פעולה מעודכנת: loginUser
     * תפקיד: ביצוע אימות ישיר מול Firebase Auth באמצעות אימייל וסיסמה, ללא צורך בשאילתת Firestore מוקדמת וחסומה.
     */
    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        // שמירה או ניקוי הפרטים בזיכרון המקומי בהתאם לבחירת ה-CheckBox
        saveDetails(email, password);

        // ביצוע התחברות ישירה ומאובטחת ל-Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();

                        // מעבר למסך הבית (HomeActivity) וסגירת מסך ההתחברות
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "שגיאה בהתחברות: וודא שהפרטים נכונים", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * פתרון באג חזרה: מימוש כפתור החזור המובנה של המכשיר (Back Button)
     * תפקיד: דריסת הפעולה הדיפולטיבית כדי שגם לחיצה על כפתור הניווט הפיזי של הטלפון
     * תזרוק את המשתמש בצורה מאובטחת ומאופסת ל-MainActivity ללא תקלות Auto-Login.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }
}