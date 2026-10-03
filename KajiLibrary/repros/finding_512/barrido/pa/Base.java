package pa;
public class Base {
    protected int f;
    protected static int sf;
    protected long lf;
    protected String s = "s";
    protected Base() { }
    protected Base(int x) { f = x; }
    protected void m() { System.out.println("m f=" + f); }
    protected int mi(int a, long b) { return a + (int) b + f; }
    protected static void sm() { System.out.println("sm sf=" + sf); }
    protected static int smi(int a) { return a * 2; }
}
