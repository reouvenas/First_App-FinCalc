package reouven.first_app;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import java.util.ArrayList;
import java.util.List;

public class GraphActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        // הסתרת ה-ActionBar אם קיים
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        lineChart = findViewById(R.id.lineChart);
        ImageButton btnBackGraph = findViewById(R.id.btnBackGraph);
        if (btnBackGraph != null) btnBackGraph.setOnClickListener(v -> finish());

        double initial = getIntent().getDoubleExtra("initial", 0);
        double monthly = getIntent().getDoubleExtra("monthly", 0);
        double rate = getIntent().getDoubleExtra("rate", 0);
        int years = getIntent().getIntExtra("years", 0);
        int months = getIntent().getIntExtra("months", 0);
        double fees = getIntent().getDoubleExtra("fees", 0);

        int totalMonths = (years * 12) + months;

        if (totalMonths > 0) {
            generateChartData(initial, monthly, rate, totalMonths, fees);
            setupChart();
        }
    }

    private void generateChartData(double principal, double monthlyDeposit, double annualRate, int totalMonths, double annualFees) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        double currentTotal = principal;
        double netMonthlyRate = ((annualRate - annualFees) / 100) / 12;

        // נקודת התחלה (חודש 0)
        entries.add(new Entry(0, (float) principal));
        labels.add("התחלה");

        int labelCounter = 1; // מונה עבור מיקום הלייבלים

        for (int m = 1; m <= totalMonths; m++) {
            if (netMonthlyRate != 0) {
                currentTotal = currentTotal * (1 + netMonthlyRate) + monthlyDeposit;
            } else {
                currentTotal += monthlyDeposit;
            }

            // הוספת נקודה לגרף בכל סוף שנה או בחודש האחרון בהחלט
            if (m % 12 == 0 || m == totalMonths) {
                entries.add(new Entry(labelCounter, (float) currentTotal));

                if (m % 12 == 0) {
                    labels.add("שנה " + (m / 12));
                } else {
                    labels.add("סוף");
                }
                labelCounter++;
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "צמיחת השקעה (₪)");
        dataSet.setColor(Color.parseColor("#1A237E"));
        dataSet.setCircleColor(Color.parseColor("#1A237E"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false); // לא מציג מספרים מעל כל נקודה (נקי יותר)
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#1A237E"));
        dataSet.setFillAlpha(30);

        // הפיכת הקו למעוגל ונעים לעין
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        // הגדרות ציר X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(labels.size(), true); // מוודא שכל הלייבלים יוצגו
        xAxis.setDrawGridLines(false);

        lineChart.invalidate(); // רענון הגרף
    }

    private void setupChart() {
        lineChart.animateX(1000); // אנימציה מצד שמאל לימין
        lineChart.getAxisRight().setEnabled(false); // ביטול ציר ימין מיותר
        lineChart.getDescription().setEnabled(false); // ביטול טקסט תיאור בפינה
        lineChart.getLegend().setEnabled(true); // הצגת מקרא (Legend)
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true); // אפשרות להגדלה עם האצבעות
    }
}