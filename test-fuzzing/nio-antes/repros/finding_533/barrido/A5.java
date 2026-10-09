public class A5 {
    public static void run() { main(new String[0]); }
    interface F { String f(); }
    static String correr(F f) { return f.f(); }
    static String sfijo = "S";
    F campo = new F() {
        public String f() { return correr(new F() { public String f() { return "campo" + sfijo + A5.this.x; } }); }
    };
    String x = "X";
    F bloque;
    { String loc = "B"; bloque = new F() { public String f() { return correr(new F() { public String f() { return "bloque" + loc + x; } }); } }; }
    static F estatico = new F() {
        public String f() { return correr(new F() { public String f() { return "estatico" + sfijo; } }); }
    };
    public static void main(String[] a) {
        A5 o = new A5();
        System.out.println(o.campo.f());
        System.out.println(o.bloque.f());
        System.out.println(estatico.f());
    }
}
