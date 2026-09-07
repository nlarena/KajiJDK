package jdk.jfr;

import java.util.concurrent.atomic.AtomicLong;

/**
 * El registro de identificadores numericos de tipo. No es API.
 *
 * <p>JFR le asigna un numero a cada tipo que aparece en una grabacion —eventos, anotaciones, tipos
 * de campo— y el archivo se refiere a ellos por ese numero en vez de por su nombre, que es lo que
 * hace que un archivo con millones de eventos no repita la cadena {@code "jdk.ObjectAllocation"} un
 * millon de veces.
 *
 * <p>El contrato de {@code getTypeId} es que el numero identifica al tipo <strong>dentro de esta
 * ejecucion de la VM</strong>; no es estable entre ejecuciones ni tiene significado fuera. Eso es
 * exactamente lo que un contador cumple, asi que estos identificadores son reales y no un relleno:
 * dos tipos distintos dan numeros distintos y el mismo tipo da siempre el mismo.
 *
 * <p>Arranca en 1 a proposito: el 0 queda libre para significar "sin asignar".
 */
final class Tipos {

    private static final AtomicLong PROXIMO = new AtomicLong(1);

    /**
     * Los identificadores viven en un {@link ClassValue} y no en un mapa para que se vayan con la
     * clase cuando se la descarga. Un mapa comun mantendria viva cualquier clase que hubiera
     * aparecido alguna vez en un evento.
     */
    private static final Registro REGISTRO = new Registro();

    private static final class Registro extends ClassValue<Long> {
        protected Long computeValue(final Class<?> type) {
            return Long.valueOf(PROXIMO.getAndIncrement());
        }
    }

    private Tipos() {
    }

    /**
     * El identificador de ese tipo, siempre el mismo.
     *
     * @param tipo el tipo, o {@code null}
     * @return el identificador, o {@code 0} si el tipo es {@code null}
     */
    static long id(final Class<?> tipo) {
        return tipo == null ? 0L : REGISTRO.get(tipo).longValue();
    }

    /** Los identificadores de los tipos que no tienen clase Java, por nombre. */
    private static final java.util.Map<String, Long> POR_NOMBRE =
            new java.util.concurrent.ConcurrentHashMap<String, Long>();

    /**
     * El identificador de un tipo que <strong>no tiene clase Java</strong>.
     *
     * <p>Existen: el campo {@code stackTrace} de todo evento es de tipo
     * {@code jdk.types.StackTrace}, que es un tipo del formato de la grabacion y no una clase. Sin
     * esto no habria como darle un identificador.
     *
     * @param nombre el nombre del tipo
     * @return el identificador, siempre el mismo para el mismo nombre
     */
    static long idDeNombre(final String nombre) {
        Long v = POR_NOMBRE.get(nombre);
        if (v == null) {
            // putIfAbsent y no put: dos hilos que pidan el mismo nombre a la vez tienen que
            // recibir el mismo numero, no uno cada uno.
            final Long nuevo = Long.valueOf(PROXIMO.getAndIncrement());
            v = POR_NOMBRE.putIfAbsent(nombre, nuevo);
            if (v == null) {
                v = nuevo;
            }
        }
        return v.longValue();
    }

    /**
     * El nombre con el que un tipo aparece en la grabacion.
     *
     * <p>Un arreglo se nombra por su componente: JFR marca la condicion de arreglo aparte, en
     * {@link ValueDescriptor#isArray}, en vez de meterla en el nombre.
     *
     * @param tipo el tipo
     * @return el nombre
     */
    static String nombre(final Class<?> tipo) {
        Class<?> t = tipo;
        while (t.isArray()) {
            t = t.getComponentType();
        }
        return t.getName();
    }
}
