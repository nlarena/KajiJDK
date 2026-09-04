import java.util.ArrayList;
import java.util.List;
public class KajiBeansHumoZ {
    static List<Object> ciclica() {
        List<Object> l = new ArrayList<Object>();
        l.add("x"); l.add(l);
        return l;
    }
    static String prueba(String nombre, java.util.Map<Object,String> m) {
        try {
            m.put(ciclica(), "v");
            StringBuilder sb = new StringBuilder();
            java.util.Iterator<String> it = m.values().iterator();
            while (it.hasNext()) { sb.append(it.next()).append(' '); }
            return nombre + ": OK [" + sb.toString().trim() + "]";
        } catch (Throwable t) { return nombre + ": ROTO " + t.getClass().getName(); }
    }
    public static int run() throws Exception {
        java.util.Map<Object,String> m = new java.util.IdentityHashMap<Object,String>();
        List<Object> c = ciclica();
        try { m.put(c, "v"); System.out.println("put OK"); }
        catch (Throwable t) { System.out.println("put ROTO " + t.getClass().getName()); }
        try { System.out.println("get = " + m.get(c)); }
        catch (Throwable t) { System.out.println("get ROTO " + t.getClass().getName()); }
        System.out.println(prueba("IdentityHashMap.values()", new java.util.IdentityHashMap<Object,String>()));
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
