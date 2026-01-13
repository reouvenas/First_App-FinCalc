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

public class GraphActivity extends AppCompatActivity {

    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        lineChart = findViewById(R.id.lineChart);
        ImageButton btnBackGraph = findViewById(R.id.btnBackGraph);

        if (btnBackGraph != null) {
            btnBackGraph.setOnClickListener(v -> finish());
        }

        // קבלת הנתונים מה-Intent
        double initial = getIntent().getDoubleExtra("initial", 0);
        double monthly = getIntent().getDoubleExtra("monthly", 0);
        double rate = getIntent().getDoubleExtra("rate", 0);
        int years = getIntent().getIntExtra("years", 0);
        int months = getIntent().getIntExtra("months", 0);
        double fees = getIntent().getDoubleExtra("fees", 0);

        int totalMonths = (years * 12) + months;

        // כאן היה חסר לך הסוגר שמסיים את ה-onCreate
        if (totalMonths > 0) {
            generateChartData(initial, monthly, rate, totalMonths, fees);
            setupChart();
        }
    } // זה הסוגר שהיה חסר לסגור את ה-onCreate!

    private void generateChartData(double principal, double monthlyDeposit, double annualRate, int totalMonths, double annualFees) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        double currentTotal = principal;
        double netMonthlyRate = ((annualRate - annualFees) / 100) / 12;

        // נקודת התחלה
        entries.add(new Entry(0, (float) principal));
        labels.add("התחלה");

        for (int m = 1; m <= totalMonths; m++) {
            if (netMonthlyRate != 0) {
                currentTotal = currentTotal * (1 + netMonthlyRate) + monthlyDeposit;
            } else {
                currentTotal += monthlyDeposit;
            }

            // הוספת נקודה לגרף כל 12 חודשים או בסוף
            if (m % 12 == 0 || m == totalMonths) {
                float yearPoint = (float) m / 12;
                entries.add(new Entry(yearPoint, (float) currentTotal));
                labels.add("שנה " + (m / 12));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "צמיחת השקעה (₪)");
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setColor(Color.parseColor("#1A237E"));
        dataSet.setCircleColor(Color.parseColor("#1A237E"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.BLUE);
        dataSet.setFillAlpha(50);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelRotationAngle(45);

        Description description = new Description();
        description.setText("");
        lineChart.setDescription(description);

        lineChart.invalidate();
    }

    private void setupChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.animateX(1500);
        lineChart.setExtraOffsets(10, 10, 10, 10);
    }
}