package reouven.first_app;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;

public class NavigationHelper {

    public static void showContactDialog(Context context) {
        String myEmail = "support@investcalc.com";

        new AlertDialog.Builder(context)
                .setTitle("יצירת קשר")
                .setMessage("נשמח לשמוע ממך!\nהמייל שלנו: " + myEmail)
                .setPositiveButton("שלח מייל", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // מבטיח פתיחת אפליקציות מייל בלבד
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{myEmail}); // הכתובת
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