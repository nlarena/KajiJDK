public class K5 { public static int run() { Object o = new Object[]{"a"}; return ((Object[]) o).length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
