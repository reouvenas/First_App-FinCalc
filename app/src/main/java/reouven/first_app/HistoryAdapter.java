package reouven.first_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> planList;

    public HistoryAdapter(List<Map<String, Object>> planList) {
        this.planList = planList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_plan, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> plan = planList.get(position);

        // 1. שליפת שם התוכנית
        String title = String.valueOf(plan.get("planName"));

        // 2. שליפת פרטים (סכום ותקופה)
        String initial = String.valueOf(plan.get("initial"));
        String years = String.valueOf(plan.get("years"));
        String symbol = String.valueOf(plan.get("currency"));
        if (symbol == null || symbol.equals("null")) symbol = "₪";

        // 3. טיפול בתאריך (הפיכת Timestamp לתאריך קריא)
        String dateString = "";
        if (plan.get("timestamp") != null) {
            long timestamp = (long) plan.get("timestamp");
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            dateString = sdf.format(date);
        }

        // 4. הצגת הנתונים בטקסטים שעיצבנו ב-XML
        holder.tvTitle.setText(title);
        holder.tvDetails.setText("סכום: " + symbol + initial + " | תקופה: " + years + " שנים");
        holder.tvDate.setText(dateString);
        holder.tvCategory.setText("מחשבון השקעות"); // כותרת קבועה כרגע
    }

    @Override
    public int getItemCount() {
        return planList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvDate, tvCategory;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvDetails = itemView.findViewById(R.id.tvPlanDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
        }
    }
}