class KH { static final int TRES = 3; }
public class IoDbgH extends KH {
    private int enCampo = TRES;
    public static int run() { return new IoDbgH().enCampo; }
}
