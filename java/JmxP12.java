import java.util.*;
public class JmxP12 {
    public static int run() {
        Map<String,String> m = new LinkedHashMap<String,String>();
        m.put("z","1"); m.put("a","2"); m.put("m","3");
        StringBuilder it = new StringBuilder();
        for (String v : m.values()) it.append(v);
        String[] a1 = m.values().toArray(new String[m.size()]);
        String[] a2 = m.values().toArray(new String[0]);
        Object[] a3 = m.values().toArray();
        StringBuilder b1 = new StringBuilder(); for (String s : a1) b1.append(s);
        StringBuilder b2 = new StringBuilder(); for (String s : a2) b2.append(s);
        StringBuilder b3 = new StringBuilder(); for (Object s : a3) b3.append(s);
        StringBuilder k = new StringBuilder(); for (String s : m.keySet()) k.append(s);
        System.out.println("iterador=" + it + " keySet=" + k
            + " toArray(new[size])=" + b1 + " toArray(new[0])=" + b2 + " toArray()=" + b3);
        return ("123".equals(it.toString()) && "123".equals(b1.toString())
             && "123".equals(b2.toString()) && "123".equals(b3.toString())) ? -1 : 0;
    }
}
