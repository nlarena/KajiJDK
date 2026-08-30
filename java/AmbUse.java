public class AmbUse {
    public static int run() {
        int[] x = { 1 };
        int[] y = { 2 };
        return AmbLib.f(x, y);
    }
}
