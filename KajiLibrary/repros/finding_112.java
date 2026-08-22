// Finding #112 — a `static final` primitive constant is neither folded into its use sites
// nor initialized by a `<clinit>`: the value is written only to the field's ConstantValue
// attribute, and uses read it with `getstatic`. If the VM does not apply ConstantValue at
// class initialization, every such constant reads back as 0 (or null).
// Real javac inlines the constant (JLS §13.1), so it never depends on the VM for this.
// Dropping `final` forces a real `<clinit>` and works. Found in FutureTask, whose four
// state constants all read 0 — so a completed task still looked NEW and get() hung forever.
public class finding_112 {
    private static final int A = 1;    // ConstantValue: int 1, no <clinit>, uses getstatic
    private static int B = 2;          // real <clinit> with putstatic — works
    static int sum() {
        return A + B;                  // A reads 0 at run time unless the VM honours ConstantValue
    }
}
