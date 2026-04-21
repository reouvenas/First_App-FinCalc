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

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText etMessage;
    private ScrollView scrollView;
    private View mainLayout;
    private boolean isDarkMode;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        checkAndApplyDarkMode();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupTopBar();
        setupBottomNavigation();
        applyCustomColorMode();
        loadChatHistory();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        chatContainer = findViewById(R.id.chatContainer);
        etMessage = findViewById(R.id.etMessage);
        scrollView = findViewById(R.id.scrollViewChat);

        ImageButton btnSend = findViewById(R.id.btnSend);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    addMessageToChat("אתה: " + message, true);
                    saveMessageToPrefs("אתה: " + message, true);
                    etMessage.setText("");
                    sendMessageToGemini(message);
                }
            });
        }

        TextView tvClearChat = findViewById(R.id.tvClearChat);
        if (tvClearChat != null) tvClearChat.setOnClickListener(v -> clearChat());

        ImageButton btnHelp = findViewById(R.id.btnHelpInfoChat);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> showHelpDialog());
        }
    }

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

    private void showContactDialog() {
        new AlertDialog.Builder(this)
                .setTitle("יצירת קשר")
                .setMessage("צריכים עזרה או יש לכם הצעה לשיפור? אנחנו כאן בשבילכם.")
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"supportInvestcalc@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc");
                    try {
                        startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(this, "לא נמצאה אפליקציית מייל", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle("היועץ האסטרטגי")
                .setMessage("כאן תוכל לשאול שאלות על השקעות, ריביות ותכנון פיננסי. ה-AI ינתח ויעזור לך לקבל החלטות.")
                .setPositiveButton("הבנתי", null).show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_ai_chat);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    startActivity(new Intent(this, HomeActivity.class));
                    finish();
                    return true;
                } else if (id == R.id.nav_history) {
                    startActivity(new Intent(this, HistoryActivity.class));
                    return true;
                }
                return id == R.id.nav_ai_chat;
            });
        }
    }

    private void sendMessageToGemini(String userPrompt) {
        // עדכון למודל Gemini 2.5 Flash לפי הרשימה שלך
        GenerativeModel gm = new GenerativeModel("gemini-2.5-flash", BuildConfig.GEMINI_API_KEY);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        Content content = new Content.Builder()
                .addText(userPrompt)
                .build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(
                response,
                new FutureCallback<GenerateContentResponse>() {
                    @Override
                    public void onSuccess(GenerateContentResponse result) {
                        String resultText = result.getText();
                        runOnUiThread(() -> {
                            addMessageToChat("AI: " + resultText, false);
                            saveMessageToPrefs("AI: " + resultText, false);
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

    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(35, 25, 35, 25);
        textView.setTextSize(16);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
        params.setMargins(20, 15, 20, 15);
        if (isUser) {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            textView.setTextColor(Color.BLACK);
            params.gravity = android.view.Gravity.END;
        } else {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_dark_frame);
            textView.setTextColor(Color.WHITE);
            params.gravity = android.view.Gravity.START;
        }
        textView.setLayoutParams(params);
        chatContainer.addView(textView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

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

    private void clearChat() {
        getSharedPreferences("ChatPrefs", MODE_PRIVATE).edit().remove("chat_history").apply();
        chatContainer.removeAllViews();
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode", !prefs.getBoolean("dark_mode", false)).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        AppCompatDelegate.setDefaultNightMode(prefs.getBoolean("dark_mode", false) ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        if (mainLayout != null) mainLayout.setBackgroundColor(isDarkMode ? Color.BLACK : Color.parseColor("#F5F7FA"));
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this).setTitle("התנתקות").setMessage("לצאת מהחשבון?").setPositiveButton("כן", (d, w) -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }).setNegativeButton("ביטול", null).show();
    }
}
