interface KG { int ONE = 1; int TWO = 2; }
public class IoDbgG implements KG {
    public static int run() { return inMethod(); }
    static int inMethod() { return TWO; }
}
