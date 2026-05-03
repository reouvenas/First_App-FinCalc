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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

/**
 * מחלקה: LoginActivity
 * תפקיד: ניהול מסך ההתחברות לאפליקציה.
 * המחלקה מאפשרת התחברות באמצעות שם משתמש (על ידי המרת השם לאימייל ב-Database)
 * ותמיכה באפשרות "זכור אותי" לשמירת פרטים מקומית.
 */
public class LoginActivity extends AppCompatActivity {

    // רכיבי הממשק
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private ImageButton ibBackArrow;
    private CheckBox cbRememberMe;

    // אובייקטים לניהול נתונים
    private FirebaseAuth mAuth;            // אימות מול Firebase
    private DatabaseReference mDatabase;   // גישה ל-Database לשליפת אימייל לפי שם משתמש
    private SharedPreferences sharedPreferences; // זיכרון מקומי לשמירת פרטי התחברות

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול שירותי Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // אתחול זיכרון פנימי תחת השם "LoginPrefs"
        sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE);

        // חיבור הרכיבים מה-XML למשתני הג'אווה
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLoginSubmit);
        tvGoToRegister = findViewById(R.id.tvGoToRegisterFromLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        ibBackArrow = findViewById(R.id.ibBackArrow);
        cbRememberMe = findViewById(R.id.cbRememberMe);

        // טעינה אוטומטית של פרטים אם המשתמש בחר "זכור אותי" בעבר
        loadRememberedDetails();

        // הגדרת לחיצה על חץ החזרה
        if (ibBackArrow != null) {
            ibBackArrow.setOnClickListener(v -> finish());
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
        String savedUser = sharedPreferences.getString("username", "");
        String savedPass = sharedPreferences.getString("password", "");
        boolean isRemembered = sharedPreferences.getBoolean("remember", false);

        if (isRemembered) {
            etUsername.setText(savedUser);
            etPassword.setText(savedPass);
            cbRememberMe.setChecked(true);
        }
    }

    /**
     * פעולה: saveDetails
     * תפקיד: שמירה או מחיקה של פרטי המשתמש מהזיכרון המקומי בהתאם למצב ה-CheckBox.
     */
    private void saveDetails(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (cbRememberMe.isChecked()) {
            editor.putString("username", username);
            editor.putString("password", password);
            editor.putBoolean("remember", true);
        } else {
            editor.clear(); // מחיקת הנתונים אם המשתמש לא מעוניין שיזכרו אותו
        }
        editor.apply(); // שמירה אסינכרונית
    }

    /**
     * פעולה: loginUser
     * תפקיד: שלב א' של ההתחברות. חיפוש שם המשתמש ב-Realtime Database כדי למצוא את האימייל המשויך אליו.
     */
    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא שם משתמש וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        // יצירת שאילתה לחיפוש המשתמש לפי השדה "name"
        Query query = mDatabase.orderByChild("name").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // המשתמש נמצא - שולפים את האימייל שלו
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);

                        // שמירת הפרטים בזיכרון המקומי (אם סומן "זכור אותי")
                        saveDetails(username, password);

                        // מעבר לשלב ב' - התחברות ל-Firebase Auth
                        performFirebaseLogin(email, password);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "שם משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "שגיאה בחיבור למסד הנתונים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * פעולה: performFirebaseLogin
     * תפקיד: שלב ב' של ההתחברות. ניסיון כניסה עם האימייל והסיסמה.
     */
    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();

                        // מעבר למסך הבית וסגירת מסך ההתחברות
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "שגיאה בהתחברות: וודא שהסיסמה נכונה", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}