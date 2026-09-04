import java.io.File;
import java.io.RandomAccessFile;

public class RafDbg {
    public static void main(String[] a) throws Exception {
        File dir = new File(System.getProperty("java.io.tmpdir"), "kaji-rafdbg");
        dir.mkdirs();
        File f = new File(dir, "t.bin");
        f.delete();
        RandomAccessFile w = new RandomAccessFile(f, "rw");
        w.write(new byte[]{1, 2, 3, 4, 5});
        w.seek(10);
        w.write(0x7F);
        System.out.println("len=" + w.length());
        w.setLength(3);
        System.out.println("shrink len=" + w.length() + " ptr=" + w.getFilePointer());
        w.setLength(8);
        System.out.println("grow len=" + w.length() + " ptr=" + w.getFilePointer());
        w.seek(0);
        byte[] todo = new byte[8];
        w.readFully(todo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < todo.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(todo[i]);
        }
        System.out.println("bytes=" + sb);
        w.close();
        f.delete();
    }
}
