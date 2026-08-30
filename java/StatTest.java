import java.util.DoubleSummaryStatistics;
import java.util.EventObject;
import java.util.IllformedLocaleException;
import java.util.IntSummaryStatistics;
import java.util.LongSummaryStatistics;

// Comportamiento de las clases nuevas de java.util: los tres resumenes estadisticos y las chicas.
public class StatTest {

    public static int run() {
        int r = 0;

        // ---- IntSummaryStatistics --------------------------------------------------------------
        IntSummaryStatistics is = new IntSummaryStatistics();
        r = r + (int) is.getCount();                       // 0
        r = r + (is.getMin() == 2147483647 ? 1 : 0);       // vacio -> MAX_VALUE
        r = r + (is.getMax() == -2147483648 ? 10 : 0);     // vacio -> MIN_VALUE
        r = r + (is.getAverage() == 0.0d ? 100 : 0);

        is.accept(3);
        is.accept(7);
        is.accept(-2);
        r = r + (int) is.getCount() * 1000;                // 3000
        r = r + (int) is.getSum() * 10000;                 // 80000
        r = r + is.getMin() * 100000;                      // -200000
        r = r + is.getMax() * 1000000;                     // 7000000
        r = r + (is.getAverage() == 8.0d / 3.0d ? 10000000 : 0);

        IntSummaryStatistics is2 = new IntSummaryStatistics();
        is2.accept(20);
        is.combine(is2);
        r = r + (int) is.getCount();                       // 4
        r = r + is.getMax();                               // 20

        // el constructor validador
        try {
            new IntSummaryStatistics(-1L, 0, 0, 0L);
            r = r + 7777;
        } catch (IllegalArgumentException ex) {
            r = r + 100000000;
        }
        try {
            new IntSummaryStatistics(2L, 9, 1, 10L);       // min > max
            r = r + 7777;
        } catch (IllegalArgumentException ex) {
            r = r + 1;
        }

        // ---- LongSummaryStatistics: acepta int Y long ------------------------------------------
        LongSummaryStatistics ls = new LongSummaryStatistics();
        ls.accept(5);                                       // accept(int)
        ls.accept(11L);                                     // accept(long)
        r = r + (int) ls.getCount();                        // 2
        r = r + (int) ls.getSum();                          // 16
        r = r + (int) ls.getMin();                          // 5

        // ---- DoubleSummaryStatistics: la suma compensada ---------------------------------------
        DoubleSummaryStatistics ds = new DoubleSummaryStatistics();
        ds.accept(1.0d);
        ds.accept(2.5d);
        ds.accept(0.5d);
        r = r + (int) ds.getCount();                        // 3
        r = r + (int) ds.getSum();                          // 4
        r = r + (ds.getMin() == 0.5d ? 1 : 0);
        r = r + (ds.getMax() == 2.5d ? 10 : 0);

        // Kahan: 1.0 mas diez millones de 1e-9. La suma ingenua pierde los sumandos chicos; la
        // compensada los conserva. El JDK devuelve 1.01 aca, no 1.0.
        DoubleSummaryStatistics k = new DoubleSummaryStatistics();
        k.accept(1.0d);
        int i = 0;
        while (i < 10000000) {
            k.accept(0.000000001d);
            i = i + 1;
        }
        r = r + (k.getSum() > 1.0d ? 1000 : 0);            // la compensacion se nota

        // ---- las chicas ------------------------------------------------------------------------
        EventObject ev = new EventObject("fuente");
        r = r + (ev.getSource().equals("fuente") ? 10000 : 0);
        try {
            new EventObject(null);
            r = r + 7777;
        } catch (IllegalArgumentException ex) {
            r = r + 100000;
        }

        IllformedLocaleException ile = new IllformedLocaleException("mal", 12);
        r = r + ile.getErrorIndex() * 1000000;              // 12000000
        r = r + (new IllformedLocaleException().getErrorIndex() == -1 ? 1 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
