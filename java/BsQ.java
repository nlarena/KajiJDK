import java.util.Arrays;
public class BsQ {
    public static int hallado() { int[] o = { 1, 3, 5, 7, 9 }; return Arrays.binarySearch(o, 7); }
    public static int medio()   { int[] o = { 1, 3, 5, 7, 9 }; return Arrays.binarySearch(o, 4); }
    public static int antes()   { int[] o = { 1, 3, 5, 7, 9 }; return Arrays.binarySearch(o, 0); }
    public static int despues() { int[] o = { 1, 3, 5, 7, 9 }; return Arrays.binarySearch(o, 99); }
}
