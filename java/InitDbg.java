public class InitDbg {

    static class C1 {
        static int v = 7;
    }

    static class C2 {
        static int v = 7;
    }

    public static void main(String[] a) throws Exception {
        Class<?> c = Class.forName("InitDbg$C1");
        java.lang.reflect.Field f = c.getDeclaredField("v");
        f.setAccessible(true);
        System.out.println("tras forName: " + f.getInt(null));

        Class<?> d = InitDbg.C2.class;
        java.lang.reflect.Field g = d.getDeclaredField("v");
        g.setAccessible(true);
        System.out.println("solo con .class: " + g.getInt(null));
    }
}
