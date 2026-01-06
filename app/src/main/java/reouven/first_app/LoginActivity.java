package reouven.first_app;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
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

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // אתחול Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // חיבור רכיבים מה-XML
        etUsername = findViewById(R.id.etLoginUsername);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLoginSubmit);
        tvGoToRegister = findViewById(R.id.tvGoToRegisterFromLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // הוספת קו תחתון למילה "הרשמה" ומעבר לדף הרשמה
        if (tvGoToRegister != null) {
            tvGoToRegister.setPaintFlags(tvGoToRegister.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            tvGoToRegister.setOnClickListener(v -> {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            });
        }

        // --- כאן החיבור ששאלת עליו! ---
        // לחיצה על "שכחת סיסמה?" תעביר אותנו לדף השחזור
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            });
        }

        // כפתור התחברות
        btnLogin.setOnClickListener(v -> {
            loginUser();
        });
    }

    private void loginUser() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "נא למלא שם משתמש וסיסמה", Toast.LENGTH_SHORT).show();
            return;
        }

        // חיפוש האימייל לפי שם המשתמש ב-Database
        Query query = mDatabase.orderByChild("name").equalTo(username);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        performFirebaseLogin(email, password);
                    }
                } else {
                    Toast.makeText(LoginActivity.this, "שם משתמש לא נמצא", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(LoginActivity.this, "שגיאה בחיבור", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "התחברת בהצלחה!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "סיסמה שגויה", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}