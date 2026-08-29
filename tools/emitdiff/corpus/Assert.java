public class Assert {
  public int div(int a, int b) {
    assert b != 0 : "div by zero";
    return a / b;
  }
  public void check(int x) {
    assert x > 0;
  }
}
