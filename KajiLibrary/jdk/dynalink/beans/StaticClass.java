package jdk.dynalink.beans;

import java.io.Serializable;
import java.util.Objects;

/**
 * El objeto que representa a una clase como <strong>portadora de sus miembros estaticos</strong>.
 *
 * <h2>Por que no alcanza con {@code Class}</h2>
 *
 * <p>Porque un {@code Class} ya significa otra cosa: es una instancia de {@code java.lang.Class},
 * con sus propios metodos. Si un lenguaje dinamico escribiera {@code String.valueOf(1)} y el objeto
 * de la izquierda fuera {@code String.class}, buscar {@code valueOf} lo encontraria... en
 * {@code Class}, que no lo tiene, y nunca llegaria al de {@code String}.
 *
 * <p>Peor todavia: {@code String.class.getName()} contestaria {@code "java.lang.String"} cuando lo
 * que se quiso escribir era el metodo estatico {@code getName} de la clase. Los dos juegos de
 * miembros se pisan.
 *
 * <p>{@code StaticClass} separa las dos cosas. Un {@code StaticClass} de {@code String} expone los
 * miembros <strong>estaticos</strong> de {@code String} y el constructor; el {@code Class} de
 * {@code String} sigue exponiendo los suyos.
 *
 * <h2>Es unico por clase</h2>
 *
 * <p>{@link #forClass} devuelve siempre la misma instancia para la misma clase, asi que se pueden
 * comparar por identidad. No tiene constructor publico por eso mismo.
 *
 * @since 9
 */
public final class StaticClass implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final ClassValue<StaticClass> CACHE = new ClassValue<StaticClass>() {
        protected StaticClass computeValue(final Class<?> type) {
            return new StaticClass(type);
        }
    };

    private final Class<?> clazz;

    private StaticClass(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * El representante de esa clase.
     *
     * <p>La cache es un {@link ClassValue} y no un mapa: queda colgada de la propia clase y se va
     * con ella cuando se descarga. Un mapa comun impediria descargar cualquier clase que alguna vez
     * hubiera pasado por aca, que en un lenguaje de scripting son muchas.
     *
     * @param clazz la clase
     * @return su representante, siempre el mismo
     * @throws NullPointerException si la clase es {@code null}
     */
    public static StaticClass forClass(final Class<?> clazz) {
        return CACHE.get(Objects.requireNonNull(clazz));
    }

    /**
     * La clase representada.
     *
     * @return la clase
     */
    public Class<?> getRepresentedClass() {
        return clazz;
    }

    /** {@inheritDoc} */
    public String toString() {
        return "JavaClass[" + clazz.getName() + "]";
    }
}
