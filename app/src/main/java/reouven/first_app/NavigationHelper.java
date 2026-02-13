package reouven.first_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class NavigationHelper {

    // --- דיאלוג אודות משודרג ---
    public static void showAboutDialog(Context context) {
        StringBuilder aboutMessage = new StringBuilder();
        aboutMessage.append("InvestCalc\n");
        aboutMessage.append("אפליקציה לניהול וחישוב השקעות חכם.\n\n");
        aboutMessage.append("מה חדש בגרסה 1.0:\n");
        aboutMessage.append("• חישוב ריבית דריבית כולל דמי ניהול.\n");
        aboutMessage.append("• גרף צמיחה אינטראקטיבי.\n");
        aboutMessage.append("• תמיכה במצב כהה/בהיר.\n\n");
        aboutMessage.append("פותח על ידי: ראובן\n");
        aboutMessage.append("כל הזכויות שמורות © 2024");

        new AlertDialog.Builder(context)
                .setTitle("אודות האפליקציה")
                .setMessage(aboutMessage.toString())
                .setIcon(R.drawable.ic_growth_graph) // שימוש בלוגו שלך
                .setPositiveButton("סגור", null)
                .setNeutralButton("דרג אותנו", (dialog, which) -> {
                    Toast.makeText(context, "תודה! קישור לחנות יתווסף בקרוב", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    // --- דיאלוג יצירת קשר ---
    public static void showContactDialog(Context context) {
        String myEmail = "support@investcalc.com";
        new AlertDialog.Builder(context)
                .setTitle("יצירת קשר")
                .setMessage("נשמח לשמוע ממך!\nהמייל שלנו: " + myEmail)
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{myEmail});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "פנייה מאפליקציית InvestCalc");
                    try {
                        context.startActivity(Intent.createChooser(intent, "בחר אפליקציית מייל:"));
                    } catch (Exception e) {
                        Toast.makeText(context, "לא נמצאה אפליקציית מייל מותקנת", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("סגור", null)
                .show();
    }
}