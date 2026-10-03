package pb;
import pa.Base;
// interna (no estatica): nombre simple (this$0 implicito), P03.this., otra instancia
public class P03 extends Base {
    class I {
        void u(P03 otra) {
            m(); P03.this.m(); f = 7; P03.this.f += 1; f++;
            otra.m(); otra.f = 9;
            System.out.println("P03 " + f + " " + otra.f + " " + mi(1, 1L));
        }
    }
    public static void main(String[] a) { P03 p = new P03(); p.new I().u(new P03()); }
}
