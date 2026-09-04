import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** java.sql y java.util.logging: fecha y hora, el registro de drivers, la herencia de niveles. */
public class SqlTest {

    public static int run() {
        int i = 0;

        // -- java.sql.Date / Time / Timestamp
        if (!Date.valueOf("2024-02-29").toString().equals("2024-02-29")) { return i; } i++;
        if (!Time.valueOf("07:05:09").toString().equals("07:05:09")) { return i; } i++;
        if (Date.valueOf("2024-02-29").getYear() != 124) { return i; } i++;
        boolean salto = false;
        try { Date.valueOf("2024-01-01").getHours(); } catch (IllegalArgumentException e) { salto = true; }
        if (!salto) { return i; } i++;
        salto = false;
        try { Time.valueOf("01:02:03").getDate(); } catch (IllegalArgumentException e) { salto = true; }
        if (!salto) { return i; } i++;

        Timestamp ts = Timestamp.valueOf("2024-01-02 03:04:05.5");
        if (ts.getNanos() != 500000000) { return i; } i++;
        if (!ts.toString().equals("2024-01-02 03:04:05.5")) { return i; } i++;
        Timestamp cero = Timestamp.valueOf("2024-01-02 03:04:05");
        if (!cero.toString().equals("2024-01-02 03:04:05.0")) { return i; } i++;
        if (cero.getNanos() != 0) { return i; } i++;
        if (ts.compareTo(cero) <= 0) { return i; } i++;
        if (!ts.equals(Timestamp.valueOf("2024-01-02 03:04:05.5"))) { return i; } i++;
        // La asimetria documentada: equals contra un java.util.Date da false.
        if (ts.equals(new java.util.Date(ts.getTime()))) { return i; } i++;
        if (Timestamp.valueOf("1970-01-01 00:00:00.000000001").getNanos() != 1) { return i; } i++;

        // -- DriverManager: sin drivers registrados, "no suitable driver"
        boolean fallo = false;
        try { DriverManager.getConnection("jdbc:noexiste:x"); } catch (SQLException e) { fallo = true; }
        if (!fallo) { return i; } i++;
        if (DriverManager.getDrivers().hasMoreElements()) { return i; } i++;
        DriverManager.setLoginTimeout(17);
        if (DriverManager.getLoginTimeout() != 17) { return i; } i++;

        // -- java.util.logging: niveles
        if (Level.parse("500") != Level.FINE) { return i; } i++;
        if (Level.parse("FINE").intValue() != 500) { return i; } i++;
        if (Level.SEVERE.intValue() <= Level.WARNING.intValue()) { return i; } i++;
        if (!Level.INFO.toString().equals("INFO")) { return i; } i++;

        // -- herencia por el arbol de nombres: el hijo llega **antes** que el intermedio
        Logger hijo = Logger.getLogger("kaji.zz.hijo");
        Logger medio = Logger.getLogger("kaji.zz");
        medio.setLevel(Level.FINEST);
        if (!hijo.isLoggable(Level.FINEST)) { return i; } i++;
        medio.setLevel(Level.SEVERE);
        if (hijo.isLoggable(Level.FINE)) { return i; } i++;
        if (!hijo.isLoggable(Level.SEVERE)) { return i; } i++;
        // Identidad por nombre.
        if (Logger.getLogger("kaji.zz") != medio) { return i; } i++;
        medio.setLevel(Level.OFF);
        if (hijo.isLoggable(Level.SEVERE)) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
