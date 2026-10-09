public class Finding402 {
    static class G<R> { final R v; G(R v) { this.v = v; } }
    public static int run() {
        G<String> a = new G<String>("D") { };
        System.out.println("//finding402 " + a.v + " ctores=" + a.getClass().getDeclaredConstructors().length);
        return 0;
    }
    public static void main(String[] x) { run(); }
}
