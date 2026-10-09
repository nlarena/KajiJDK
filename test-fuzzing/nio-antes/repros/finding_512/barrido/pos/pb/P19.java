package pb;
import pa.Base;
// la S es una LOCAL con nombre que extiende Base; adentro, una anonima usa lo protegido
public class P19 {
    public static void main(String[] a) {
        class L extends Base {
            L() { super(3); }
            void x() { new Runnable() { public void run() { m(); f += 10; sm(); System.out.println("P19 " + f); } }.run(); }
        }
        new L().x();
    }
}
