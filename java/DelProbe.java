import java.io.*;
public class DelProbe {
    public static void main(String[] a) throws Exception {
        String p = "H:/Trabajo/cv/KajiJDK/scratchpad/kjdel.txt";
        FileOutputStream o = new FileOutputStream(p); o.write(65); o.close();
        File f = new File(p);
        System.out.println("exists=" + f.exists() + " delete=" + f.delete() + " despues=" + f.exists());
        File d = new File("H:/Trabajo/cv/KajiJDK/scratchpad/kjdir");
        System.out.println("mkdir=" + d.mkdirs() + " isDir=" + d.isDirectory());
        System.out.println("rmdir=" + d.delete());
    }
}
