package pb;
import pa.Base;
// la S es una ANONIMA; la de adentro usa lo protegido con receptor escrito del tipo de la S (via this)
public class P21 {
    interface G { void g(); }
    public static void main(String[] a) {
        new Base(5) {
            void x() { final Base yo = this; G g = new G() { public void g() { System.out.println("P21 " + f); } }; g.g(); }
            { x(); }
        };
    }
}
