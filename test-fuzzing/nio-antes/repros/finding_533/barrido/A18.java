import java.util.*;
import java.util.function.*;
public class A18 {
    public static void run() { main(new String[0]); }
    public static void main(String[] a) {
        List<String> lista = new ArrayList<String>(); lista.add("b"); lista.add("a"); lista.add("c");
        String pre = ">";
        Function<String, Comparator<String>> fab = new Function<String, Comparator<String>>() {
            public Comparator<String> apply(String modo) {
                return new Comparator<String>() {
                    public int compare(String x, String y) { return modo.equals("inv") ? y.compareTo(x) : x.compareTo(y); }
                    public String toString() { return pre + modo; }
                };
            }
        };
        Comparator<String> c = fab.apply("inv");
        lista.sort(c);
        System.out.println(lista + " " + c);
        Iterator<Integer> it = new Iterator<Integer>() {
            int i = 0;
            public boolean hasNext() { return i < 3; }
            public Integer next() {
                int base = i++;
                return new Supplier<Integer>() { public Integer get() { return base * 10 + lista.size(); } }.get();
            }
        };
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) sb.append(it.next()).append(' ');
        System.out.println(sb);
    }
}
