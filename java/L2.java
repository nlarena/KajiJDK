public class L2 {
    public static int run() {
        int n = 5;
        Op2 f = a -> a + n;
        return f.apply(10);
    }
}
interface Op2 { int apply(int a); }
