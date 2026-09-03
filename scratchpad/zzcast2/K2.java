public class K2 { public static int run() { Object o = new String[]{"a"}; String[] k = (String[]) o; return k.length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
