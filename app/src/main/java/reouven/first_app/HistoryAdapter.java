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

/**
 * מחלקה: HistoryAdapter
 * תפקיד: ניהול והצגת רשימת התוכניות שנשמרו ב-RecyclerView.
 * האדפטר אחראי ליצור את התצוגה לכל שורה (ViewHolder) ולחבר את הנתונים מהענן לרכיבי הטקסט.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<Map<String, Object>> planList; // רשימת המפות המכילה את נתוני התוכניות
    private OnPlanClickListener listener;       // ממשק (Interface) לטיפול בלחיצות

    /**
     * ממשק פנימי לניהול אירועים: לחיצה על פריט או לחיצה על כפתור המחיקה.
     */
    public interface OnPlanClickListener {
        void onPlanClick(Map<String, Object> plan);
        void onDeleteClick(Map<String, Object> plan, int position);
    }

    // בנאי (Constructor) שמקבל את רשימת הנתונים והמאזין מה-Activity
    public HistoryAdapter(List<Map<String, Object>> planList, OnPlanClickListener listener) {
        this.planList = planList;
        this.listener = listener;
    }

    /**
     * פעולה: onCreateViewHolder
     * תפקיד: ניפוח (Inflate) של קובץ ה-XML של שורה בודדת ויצירת ה-ViewHolder.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_saved_plan, parent, false);
        return new ViewHolder(view);
    }

    /**
     * פעולה: onBindViewHolder
     * תפקיד: חיבור הנתונים מהרשימה לתוך רכיבי ה-UI של שורה ספציפית לפי המיקום שלה (position).
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> plan = planList.get(position);

        // זיהוי סוג המחשבון (investment או mortgage) כדי להתאים את התצוגה
        String type = (String) plan.getOrDefault("type", "investment");
        boolean isMortgage = "mortgage".equals(type);

        // שליפת שם התוכנית והסכום (ראשוני להשקעה או סכום הלוואה למשכנתא)
        String title = String.valueOf(plan.getOrDefault("planName", isMortgage ? "חישוב משכנתא" : "תוכנית השקעה"));
        String amount = isMortgage ?
                String.valueOf(plan.getOrDefault("loanAmount", "0")) :
                String.valueOf(plan.getOrDefault("initial", "0"));
        String symbol = String.valueOf(plan.getOrDefault("currency", "₪"));

        // הצגת הטקסטים בתוך רכיבי ה-ViewHolder
        holder.tvCategory.setText(isMortgage ? "מחשבון משכנתא" : "מחשבון השקעות");
        holder.tvTitle.setText(title);
        holder.tvDetails.setText("סכום: " + symbol + amount);

        // עיבוד התאריך: המרה מפורמט Long (מילישניות) לתצוגת תאריך ושעה קריאה
        if (plan.get("timestamp") != null) {
            try {
                long ts = Long.parseLong(String.valueOf(plan.get("timestamp")));
                // פורמט: יום/חודש/שנה שעה:דקות
                holder.tvDate.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(ts)));
            } catch (Exception e) {
                holder.tvDate.setText("");
            }
        }

        // הגדרת מאזיני לחיצה:
        // לחיצה על כל הכרטיס - פתיחת פירוט התוכנית
        holder.itemView.setOnClickListener(v -> listener.onPlanClick(plan));

        // לחיצה על אייקון הפח - הפעלת מנגנון המחיקה
        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(plan, position));
    }

    // החזרת מספר הפריטים הכולל ברשימה
    @Override
    public int getItemCount() { return planList.size(); }

    /**
     * מחלקה פנימית: ViewHolder
     * תפקיד: "מחזיק" את ההפניות לרכיבי ה-View של שורה בודדת בזיכרון, לשיפור ביצועים.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDetails, tvDate, tvCategory;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // מציאת הרכיבים בתוך ה-View של השורה (item_saved_plan.xml)
            tvTitle = itemView.findViewById(R.id.tvPlanTitle);
            tvDetails = itemView.findViewById(R.id.tvPlanDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}