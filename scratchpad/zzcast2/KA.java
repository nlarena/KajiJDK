public class KA { public static int run(Object o) {
    if (!(o instanceof int[])) { return 1; }
    int[] k = (int[]) o; return k.length == 1 ? -1 : 1; }
  public static int run() { return run((Object) new int[]{1}); }
  public static void main(String[] a){ System.out.println(run()); } }
