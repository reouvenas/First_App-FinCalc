package reouven.first_app;

public class User {
    // השמות כאן חייבים להיות בדיוק כמו בקוד של ה-RegisterActivity
    public String name;
    public String email;
    public String phone;

    // חובה שיהיה בנאי ריק (ככה Firebase עובד)
    public User() {}

    public User(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }
}