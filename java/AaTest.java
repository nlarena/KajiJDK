public class AaTest {
    public static int run() {
        Object[][] m = new Object[][] {{"a", "b"}, {"c", "d"}};
        return ((String) m[1][0]).equals("c") ? -1 : 0;
    }
    public static void main(String[] a) { System.out.println(run()); }
}
