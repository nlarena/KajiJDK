public class Generics<T> {
  private T v;
  public void set(T x) { v = x; }
  public T get() { return v; }
  public static <U> U id(U x) { return x; }
}
