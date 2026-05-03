package reouven.first_app;

/**
 * מחלקה: User
 * תפקיד: מחלקת מודל (POJO - Plain Old Java Object) המייצגת משתמש במערכת.
 * שימוש: משמשת להעברת נתונים בין האפליקציה לבין Firebase Realtime Database.
 * הערה חשובה: שמות המשתנים כאן חייבים להתאים בדיוק למפתחות (Keys) שנשמרים ב-Database.
 */
public class User {
    private String name;
    private String email;
    private String phone;

    /**
     * בנאי ריק (Default Constructor)
     * חובה עבור Firebase: המערכת משתמשת בו כדי ליצור אובייקט חדש לפני שהיא מאכלסת אותו בנתונים.
     */
    public User() {}

    /**
     * בנאי מלא
     * משמש ליצירת אובייקט משתמש חדש בזמן ההרשמה או העדכון.
     * @param name שם המשתמש
     * @param email אימייל המשתמש
     * @param phone מספר טלפון (כולל קידומת)
     */
    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    // --- Getters & Setters ---
    // אלו הפעולות שמאפשרות ל-Firebase ולשאר חלקי האפליקציה לקרוא ולכתוב את הנתונים

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}