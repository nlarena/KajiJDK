import java.io.*;
public class IoProbe {
    public static void main(String[] a) throws Exception {
        String p = "H:/Trabajo/cv/KajiJDK/scratchpad/kjprobe.txt";
        try {
            FileOutputStream o = new FileOutputStream(p);
            o.write("hola".getBytes("UTF-8"));
            o.close();
            File f = new File(p);
            System.out.println("exists=" + f.exists() + " len=" + f.length());
            FileInputStream in = new FileInputStream(p);
            byte[] b = new byte[10];
            int k = in.read(b);
            in.close();
            System.out.println("leido=" + new String(b, 0, k, "UTF-8"));
            File g = new File(p + ".2");
            System.out.println("rename=" + f.renameTo(g) + " g=" + g.exists());
            System.out.println("delete=" + g.delete());
        } catch (Throwable t) { System.out.println("EX " + t.getClass().getName() + " " + t.getMessage()); }
    }
}
