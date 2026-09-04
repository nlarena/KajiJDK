public class KajiBeansHumoU {
    public static int run() throws Exception {
        java.lang.reflect.Method h = String.class.getMethod("hashCode", new Class<?>[0]);
        Object a = h.invoke("x", new Object[0]);
        System.out.println("hashCode=" + a);
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
