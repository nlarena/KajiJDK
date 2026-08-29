// Probe de regresion de #265: la resolucion de metodos tiene que subir por la jerarquia.
//
// JVMS 5.4.3.3 manda buscar en la clase, DESPUES en sus superclases y DESPUES en sus
// superinterfaces. `resolve_method` hacia un `find` sobre la clase nombrada y devolvia `None`:
// el sitio quedaba NoTarget, no se empujaba nada, y el llamador moria con "operand stack
// underflow" -- lejos del origen y sin decir que fue.
//
// No es un caso raro: `super.m()` emite `invokespecial <superclase directa>.m`, que es lo que
// emite el javac real, y la superclase directa no tiene por que declarar el metodo.
//
// Un bit por propiedad, para que una falla parcial se nombre sola: heredado por superclase (1),
// `default` de una superinterfaz (2), `static` heredado (4). Las tres -> 7.
public class SuperProbe extends SuperProbe_B implements SuperProbe_I {

    int f() { return super.f(); }   // invokespecial SuperProbe_B.f, declarado en SuperProbe_A

    static int run() {
        int score = 0;
        SuperProbe p = new SuperProbe();
        if (p.f() == 1) { score += 1; }
        if (p.g() == 2) { score += 2; }
        if (SuperProbe_B.h() == 4) { score += 4; }
        return score;
    }
}
