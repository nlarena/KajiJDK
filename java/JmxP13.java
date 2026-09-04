import javax.management.MXBean;
public class JmxP13 {
    @MXBean(false)
    public interface A { int getX(); }
    @MXBean
    public interface B { int getX(); }
    public static int run() {
        MXBean a = A.class.getAnnotation(MXBean.class);
        MXBean b = B.class.getAnnotation(MXBean.class);
        System.out.println("A anot=" + a + " value=" + (a == null ? "n/a" : String.valueOf(a.value())));
        System.out.println("B anot=" + b + " value=" + (b == null ? "n/a" : String.valueOf(b.value())));
        System.out.println("declaradas A=" + A.class.getAnnotations().length);
        return -1;
    }
}
