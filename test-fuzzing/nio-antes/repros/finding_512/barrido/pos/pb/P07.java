package pb;
import pa.Base;
// constructor: super(...) en la subclase; anonima `new Base(){}` desde una anidada (6.6.2.2)
public class P07 extends Base {
    P07() { super(3); }
    static class N {
        Base u() { return new Base(8) { }; }
    }
    public static void main(String[] a) { new P07().m(); new N().u(); System.out.println("P07 ok"); }
}
