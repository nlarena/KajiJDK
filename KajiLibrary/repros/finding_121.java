// Finding #121 — `super(...)` fails to resolve when the target constructor's parameter is a
// PARAMETERIZED type mentioning the superclass's type variable.
//
//     abstract class G1<E>   { G1(Class<E> t) {} }
//     final class G1Sub<E> extends G1<E> { G1Sub(Class<E> t) { super(t); } }
//
// gives: "el generador de bytecode todavia no soporta un super(...)/this(...) que no resolvio
// a ningun constructor". It is a resolution failure, not a codegen gap: the constructor exists
// and the argument type is exactly right.
//
// It works with `int`, `Object`, a bare `E`, or `Class<?>` on a NON-generic superclass, and it
// fails the same way with a user-defined `Box<E>` — so the trigger is a parameterized parameter
// whose argument is the superclass's own type variable. Fails within one file and across files.
//
// This is exactly the shape of the JDK's `EnumSet(Class<E>, Enum[])`, which is why KajiLibrary's
// EnumSet had to be given a no-argument constructor with the subclass assigning the field
// directly instead.
public class finding_121 {
}

abstract class G121<E> {
    G121(Box121<E> t) {
    }
}

final class G121Sub<E> extends G121<E> {
    // BROKEN: super(t) does not resolve, though G121(Box121<E>) is right there.
    G121Sub(Box121<E> t) {
        super(t);
    }
}

class Box121<E> {
}
