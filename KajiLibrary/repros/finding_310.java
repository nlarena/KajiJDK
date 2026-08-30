// Repro del finding #310: un primitivo pasado a un `Object...` no se boxeaba.
//
//     String.format("%d", 42)
//
// El `42` entraba **crudo** al `Object[]` que arma el varargs, y la VM cortaba con
// "expected a reference, found Int(42)". Es una de las lineas mas comunes que tiene Java.
//
// La causa estaba escrita en un comentario, y decia lo contrario de lo que hacia falta:
//
//     // Con varargs los argumentos de cola aun no se empaquetaron en un array (eso lo hace el
//     // desugar, despues): se convierten solo los del prefijo fijo.
//
// De "el array se arma despues" se concluia "no hay nada que convertir", y no: **el elemento** ya es
// un target valido. Los de cola se convierten ahora contra el tipo elemento del array, y eso cubre
// las cuatro formas de una sola vez -- un `int` contra `Object` boxea, un `Integer` contra `int...`
// desboxea, un `String` contra `Object` no hace nada, y el paso directo del array tampoco.
//
// Da 212112, igual que `java` real.
public class finding_310 {

    static int cuantos(Object... xs) {
        return xs.length;
    }

    static String tipo(Object... xs) {
        return xs[0].getClass().getName();
    }

    static int soloInt(int... xs) {
        return xs.length;
    }

    public static int run() {
        int r = 0;
        r = r * 10 + cuantos("x", 42);                    // 2 -- mezcla referencia y primitivo
        r = r * 10 + cuantos(42);                          // 1 -- un primitivo solo
        r = r * 10 + cuantos("x", "y");                    // 2 -- de control: dos referencias
        r = r * 10 + ("java.lang.Integer".equals(tipo(42)) ? 1 : 0);   // se boxea a Integer
        r = r * 10 + soloInt(Integer.valueOf(7));          // 1 -- el camino inverso: desboxea
        r = r * 10 + String.format("%d", 42).length();     // 2 -- el que lo destapo
        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
