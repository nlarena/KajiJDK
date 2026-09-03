public class K7 { public static int run(Object o) {
    if (!(o instanceof Object[])) { return 1; }
    Object[] k = (Object[]) o; return k.length == 1 ? -1 : 1; }
  public static int run() { return run((Object) new Object[]{"a"}); }
  public static void main(String[] a){ System.out.println(run()); } }
