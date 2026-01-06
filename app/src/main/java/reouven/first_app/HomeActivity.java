package reouven.first_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class HomeActivity extends AppCompatActivity {

    private TextView tvWelcomeName;
    private Button btnLogout;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tvWelcomeName = findViewById(R.id.tvWelcomeName);
        btnLogout = findViewById(R.id.btnLogout);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // 1. קבלת סוג המשתמש מה-Intent
        String userType = getIntent().getStringExtra("USER_TYPE");

        // 2. בדיקה אם מדובר באורח
        if ("guest".equals(userType)) {
            tvWelcomeName.setText("שלום, אורח!");
            btnLogout.setText("חזרה למסך ראשי"); // שינוי טקסט הכפתור לאורח
            Toast.makeText(this, "נכנסת במצב אורח", Toast.LENGTH_SHORT).show();
        }
        // 3. אם זה לא אורח, בודקים משתמש רשום ב-Firebase
        else if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(userId);

            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String name = dataSnapshot.child("name").getValue(String.class);
                        tvWelcomeName.setText("שלום, " + name + "!");
                    } else {
                        tvWelcomeName.setText("שלום!");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    tvWelcomeName.setText("שגיאה בטעינה");
                }
            });
        }

        // כפתור התנתקות / חזרה
        btnLogout.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                mAuth.signOut();
            }
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        });
    }
}