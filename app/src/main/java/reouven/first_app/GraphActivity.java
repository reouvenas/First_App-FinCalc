package reouven.first_app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GraphActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        lineChart = findViewById(R.id.lineChart);
        ImageButton btnBackGraph = findViewById(R.id.btnBackGraph);
        btnBackGraph.setOnClickListener(v -> finish());

        // קבלת נתוני ההשקעה מה-Intent
        double initial = getIntent().getDoubleExtra("INITIAL_SUM", 0);
        double monthly = getIntent().getDoubleExtra("MONTHLY_SUM", 0);
        double annualRate = getIntent().getDoubleExtra("RATE", 0) / 100;
        int years = getIntent().getIntExtra("YEARS", 0);
        double annualFees = getIntent().getDoubleExtra("FEES", 0) / 100;

        generateChartData(initial, monthly, annualRate, years, annualFees);
        setupChart();
    }

    private void generateChartData(double principal, double monthlyDeposit, double annualRate, int totalYears, double annualFees) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        double currentTotal = principal;
        double netMonthlyRate = (annualRate - annualFees) / 12;

        entries.add(new Entry(0, (float) principal)); // נקודת התחלה (שנה 0)
        labels.add("שנה 0");

        for (int year = 1; year <= totalYears; year++) {
            for (int month = 1; month <= 12; month++) {
                currentTotal *= (1 + netMonthlyRate); // צמיחה על הקרן
                currentTotal += monthlyDeposit; // הוספת הפקדה חודשית
            }
            entries.add(new Entry(year, (float) currentTotal));
            labels.add("שנה " + year);
        }

        LineDataSet dataSet = new LineDataSet(entries, "ערך השקעה מצטבר");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.GRAY);
        dataSet.setCircleColor(Color.BLUE);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false); // לא להציג את הערכים על הגרף עצמו

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // הגדרת תוויות ציר X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f); // מציג כל שנה
        xAxis.setLabelRotationAngle(45); // סיבוב התוויות למניעת התנגשות
        xAxis.setDrawGridLines(false);

        // תיאור הגרף
        Description description = new Description();
        description.setText("צמיחת הון לאורך שנים");
        description.setTextSize(12f);
        lineChart.setDescription(description);

        lineChart.invalidate(); // רענן את הגרף
    }

    private void setupChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setExtraOffsets(5, 10, 5, 5); // מרווחים מסביב
        lineChart.getAxisRight().setEnabled(false); // אל תציג ציר Y ימני
        lineChart.animateX(1500); // אנימציה בפתיחה
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setGridBackgroundColor(Color.LTGRAY);
        lineChart.setBorderColor(Color.DKGRAY);
        lineChart.setBorderWidth(1f);
    }
}