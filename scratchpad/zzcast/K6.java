public class K6 { public static int run() { Object o = new Object[]{"a"};
    if (!(o instanceof Object[])) { return 1; }
    Object[] k = (Object[]) o; return k.length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
