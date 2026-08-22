public class Switches {
  public int stmt(int x) { int r = 0; switch (x) { case 1: r = 10; break; case 2: r = 20; break; default: r = -1; } return r; }
  public int expr(int x) { return switch (x) { case 1 -> 10; case 2 -> 20; default -> -1; }; }
}
