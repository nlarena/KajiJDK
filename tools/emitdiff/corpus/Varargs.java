public class Varargs {
  public int sum(int... xs) {
    int s = 0;
    for (int x : xs) s += x;
    return s;
  }
  @SafeVarargs
  public final <T> int count(T... xs) { return xs.length; }
  public int call() {
    return sum(1, 2, 3) + count("a", "b");
  }
  public int empty() { return sum(); }
}
