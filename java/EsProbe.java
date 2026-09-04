import java.util.EnumSet;
public class EsProbe {
    enum C { A, B, D }
    public static void main(String[] x) {
        EnumSet<C> s1 = EnumSet.of(C.A);
        EnumSet<C> s2 = EnumSet.of(C.A, C.B);
        EnumSet<C> s3 = EnumSet.of(C.A, C.B, C.D);
        System.out.println(s1.size() + " " + s2.size() + " " + s3.size());
    }
}
