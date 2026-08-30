import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
public class CalDiag2 {
    static GregorianCalendar gmt(int y, int m, int d) {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        c.set(Calendar.YEAR,y); c.set(Calendar.MONTH,m); c.set(Calendar.DAY_OF_MONTH,d);
        c.set(Calendar.HOUR_OF_DAY,0); c.set(Calendar.MINUTE,0); c.set(Calendar.SECOND,0); c.set(Calendar.MILLISECOND,0);
        return c;
    }
    public static int epMonth() { GregorianCalendar c=new GregorianCalendar(TimeZone.getTimeZone("GMT"),Locale.US); c.setTimeInMillis(0L); return c.get(Calendar.MONTH); }
    public static int epDom()   { GregorianCalendar c=new GregorianCalendar(TimeZone.getTimeZone("GMT"),Locale.US); c.setTimeInMillis(0L); return c.get(Calendar.DAY_OF_MONTH); }
    public static int leap1900(){ return gmt(2000,0,1).isLeapYear(1900) ? 1 : 0; }
    public static int leap2000(){ return gmt(2000,0,1).isLeapYear(2000) ? 1 : 0; }
    public static int leap2024(){ return gmt(2000,0,1).isLeapYear(2024) ? 1 : 0; }
    public static int rollYear(){ GregorianCalendar c=gmt(2024,11,15); c.roll(Calendar.MONTH,1); return c.get(Calendar.YEAR); }
    public static int addYear() { GregorianCalendar c=gmt(2024,11,15); c.add(Calendar.MONTH,1); return c.get(Calendar.YEAR); }
    public static int finYear() { GregorianCalendar c=gmt(2023,11,31); c.add(Calendar.DAY_OF_MONTH,1); return c.get(Calendar.YEAR); }
    public static int finMonth(){ GregorianCalendar c=gmt(2023,11,31); c.add(Calendar.DAY_OF_MONTH,1); return c.get(Calendar.MONTH); }
    public static int finDom()  { GregorianCalendar c=gmt(2023,11,31); c.add(Calendar.DAY_OF_MONTH,1); return c.get(Calendar.DAY_OF_MONTH); }
    public static int sumaDom() { GregorianCalendar c=gmt(2024,0,31); c.add(Calendar.MONTH,1); return c.get(Calendar.DAY_OF_MONTH); }
    public static void main(String[] a) throws Exception {
        String[] ms={"epMonth","epDom","leap1900","leap2000","leap2024","rollYear","addYear","finYear","finMonth","finDom","sumaDom"};
        for(String m: ms) System.out.println(m+"="+CalDiag2.class.getMethod(m).invoke(null));
    }
}
