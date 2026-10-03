public class A2 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "met";
        int n = 7;
        String r = correr(new F() {
            public String f() {
                String t = "anon";
                return correr(new F() { public String f() { return s + n + t; } });
            }
        });
        System.out.println(r);
    }
}
