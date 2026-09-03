public class T4 { static char[] copy(char[] s){ return s; }
  static char[] f(char[] p){ if (p == null) { return new char[0]; } return copy(p); }
  public static int run(){ return f(new char[]{'a'}).length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
