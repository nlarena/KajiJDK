import java.util.List;

public class Wild {
  public double sumNums(List<? extends Number> xs) {
    double s = 0;
    for (Number n : xs) s += n.doubleValue();
    return s;
  }
  public void fill(List<? super Integer> xs, int n) {
    for (int i = 0; i < n; i++) xs.add(i);
  }
  public <T extends Comparable<T>> T maxOf(T a, T b) {
    return a.compareTo(b) >= 0 ? a : b;
  }
}
