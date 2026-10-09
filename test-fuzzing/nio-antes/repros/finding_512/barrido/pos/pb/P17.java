package pb;
import pa.Gen;
// S generica: S<X> extends Gen<X>
public class P17<X> extends Gen<X> {
    static class N {
        <Y> Y u(P17<Y> h, Y y) { h.val = y; Y z = h.get(); return h.val; }
    }
    public static void main(String[] a) { System.out.println("P17 " + new N().u(new P17<String>(), "w")); }
}
