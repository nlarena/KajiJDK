public class AmbLib {
    public static int f(boolean[] a, boolean[] b) { return 1; }
    public static int f(byte[] a, byte[] b) { return 1; }
    public static int f(char[] a, char[] b) { return 1; }
    public static int f(short[] a, short[] b) { return 1; }
    public static int f(int[] a, int[] b) { return 1; }
    public static int f(long[] a, long[] b) { return 1; }
    public static int f(float[] a, float[] b) { return 1; }
    public static int f(double[] a, double[] b) { return 1; }
    public static <T extends Comparable<? super T>> int f(T[] a, T[] b) { return 2; }
    public static <T> int f(T[] a, T[] b, java.util.Comparator<? super T> c) { return 3; }
}
