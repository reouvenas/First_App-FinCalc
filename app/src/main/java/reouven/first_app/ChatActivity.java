package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer, inputArea;
    private EditText etMessage;
    private ScrollView scrollView;
    private View mainLayout;
    private TextView tvClearChat;
    private GenerativeModelFutures model;
    private boolean isDarkMode;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private static final String PREFS_NAME = "ChatPrefs";
    private static final String HISTORY_KEY = "chat_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // בדיקת מצב לילה לפני הכל
        checkAndApplyDarkMode();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // אתחול Gemini
        try {
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.7f;
            GenerationConfig config = configBuilder.build();

            GenerativeModel gm = new GenerativeModel(
                    "gemini-2.5-flash",
                    "AIzaSyBFdA5ZMzi4Vj4TvXXH0NnF8amEmOeIyFw",
                    config
            );

            model = GenerativeModelFutures.from(gm);
        } catch (Exception e) {
            android.util.Log.e("GEMINI_INIT_ERROR", "Failed to init model", e);
        }

        initViews();
        setupTopBar();
        setupBottomNavigation();
        applyCustomColorMode();
        loadChatHistory();
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        chatContainer = findViewById(R.id.chatContainer);
        inputArea = findViewById(R.id.inputArea);
        etMessage = findViewById(R.id.etMessage);
        scrollView = findViewById(R.id.scrollViewChat);
        tvClearChat = findViewById(R.id.tvClearChat);
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

        if (tvClearChat != null) {
            tvClearChat.setOnClickListener(v -> clearChat());
        }
    }

    private void sendMessageToGemini(String userPrompt) {
        if (model == null) {
            addMessageToChat("AI שגיאה: המודל לא הופעל כראוי", false);
            return;
        }

        String instructions = "אתה יועץ פיננסי ואסטרטגי בכיר. ענה בביטחון, בקצרה ולא להאריך בסתם הסברים, וענה לעניין. השאלה: ";
        Content content = new Content.Builder().addText(instructions + userPrompt).build();

        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                runOnUiThread(() -> {
                    if (result.getText() != null) {
                        String aiMsg = "AI: " + result.getText();
                        addMessageToChat(aiMsg, false);
                        saveMessageToPrefs(aiMsg, false);
                    } else {
                        addMessageToChat("AI: התשובה נחסמה מטעמי בטיחות.", false);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> {
                    addMessageToChat("AI שגיאה: " + t.getMessage(), false);
                });
            }
        }, executor);
    }

    private void clearChat() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().remove(HISTORY_KEY).apply();
        chatContainer.removeAllViews();
        addMessageToChat("הצ'אט נוקה. אני כאן לכל שאלה אסטרטגית חדשה.", false);
    }

    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(35, 25, 35, 25);
        textView.setTextSize(16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
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
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentHistory = prefs.getString(HISTORY_KEY, "[]");
        try {
            JSONArray array = new JSONArray(currentHistory);
            JSONObject obj = new JSONObject();
            obj.put("text", text);
            obj.put("isUser", isUser);
            array.put(obj);
            prefs.edit().putString(HISTORY_KEY, array.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentHistory = prefs.getString(HISTORY_KEY, "[]");
        try {
            JSONArray array = new JSONArray(currentHistory);
            chatContainer.removeAllViews();
            if (array.length() == 0) {
                addMessageToChat("שלום! אני היועץ האסטרטגי והפיננסי שלך. במה נתמקד היום?", false);
            } else {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    addMessageToChat(obj.getString("text"), obj.getBoolean("isUser"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setupTopBar() {
        View btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) btnBack.setOnClickListener(v -> onBackPressed());

        View btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                        return true;
                    } else if (id == R.id.menu_profile) {
                        startActivity(new Intent(this, ProfileActivity.class));
                        return true;
                    } else if (id == R.id.menu_contact) {
                        NavigationHelper.showContactDialog(this);
                        return true;
                    } else if (id == R.id.menu_about) {
                        showAboutDialog();
                        return true;
                    } else if (id == R.id.menu_logout) {
                        showLogoutDialog();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        if (isDarkMode) {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.BLACK);
            if (inputArea != null) inputArea.setBackgroundColor(Color.parseColor("#121212"));
            etMessage.setTextColor(Color.WHITE);
            etMessage.setHintTextColor(Color.GRAY);
            if(tvClearChat != null) tvClearChat.setTextColor(Color.WHITE);
        } else {
            if (mainLayout != null) mainLayout.setBackgroundColor(Color.parseColor("#F5F7FA"));
            if (inputArea != null) inputArea.setBackgroundColor(Color.WHITE);
            etMessage.setTextColor(Color.BLACK);
            etMessage.setHintTextColor(Color.parseColor("#9E9E9E"));
            if(tvClearChat != null) tvClearChat.setTextColor(Color.parseColor("#1A237E"));
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void checkAndApplyDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(isDark ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("אודות InvestCalc")
                .setMessage("InvestCalc v1.0\nיועץ מחשבון חכם בשילוב AI אסטרטגי ופיננסי.\nפותח על ידי: ראובן")
                .setPositiveButton("סגור", null).show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("התנתקות")
                .setMessage("האם ברצונך להתנתק מהחשבון?")
                .setPositiveButton("כן, צא", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("ביטול", null).show();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_ai_chat);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
                else if (id == R.id.nav_history) intent = new Intent(this, HistoryActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return id == R.id.nav_ai_chat;
            });
        }
    }
}