package reouven.first_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

/**
 * מחלקה: ForgotPasswordActivity
 * תפקיד: ניהול תהליך שחזור סיסמה למשתמשים ששכחו את פרטי הגישה שלהם.
 * המחלקה שולחת בקשת איפוס ל-Firebase Auth ושולחת אימייל אוטומטי למשתמש.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    // הגדרת רכיבי הממשק (UI)
    private EditText etEmail;      // שדה להזנת כתובת האימייל לשחזור
    private Button btnReset;       // כפתור לאישור ושליחת המייל
    private TextView tvBack;       // קישור טקסטואלי לחזרה למסך ההתחברות
    private ImageButton ibBackArrow; // חץ גרפי לחזרה אחורה
    private FirebaseAuth mAuth;    // אובייקט Firebase לניהול אימות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * פעולת onCreate: אתחול המסך, קישור רכיבי ה-XML והגדרת הלוגיקה של השחזור.
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // הסתרת סרגל הפעולות (ActionBar) המובנה אם קיים
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // חיבור הרכיבים מה-XML למשתני הג'אווה
        etEmail = findViewById(R.id.etForgotEmail);
        btnReset = findViewById(R.id.btnResetPassword);
        tvBack = findViewById(R.id.tvBackToLogin);
        ibBackArrow = findViewById(R.id.ibBackArrow);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // הגדרת לחיצה על חץ החזרה - סוגר את המסך הנוכחי וחוזר לאחור
        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> finish());
        }

        // הגדרת לחיצה על הטקסט "חזרה להתחברות"
        tvBack.setOnClickListener(v -> finish());

        /**
         * לחיצה על כפתור שחזור סיסמה:
         * 1. בדיקת תקינות הקלט (שדה לא ריק).
         * 2. שליחת בקשה לשרתי Firebase.
         * 3. הצגת הודעה למשתמש על הצלחה או שגיאה.
         */
        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // בדיקה בסיסית שהמשתמש הזין כתובת כלשהי
            if (email.isEmpty()) {
                Toast.makeText(this, "נא להזין אימייל", Toast.LENGTH_SHORT).show();
                return;
            }

            // פנייה ל-Firebase לשליחת מייל איפוס סיסמה
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // הצלחה: המייל נשלח בהצלחה
                            Toast.makeText(this, "קישור נשלח לאימייל שלך!", Toast.LENGTH_LONG).show();
                            finish(); // חזרה אוטומטית למסך ההתחברות לאחר השליחה
                        } else {
                            // כישלון: כתובת לא קיימת או בעיה בתקשורת
                            Toast.makeText(this, "שגיאה: וודא שהאימייל תקין ורשום במערכת", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}