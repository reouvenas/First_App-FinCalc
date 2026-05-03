package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * מחלקה: ChatActivity
 * תפקיד: ניהול ממשק צ'אט מול מודל ה-AI של Google (Gemini) לצורך ייעוץ פיננסי.
 * המחלקה תומכת במצב כהה, שמירת היסטוריית שיחות מקומית וניווט מלא.
 */
public class ChatActivity extends AppCompatActivity {

    // רכיבי ממשק המשתמש (UI)
    private LinearLayout chatContainer; // מיכל להצגת בועות ההודעות
    private EditText etMessage;         // שדה הזנת טקסט למשתמש
    private ScrollView scrollView;      // מאפשר גלילה של היסטוריית הצ'אט
    private View mainLayout;            // הרקע הראשי של המסך
    private boolean isDarkMode;         // משתנה לבדיקת מצב תצוגה
    private FirebaseAuth mAuth;         // אובייקט לאימות משתמשים מול Firebase

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /**
         * פעולת onCreate: אתחול הדף, הגדרת עיצוב וטעינת נתונים קיימים.
         */
        checkAndApplyDarkMode(); // בדיקת מצב כהה לפני הצגת ה-Layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();               // אתחול רכיבי ה-UI ומאזיני לחיצה
        setupTopBar();             // הגדרת סרגל הכלים העליון
        setupBottomNavigation();   // הגדרת תפריט הניווט התחתון
        applyCustomColorMode();    // התאמת צבעים אישית
        loadChatHistory();         // טעינת הודעות קודמות מהזיכרון המקומי
    }

    /**
     * פעולה: initViews
     * תפקיד: קישור משתני הג'אווה לרכיבי ה-XML והגדרת הלוגיקה של כפתורי שליחה ועזרה.
     */
    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        chatContainer = findViewById(R.id.chatContainer);
        etMessage = findViewById(R.id.etMessage);
        scrollView = findViewById(R.id.scrollViewChat);

        // כפתור שליחת הודעה
        ImageButton btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    addMessageToChat("אתה: " + message, true); // הצגת ההודעה בצ'אט
                    saveMessageToPrefs("אתה: " + message, true); // שמירה בזיכרון
                    etMessage.setText(""); // ניקוי שדה ההזנה
                    sendMessageToGemini(message); // שליחת השאלה למודל ה-AI
                }
            });
        }

        // כפתור לניקוי היסטוריית הצ'אט
        TextView tvClearChat = findViewById(R.id.tvClearChat);
        if (tvClearChat != null) tvClearChat.setOnClickListener(v -> clearChat());

        // כפתור עזרה והסבר על היועץ האסטרטגי
        ImageButton btnHelp = findViewById(R.id.btnHelpInfoChat);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> showHelpDialog());
        }
    }

    /**
     * פעולה: setupTopBar
     * תפקיד: הגדרת כפתור חזור ותפריט אפשרויות עליון (Popup Menu).
     */
    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_profile) {
                        startActivity(new Intent(this, ProfileActivity.class));
                    } else if (id == R.id.menu_about) {
                        showAboutDialog();
                    } else if (id == R.id.menu_contact) {
                        showContactDialog();
                    } else if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                    } else if (id == R.id.menu_logout) {
                        showLogoutDialog();
                    }
                    return true;
                });
                popup.show();
            });
        }
    }

    /**
     * פעולה: showAboutDialog
     * תפקיד: הצגת תיבת מידע על האפליקציה והגרסה שלה.
     */
    private void showAboutDialog() {
        String aboutMessage = "InvestCalc הוא הכלי שלך לניהול ותכנון פיננסי חכם.\n\n" +
                "האפליקציה פותחה כדי לתת לכם את היכולת לחשב ריבית דריבית ותחזיות בצורה מדויקת.\n\n" +
                "פותח ע\"י ראובן\n" +
                "גרסה: 1.0";
        new AlertDialog.Builder(this).setTitle("אודות InvestCalc").setMessage(aboutMessage).setPositiveButton("סגור", null).show();
    }


    /**
     * פעולה: showContactDialog
     * תפקיד: מאפשרת למשתמש לשלוח מייל לתמיכה הטכנית של האפליקציה.
     */
    private void showContactDialog() {
        new AlertDialog.Builder(this).setTitle("יצירת קשר").setMessage("צריכים עזרה?")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    try { startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:")); }
                    catch (Exception e) { Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show(); }
                }).setNegativeButton("סגור", null).show();
    }

    /**
     * פעולה: showHelpDialog
     * תפקיד: הסבר קצר למשתמש על מטרת דף הצ'אט וה-AI.
     */
    private void showHelpDialog() {
        new AlertDialog.Builder(this).setTitle("היועץ האסטרטגי")
                .setMessage("כאן תוכל לשאול שאלות על השקעות וחישובים פיננסיים.")
                .setPositiveButton("הבנתי", null).show();
    }

    /**
     * פעולה: setupBottomNavigation
     * תפקיד: ניהול המעברים בין חלקי האפליקציה דרך סרגל הניווט התחתון.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_ai_chat); // סימון הצ'אט כדף פעיל

            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_tips) {
                    startActivity(new Intent(this, TipsActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                } else if (id == R.id.nav_ai_chat) {
                    return true;
                }
                return false;
            });
        }
    }

    /**
     * פעולה: sendMessageToGemini
     * תפקיד: התקשרות אסינכרונית מול ה-API של Gemini. שליחת הטקסט וקבלת תשובה.
     */
    private void sendMessageToGemini(String userPrompt) {
        // אתחול מודל ה-Gemini באמצעות מפתח ה-API מה-BuildConfig
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder().addText(userPrompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        // הוספת Callback לטיפול בתשובה שתחזור מהשרת
        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String resultText = result.getText();
                        runOnUiThread(() -> {
                            addMessageToChat("AI: " + resultText, false); // הצגת תשובת ה-AI
                            saveMessageToPrefs("AI: " + resultText, false); // שמירה בזיכרון
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        runOnUiThread(() -> addMessageToChat("AI שגיאה: " + t.getMessage(), false));
                    }
                },
                ContextCompat.getMainExecutor(this)
        );
    }

    /**
     * פעולה: addMessageToChat
     * תפקיד: יצירת בועת טקסט (TextView) באופן דינמי והוספתה למסך הצ'אט.
     */
    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(35, 25, 35, 25);
        textView.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(20, 15, 20, 15);

        if (isUser) { // עיצוב הודעת משתמש (ימין)
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            textView.setTextColor(Color.BLACK);
            params.gravity = android.view.Gravity.END;
        } else { // עיצוב הודעת AI (שמאל)
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_dark_frame);
            textView.setTextColor(Color.WHITE);
            params.gravity = android.view.Gravity.START;
        }

        textView.setLayoutParams(params);
        chatContainer.addView(textView);
        // גלילה אוטומטית לסוף הצ'אט
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    /**
     * פעולה: saveMessageToPrefs
     * תפקיד: שמירת הודעות הצ'אט במבנה JSON בתוך ה-SharedPreferences (זיכרון מקומי).
     */
    private void saveMessageToPrefs(String text, boolean isUser) {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        String currentHistory = prefs.getString("chat_history", "[]");
        try {
            JSONArray array = new JSONArray(currentHistory);
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("isUser", isUser);
            array.put(obj);
            prefs.edit().putString("chat_history", array.toString()).apply();
        } catch (JSONException e) { e.printStackTrace(); }
    }

    /**
     * פעולה: loadChatHistory
     * תפקיד: טעינת ההודעות השמורות בעת פתיחת המסך מחדש.
     */
    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        String currentHistory = prefs.getString("chat_history", "[]");
        try {
            JSONArray array = new JSONArray(currentHistory);
            chatContainer.removeAllViews();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                addMessageToChat(obj.getString("text"), obj.getBoolean("isUser"));
            }
        } catch (JSONException e) { e.printStackTrace(); }
    }

    /**
     * פעולה: clearChat
     * תפקיד: מחיקה מוחלטת של היסטוריית השיחות מהזיכרון ומהמסך.
     */
    private void clearChat() {
        getSharedPreferences("ChatPrefs", MODE_PRIVATE).edit().remove("chat_history").apply();
        chatContainer.removeAllViews();
    }

    /**
     * פעולה: toggleDarkMode
     * תפקיד: שינוי מצב כהה/בהיר ושמירת הבחירה בהגדרות האפליקציה.
     */
    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate(); // טעינה מחדש של הדף להחלת העיצוב
    }

    /**
     * פעולה: checkAndApplyDarkMode
     * תפקיד: בדיקה בעת טעינה האם להחיל את המצב הכהה.
     */
    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /**
     * פעולה: applyCustomColorMode
     * תפקיד: התאמת צבע הרקע של ה-Layout הראשי לפי מצב התצוגה.
     */
    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        if (mainLayout != null) mainLayout.setBackgroundColor(isDarkMode ? Color.BLACK : Color.parseColor("#F5F7FA"));
    }

    /**
     * פעולה: showLogoutDialog
     * תפקיד: הצגת דיאלוג אישור לפני ניתוק המשתמש מהחשבון.
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("לצאת מהחשבון?").setPositiveButton("כן", (d, w) -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }).setNegativeButton("ביטול", null).show();
    }
}