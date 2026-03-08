package reouven.first_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    private LineChart lineChart;
    private View mainLayout;
    private double initial, monthly, rate, fees;
    private int years, extraMonths;
    private boolean isDarkMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        mainLayout = findViewById(R.id.main_layout);
        lineChart = findViewById(R.id.lineChart);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            initial = extras.getDouble("initial", 0);
            monthly = extras.getDouble("monthly", 0);
            rate = extras.getDouble("rate", 0);
            fees = extras.getDouble("fees", 0);
            years = extras.getInt("years", 0);
            extraMonths = extras.getInt("months", 0);
        }

        setupTopBar();
        setupBottomNavigation();
        applyCustomColorMode();
        setupGraph();
    }

    private void setupTopBar() {
        findViewById(R.id.btnBackHeader).setOnClickListener(v -> onBackPressed());
        findViewById(R.id.btnMenuHeader).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                } else if (id == R.id.menu_logout) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, LoginActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
            bottomNav.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                Intent intent = null;
                if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
                else if (id == R.id.nav_ai_chat) intent = new Intent(this, ChatActivity.class);
                else if (id == R.id.nav_tips) intent = new Intent(this, TipsActivity.class);
                if (intent != null) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                return true;
            });
        }
    }

    private void setupGraph() {
        List<Entry> entries = new ArrayList<>();
        int totalMonths = (years * 12) + extraMonths;
        double monthlyRate = ((rate - fees) / 100) / 12;
        double currentBalance = initial;
        entries.add(new Entry(0, (float) currentBalance));
        for (int i = 1; i <= totalMonths; i++) {
            currentBalance = currentBalance * (1 + monthlyRate) + monthly;
            if (i % 12 == 0 || i == totalMonths) entries.add(new Entry(i / 12f, (float) currentBalance));
        }
        LineDataSet dataSet = new LineDataSet(entries, "צמיחה");
        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
    }

    private void applyCustomColorMode() {
        isDarkMode = getSharedPreferences("AppConfig", MODE_PRIVATE).getBoolean("dark_mode", false);
        if (isDarkMode) mainLayout.setBackgroundColor(Color.BLACK);
    }
}