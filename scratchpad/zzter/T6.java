public class T6 { static int[] copy(int[] s){ return s; }
  static int[] f(int[] p){ return p == null ? new int[0] : copy(p); }
  public static int run(){ return f(new int[]{1}).length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
