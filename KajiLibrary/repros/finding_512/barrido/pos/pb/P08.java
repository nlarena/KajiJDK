package pb;
import pa.Base;
// receptor de una subclase de la subclase
public class P08 extends Base {
    static class Nieta extends P08 { }
    static class N { void u(Nieta n) { n.m(); n.f = 2; System.out.println("P08 " + n.f); } }
    public static void main(String[] a) { new N().u(new Nieta()); }
}
