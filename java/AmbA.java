public class AmbA {
    static int f(int[] a, int[] b) { return 1; }
    static <T extends Comparable<? super T>> int f(T[] a, T[] b) { return 2; }
    public static int run() {
        int[] x = { 1 };
        int[] y = { 2 };
        return f(x, y);      // deberia elegir f(int[], int[]) -> 1
    }
}
