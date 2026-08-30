import java.util.Currency;
import java.util.Locale;
public class CurTest {
    public static int run() {
        int r = 0;
        Currency usd = Currency.getInstance("USD");
        r = r + usd.getNumericCode();                        // 840
        r = r + usd.getDefaultFractionDigits() * 10000;      // 20000
        r = r + (usd.getCurrencyCode().equals("USD") ? 100000 : 0);
        r = r + (Currency.getInstance("USD") == usd ? 1000000 : 0);   // misma instancia
        r = r + Currency.getInstance("JPY").getDefaultFractionDigits() * 7777;  // 0
        r = r + Currency.getInstance("KWD").getDefaultFractionDigits() * 10000000; // 3
        r = r + (Currency.getInstance("ARS").getNumericCodeAsString().equals("032") ? 100000000 : 0);
        r = r + (Currency.getInstance(Locale.US) == usd ? 1000 : 0);
        try {
            Currency.getInstance("ZZZ");
            r = r + 7777;
        } catch (IllegalArgumentException e) {
            r = r + 100;
        }
        return r;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
