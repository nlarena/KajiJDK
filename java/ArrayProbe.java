// Probe de regresion de #262: los miembros que un array hereda de Object.
//
// JLS 10.7: los miembros de un array son los de Object (mas `length` y un `clone` covariante).
// Las clases array son sinteticas -sin class file- asi que su vtable se construia vacia: todo
// `array.hashCode()` erraba su slot y moria como NoSuchMethodError. El sitio tenia el slot bien
// (javac emite owner `java/lang/Object`); lo que faltaba era la tabla del receptor.
//
// Y su mirror se alocaba SIN escribir el header, o sea un objeto sin clase. Que `getClass()`
// devolviera algo no-nulo tapaba el hueco: devolvia un objeto a medio construir, y cualquier cosa
// que leyera su clase -- un `instanceof`, un `checkcast`, `getName()` -- terminaba en "could not
// resolve the object's class from its header".
//
// El `instanceof` sobre el mirror es la asercion que cubre esa segunda mitad, y se elige a
// proposito por sobre `getClass().getName()`: el `instanceof` lo resuelve la VM leyendo el header,
// sin ejecutar una linea de `java.lang.Class`, asi que mide LO QUE ESTE PROBE MIDE y no el estado
// de la biblioteca.
//
// Un bit por propiedad, para que una falla parcial se nombre sola. Las cuatro -> 15.
public class ArrayProbe {

    static int run() {
        int score = 0;
        char[] c = new char[2];
        if (c.hashCode() != 0) { score += 1; }
        Object k = c.getClass();
        if (k != null) { score += 2; }
        if (k instanceof Class) { score += 4; }
        if (c.equals(c)) { score += 8; }
        return score;
    }
}
