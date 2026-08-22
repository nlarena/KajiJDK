public class Basics {
  private int x;
  public Basics(int v) { this.x = v; }
  public int add(int a, int b) { int s = a + b; return s + x; }
  public int max(int a, int b) { if (a > b) return a; return b; }
  public long wide(long a, int b) { return a + b; }
}
