public class B1 { public static int run() {
    Object v = new Object();
    int h = v.hashCode();            // invokevirtual sobre un objeto plano
    Object[] o = new Object[1];
    o[0] = v;
    return h != 0 ? -1 : -1; }
  public static void main(String[] a){System.out.println(run());} }
