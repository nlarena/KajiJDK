public class K1 { public static int run() { Object o = new Object[]{"a"}; Object[] k = (Object[]) o; return k.length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
