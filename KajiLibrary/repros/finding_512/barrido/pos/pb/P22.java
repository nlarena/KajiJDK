package pb;
import pa.Base;
// control SIN protected: anonima dentro de anonima que lee un campo publico de la de afuera
public class P22 {
    interface G { void g(); }
    public static void main(String[] a) {
        new Object() {
            public int k = 7;
            void x() { G g = new G() { public void g() { System.out.println("P22 " + k); } }; g.g(); }
            { x(); }
        };
    }
}
