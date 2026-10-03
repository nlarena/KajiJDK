public class A1 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String r = correr(new F() {
            public String f() {
                return correr(new F() { public String f() { return "adentro"; } });
            }
        });
        System.out.println(r);
    }
}
