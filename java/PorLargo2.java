import java.util.Comparator;
public class PorLargo2 implements Comparator<String> {
    public int compare(String x, String y) { return x.length() - y.length(); }
}
