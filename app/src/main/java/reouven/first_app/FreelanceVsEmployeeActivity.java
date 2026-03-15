package reouven.first_app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FreelanceVsEmployeeActivity extends AppCompatActivity {

    private EditText etEmployeeSalary;
    private TextView tvBreakEvenResult, tvDetails;
    private Button btnCalculate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_freelance_vs_employee);

        etEmployeeSalary = findViewById(R.id.etEmployeeSalary);
        tvBreakEvenResult = findViewById(R.id.tvBreakEvenResult);
        tvDetails = findViewById(R.id.tvDetails);
        btnCalculate = findViewById(R.id.btnCalculate);

        btnCalculate.setOnClickListener(v -> calculateBreakEven());
    }

    private void calculateBreakEven() {
        String input = etEmployeeSalary.getText().toString();
        if (input.isEmpty()) {
            Toast.makeText(this, "נא להכניס שכר ברוטו", Toast.LENGTH_SHORT).show();
            return;
        }

        double salary = Double.parseDouble(input);
        double overheadFactor = 1.35;
        double breakEvenTotal = salary * overheadFactor;
        double hourlyBreakEven = breakEvenTotal / 182;

        tvBreakEvenResult.setText(String.format("כעצמאי, עליך להכניס:\n%.0f ₪ בחודש", breakEvenTotal));

        String details = String.format(
                "• שכר לשעה מינימלי: %.2f ₪\n" +
                        "• הפרשות פנסיוניות (12.5%%): %.0f ₪\n" +
                        "• שווי ימי חופש ומחלה: %.0f ₪",
                hourlyBreakEven, salary * 0.125, salary * 0.10);

        tvDetails.setText(details);
    }
}