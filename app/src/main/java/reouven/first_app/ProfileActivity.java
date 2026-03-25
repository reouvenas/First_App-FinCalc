package reouven.first_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvName, tvEmail, tvPhone, tvProfileLetter, tvJoinDate, tvCalcCount, tvResetPassword;
    private Button btnEditProfile;
    private final String dbUrl = "https://androidproject-91b41-default-rtdb.firebaseio.com";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- חסימת אורחים - התחלה ---
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.isAnonymous()) {
            Toast.makeText(this, "פרופיל אישי זמין למשתמשים רשומים בלבד", Toast.LENGTH_LONG).show();
            finish(); // סוגר את הדף ומחזיר את המשתמש אחורה
            return;   // עוצר את המשך טעינת הדף
        }
        // --- חסימת אורחים - סוף ---

        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupButtons();
        setupBottomNavigation();
        loadUserData();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        tvPhone = findViewById(R.id.tvProfilePhone);
        tvProfileLetter = findViewById(R.id.tvProfileLetter);
        tvJoinDate = findViewById(R.id.tvJoinDate);
        tvCalcCount = findViewById(R.id.tvCalcCount);
        tvResetPassword = findViewById(R.id.tvResetPassword);
        btnEditProfile = findViewById(R.id.btnEditProfile);
    }

    private void setupButtons() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(this::showPopupMenu);
        }

        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditDialog());

        if (tvResetPassword != null) {
            tvResetPassword.setOnClickListener(v -> {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null && user.getEmail() != null) {
                    mAuth.sendPasswordResetEmail(user.getEmail())
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "אימייל לאיפוס סיסמה נשלח!", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });
        }
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_profile) {
                return true;
            } else if (id == R.id.menu_contact) {
                // כאן הנחתי שיש לך מחלקת עזר כזו לפי הקוד הקודם
                try {
                    NavigationHelper.showContactDialog(this);
                } catch (Exception e) {
                    Toast.makeText(this, "יצירת קשר", Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (id == R.id.menu_logout) {
                mAuth.signOut();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvEmail.setText(user.getEmail());
        if (user.getMetadata() != null) {
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            tvJoinDate.setText(sdf.format(new Date(creationTimestamp)));
        }

        FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(user.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);
                            if (name != null && !name.isEmpty()) {
                                tvName.setText(name);
                                tvProfileLetter.setText(name.substring(0, 1).toUpperCase());
                            }
                            if (phone != null) tvPhone.setText(phone);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });

        FirebaseFirestore.getInstance().collection("saved_plans")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        tvCalcCount.setText(String.valueOf(value.size()));
                    } else {
                        tvCalcCount.setText("0");
                    }
                });
    }

    private void showEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("עריכת פרופיל");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 40, 60, 10);

        final EditText inputName = new EditText(this);
        inputName.setHint("שם מלא");
        inputName.setText(tvName.getText().toString());
        layout.addView(inputName);

        final EditText inputPhone = new EditText(this);
        inputPhone.setHint("מספר טלפון");
        inputPhone.setText(tvPhone.getText().toString());
        layout.addView(inputPhone);

        builder.setView(layout);
        builder.setPositiveButton("שמור", (dialog, which) -> {
            updateProfile(inputName.getText().toString().trim(), inputPhone.getText().toString().trim());
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    private void updateProfile(String name, String phone) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phone", phone);
            FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(user.getUid())
                    .updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "הפרופיל עודכן!", Toast.LENGTH_SHORT).show());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.getMenu().setGroupCheckable(0, false, true);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
                else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);
                else if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}