import java.util.logging.*;
import java.io.*;
public class UpdProbe {
    static void upd(LogManager m, String s) throws Exception { m.updateConfiguration(new ByteArrayInputStream(s.getBytes("UTF-8")), null); }
    public static void main(String[] a) throws Exception {
        LogManager m = LogManager.getLogManager();
        m.readConfiguration(new ByteArrayInputStream(".level=WARNING\n".getBytes("UTF-8")));
        Logger p = Logger.getLogger("up.a");
        System.out.println("A " + p.getLevel());
        upd(m, ".level=WARNING\nup.a.level=FINE\n");
        System.out.println("B " + p.getLevel() + " prop=" + m.getProperty("up.a.level"));
        upd(m, ".level=WARNING\n");
        System.out.println("C " + p.getLevel() + " prop=" + m.getProperty("up.a.level"));
    }
}
