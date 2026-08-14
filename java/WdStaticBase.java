// Superclass for `WdStatic`: it declares a static that the subclass's `getstatic` names but does
// not declare, so resolving it means walking the superclass chain — the path
// `class_operations::static_int_address` has to reproduce read-only.
public class WdStaticBase {
    static int INHERITED = 5;
}
