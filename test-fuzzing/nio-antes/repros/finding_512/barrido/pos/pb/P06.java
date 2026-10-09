package pb;
import pa.Base;
// dos niveles de anidamiento, estatica y interna
public class P06 extends Base {
    static class A { C mk() { return new C(); }
        static class B { void u(P06 h) { h.m(); h.f = 1; System.out.println("P06a " + h.f); } }
        class C { void u(P06 h) { h.f += 2; h.m(); } }
    }
    class I { J mk() { return new J(); } class J { void u() { m(); f = 11; P06.this.f++; System.out.println("P06b " + f); } } }
    public static void main(String[] a) {
        P06 h = new P06(); new A.B().u(h); new A().mk().u(h);
        I i0 = h.new I(); i0.mk().u();
    }
}
