public class KajiBeansHumoQ {
    public static int run() throws Exception {
        System.out.println("java.version=[" + System.getProperty("java.version") + "]");
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        java.io.Writer w = new java.io.OutputStreamWriter(b, "UTF-8");
        w.write("hola ñ"); w.flush(); w.close();
        System.out.println("osw ok bytes=" + b.toByteArray().length);
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
