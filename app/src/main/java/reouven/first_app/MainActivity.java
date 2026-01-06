package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private Button btnGoToLogin, btnGoToRegister, btnGuest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- קוד אוטו-לוגין ---
        // אם המשתמש כבר מחובר, הוא עובר ישר למחשבון כמשתמש רשום
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            intent.putExtra("USER_TYPE", "registered"); // סימון כמשתמש רשום
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        // אתחול הכפתורים
        btnGoToLogin = findViewById(R.id.btnLogin);
        btnGoToRegister = findViewById(R.id.btnRegister);
        btnGuest = findViewById(R.id.btnGuest);

        // כפתור התחברות
        btnGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
        });

        // כפתור הרשמה
        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterActivity.class));
        });

        // כפתור כניסה כאורח (החיבור החדש)
        btnGuest.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);

            // אנחנו שולחים "מפתח" שאומר לדף הבא: "הבן אדם הזה הוא אורח"
            intent.putExtra("USER_TYPE", "guest");

            startActivity(intent);
            // לא עושים finish() כדי שהאורח יוכל לחזור אחורה ולהירשם אם ירצה
        });
    }
}