public class T2 { static String copy(String s){ return s; }
  static String f(String p){ return p == null ? "" : copy(p); }
  public static int run(){ return f("a").length() == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
