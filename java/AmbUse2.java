public class AmbUse2 {
    public static int run() {
        int[] x = { 1 };
        int[] y = { 2 };
        int r = 0;
        r = r + (AmbLib.f(x, y) < 0 ? 10 : 0);
        return r;
    }
}
