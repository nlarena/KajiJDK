public class K3 { public static int run() { Object o = new int[]{1}; int[] k = (int[]) o; return k.length == 1 ? -1 : 1; }
  public static void main(String[] a){ System.out.println(run()); } }
