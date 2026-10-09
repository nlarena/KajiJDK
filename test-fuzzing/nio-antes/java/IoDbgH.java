class KH { static final int THREE = 3; }
public class IoDbgH extends KH {
    private int inField = THREE;
    public static int run() { return new IoDbgH().inField; }
}
