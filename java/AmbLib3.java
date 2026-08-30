import java.util.Comparator;
public class AmbLib3 {
    public static int f(boolean[] a, boolean[] b) { return 1; }
    public static int f(byte[] a, byte[] b) { return 1; }
    public static int f(char[] a, char[] b) { return 1; }
    public static int f(short[] a, short[] b) { return 1; }
    public static int f(int[] a, int[] b) { return 1; }
    public static int f(long[] a, long[] b) { return 1; }
    public static int f(float[] a, float[] b) { return 1; }
    public static int f(double[] a, double[] b) { return 1; }
    public static int f(boolean[] a, int af, int at, boolean[] b, int bf, int bt) { return 9; }
    public static int f(byte[] a, int af, int at, byte[] b, int bf, int bt) { return 9; }
    public static int f(char[] a, int af, int at, char[] b, int bf, int bt) { return 9; }
    public static int f(short[] a, int af, int at, short[] b, int bf, int bt) { return 9; }
    public static int f(int[] a, int af, int at, int[] b, int bf, int bt) { return 9; }
    public static int f(long[] a, int af, int at, long[] b, int bf, int bt) { return 9; }
    public static int f(float[] a, int af, int at, float[] b, int bf, int bt) { return 9; }
    public static int f(double[] a, int af, int at, double[] b, int bf, int bt) { return 9; }
    public static <T extends Comparable<? super T>> int f(T[] a, T[] b) { return 2; }
    public static <T extends Comparable<? super T>> int f(T[] a, int af, int at, T[] b, int bf, int bt) { return 3; }
    public static <T> int f(T[] a, T[] b, Comparator<? super T> c) { return 4; }
    public static <T> int f(T[] a, int af, int at, T[] b, int bf, int bt, Comparator<? super T> c) { return 5; }
}
