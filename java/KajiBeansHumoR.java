public class KajiBeansHumoR {
    public static int run() throws Exception {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        java.io.Writer w = new java.io.OutputStreamWriter(b, "UTF-8");
        w.write("\u00f1\u20ac"); w.flush(); w.close();
        byte[] r = b.toByteArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < r.length; i++) { sb.append(Integer.toHexString(r[i] & 0xff)).append(' '); }
        System.out.println("bytes=" + r.length + " [" + sb + "] esperado 5 [c3 b1 e2 82 ac]");
        return r.length == 5 ? -1 : r.length;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
