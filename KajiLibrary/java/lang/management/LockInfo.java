package java.lang.management;

import javax.management.openmbean.CompositeData;

/**
 * KajiLibrary's java.lang.management.LockInfo -- que candado esta esperando un hilo.
 *
 * <p>Dos datos: la clase del objeto que hace de candado, y su {@code System.identityHashCode}. No
 * guarda el objeto: si lo hiciera, un volcado de hilos impediria que el recolector se llevara todo lo
 * que alguien estaba esperando.
 *
 * <p>El codigo de identidad es lo que permite cruzar la informacion: dos hilos esperando el
 * <b>mismo</b> candado muestran el mismo par de clase y codigo. Es asi como se detecta un ciclo de
 * espera a mano.
 *
 * <p>No es un identificador perfecto -- dos objetos pueden compartir codigo de identidad-- pero para
 * un volcado alcanza.
 *
 * <p>Cubre tanto los monitores como los candados de {@code java.util.concurrent};
 * {@link MonitorInfo} es la subclase que agrega lo que solo tienen los monitores.
 */
public class LockInfo {

    /** La clase del candado. */
    private final String className;

    /** Su codigo de identidad. */
    private final int identityHashCode;

    /**
     * @throws NullPointerException si el nombre de clase es null
     */
    public LockInfo(String className, int identityHashCode) {
        if (className == null) {
            throw new NullPointerException("Parameter className cannot be null");
        }
        this.className = className;
        this.identityHashCode = identityHashCode;
    }

    /** La clase del candado. */
    public String getClassName() {
        return this.className;
    }

    /** Su codigo de identidad. */
    public int getIdentityHashCode() {
        return this.identityHashCode;
    }

    /** La clase, arroba, y el codigo en hexadecimal. Igual que {@code Object.toString}. */
    @Override
    public String toString() {
        return this.className + '@' + Integer.toHexString(this.identityHashCode);
    }

    /**
     * Lo mismo, leido de un {@link CompositeData}.
     *
     * @return el objeto, o null si el dato es null
     * @throws IllegalArgumentException si el dato no describe un {@code LockInfo}
     */
    public static LockInfo from(CompositeData cd) {
        if (cd == null) {
            return null;
        }
        return new LockInfo(CompositeItems.string(cd, "className", "LockInfo"),
                            CompositeItems.integer(cd, "identityHashCode", "LockInfo"));
    }
}
