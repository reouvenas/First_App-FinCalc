package reouven.first_app;

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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * מחלקה: ProfileActivity
 * תפקיד: הצגת פרופיל המשתמש, עריכת פרטים אישיים, וצפייה בסטטיסטיקות (כמות חישובים) מתוך Cloud Firestore.
 * תכונות מרכזיות:
 * 1. חסימת גישה לאורחים (Anonymous Users).
 * 2. הצגת תאריך הצטרפות מתוך ה-Metadata של Firebase Auth.
 * 3. סנכרון מלא ובזמן אמת מול ארכיטקטורת הנתונים המעודכנת ב-Firestore.
 */
public class ProfileActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private TextView tvName, tvEmail, tvPhone, tvProfileLetter, tvJoinDate, tvCalcCount, tvResetPassword;
    private Button btnEditProfile;

    // הגדרות Firebase Auth ו-Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- אבטחה: חסימת אורחים ---
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.isAnonymous()) {
            Toast.makeText(this, "פרופיל אישי זמין למשתמשים רשומים בלבד", Toast.LENGTH_LONG).show();
            finish(); // סגירת הדף אם המשתמש לא מחובר כראוי
            return;
        }

        setContentView(R.layout.activity_profile);

        // הסתרת שורת הפעולה העליונה המובנית לטובת עיצוב מותאם אישית
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupButtons();
        setupBottomNavigation();
        loadUserData();
    }

    /**
     * פעולה: initViews
     * תפקיד: קישור משתני ה-Java לרכיבי ה-XML.
     */
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

    /**
     * פעולה: setupButtons
     * תפקיד: הגדרת מאזינים לכפתורי החזרה, התפריט, העריכה ואיפוס הסיסמה.
     */
    private void setupButtons() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(this::showPopupMenu);
        }

        if (btnEditProfile != null) btnEditProfile.setOnClickListener(v -> showEditDialog());

        // שליחת אימייל לאיפוס סיסמה ישירות מהפרופיל
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

    /**
     * פעולה מעודכנת: loadUserData
     * תפקיד: משיכת נתונים מלאה ומסונכרנת מתוך Cloud Firestore:
     * 1. Auth - אימייל ותאריך יצירה ראשוני.
     * 2. Firestore (אוסף users) - שם מלא ומספר טלפון מעודכן בזמן אמת.
     * 3. Firestore (תת-אוסף history) - ספירה דינמית של כמות החישובים השמורים השייכים ל-UID.
     */
    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // 1. נתוני אימות בסיסיים מתוך ה-Auth Metadata
        tvEmail.setText(user.getEmail());
        if (user.getMetadata() != null) {
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
            tvJoinDate.setText(sdf.format(new Date(creationTimestamp)));
        }

        // 2. עדכון חכם: משיכת פרטים אישיים (שם וטלפון) בזמן אמת מתוך Cloud Firestore
        db.collection("users").document(user.getUid())
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        String name = snapshot.getString("name");
                        String phone = snapshot.getString("phone");

                        if (name != null && !name.isEmpty()) {
                            tvName.setText(name);
                            // עדכון האות הראשונה בעיגול הויזואלי של הפרופיל
                            tvProfileLetter.setText(name.substring(0, 1).toUpperCase());
                        }
                        if (phone != null && !phone.isEmpty()) {
                            tvPhone.setText(phone);
                        } else {
                            tvPhone.setText("לא עודכן מספר טלפון");
                        }
                    }
                });

        // 3. עדכון חכם: ספירת כמות החישובים השמורים מתוך תת-האוסף הפנימי history של המשתמש
        db.collection("users").document(user.getUid()).collection("history")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        // שליפת גודל הרשימה (כמות המסמכים שנמצאים בתוך תת-האוסף)
                        tvCalcCount.setText(String.valueOf(value.size()));
                    } else {
                        tvCalcCount.setText("0");
                    }
                });
    }

    /**
     * פעולה: showEditDialog
     * תפקיד: הצגת דיאלוג עם שדות קלט לעדכון שם המשתמש ומספר הטלפון.
     */
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
        // מונע הצגת טקסט ברירת המחדל בשדה הקלט בזמן עריכה
        String currentPhone = tvPhone.getText().toString();
        if (currentPhone.equals("לא עודכן מספר טלפון")) currentPhone = "";
        inputPhone.setText(currentPhone);
        layout.addView(inputPhone);

        builder.setView(layout);
        builder.setPositiveButton("שמור", (dialog, which) -> {
            updateProfile(inputName.getText().toString().trim(), inputPhone.getText().toString().trim());
        });
        builder.setNegativeButton("ביטול", null);
        builder.show();
    }

    /**
     * פעולה מעודכנת: updateProfile
     * תפקיד: עדכון הנתונים החדשים ישירות בתוך מסמך המשתמש (UID) ב-Cloud Firestore.
     */
    private void updateProfile(String name, String phone) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phone", phone);

            // שמירה מאובטחת ישירות בתוך אוסף users תחת ה-UID הספציפי
            db.collection("users").document(user.getUid())
                    .update(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "הפרופיל עודכן בהצלחה!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "שגיאה בעדכון הנתונים", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * פעולה: setupBottomNavigation
     * תפקיד: ניהול הניווט בתפריט התחתון ומעבר בין המסכים השונים.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.getMenu().setGroupCheckable(0, false, true);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, CalcRibitActivity.class); // שינוי למסך המחשבון הראשי
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

    /**
     * פעולה: showPopupMenu
     * תפקיד: הצגת תפריט אפשרויות (מצב כהה, התנתקות וכו') בלחיצה על כפתור התפריט בראש הדף.
     */
    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
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
}