import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

// Aritmetica del calendario gregoriano. Todo en GMT explicito: el default de `java` real es la
// zona del sistema operativo y el nuestro es GMT, asi que sin fijarla los dos lados no serian
// comparables.
public class CalTest {

    static GregorianCalendar gmt(int y, int m, int d) {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        c.set(Calendar.YEAR, y);
        c.set(Calendar.MONTH, m);
        c.set(Calendar.DAY_OF_MONTH, d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    public static int run() {
        int r = 0;

        // ---- la epoca: 1970-01-01 fue jueves ---------------------------------------------------
        GregorianCalendar epoca = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        epoca.setTimeInMillis(0L);
        r = r + epoca.get(Calendar.YEAR);                              // 1970
        r = r + epoca.get(Calendar.MONTH);                             // 0
        r = r + epoca.get(Calendar.DAY_OF_MONTH);                      // 1
        r = r + epoca.get(Calendar.DAY_OF_WEEK) * 10;                  // THURSDAY = 5 -> 50
        r = r + epoca.get(Calendar.DAY_OF_YEAR) * 100;                 // 100

        // ---- 29 de febrero de 2024 (bisiesto) --------------------------------------------------
        GregorianCalendar bis = gmt(2024, 1, 29);
        r = r + bis.get(Calendar.DAY_OF_MONTH) * 1000;                 // 29000
        r = r + bis.get(Calendar.DAY_OF_YEAR) * 10000;                 // 60 -> 600000
        r = r + bis.get(Calendar.DAY_OF_WEEK) * 100000;                // jueves = 5

        r = r + (bis.isLeapYear(2024) ? 1 : 0);
        r = r + (bis.isLeapYear(1900) ? 7777 : 0);                     // secular no bisiesto
        r = r + (bis.isLeapYear(2000) ? 2 : 0);                        // secular bisiesto

        // ---- getActualMaximum depende de la fecha ----------------------------------------------
        r = r + bis.getActualMaximum(Calendar.DAY_OF_MONTH) * 1000000; // febrero 2024 -> 29
        r = r + gmt(2023, 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH);  // febrero 2023 -> 28
        r = r + gmt(2024, 0, 1).getActualMaximum(Calendar.DAY_OF_MONTH);  // enero -> 31

        // ---- add propaga; roll no ---------------------------------------------------------------
        //
        // 31 de enero mas un mes es el 29 de febrero, no el 3 de marzo: el dia se recorta al
        // largo del mes destino.
        GregorianCalendar suma = gmt(2024, 0, 31);
        suma.add(Calendar.MONTH, 1);
        r = r + suma.get(Calendar.MONTH) * 10;                         // 1 (febrero)
        r = r + suma.get(Calendar.DAY_OF_MONTH);                       // 29

        // roll sobre diciembre da enero del MISMO año
        GregorianCalendar vuelta = gmt(2024, 11, 15);
        vuelta.roll(Calendar.MONTH, 1);
        r = r + vuelta.get(Calendar.MONTH);                            // 0
        r = r + (vuelta.get(Calendar.YEAR) == 2024 ? 100 : 0);         // el año NO cambia

        // add sobre diciembre si cambia el año
        GregorianCalendar avanza = gmt(2024, 11, 15);
        avanza.add(Calendar.MONTH, 1);
        r = r + (avanza.get(Calendar.YEAR) == 2025 ? 200 : 0);
        r = r + avanza.get(Calendar.MONTH);                            // 0

        // ---- cruzar años sumando dias -----------------------------------------------------------
        GregorianCalendar fin = gmt(2023, 11, 31);
        fin.add(Calendar.DAY_OF_MONTH, 1);
        r = r + (fin.get(Calendar.YEAR) == 2024 ? 2000 : 0);
        r = r + (fin.get(Calendar.MONTH) == 0 ? 20000 : 0);
        r = r + fin.get(Calendar.DAY_OF_MONTH);                        // 1

        // ---- una fecha lejana, para probar la aritmetica de siglos ------------------------------
        GregorianCalendar lejos = gmt(1800, 6, 4);
        r = r + lejos.get(Calendar.DAY_OF_WEEK) * 200000;              // el 4/7/1800 fue viernes = 6
        r = r + lejos.get(Calendar.DAY_OF_YEAR);                       // 185

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
