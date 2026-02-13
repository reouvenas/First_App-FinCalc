package reouven.first_app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // הסתרת ה-ActionBar המובנה אם קיים
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // קישור הרכיבים מה-XML
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);

        // הגדרת כפתור חזור (נמצא בתוך ה-top_bar)
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        // הסתרת כפתור התפריט בדף הפרופיל עצמו
        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setVisibility(View.GONE);

        loadUserData();
    }

    private void loadUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            tvEmail.setText(user.getEmail());

            // הצגת שם (אם קיים ב-Firebase)
            String name = user.getDisplayName();
            tvName.setText((name != null && !name.isEmpty()) ? name : "משתמש InvestCalc");

            // טלפון - כרגע הודעה זמנית
            tvPhone.setText("טלפון: טרם הוגדר");
        }
    }
}