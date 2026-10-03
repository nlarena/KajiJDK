package pb;
import pa.Base;
// la anidada TAMBIEN hereda de Base: javac prueba cada envolvente (Resolve.isProtectedAccessible),
// asi que h.m() con h de tipo P09 vale por P09 aunque la mas interna sea I.
public class P09 extends Base {
    static class I extends Base {
        void u(P09 h) { h.m(); m(); System.out.println("P09 " + h.f); }
    }
    public static void main(String[] a) { new I().u(new P09()); }
}
