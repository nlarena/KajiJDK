public class Nested {
  static class Inner { int f() { return 7; } }
  int outer() { Inner i = new Inner(); return i.f(); }
}
