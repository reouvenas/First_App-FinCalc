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

public class MainActivity extends AppCompatActivity {

    private Button btnGoToLogin, btnGoToRegister, btnGuest;
    private ImageButton btnGoogle; // כפתור הגוגל מה-XML שלך

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001; // קוד זיהוי לחלונית גוגל

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();

        // --- קוד אוטו-לוגין ---
        if (mAuth.getCurrentUser() != null) {
            goToHome("registered");
            return;
        }

        setContentView(R.layout.activity_main);

        // --- אתחול הגדרות גוגל (משתמש ב-SHA-1 מה-JSON) ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // מזהה אוטומטי מהקובץ שהחלפת
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // --- אתחול כפתורים ---
        btnGoToLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);
        btnGoogle = findViewById(R.id.btnGoogle); // ה-ImageButton מה-XML

        // --- מאזינים ללחיצות ---

        // כפתור גוגל
        btnGoogle.setOnClickListener(v -> {
            signInWithGoogle();
        });

        // כפתור התחברות רגיל
        btnGoToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            // מונע פתיחה של אותו דף פעמיים אם הוא כבר פתוח
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

// כפתור הרשמה
        btnGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        // כפתור כניסה כאורח
        btnGuest.setOnClickListener(v -> {
            goToHome("guest");
        });
    }

    // פונקציה לפתיחת חלונית בחירת חשבון גוגל
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    // קבלת התוצאה מחלונית גוגל
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // הצלחנו לקבל חשבון גוגל
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                // אם הגעת לפה עם שגיאה 10 או 12500 - סימן שיש בעיה ב-SHA-1 או ב-JSON
                Toast.makeText(this, "שגיאה בחיבור גוגל: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // אימות הטוקן של גוגל מול פיירבייס
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // התחברות הצליחה!
                        Toast.makeText(MainActivity.this, "התחברת עם גוגל!", Toast.LENGTH_SHORT).show();
                        goToHome("registered");
                    } else {
                        Toast.makeText(MainActivity.this, "האימות מול פיירבייס נכשל", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // פונקציית עזר למעבר דף
    private void goToHome(String type) {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        intent.putExtra("USER_TYPE", type);
        startActivity(intent);
        if (type.equals("registered")) {
            finish();
        }
    }
}