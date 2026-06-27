import java.util.List;
public class LocalVars {
    long add(int a, long b) {
        int sum = a + (int) b;
        String s = "x";
        return sum + b;
    }
    <T> void g(List<T> items) {
        for (T t : items) { System.out.println(t); }
    }
}
