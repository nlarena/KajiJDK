import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
public class CalDiag {
    static GregorianCalendar gmt(int y, int m, int d) {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        c.set(Calendar.YEAR, y); c.set(Calendar.MONTH, m); c.set(Calendar.DAY_OF_MONTH, d);
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0);
        return c;
    }
    static GregorianCalendar ep() {
        GregorianCalendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT"), Locale.US);
        c.setTimeInMillis(0L); return c;
    }
    public static int epYear()  { return ep().get(Calendar.YEAR); }
    public static int epDow()   { return ep().get(Calendar.DAY_OF_WEEK); }
    public static int epDoy()   { return ep().get(Calendar.DAY_OF_YEAR); }
    public static int bisDom()  { return gmt(2024,1,29).get(Calendar.DAY_OF_MONTH); }
    public static int bisDoy()  { return gmt(2024,1,29).get(Calendar.DAY_OF_YEAR); }
    public static int bisDow()  { return gmt(2024,1,29).get(Calendar.DAY_OF_WEEK); }
    public static int maxFeb24(){ return gmt(2024,1,1).getActualMaximum(Calendar.DAY_OF_MONTH); }
    public static int maxFeb23(){ return gmt(2023,1,1).getActualMaximum(Calendar.DAY_OF_MONTH); }
    public static int maxEne()  { return gmt(2024,0,1).getActualMaximum(Calendar.DAY_OF_MONTH); }
    public static int addMes()  { GregorianCalendar c=gmt(2024,0,31); c.add(Calendar.MONTH,1); return c.get(Calendar.MONTH)*100+c.get(Calendar.DAY_OF_MONTH); }
    public static int rollMes() { GregorianCalendar c=gmt(2024,11,15); c.roll(Calendar.MONTH,1); return c.get(Calendar.YEAR)*100+c.get(Calendar.MONTH); }
    public static int addDic()  { GregorianCalendar c=gmt(2024,11,15); c.add(Calendar.MONTH,1); return c.get(Calendar.YEAR)*100+c.get(Calendar.MONTH); }
    public static int cruzaAno(){ GregorianCalendar c=gmt(2023,11,31); c.add(Calendar.DAY_OF_MONTH,1); return c.get(Calendar.YEAR)*10000+c.get(Calendar.MONTH)*100+c.get(Calendar.DAY_OF_MONTH); }
    public static int lejosDow(){ return gmt(1800,6,4).get(Calendar.DAY_OF_WEEK); }
    public static int lejosDoy(){ return gmt(1800,6,4).get(Calendar.DAY_OF_YEAR); }
    public static void main(String[] a) throws Exception {
        String[] ms = {"epYear","epDow","epDoy","bisDom","bisDoy","bisDow","maxFeb24","maxFeb23","maxEne","addMes","rollMes","addDic","cruzaAno","lejosDow","lejosDoy"};
        for (String m : ms) System.out.println(m + "=" + CalDiag.class.getMethod(m).invoke(null));
    }
}
