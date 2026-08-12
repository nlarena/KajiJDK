// A7: default methods (JSR 335). DefSub redefine el default f() de DefI (shadowing por
// especificidad); DefA hereda ambos defaults; DefB los pisa con un override de clase (la clase
// siempre gana). Se invoca por receptor tipado interface (invokeinterface) Y por receptor tipado
// clase (invokevirtual), más un static de interface. 2 + 10 + 3 + 100 = 115.
interface DefI {
    default int f() { return 1; }
    default int g() { return 10; }
    static int s() { return 100; }
}

interface DefSub extends DefI {
    default int f() { return 2; } // más específico: shadowa a DefI.f
}

class DefA implements DefSub {}

class DefB implements DefSub {
    public int f() { return 3; } // override de clase: gana a ambos defaults
}

public class DefProbe {
    static int run() {
        DefSub a = new DefA();  // invokeinterface
        DefA a2 = new DefA();   // invokevirtual (receptor tipado por la clase)
        DefB b = new DefB();
        return a.f() + a2.g() + b.f() + DefI.s(); // 2 + 10 + 3 + 100 = 115
    }
}
