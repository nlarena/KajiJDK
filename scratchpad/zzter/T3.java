public class T3 { static char[] f(char[] p){ return p == null ? new char[0] : new char[2]; }
  public static int run(){ return f(new char[]{'a'}).length == 2 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
