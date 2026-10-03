package pb;
import pa.Base;
// anidada dentro de un metodo (local) y anonima dentro de una anidada estatica
public class P11 extends Base {
    void go() { class L { void u() { m(); f = 3; } } new L().u(); System.out.println("P11a " + f); }
    static class N { void u(P11 h) { new Runnable() { public void run() { h.m(); h.f++; } }.run(); System.out.println("P11b " + h.f); } }
    public static void main(String[] a) { new P11().go(); new N().u(new P11()); }
}
