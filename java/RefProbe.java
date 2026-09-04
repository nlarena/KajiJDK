public class RefProbe {
    public static void main(String[] a) throws Exception {
        Class<?> c = Class.forName("java.util.logging.ConsoleHandler");
        Object o = c.getDeclaredConstructor().newInstance();
        System.out.println("clase=" + o.getClass().getName());
        Object o2 = Class.forName("java.util.logging.SimpleFormatter").newInstance();
        System.out.println("clase2=" + o2.getClass().getName());
        try { Class.forName("no.existe.Nada"); System.out.println("sin error"); }
        catch (Throwable t) { System.out.println("err=" + t.getClass().getName()); }
    }
}
