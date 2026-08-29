public sealed interface Sealed permits Sealed.A, Sealed.B {
  int val();
  record A(int x) implements Sealed { public int val() { return x; } }
  record B(int y, int z) implements Sealed { public int val() { return y + z; } }
}
