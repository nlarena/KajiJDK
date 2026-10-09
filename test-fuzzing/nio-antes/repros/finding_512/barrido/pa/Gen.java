package pa;
public class Gen<T> {
    protected T val;
    protected byte b;
    protected int[] arr = {1, 2, 3};
    protected <U> U id(U u) { return u; }
    protected T get() { return val; }
    protected int va(String p, int... xs) { int s = 0; for (int x : xs) s += x; return s + p.length(); }
    protected String conSecreto(Secreto s) { return "secreto:" + s.n; }
    protected static String sva(Object... os) { return "sva" + os.length; }
}
