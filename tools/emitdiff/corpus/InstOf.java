public class InstOf {
  public int describe(Object o) {
    if (o instanceof String s) return s.length();
    if (o instanceof Integer i) return i;
    if (o instanceof int[] a) return a.length;
    return -1;
  }
  public boolean both(Object o) {
    return o instanceof String s && s.length() > 3;
  }
}
