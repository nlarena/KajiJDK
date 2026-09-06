package java.time.format;

// La gramatica de una plantilla de formato: que simbolos admite, en que orden y cuantas veces.
//
// ===============================================================================================
// POR QUE HAY DOS FORMAS DE FALLAR
// ===============================================================================================
//
// Una plantilla mal escrita --`abc`, `dE` al reves, `ddd`-- es un error del programa y da
// `IllegalArgumentException`. Una plantilla bien escrita que ese idioma no sabe formatear --`w`,
// `zzz`-- no es un error del programa: da `DateTimeException`, y podria resolverse en otro idioma.
// La distincion es del JDK y hay que respetarla, porque un programa que atrapa una de las dos no
// espera la otra.
//
// Los limites de cuenta salen de medir el JDK 25 simbolo por simbolo, no de leer la documentacion:
// `y` y `w` no tienen tope --`yyyyyyyyyyyyyy` es una plantilla valida que no resuelve-- y `K` y `k`
// no se admiten nunca, que es lo que menos se espera de los dos.
final class Plantilla {

    // Los simbolos en el orden en que tienen que aparecer, y cuantas veces como mucho. Un cero
    // significa que no hay tope. Los que no estan --`K`, `k`, `a`, `L`, `c` y todos los demas-- no
    // se admiten.
    private static final String ORDEN = "GyQMwEdBhHjmsvz";
    private static final int[] TOPE = {5, 0, 5, 5, 0, 5, 2, 5, 2, 2, 2, 2, 2, 4, 4};

    // Donde empieza la mitad de hora. Los ocho ultimos simbolos del orden --de `B` en adelante--
    // son de hora; `B` es el periodo del dia, que es hora aunque no lleve numeros.
    private static final int PRIMERO_DE_HORA = 7;

    private Plantilla() {
    }

    /**
     * Comprueba que la plantilla este bien escrita.
     *
     * @param plantilla la plantilla
     * @throws IllegalArgumentException si no lo esta
     */
    static void comprobar(String plantilla) {
        int esperado = 0;
        int i = 0;
        while (i < plantilla.length()) {
            char c = plantilla.charAt(i);
            int cual = ORDEN.indexOf(c);
            if (cual < 0 || cual < esperado) {
                throw new IllegalArgumentException(
                        "Requested template \"" + plantilla + "\" is invalid.");
            }
            int cuantos = 0;
            while (i < plantilla.length() && plantilla.charAt(i) == c) {
                cuantos = cuantos + 1;
                i = i + 1;
            }
            if (TOPE[cual] > 0 && cuantos > TOPE[cual]) {
                throw new IllegalArgumentException(
                        "Requested template \"" + plantilla + "\" is invalid.");
            }
            esperado = cual + 1;
        }
    }

    /**
     * Donde termina la mitad de fecha y empieza la de hora.
     *
     * <p>El corte es unico porque el orden de los simbolos esta fijado: todo lo de fecha va antes
     * que todo lo de hora.
     *
     * @param plantilla la plantilla, ya comprobada
     * @return la posicion, que puede ser cero o el largo entero
     */
    static int corte(String plantilla) {
        for (int i = 0; i < plantilla.length(); i++) {
            if (ORDEN.indexOf(plantilla.charAt(i)) >= PRIMERO_DE_HORA) {
                return i;
            }
        }
        return plantilla.length();
    }
}
