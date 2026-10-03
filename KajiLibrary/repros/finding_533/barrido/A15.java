public class A15 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "s";
        System.out.println(correr(new F() {
            F g = new F() { public String f() { return "campo" + s; } };
            public String f() { return g.f(); }
        }));
    }
}
