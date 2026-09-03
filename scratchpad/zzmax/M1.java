public class M1 { byte[] f; static byte[] g(byte[] a,int b,int c){return a;}
  M1(byte[] p){ this.f = p == null ? null : g(p, 0, p.length); }
  public static int run(){ return new M1(new byte[3]).f.length == 3 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
