public class Rep505 {
    static class Base {
        int v;
        Base() { this.v = 0; }
        Base(int v) { this.v = v; }
    }
    static class Externa {
        public int deLaExterna() { return 42; }
        SinCtor crearSinCtor() { return new SinCtor(); }
        CtorSinSuper crearCtorSinSuper() { return new CtorSinSuper(1); }
        CtorConSuper crearCtorConSuper() { return new CtorConSuper(1); }
        class SinCtor extends Base {
            int llama() { return deLaExterna(); }
        }
        class CtorSinSuper extends Base {
            CtorSinSuper(int x) { this.v = x; }
            int llama() { return deLaExterna(); }
        }
        class CtorConSuper extends Base {
            CtorConSuper(int x) { super(x); }
            int llama() { return deLaExterna(); }
        }
    }
    public static int run() {
        Externa e = new Externa();
        System.out.println("//sinCtor " + e.crearSinCtor().llama());
        System.out.println("//ctorSinSuper " + e.crearCtorSinSuper().llama());
        System.out.println("//ctorConSuper " + e.crearCtorConSuper().llama());
        return 0;
    }
    public static void main(String[] x) { run(); }
}
