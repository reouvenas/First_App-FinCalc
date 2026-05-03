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
import java.util.Locale;

/**
 * מחלקה: TipsActivity
 * תפקיד: הצגת טיפים פיננסיים יומיים ועקרונות יסוד.
 * יישום: כולל לוגיקת תפריט מלאה, יצירת קשר, אודות וחסימת אורחים.
 */
public class TipsActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש
    private TextView tvDailyTipTitle, tvDailyTipContent, tvPrinciplesTitle;
    private View mainLayout;
    private BottomNavigationView bottomNav;
    private FirebaseAuth mAuth;

    // מאגרי מידע לטיפים היומיים
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
        // החלת הגדרות מצב לילה לפני טעינת ה-Layout
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        // חסימת גישה ראשונית לאורחים
        if (user == null || user.isAnonymous()) {
            showGuestBlockedDialog();
        }

        setContentView(R.layout.activity_tips);

        // הסתרת ה-ActionBar המובנית
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBar();
        applyCustomColorMode();
        setDailyTip();
        setupNavigation();
    }

    /**
     * פעולה: initViews
     * תפקיד: חיבור רכיבי ה-XML ל-Java.
     */
    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        tvDailyTipTitle = findViewById(R.id.tvDailyTipTitle);
        tvDailyTipContent = findViewById(R.id.tvDailyTipContent);
        tvPrinciplesTitle = findViewById(R.id.tvPrinciplesTitle);
        bottomNav = findViewById(R.id.bottom_navigation);
    }

    /**
     * פעולה: setupTopBar
     * תפקיד: הגדרת כפתורי החזרה, המידע והתפריט בראש המסך.
     */
    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        View btnInfo = findViewById(R.id.btnHelpInfoTips);
        if (btnInfo != null) btnInfo.setOnClickListener(v -> showTipsInfoDialog());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) btnMenu.setOnClickListener(this::showPopupMenu);
    }

    /**
     * פעולה: showPopupMenu
     * תפקיד: ניהול התפריט העליון כולל בדיקת סטטוס אורח עבור הפרופיל.
     */
    private void showPopupMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int id = menuItem.getItemId();

            if (id == R.id.menu_profile) {
                if (isUserGuest()) {
                    showGuestRestrictionDialog("הפרופיל שמור למשתמשים רשומים.");
                } else {
                    startActivity(new Intent(this, ProfileActivity.class));
                }
                return true;
            } else if (id == R.id.menu_dark_mode) {
                toggleDarkMode();
                return true;
            } else if (id == R.id.menu_about) {
                showAboutDialog();
                return true;
            } else if (id == R.id.menu_contact) {
                showContactDialog();
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

    /**
     * פעולה: showContactDialog
     * תפקיד: פתיחת אפשרות ליצירת קשר במייל.
     */
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

    /**
     * פעולה: showAboutDialog
     * תפקיד: הצגת מידע על האפליקציה.
     */
    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית ותחזיות בצורה מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";

        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage(aboutMessage)
                .setPositiveButton("סגור", null)
                .show();
    }

    /**
     * פעולה: showGuestRestrictionDialog
     * תפקיד: הודעת חסימה לפעולות ספציפיות בתפריט עבור אורחים.
     */
    private void showGuestRestrictionDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("פעולה חסומה")
                .setMessage(message + "\nרוצה להירשם עכשיו?")
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private boolean isUserGuest() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user == null || user.isAnonymous();
    }

    private void showGuestBlockedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("תוכן לאורחים חסום")
                .setMessage("דף הטיפים והעקרונות הפיננסיים זמין למשתמשים רשומים בלבד.\nרוצה להירשם עכשיו?")
                .setCancelable(false)
                .setPositiveButton("להרשמה", (d, w) -> {
                    startActivity(new Intent(this, RegisterActivity.class));
                    finish();
                })
                .setNegativeButton("חזור", (d, w) -> finish())
                .show();
    }

    private void setDailyTip() {
        int dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        int index = dayOfYear % dailyTitles.length;
        if (tvDailyTipTitle != null) tvDailyTipTitle.setText(dailyTitles[index]);
        if (tvDailyTipContent != null) tvDailyTipContent.setText(dailyContents[index]);
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
            if (tvDailyTipTitle != null) tvDailyTipTitle.setTextColor(Color.WHITE);
            if (tvDailyTipContent != null) tvDailyTipContent.setTextColor(Color.LTGRAY);

            for (CardView card : cards) {
                if (card != null) card.setCardBackgroundColor(Color.parseColor("#1E1E1E"));
            }
            for (TextView content : contents) {
                if (content != null) content.setTextColor(Color.parseColor("#B0B0B0"));
            }
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F7FA"));
            if (tvPrinciplesTitle != null) tvPrinciplesTitle.setTextColor(Color.parseColor("#455A64"));
            if (dailyCard != null) dailyCard.setCardBackgroundColor(Color.parseColor("#E8EAF6"));

            for (CardView card : cards) {
                if (card != null) card.setCardBackgroundColor(Color.WHITE);
            }
        }
    }

    private void setupNavigation() {
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_tips);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                FirebaseUser user = mAuth.getCurrentUser();

                if (id == R.id.nav_history && (user == null || user.isAnonymous())) {
                    showGuestRestrictionDialog("ההיסטוריה שמורה למשתמשים רשומים.");
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

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isCurrentlyDark = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !isCurrentlyDark).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void showTipsInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("טיפים ועקרונות")
                .setMessage("בדף זה תמצא טיפ יומי משתנה ועקרונות ברזל להשקעה נכונה.")
                .setPositiveButton("הבנתי", null)
                .show();
    }
}