package pb;
import pa.Base;
// la S es una ANONIMA que extiende Base; adentro, otra anonima (Runnable) usa lo protegido
public class P12 {
    public static void main(String[] a) {
        Base b = new Base(3) {
            void x() { new Runnable() { public void run() { m(); f += 10; sm(); System.out.println("P12 " + f); } }.run(); }
            { x(); }
        };
    }
}
