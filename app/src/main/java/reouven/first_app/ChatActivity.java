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
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer, inputArea;
    private EditText etMessage;
    private ScrollView scrollView;
    private View mainLayout;
    private GenerativeModelFutures model;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // הגדרת Gemini
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", BuildConfig.GEMINI_API_KEY);
        model = GenerativeModelFutures.from(gm);

        initViews();
        setupTopBar();
        setupBottomNavigation();

        // החלת צבעים ידנית
        applyCustomColorMode();

        addMessageToChat("שלום! אני Gemini, עוזר ה-AI הפיננסי שלך. איך אוכל לעזור?", false);
    }

    private void initViews() {
        mainLayout = findViewById(R.id.main_layout);
        chatContainer = findViewById(R.id.chatContainer);
        inputArea = findViewById(R.id.inputArea);
        etMessage = findViewById(R.id.etMessage);
        scrollView = findViewById(R.id.scrollViewChat);
        ImageButton btnSend = findViewById(R.id.btnSend);

        if (btnSend != null) {
            btnSend.setOnClickListener(v -> {
                String message = etMessage.getText().toString().trim();
                if (!message.isEmpty()) {
                    addMessageToChat("אתה: " + message, true);
                    etMessage.setText("");
                    sendMessageToGemini(message);
                }
            });
        }
    }

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            inputArea.setBackgroundColor(Color.parseColor("#121212"));
            etMessage.setTextColor(Color.WHITE);
            etMessage.setHintTextColor(Color.GRAY);
        } else {
            mainLayout.setBackgroundColor(Color.parseColor("#F5F7FA"));
            inputArea.setBackgroundColor(Color.WHITE);
            etMessage.setTextColor(Color.BLACK);
        }
    }

    private void toggleDarkMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        boolean current = prefs.getBoolean("dark_mode", false);
        prefs.edit().putBoolean("dark_mode", !current).apply();
        recreate();
    }

    private void setupTopBar() {
        View btnBackHeader = findViewById(R.id.btnBackHeader);
        if (btnBackHeader != null) {
            btnBackHeader.setOnClickListener(v -> onBackPressed());
        }

        View btnMenuHeader = findViewById(R.id.btnMenuHeader);
        if (btnMenuHeader != null) {
            btnMenuHeader.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_dark_mode) {
                        toggleDarkMode();
                        return true;
                    } else if (id == R.id.menu_contact) {
                        NavigationHelper.showContactDialog(this);
                        return true;
                    } else if (id == R.id.menu_logout) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(this, LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        }
    }

    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(35, 25, 35, 25);
        textView.setTextSize(16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 15, 0, 15);

        if (isUser) {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            textView.setTextColor(Color.BLACK);
            params.gravity = android.view.Gravity.START;
        } else {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_dark_frame);
            textView.setTextColor(Color.WHITE);
            params.gravity = android.view.Gravity.END;
        }

        textView.setLayoutParams(params);
        chatContainer.addView(textView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessageToGemini(String userPrompt) {
        Content content = new Content.Builder().addText(userPrompt).build();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);
        Executor executor = Executors.newSingleThreadExecutor();

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String aiResponse = result.getText();
                runOnUiThread(() -> addMessageToChat("AI: " + aiResponse, false));
            }
            @Override
            public void onFailure(Throwable t) {
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "שגיאה בחיבור", Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_ai_chat);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;

                if (id == R.id.nav_home) {
                    intent = new Intent(this, HomeActivity.class);
                } else if (id == R.id.nav_history) {
                    intent = new Intent(this, HistoryActivity.class);
                } else if (id == R.id.nav_tips) {
                    intent = new Intent(this, TipsActivity.class);
                }

                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                return true;
            });
        }
    }
}