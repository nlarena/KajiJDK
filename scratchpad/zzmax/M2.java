public class M2 { static byte[] g(byte[] a,int b,int c){return a;}
  static byte[] f(byte[] p){ byte[] r = p == null ? null : g(p, 0, p.length); return r; }
  public static int run(){ return f(new byte[3]).length == 3 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
