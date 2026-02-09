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

    private void applyCustomColorMode() {
        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
        isDarkMode = prefs.getBoolean("dark_mode", false);
        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        View toolbar = findViewById(R.id.toolbar_graph);

        if (isDarkMode) {
            mainLayout.setBackgroundColor(Color.BLACK);
            nav.setBackgroundColor(Color.BLACK);
            toolbar.setBackgroundColor(Color.parseColor("#121212"));
        } else {
            mainLayout.setBackgroundColor(Color.WHITE);
            nav.setBackgroundColor(Color.WHITE);
            toolbar.setBackgroundColor(Color.parseColor("#1A237E"));
        }
    }

    private void setupTopBar() {
        // שינוי ל-ID הנכון לפי ה-XML החדש
        ImageButton btnBack = findViewById(R.id.btnBackHeader);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        ImageButton btnMenu = findViewById(R.id.btnMenuHeader);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, v);
                popup.getMenuInflater().inflate(R.menu.home_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_dark_mode) {
                        SharedPreferences prefs = getSharedPreferences("AppConfig", MODE_PRIVATE);
                        boolean current = prefs.getBoolean("dark_mode", false);
                        prefs.edit().putBoolean("dark_mode", !current).apply();
                        recreate();
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

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_history);
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
                return true;
            });
        }
    }

    private void setupGraph() {
        List<Entry> entries = new ArrayList<>();
        int totalMonths = (years * 12) + extraMonths;
        double netAnnualRate = (rate - fees) / 100;
        double monthlyRate = netAnnualRate / 12;
        double currentBalance = initial;
        entries.add(new Entry(0, (float) currentBalance));

        for (int i = 1; i <= totalMonths; i++) {
            if (monthlyRate != 0) currentBalance = currentBalance * (1 + monthlyRate) + monthly;
            else currentBalance += monthly;
            if (i % 12 == 0 || i == totalMonths) entries.add(new Entry(i / 12f, (float) currentBalance));
        }

        LineDataSet dataSet = new LineDataSet(entries, "צמיחת הון (בשנים)");
        dataSet.setColor(Color.parseColor("#4CAF50"));
        dataSet.setCircleColor(Color.parseColor("#1A237E"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawValues(false);

        int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        lineChart.getAxisLeft().setTextColor(textColor);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setTextColor(textColor);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getLegend().setTextColor(textColor);
        lineChart.getDescription().setEnabled(false);

        int gridColor = isDarkMode ? Color.parseColor("#333333") : Color.parseColor("#DDDDDD");
        lineChart.getXAxis().setGridColor(gridColor);
        lineChart.getAxisLeft().setGridColor(gridColor);

        lineChart.setData(new LineData(dataSet));
        lineChart.invalidate();
    }
}