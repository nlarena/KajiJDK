public class Ternary {
  public int nested(int a, int b, int c) {
    return a > b ? (a > c ? a : c) : (b > c ? b : c);
  }
  public String pick(int x) {
    return x < 0 ? "neg" : x == 0 ? "zero" : "pos";
  }
  public int chain(boolean p, boolean q, int a, int b, int c) {
    return p ? a : q ? b : c;
  }
}
