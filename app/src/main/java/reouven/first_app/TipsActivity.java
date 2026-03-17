package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;

public class TipsActivity extends AppCompatActivity {

    private TextView tvDailyTipTitle, tvDailyTipContent, tvPrinciplesTitle;
    private View mainLayout;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    private final String[] dailyTitles = {
            "חוק ה-72", "הכוח של 100 ש''ח", "אינפלציה שוחקת", "הפסיכולוגיה של ההפסד", "מדד ה-S&P 500"
    };

    private final String[] dailyContents = {
            "חלקו 72 בריבית השנתית ותדעו תוך כמה שנים הכסף שלכם יכפיל את עצמו!",
            "אפילו 100 ש''ח בחודש לאורך 30 שנה יכולים להפוך לעשרות אלפי שקלים בזכות הריבית דריבית.",
            "כסף בעו''ש מאבד ערך בגלל עליית המחירים. השקעה היא הדרך להגן עליו.",
            "הפחד מהפסד גורם לאנשים למכור כשהשוק יורד - זה לרוב הזמן הכי גרוע למכור.",
            "זהו מדד של 500 החברות הגדולות בארה''ב. היסטורית, הוא הניב תשואה יפה לטווח ארוך."
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // בדיקה: האם המשתמש הוא אורח?
        if (user == null || user.isAnonymous()) {
            showGuestBlockedDialog();
        }

        setContentView(R.layout.activity_tips);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBar(); // קריאה לפונקציית הבר העליון המעודכנת
        applyCustomColorMode();
        setDailyTip();
        setupNavigation();
    }

    private void showGuestBlockedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("תוכן לאורחים חסום")
                .setMessage("דף הטיפים והעקרונות הפיננסיים זמין למשתמשים רשומים בלבד.\nרוצה להירשם עכשיו ולקבל את כל הטיפים?")
                .setCancelable(false)
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                    finish();
                })
                .setNegativeButton("חזור", (d, w) -> {
                    finish();
                })
                .show();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvDailyTipTitle = findViewById(R.id.tvDailyTipTitle);
        tvDailyTipContent = findViewById(R.id.tvDailyTipContent);
        tvPrinciplesTitle = findViewById(R.id.tvPrinciplesTitle);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        // כפתור המידע החדש של דף הטיפים
        View btnInfo = findViewById(R.id.btnHelpInfoTips);
        if (btnInfo != null) btnInfo.setOnClickListener(v -> showTipsInfoDialog());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(this::showPopupMenu);
    }

    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();
            FirebaseUser user = mAuth.getCurrentUser();

            if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_profile) {
                if (user == null || user.isAnonymous()) {
                    showGuestBlockedDialog();
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            } else if (id == R.id.menu_contact) {
                showContactDialog();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
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

    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("יצירת קשר")
                .setMessage("צריכים עזרה או יש לכם הצעה לשיפור? אנחנו כאן בשבילכם.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc - טיפים");
                    try {
                        startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית, החזרי משכנתא ותחזיות בצורה הכי מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";

        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage(aboutMessage)
                .setPositiveButton("סגור", null)
                .show();
    }

    private void showTipsInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("טיפים ועקרונות")
                .setMessage("בדף זה תמצא טיפ יומי משתנה ועקרונות ברזל להשקעה נכונה.\n\nהמידע מבוסס על ידע פיננסי מקצועי ונועד לעזור לך לבנות אופק כלכלי יציב.")
                .setPositiveButton("הבנתי", null)
                .show();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);

        CardView dailyCard = findViewById(R.id.cardDailyTip);
        TextView dailyLabel = findViewById(R.id.tvDailyTipLabel);

        CardView[] cards = {
                findViewById(R.id.card1), findViewById(R.id.card2),
                findViewById(R.id.card3), findViewById(R.id.card4),
                findViewById(R.id.card5)
        };

        TextView[] contents = {
                findViewById(R.id.tvCard1Content), findViewById(R.id.tvCard2Content),
                findViewById(R.id.tvCard3Content), findViewById(R.id.tvCard4Content),
                findViewById(R.id.tvCard5Content)
        };

        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (tvPrinciplesTitle != null) tvPrinciplesTitle.setTextColor(Color.WHITE);
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.BLACK);
            if (dailyCard != null) dailyCard.setCardBackgroundColor(Color.parseColor("#1A237E"));
            if (dailyLabel != null) dailyLabel.setTextColor(Color.parseColor("#9FA8DA"));
            if (tvDailyTipTitle != null) tvDailyTipTitle.setTextColor(Color.WHITE);
            if (tvDailyTipContent != null) tvDailyTipContent.setTextColor(Color.LTGRAY);

            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) cards[i].setCardBackgroundColor(Color.parseColor("#1E1E1E"));
                if (contents[i] != null) contents[i].setTextColor(Color.parseColor("#B0B0B0"));
            }
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F7FA"));
            if (tvPrinciplesTitle != null) tvPrinciplesTitle.setTextColor(Color.parseColor("#455A64"));
            if (bottomNav != null) bottomNav.setBackgroundColor(Color.WHITE);
            if (dailyCard != null) dailyCard.setCardBackgroundColor(Color.parseColor("#E8EAF6"));
            if (dailyLabel != null) dailyLabel.setTextColor(Color.parseColor("#3F51B5"));
            if (tvDailyTipTitle != null) tvDailyTipTitle.setTextColor(Color.parseColor("#1A237E"));
            if (tvDailyTipContent != null) tvDailyTipContent.setTextColor(Color.parseColor("#333333"));

            for (int i = 0; i < cards.length; i++) {
                if (cards[i] != null) cards[i].setCardBackgroundColor(Color.WHITE);
                if (contents[i] != null) contents[i].setTextColor(Color.parseColor("#666666"));
            }
        }
    }

    private void setDailyTip() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int index = dayOfYear % dailyTitles.length;
        if (tvDailyTipTitle != null) tvDailyTipTitle.setText(dailyTitles[index]);
        if (tvDailyTipContent != null) tvDailyTipContent.setText(dailyContents[index]);
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isCurrentlyDark = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !isCurrentlyDark).apply();
        recreate();
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tips);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                FirebaseUser user = mAuth.getCurrentUser();

                if (id == R.id.nav_history && (user == null || user.isAnonymous())) {
                    showGuestBlockedDialog();
                    return false;
                }

                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
                else if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
                else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return id == R.id.nav_tips;
            });
        }
    }
}