package reouven.first_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnReset;
    private TextView tvBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etEmail = findViewById(R.id.etForgotEmail);
        btnReset = findViewById(R.id.btnResetPassword);
        tvBack = findViewById(R.id.tvBackToLogin);
        mAuth = FirebaseAuth.getInstance();

        btnReset.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "נא להזין אימייל", Toast.LENGTH_SHORT).show();
                return;
            }

            // פונקציית הקסם של פיירבייס לשחזור סיסמה
            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "קישור נשלח לאימייל שלך!", Toast.LENGTH_LONG).show();
                            finish(); // סוגר את המסך ומחזיר את המשתמש ללוגין
                        } else {
                            Toast.makeText(this, "שגיאה: וודא שהאימייל תקין", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        tvBack.setOnClickListener(v -> finish());
    }
}