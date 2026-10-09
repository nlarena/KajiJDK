package pb;
import pa.Base;
// lambda en la subclase y lambda dentro de una anidada
public class P05 extends Base {
    void go() { Runnable r = () -> { m(); f += 5; System.out.println("P05a " + f); }; r.run(); }
    static class N {
        void u(P05 h) { Runnable r = () -> { h.m(); h.f = 6; h.f++; sm(); System.out.println("P05b " + h.f); }; r.run(); }
    }
    public static void main(String[] a) { new P05().go(); new N().u(new P05()); }
}
