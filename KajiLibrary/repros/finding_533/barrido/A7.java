public class A7 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static abstract class B implements F { final String base; B(String b) { base = b; } }
    static String correr(F f) { return f.f(); }
    public static void main(String[] a) {
        String s = "s";
        System.out.println(correr(new B("arg1") {
            String x = base + s;
            public String f() {
                String t = "t";
                return correr(new B(t + s) {
                    String y = base + x + t;
                    public String f() { return y + "/" + base + "/" + x; }
                });
            }
        }));
    }
}
