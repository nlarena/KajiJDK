public class KD { public static int run() {
    Object o = "no soy un arreglo";
    try { Object[] k = (Object[]) o; return k.length; }
    catch (ClassCastException e) { return -1; } }
  public static void main(String[] a){ System.out.println(run()); } }
