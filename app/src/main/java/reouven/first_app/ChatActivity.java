package reouven.first_app;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChatActivity extends AppCompatActivity {

    private LinearLayout chatContainer;
    private EditText etMessage;
    private ScrollView scrollView;
    private GenerativeModelFutures model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // הגדרת המודל של Gemini עם המפתח שלך
        GenerativeModel gm = new GenerativeModel("gemini-1.5-flash", "AIzaSyA8XCmICJT0wJ_8BliRgwbcw1_6dSzjtqM");
        model = GenerativeModelFutures.from(gm);

        chatContainer = findViewById(R.id.chatContainer);
        etMessage = findViewById(R.id.etMessage);
        scrollView = findViewById(R.id.scrollViewChat);

        findViewById(R.id.btnBackHeader).setOnClickListener(v -> finish());

        ImageButton btnSend = findViewById(R.id.btnSend);
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                addMessageToChat("אתה: " + message, true);
                etMessage.setText("");
                sendMessageToGemini(message); // שליחה ל-AI האמיתי
            }
        });

        addMessageToChat("שלום! אני Gemini, עוזר ה-AI שלך. במה אוכל לעזור היום?", false);
    }

    private void addMessageToChat(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setPadding(30, 20, 30, 20);
        textView.setTextSize(16);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(10, 15, 10, 15);

        if (isUser) {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_light_frame);
            params.gravity = android.view.Gravity.START;
        } else {
            textView.setBackgroundResource(android.R.drawable.editbox_dropdown_dark_frame);
            textView.setTextColor(android.graphics.Color.WHITE);
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
                runOnUiThread(() -> Toast.makeText(ChatActivity.this, "שגיאה בחיבור ל-AI", Toast.LENGTH_SHORT).show());
            }
        }, executor);
    }
}
