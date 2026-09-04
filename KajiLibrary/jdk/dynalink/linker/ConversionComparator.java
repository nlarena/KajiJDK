package jdk.dynalink.linker;

/**
 * Desempata entre dos conversiones posibles cuando ninguna es obviamente mejor.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Un lenguaje dinamico invoca `f(x)` y la clase destino tiene `f(int)` y `f(String)`. Si `x`
 * es un `double`, las dos sobrecargas son alcanzables: el lenguaje sabe convertir un numero a
 * `int` y tambien sabe convertirlo a `String`. Java no tiene una regla para elegir, porque el
 * conjunto de conversiones no es el de Java — lo aporta el enlazador del lenguaje.
 *
 * <p>Esta interfaz es donde ese enlazador dice cual prefiere. Un {@link GuardingDynamicLinker}
 * que ademas la implementa participa del desempate; uno que no la implementa simplemente no
 * opina.
 *
 * <h2>Por que puede no saber</h2>
 *
 * <p>Por {@link Comparison#INDETERMINATE}, que no es un error sino la respuesta honesta de quien
 * no tiene preferencia entre esos dos destinos. Si todos los comparadores contestan eso, la
 * ambiguedad queda sin resolver y el que invoca decide con sus propias reglas.
 *
 * @since 9
 */
public interface ConversionComparator {

    /** La preferencia entre dos tipos destino. */
    enum Comparison {
        /** Sin preferencia: este comparador no distingue entre los dos destinos. */
        INDETERMINATE,
        /** El primer destino es mejor. */
        TYPE_1_BETTER,
        /** El segundo destino es mejor. */
        TYPE_2_BETTER
    }

    /**
     * Cual de los dos destinos conviene para un valor de {@code sourceType}.
     *
     * @param sourceType el tipo del valor que hay que convertir
     * @param targetType1 el primer destino candidato
     * @param targetType2 el segundo destino candidato
     * @return la preferencia, o {@link Comparison#INDETERMINATE} si no la hay
     */
    Comparison compareConversion(Class<?> sourceType, Class<?> targetType1, Class<?> targetType2);
}
