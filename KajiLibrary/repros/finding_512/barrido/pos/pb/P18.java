package pb;
import pa.Base;
// la S es una clase LOCAL que extiende Base; adentro, otra local
public class P18 {
    public static void main(String[] a) {
        class Loc extends Base {
            void x() {
                class Adentro { int g(Loc o) { m(); f += 10; return o.f++; } }
                System.out.println("P18 " + new Adentro().g(this) + " " + f);
            }
        }
        new Loc().x();
    }
}
