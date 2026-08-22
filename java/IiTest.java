// A7 #13 (JVMS 5.5): the fine-grained rule for *interface* initialization.
//
//  - Initializing a class initializes its superclass and those of its superinterfaces
//    (direct or indirect) that declare a **default** method -- and only those.
//  - Initializing an interface does NOT initialize its superinterfaces, not even the
//    ones declaring default methods.
//  - An interface is initialized by the first active use of its own static fields.
//
// Every auxiliary type is prefixed `Ii` because all .class files share java/.
public class IiTest {

    public static int run() {
        int score = 0;
        int sink = 0;

        // (a) class -> superinterface WITH a default method: must be initialized.
        sink += IiAImpl.touch;
        if (IiCnt.a == 1) {
            score += 8;
        }

        // (b) class -> superinterface WITHOUT default methods (abstract only): must NOT run.
        sink += IiBImpl.touch;
        if (IiCnt.b == 0) {
            score += 8;
        }

        // (c) interface -> superinterface: initializing IiSub must NOT initialize IiSuper,
        //     even though IiSuper declares a default method. IiSub's own <clinit> does run.
        sink += IiSub.W;
        if (IiCnt.c == 0) {
            score += 8;
        }
        if (IiCnt.d == 1) {
            score += 4;
        }

        // (d) class -> *indirect* superinterface with a default (IiDImpl -> IiMid -> IiGrand).
        sink += IiDImpl.touch;
        if (IiCnt.e == 1) {
            score += 4;
        }

        // Positive controls: the skipped <clinit>s are not broken, merely deferred --
        // reading the interface's own static field initializes it right now.
        sink += IiB.Y;
        if (IiCnt.b == 1) {
            score += 5;
        }
        sink += IiSuper.Z;
        if (IiCnt.c == 1) {
            score += 5;
        }

        return score + sink; // every bump() returns 0, so sink is a no-op guard
    }
}

/// Observable side effect of each <clinit>: a counter in a separate class.
class IiCnt {
    static int a;
    static int b;
    static int c;
    static int d;
    static int e;

    static int bumpA() { a = a + 1; return 0; }
    static int bumpB() { b = b + 1; return 0; }
    static int bumpC() { c = c + 1; return 0; }
    static int bumpD() { d = d + 1; return 0; }
    static int bumpE() { e = e + 1; return 0; }
}

// (a) <clinit> + a default method.
interface IiA {
    int X = IiCnt.bumpA(); // not a compile-time constant -> forces a real <clinit>
    int f();
    default int g() { return 3; }
}

class IiAImpl implements IiA {
    static int touch;
    public int f() { return 0; }
}

// (b) <clinit> but only abstract methods -- no default.
interface IiB {
    int Y = IiCnt.bumpB();
    int f();
}

class IiBImpl implements IiB {
    static int touch;
    public int f() { return 0; }
}

// (c) a superinterface with a default, extended by a sub-interface with its own <clinit>.
interface IiSuper {
    int Z = IiCnt.bumpC();
    default int h() { return 4; }
}

interface IiSub extends IiSuper {
    int W = IiCnt.bumpD();
}

// (d) the default-declaring interface is an *indirect* superinterface of the class.
interface IiGrand {
    int V = IiCnt.bumpE();
    default int k() { return 5; }
}

interface IiMid extends IiGrand {
}

class IiDImpl implements IiMid {
    static int touch;
}
