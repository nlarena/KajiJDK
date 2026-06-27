// Exercises the verifier's uninitialized-object rules (JVMS §4.10.2.4): a constructor
// where `this` starts UninitializedThis, becomes initialized by the implicit
// `super()` (invokespecial Object.<init>), and is only then used as a real reference
// (putfield this.a / this.b). It also runs, so the model stays faithful end to end.
public class Init {
    int a;
    int b;

    Init(int x) {
        a = x;          // putfield on `this` — legal only after super() ran
        b = x + 1;
    }

    static int run() {
        Init i = new Init(10);
        return i.a + i.b; // 10 + 11 = 21
    }
}
