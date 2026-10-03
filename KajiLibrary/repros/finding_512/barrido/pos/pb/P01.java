package pb;
import pa.Base;
// anidada estatica: metodo, campo (leer, escribir, compuesto, ++), long, String
public class P01 extends Base {
    static class N {
        final P01 h; N(P01 h) { this.h = h; }
        void u() {
            h.m();
            int x = h.f; h.f = x + 1; h.f += 2; h.f++; ++h.f; int y = h.f--;
            h.lf = 5L; h.lf += 3; long z = h.lf++;
            h.s += "t";
            System.out.println("P01 " + h.f + " " + y + " " + h.lf + " " + z + " " + h.s + " " + h.mi(1, 2L));
        }
    }
    public static void main(String[] a) { new N(new P01()).u(); }
}
