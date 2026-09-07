package jdk.incubator.vector;

/**
 * La clase que va entre {@link Vector} y las seis concretas.
 *
 * <h2>Por que existe si no la puede nombrar nadie</h2>
 *
 * <p>No es publica, asi que desde afuera del paquete no se la puede escribir. Y sin embargo se la
 * <strong>ve</strong>: cuando {@code IntVector} declara {@code IntVector slice(int)} y arriba hay
 * dos declaraciones con retornos cada vez mas anchos, javac deja en {@code IntVector} un metodo
 * puente por cada una. Uno de esos puentes es {@code public AbstractVector slice(int)}, y es publico
 * y esta en el archivo compilado del JDK.
 *
 * <p>Por eso esta clase esta aca. La primera version de este paquete colgaba las seis directamente
 * de {@link Vector}, con el argumento de que una clase vacia en el medio no le daba a nadie un
 * metodo nuevo. Era falso: doce miembros publicos de las seis clases concretas dependen de que este
 * eslabon exista, y sin el no aparecen.
 *
 * <p>Declara lo minimo que hace falta para que esos puentes salgan: los dos {@code slice}, que son
 * los unicos que el JDK estrecha aca en vez de estrecharlos recien abajo.
 *
 * @param <E> el tipo de la posicion, en su version envuelta
 */
abstract class AbstractVector<E> extends Vector<E> {

    /**
     * Con esa carga util.
     *
     * <p>Va escrito aunque no haga nada mas que delegar: {@link Vector} no tiene constructor sin
     * argumentos, y sin este javac generaria uno que llama a un {@code super()} inexistente.
     *
     * @param payload el arreglo de posiciones
     */
    AbstractVector(Object payload) {
        super(payload);
    }

    /**
     * Una parte del vector, empezando en esa posicion.
     *
     * @param origin desde donde
     * @return el pedazo
     */
    @Override
    public abstract AbstractVector<E> slice(int origin);

    /**
     * Una parte que arranca en este vector y sigue en el otro.
     *
     * @param origin desde donde
     * @param v1 el vector que sigue
     * @return el pedazo
     */
    @Override
    public abstract AbstractVector<E> slice(int origin, Vector<E> v1);
}
