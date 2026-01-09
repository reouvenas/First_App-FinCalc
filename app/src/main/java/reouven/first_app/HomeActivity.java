package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private FirebaseAuth mAuth;
    private CardView cardCompoundInterest;
    private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // אתחול רכיבים
        mAuth = FirebaseAuth.getInstance();
        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        cardCompoundInterest = findViewById(R.id.cardCompoundInterest);
        btnLogout = findViewById(R.id.btnLogout);

        // בדיקה איזה סוג משתמש נכנס
        String userType = getIntent().getStringExtra("USER_TYPE");
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if ("guest".equals(userType)) {
            tvWelcomeName.setText("שלום, אורח!");
        } else if (currentUser != null) {
            // מנסה להביא שם, אם אין מציג אימייל
            String name = currentUser.getDisplayName();
            if (name == null || name.isEmpty()) {
                name = currentUser.getEmail();
            }
            tvWelcomeName.setText("שלום, " + name + "!");
        }

        // לחיצה על המחשבון
        cardCompoundInterest.setOnClickListener(v -> {
            // כאן נחבר בעתיד את המעבר למסך המחשבון החדש
            Toast.makeText(this, "עובר למחשבון ריבית דריבית", Toast.LENGTH_SHORT).show();
        });

        // כפתור התנתקות
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}