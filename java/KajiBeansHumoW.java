import java.util.ArrayList;
import java.util.List;
public class KajiBeansHumoW {
    public static int run() throws Exception {
        List<Object> l = new ArrayList<Object>();
        l.add("x");
        l.add(l);
        System.out.println("A: construida, size=" + l.size());
        try { System.out.println("B: toString = " + l.toString()); }
        catch (Throwable t) { System.out.println("B: ROTO " + t.getClass().getName()); }
        try { System.out.println("C: equals a si misma = " + l.equals(l)); }
        catch (Throwable t) { System.out.println("C: ROTO " + t.getClass().getName()); }
        try { System.out.println("D: hashCode = " + l.hashCode()); }
        catch (Throwable t) { System.out.println("D: ROTO " + t.getClass().getName()); }
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
