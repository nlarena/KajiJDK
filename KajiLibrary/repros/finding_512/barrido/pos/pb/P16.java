package pb;
import pa.Base;
// inicializadores de campo y de instancia de una anidada, switch, ternario, local dentro de lambda
public class P16 extends Base {
    static class N {
        int k = sf + 1;
        final P16 h;
        N(P16 h) { this.h = h; }
        { sf = 20; }
        int u() {
            int r = switch (h.f) { case 0 -> h.mi(1, 1L); default -> -1; };
            class L { int g() { return h.f + sf; } }
            Runnable run = () -> System.out.println("P16l " + new L().g());
            run.run();
            return h.f == 0 ? r + k : -1;
        }
    }
    public static void main(String[] a) { System.out.println("P16 " + new N(new P16()).u()); }
}
