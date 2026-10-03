package pb;
import pa.Base;
// anidada estatica: estaticos, por nombre simple, por Base., por P02., y sobre instancia de Base
public class P02 extends Base {
    static class N {
        void u(Base b) {
            sm(); Base.sm(); P02.sm();
            sf = 3; Base.sf += 1; P02.sf++; int k = sf;
            b.sm();
            System.out.println("P02 " + k + " " + smi(4) + " " + Base.smi(5));
        }
    }
    public static void main(String[] a) { new N().u(new P02()); }
}
