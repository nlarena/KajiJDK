public class Exc {
  public int run(int x) {
    try { if (x < 0) throw new RuntimeException("neg"); return x; }
    catch (RuntimeException e) { return -1; }
    finally { int y = 0; }
  }
}
