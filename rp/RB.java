public class RB {
    public static class G extends RA.F {
        protected G(String n) { super(n); }
    }
    public static int run() {
        RB.G g = new RB.G("x");
        new RA().tomar(g);
        return -1;
    }
}
