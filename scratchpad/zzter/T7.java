public class T7 { static String[] copy(String[] s){ return s; }
  static String[] f(String[] p){ return p == null ? new String[0] : copy(p); }
  public static int run(){ return f(new String[]{"a"}).length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
