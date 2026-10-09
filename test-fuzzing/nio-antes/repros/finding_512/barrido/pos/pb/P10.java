package pb;
import pa.Base;
// method refs a un protected desde la subclase y desde una anidada
public class P10 extends Base {
    void go() { Runnable r = this::m; r.run(); }
    static class N { void u(P10 h) { Runnable r = h::m; r.run(); Runnable s = Base::sm; s.run(); } }
    public static void main(String[] a) { new P10().go(); new N().u(new P10()); System.out.println("P10 ok"); }
}
