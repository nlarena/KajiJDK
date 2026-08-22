// A second class holding statics, so `WdStatic`'s compiled `getstatic` has to resolve a mirror
// that is not its own — and, for `TEXT`, a static that is not an `int` and must therefore make the
// reading method ineligible rather than be read as one.
public class WdStaticOther {
    static int FAR = 13;
    static long WIDE = 99L;
    static String TEXT = "no";
}
