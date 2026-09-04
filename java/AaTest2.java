public class AaTest2 {
    public static int run() {
        Object[] flat = new Object[] {"a", "b"};      // aastore String -> Object[]
        if (!flat[0].equals("a")) return 0;
        Object[][] m = new Object[2][];
        Object[] row = new Object[] {"c"};
        m[0] = row;                                    // aastore Object[] -> Object[][]
        if (!m[0][0].equals("c")) return 1;
        String[] s = new String[] {"z"};
        Object[] up = new Object[1];
        up[0] = s;                                     // aastore String[] -> Object[]
        return -1;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
