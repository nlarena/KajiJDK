// Cast de interseccion (§15.16): `(A & B) e`.
//
// Se baja a una CADENA de casts comunes, y el orden sale medido contra el javac real: para
// `(P & Q & R) o` emite `checkcast Q; checkcast R; checkcast P` --las cotas posteriores en orden de
// fuente, y la primera al final--, o sea `(P)((R)((Q) o))`. El cuerpo de los metodos de `C1`/`C5`
// salio IDENTICO al del real, instruccion por instruccion.
//
// `falla` es la que importa: el cast tiene que lanzar ClassCastException cuando el objeto no
// implementa TODAS las cotas, no solo la primera.
interface Alfa { int alfa(); }
interface Beta { int beta(); }
interface Gama { int gama(); }

class Todas implements Alfa, Beta, Gama {
    public int alfa() { return 1; }
    public int beta() { return 2; }
    public int gama() { return 3; }
}
class SoloAlfa implements Alfa { public int alfa() { return 9; } }

public class InterTest {
    // Dos cotas: el tipo estatico es la primera.
    static int dos(Object o) {
        Alfa x = (Alfa & Beta) o;
        return x.alfa();
    }
    // Tres cotas.
    static int tres(Object o) {
        Alfa x = (Alfa & Beta & Gama) o;
        return x.alfa() * 100;
    }
    // Que FALLE si no implementa todas, y con ClassCastException.
    static String falla(Object o) {
        try { Alfa x = (Alfa & Beta) o; return "ok:" + x.alfa(); }
        catch (ClassCastException e) { return "cce"; }
    }

    public static void main(String[] z) {
        System.out.println(dos(new Todas()));
        System.out.println(tres(new Todas()));
        System.out.println(falla(new Todas()));
        System.out.println(falla(new SoloAlfa()));
        System.out.println(falla("x"));
    }
}
