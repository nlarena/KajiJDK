package pb;
import pa.Base;
import java.util.function.*;
// method refs desde anidadas: no ligada, estatica, ligada a Outer.this, ligada a una Nieta
public class P14 extends Base {
    static class Nieta extends P14 { }
    class I {
        void u(Nieta n) {
            Consumer<P14> c = P14::m; c.accept(P14.this);
            IntUnaryOperator op = Base::smi; 
            P14 yo = P14.this; Runnable r = yo::m; r.run();
            Runnable q = n::m; q.run();
            BiFunction<Integer, Long, Integer> mi = n::mi;
            System.out.println("P14 " + op.applyAsInt(21) + " " + mi.apply(1, 2L));
        }
    }
    public static void main(String[] a) { new P14().new I().u(new Nieta()); }
}
