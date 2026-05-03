package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

/**
 * מחלקה: MainActivity
 * תפקיד: מסך הפתיחה של האפליקציה.
 * מנהל את הניתוב הראשוני של המשתמש (מחובר/לא מחובר) ומציע אפשרויות כניסה שונות,
 * כולל אימות מול שרתי Google וחיבורם ל-Firebase.
 */
public class MainActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private Button btnGoToLogin, btnGoToRegister, btnGuest;
    private ImageButton btnGoogle;

    // אובייקטים לאימות (Authentication)
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // קוד זיהוי עבור תוצאת הפעילות של גוגל
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // אתחול Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        /**
         * מנגנון "אוטו-לוגין":
         * אם המשתמש כבר ביצע התחברות בעבר והוא עדיין רשום במערכת,
         * נדלג על מסך הכניסה ונעביר אותו ישר למסך הבית.
         */
        if (mAuth.getCurrentUser() != null) {
            goToHome("registered");
            return;
        }

        // טעינת עיצוב המסך רק אם המשתמש לא מחובר
        setContentView(R.layout.activity_main);

        /**
         * הגדרת אפשרויות ההתחברות של גוגל:
         * מבקשים את ה-ID Token (כדי ש-Firebase יוכל לאמת אותו) ואת כתובת המייל.
         */
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // מזהה הלקוח מפרויקט ה-Firebase
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // קישור רכיבי ה-UI
        btnGoToLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);
        btnGoogle = findViewById(R.id.btnGoogle);

        // --- הגדרת מאזינים ללחיצות ---

        // כפתור התחברות עם גוגל
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        // מעבר למסך התחברות רגיל
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // מעבר למסך הרשמה
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // כניסה כאורח (ללא אימות)
        btnGuest.setOnClickListener(v -> goToHome("guest"));
    }

    /**
     * פתיחת חלון בחירת חשבון גוגל של המכשיר.
     */
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /**
     * קבלת התוצאה מחלון ההתחברות של גוגל.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // בדיקה אם חזרנו מהתחברות גוגל
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // המרת התוצאה לאובייקט חשבון גוגל
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    // שלב ב': אימות ה-Token מול Firebase
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "שגיאה בחיבור גוגל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * אימות ה-Token שהתקבל מגוגל והפיכתו למשתמש Firebase רשמי.
     */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "התחברת עם גוגל!", Toast.LENGTH_SHORT).show();
                        goToHome("registered");
                    } else {
                        Toast.makeText(MainActivity.this, "האימות מול פיירבייס נכשל", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * פעולה: goToHome
     * תפקיד: העברת המשתמש למסך הבית (HomeActivity).
     * @param type - מגדיר האם המשתמש נכנס כאורח או כמשתמש רשום.
     */
    private void goToHome(String type) {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.putExtra("USER_TYPE", type);
        startActivity(intent);

        // אם המשתמש רשום, נסגור את ה-Activity הנוכחי כדי שלא יוכל לחזור אחורה למסך הכניסה
        if (type.equals("registered")) {
            finish();
        }
    }
}