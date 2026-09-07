package javax.swing;

/**
 * Los tamanos de una fila de cosas puestas una atras de otra, y donde empieza cada una.
 *
 * <h2>Para que sirve</h2>
 *
 * <p>Una tabla con mil filas de alturas distintas necesita responder dos preguntas todo el tiempo:
 * en que pixel empieza la fila 700, y a que fila corresponde el pixel 4823. Sumar de a una es
 * demasiado lento cuando pasa en cada repintado, y guardar las posiciones ya sumadas obliga a
 * recalcular la mitad de la tabla cada vez que una fila cambia de alto.
 *
 * <p>Esta clase es la respuesta a las dos preguntas con la misma estructura. Guarda los
 * <em>tamanos</em> -- que es lo que cambia -- y deriva las posiciones.
 *
 * <h2>Las dos preguntas no son simetricas</h2>
 *
 * <p>{@link #getPosition} de un indice fuera de rango devuelve el total, y {@link #getIndex} de una
 * posicion pasada del final devuelve la cantidad de entradas. Las dos son la misma respuesta dicha
 * de dos maneras: "esta despues de todo lo que hay".
 */
public class SizeSequence {

    private static final int[] VACIO = new int[0];

    private int[] tamanos;

    /** Sin entradas. */
    public SizeSequence() {
        tamanos = VACIO;
    }

    /** Esa cantidad de entradas, todas de tamano cero. */
    public SizeSequence(int numEntries) {
        this(numEntries, 0);
    }

    /** Esa cantidad de entradas, todas de ese tamano. */
    public SizeSequence(int numEntries, int value) {
        this();
        insertEntries(0, numEntries, value);
    }

    /** Con esos tamanos. */
    public SizeSequence(int[] sizes) {
        this();
        setSizes(sizes);
    }

    /** Le pone ese tamano a las primeras {@code length} entradas. */
    void setSizes(int length, int size) {
        int[] nuevos = new int[length];
        for (int i = 0; i < length; i++) {
            nuevos[i] = size;
        }
        setSizes(nuevos);
    }

    /** Reemplaza todos los tamanos; se guarda una copia. */
    public void setSizes(int[] sizes) {
        int[] copia = new int[sizes.length];
        System.arraycopy(sizes, 0, copia, 0, sizes.length);
        tamanos = copia;
    }

    /** Una copia de los tamanos. */
    public int[] getSizes() {
        int[] copia = new int[tamanos.length];
        System.arraycopy(tamanos, 0, copia, 0, tamanos.length);
        return copia;
    }

    /**
     * Donde empieza esa entrada.
     *
     * <p>Un indice pasado del final da el total; ver la nota de la clase.
     */
    public int getPosition(int index) {
        int suma = 0;
        int tope = index;
        if (tope > tamanos.length) {
            tope = tamanos.length;
        }
        for (int i = 0; i < tope; i++) {
            suma = suma + tamanos[i];
        }
        return suma;
    }

    /**
     * A que entrada corresponde esa posicion.
     *
     * <p>Una posicion pasada del final da la cantidad de entradas; ver la nota de la clase. Las
     * entradas de tamano cero no ocupan lugar y por lo tanto no se pueden alcanzar: la posicion cae
     * en la primera que si ocupe algo.
     */
    public int getIndex(int position) {
        int suma = 0;
        for (int i = 0; i < tamanos.length; i++) {
            suma = suma + tamanos[i];
            if (position < suma) {
                return i;
            }
        }
        return tamanos.length;
    }

    /** El tamano de esa entrada; cero si el indice esta fuera de rango. */
    public int getSize(int index) {
        if (index < 0 || index >= tamanos.length) {
            return 0;
        }
        return tamanos[index];
    }

    /**
     * Le cambia el tamano a una entrada.
     *
     * <p>Un indice fuera de rango no hace nada. <strong>Con un indice negativo el JDK difiere</strong>:
     * su implementacion guarda un arbol de sumas parciales en vez de los tamanos, y un indice
     * negativo termina sumandole el tamano pedido a la entrada cero -- pedirle {@code setSize(-1, 100)}
     * a una secuencia que empieza en 5 la deja en 105. Es un efecto de su estructura interna, no una
     * regla; aca no se copia, y queda dicho porque es la unica diferencia observable entre las dos
     * implementaciones.
     */
    public void setSize(int index, int size) {
        if (index < 0 || index >= tamanos.length) {
            return;
        }
        tamanos[index] = size;
    }

    /**
     * Mete {@code length} entradas de ese tamano a partir de {@code start}.
     *
     * <p>Las que estaban de {@code start} en adelante se corren; es una insercion, no un
     * reemplazo.
     */
    public void insertEntries(int start, int length, int value) {
        int[] sizes = getSizes();
        int end = start + length;
        int newLength = sizes.length + length;
        int[] newSizes = new int[newLength];
        for (int i = 0; i < start; i++) {
            newSizes[i] = sizes[i];
        }
        for (int i = start; i < end; i++) {
            newSizes[i] = value;
        }
        for (int i = end; i < newLength; i++) {
            newSizes[i] = sizes[i - length];
        }
        setSizes(newSizes);
    }

    /** Saca {@code length} entradas a partir de {@code start}. */
    public void removeEntries(int start, int length) {
        int[] sizes = getSizes();
        int newLength = sizes.length - length;
        int[] newSizes = new int[newLength];
        for (int i = 0; i < start; i++) {
            newSizes[i] = sizes[i];
        }
        for (int i = start; i < newLength; i++) {
            newSizes[i] = sizes[i + length];
        }
        setSizes(newSizes);
    }
}
