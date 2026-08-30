// Repro del finding #313: llamar a un metodo de `Object` sobre `this`, dentro de un `default` de
// interfaz, no resolvia.
//
//   interface Base {
//       default String nombre() { return this.toString(); }
//   }
//
//   error: no se encuentra el simbolo: toString
//     ubicacion: clase Base
//
// Y es codigo legal: una interfaz **declara implicitamente** un metodo abstracto por cada metodo
// publico de `Object` (JLS 9.2), justamente para que esto se pueda escribir. `javac` real lo compila.
//
// La causa: la busqueda de miembros sube por `super_class` y por las superinterfaces, y una interfaz
// **no tiene superclase** -- asi que a `Object` no se llegaba nunca. El arreglo agrega `Object` al
// cierre cuando el tipo es una interfaz sin superclase, que es exactamente lo que dice la regla.
//
// Es la misma familia que el #292, del otro lado: aquel era la **medicion** ignorando los miembros
// de `Object`; este es la **resolucion** no llegando a ellos.
//
// Da 431, igual que `java` real.
interface Base313 {

    // Los tres que un `default` puede querer: el que se sobreescribe, el que no, y equals.
    default int largoDelNombre() {
        return this.toString().length();
    }

    default boolean esElMismo(Object otro) {
        return this.equals(otro);
    }

    default int claseDelNombre() {
        return this.getClass().getSimpleName().length();
    }
}

public class finding_313 implements Base313 {

    public String toString() {
        return "hola";
    }

    public static int run() {
        finding_313 a = new finding_313();
        finding_313 b = new finding_313();
        int r = 0;
        r = r * 10 + a.largoDelNombre();                  // 4 -- "hola"
        r = r * 10 + (a.esElMismo(a) ? 1 : 0);            // 1 -- equals consigo mismo
        r = r * 10 + (a.esElMismo(b) ? 1 : 0);            // 0 -- otro objeto, sin equals propio
        r = r * 10 + a.claseDelNombre() / 4;              // "finding_313" son 11 -> 2
        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
