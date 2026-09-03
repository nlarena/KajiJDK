public class M3 { String f; static String g(String a,int b,int c){return a;}
  M3(String p){ this.f = p == null ? null : g(p, 0, p.length()); }
  public static int run(){ return new M3("abc").f.length() == 3 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
