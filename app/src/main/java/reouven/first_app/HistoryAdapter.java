package reouven.first_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
    private OnPlanClickListener listener;

    public interface OnPlanClickListener {
        void onPlanClick(Map<String, Object> plan);
        void onDeleteClick(Map<String, Object> plan, int position);
    }

    public HistoryAdapter(List<Map<String, Object>> planList, OnPlanClickListener listener) {
        this.planList = planList;
        this.listener = listener;
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

        // זיהוי סוג המחשבון לטובת תצוגה דינמית
        String type = (String) plan.getOrDefault("type", "investment");
        boolean isMortgage = "mortgage".equals(type);

        // הגדרת תוכן הכרטיס
        String title = String.valueOf(plan.getOrDefault("planName", isMortgage ? "חישוב משכנתא" : "תוכנית השקעה"));
        String amount = isMortgage ?
                String.valueOf(plan.getOrDefault("loanAmount", "0")) :
                String.valueOf(plan.getOrDefault("initial", "0"));
        String symbol = String.valueOf(plan.getOrDefault("currency", "₪"));

        holder.tvCategory.setText(isMortgage ? "מחשבון משכנתא" : "מחשבון השקעות");
        holder.tvTitle.setText(title);
        holder.tvDetails.setText("סכום: " + symbol + amount);

        // הצגת תאריך ושעה של השמירה
        if (plan.get("timestamp") != null) {
            try {
                long ts = Long.parseLong(String.valueOf(plan.get("timestamp")));
                holder.tvDate.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(ts)));
            } catch (Exception e) { holder.tvDate.setText(""); }
        }

        holder.itemView.setOnClickListener(v -> listener.onPlanClick(plan));
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(plan, position));
    }

    @Override
    public int getItemCount() { return planList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvDate, tvCategory;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvDetails = itemView.findViewById(R.id.tvPlanDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}