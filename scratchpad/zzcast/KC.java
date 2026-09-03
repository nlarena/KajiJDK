public class KC { public static int run(Object o) {
    Object[] k = (Object[]) o; return k.length == 1 ? -1 : 1; }
  public static int run() { return run((Object) new Object[]{"a"}); }
  public static void main(String[] a){ System.out.println(run()); } }
