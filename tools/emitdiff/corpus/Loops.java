public class Loops {
  public int sum(int n) { int s = 0; for (int i = 0; i < n; i++) s += i; return s; }
  public int count(int n) { int c = 0; while (n > 0) { n--; if (n == 3) continue; if (n == 1) break; c++; } return c; }
  public int dw(int n) { int c = 0; do { c++; n--; } while (n > 0); return c; }
}
