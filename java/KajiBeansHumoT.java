public class KajiBeansHumoT {
    public static int run() throws Exception {
        try {
            java.beans.Encoder e = new java.beans.Encoder() { };
            java.lang.reflect.Method m = java.beans.Encoder.class.getDeclaredMethod("writeObject", Object.class);
            m.invoke(e, new Object[] { new XPunto() });
        } catch (Throwable t) {
            System.out.println("TIPO: " + t.getClass().getName());
            StackTraceElement[] st = t.getStackTrace();
            System.out.println("frames=" + st.length);
            for (int i = 0; i < st.length && i < 30; i++) { System.out.println("  " + st[i]); }
        }
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
