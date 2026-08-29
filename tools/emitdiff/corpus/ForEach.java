import java.util.List;

public class ForEach {
  public int arr(int[] xs) {
    int s = 0;
    for (int x : xs) s += x;
    return s;
  }
  public int objArr(String[] xs) {
    int n = 0;
    for (String x : xs) n += x.length();
    return n;
  }
  public int iter(List<Integer> xs) {
    int s = 0;
    for (int x : xs) s += x;
    return s;
  }
}
