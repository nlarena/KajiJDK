public class T5 { static char[] copy(char[] s){ return s; }
  char[] pw;
  T5(char[] p){ this.pw = p == null ? new char[0] : copy(p); }
  public static int run(){ return new T5(new char[]{'a'}).pw.length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
