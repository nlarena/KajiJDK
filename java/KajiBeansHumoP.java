import java.lang.reflect.Array;

public class KajiBeansHumoP {
    public static int run() throws Exception {
        Object a = Array.newInstance(int.class, 3);
        System.out.println("newInstance ok: " + a.getClass().getName() + " len=" + ((int[]) a).length);
        java.beans.Expression e = new java.beans.Expression(Array.class, "newInstance",
            new Object[] { String.class, Integer.valueOf(2) });
        Object v = e.getValue();
        System.out.println("expr ok: " + v.getClass().getName());
        java.beans.Encoder enc = new java.beans.Encoder();
        System.out.println("delegado String -> " + enc.getPersistenceDelegate(String.class).getClass().getName());
        System.out.println("delegado int[] -> " + enc.getPersistenceDelegate(int[].class).getClass().getName());
        return -1;
    }
    public static void main(String[] x) throws Exception { System.out.println(run()); }
}
