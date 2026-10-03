package pb;
import pa.Base;
// anonima dentro de un metodo de la subclase
public class P04 extends Base {
    void go() {
        Runnable r = new Runnable() { public void run() { m(); f = 4; f++; System.out.println("P04 " + f + " " + mi(2, 3L)); sm(); } };
        r.run();
    }
    public static void main(String[] a) { new P04().go(); }
}
