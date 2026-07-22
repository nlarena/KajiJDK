public class ObjConcat {
    public static int run() {
        Object o = new Thing();
        String s = "x" + o;
        return s.equals("xTHING") ? 42 : -1;
    }
}
class Thing { public String toString() { return "THING"; } }
