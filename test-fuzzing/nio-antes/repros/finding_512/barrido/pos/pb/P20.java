package pb;
import pa.Base;
// la S es una ANONIMA; la que usa lo protegido es una lambda adentro
public class P20 {
    public static void main(String[] a) {
        Base b = new Base(4) {
            void x() { Runnable r = () -> { m(); f += 1; System.out.println("P20 " + f); }; r.run(); }
            { x(); }
        };
    }
}
