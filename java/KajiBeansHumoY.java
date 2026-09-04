import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
public class KajiBeansHumoY {
    public static int run() throws Exception {
        List<Object> l = new ArrayList<Object>();
        l.add("x"); l.add(l);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        XMLEncoder e = new XMLEncoder(b);
        e.writeObject(l);
        try { e.close(); } catch (Throwable t) { System.out.println("TOPE " + t.getClass().getName()); }
        System.out.println(new String(b.toByteArray()));
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
