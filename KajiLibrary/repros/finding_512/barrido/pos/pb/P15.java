package pb;
import pa.Base;
// super cualificado desde una interna: P15.super.m()
public class P15 extends Base {
    protected void m() { System.out.println("P15.m"); }
    class I { void u() { P15.super.m(); m(); } }
    public static void main(String[] a) { new P15().new I().u(); }
}
