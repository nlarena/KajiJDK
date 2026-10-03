// La forma `case X:` de un switch de patrones (Hito B7, segunda parte).
//
// El desugar la normaliza a la forma de flecha, y lo que esa conversion tiene que resolver son las
// dos cosas que la forma de dos puntos agrega: el `break` del grupo --que apunta al switch y en la
// cadena de `if (instanceof)` se quedaria sin destino-- y la CAIDA de un grupo al siguiente, que
// Java solo permite hacia el `default` (§14.11.1). `breakDeBucle` es la que importa de las cinco:
// custodia que un `break` de un bucle de adentro del grupo NO se reoriente.
public class ColonTest {
    // `break` explicito del grupo: sale del switch.
    static int conBreak(Object o) {
        int acc = 0;
        switch (o) {
            case Integer i: acc = i; break;
            case String s: acc = s.length(); break;
            default: acc = -1;
        }
        return acc;
    }

    // Caida de un grupo al `default`: corren los dos.
    static int caeAlDefault(Object o) {
        int acc = 0;
        switch (o) {
            case Integer i: acc = 100; break;
            case String s: acc = 7;
            default: acc = acc + 10;
        }
        return acc;
    }

    // Un `break` de un bucle ADENTRO del grupo tiene que quedarse con el bucle.
    static int breakDeBucle(Object o) {
        int acc = 0;
        switch (o) {
            case Integer i:
                for (int k = 0; k < 10; k++) {
                    if (k == 3) break;   // corta el for, NO el switch
                    acc = acc + k;
                }
                acc = acc + 1000;        // se ejecuta: el break era del for
                break;
            default: acc = -1;
        }
        return acc;
    }

    // Un grupo que retorna directo.
    static int retorna(Object o) {
        switch (o) {
            case Integer i: return i * 2;
            case String s: return s.length();
            default: return -1;
        }
    }

    // Guarda con dos puntos.
    static int conGuarda(Object o) {
        int acc = 0;
        switch (o) {
            case Integer i when i > 10: acc = 1; break;
            case Integer i: acc = 2; break;
            default: acc = 3;
        }
        return acc;
    }

    public static void main(String[] a) {
        System.out.println(conBreak(5));
        System.out.println(conBreak("hola"));
        System.out.println(conBreak(1.5));
        System.out.println(caeAlDefault(3));
        System.out.println(caeAlDefault("x"));
        System.out.println(caeAlDefault(2.5));
        System.out.println(breakDeBucle(0));
        System.out.println(breakDeBucle("x"));
        System.out.println(retorna(21));
        System.out.println(retorna("abcd"));
        System.out.println(retorna(2.5));
        System.out.println(conGuarda(50));
        System.out.println(conGuarda(5));
        System.out.println(conGuarda("x"));
    }
}
