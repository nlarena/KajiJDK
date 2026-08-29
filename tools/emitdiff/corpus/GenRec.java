public record GenRec<A, B>(A first, B second) {
  public A left() { return first; }
  public <C> GenRec<A, C> withSecond(C c) { return new GenRec<>(first, c); }
}
