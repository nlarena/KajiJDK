// Repro de #231 (= #125) - `super.metodo()` no lo soportaba el generador de bytecode.
//
// Eran dos cosas, no una:
//   1. `super` como RECEPTOR caia en `unsupported("`super`")`. Es el mismo `this`: lo que cambia
//      no es el objeto sino el despacho.
//   2. El despacho. `super.m()` NO es virtual (JLS 15.12.4.4): saltea el override de ESTA clase.
//      Va por `invokespecial`, y el dueño del methodref es la SUPERCLASE DIRECTA -- no la clase
//      que declara el metodo. Con `C extends B extends A` y `f` declarado en `A`, el javac del
//      JDK 25 emite `invokespecial B.f`, no `A.f`; nosotros ahora emitimos lo mismo.
//
//   bin\javac.exe --emit KajiLibrary\repros\finding_231.java
//   javap -c -p KajiLibrary\repros\finding_231$C.class
//        -> 0: aload_0   1: invokespecial finding_231$B.f:()I
// OJO -- `run` todavia NO devuelve 0: revienta con `operand stack underflow`. Eso ya NO es del
// compilador; el bytecode de las cuatro clases es identico al del javac del JDK 25. Es #265, de
// la VM: `resolve_method` busca el metodo SOLO en la clase nombrada, y `invokespecial B.f`
// nombra a `B`, que no declara `f` -- lo hereda de `A`. JVMS 5.4.3.3 manda buscar en la clase,
// despues en sus superclases y despues en sus superinterfaces. Por eso este repro tiene la
// clase intermedia `B` vacia: es justo el caso que lo destapa.
public class finding_231 {

    static class A {
        int f() { return 1; }
        int g() { return 9; }
    }

    /* Intermedia sin declarar nada: es la que prueba que el dueño del methodref es la superclase
       DIRECTA y no la declarante. */
    static class B extends A { }

    static class C extends B {
        /* Override que llama al de arriba: sin `invokespecial` seria recursion infinita. */
        int f() { return super.f() + 1; }
        /* Y un metodo heredado de mas lejos, que nadie sobreescribe. */
        int h() { return super.g(); }
    }

    public static int run() {
        C c = new C();
        if (c.f() != 2) { return 1; }
        if (c.h() != 9) { return 2; }
        /* Y el despacho virtual normal sigue viendo el override. */
        A comoA = c;
        if (comoA.f() != 2) { return 3; }
        return 0;
    }
}
