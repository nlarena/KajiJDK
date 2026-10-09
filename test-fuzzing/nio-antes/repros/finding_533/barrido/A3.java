public class A3 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    String nombre = "externa";
    String ir(String p) {
        String s = "loc";
        return correr(new F() {
            String campo = "c1";
            public String f() {
                String t = "t";
                return correr(new F() {
                    String campo2 = s + "|" + t + "|" + p + "|" + campo;
                    public String f() { return nombre + ":" + campo2 + ":" + campo + ":" + A3.this.nombre + ":" + toString().startsWith("A3$1$1"); }
                });
            }
        });
    }
    public static void main(String[] a) {
        System.out.println(new A3().ir("par"));
    }
}
