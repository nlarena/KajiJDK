package javax.management.loading;

/**
 * KajiLibrary's javax.management.loading.ClassLoaderRepository -- los cargadores que conoce un
 * agente.
 *
 * <p>Un agente JMX tiene que poder cargar clases que no estan en su propio classpath: el nombre de
 * una clase llega por la red, desde un cliente remoto, y el MBean que la implementa puede haberlo
 * traido cualquiera de los cargadores registrados. Este repositorio es esa lista, y se consulta en
 * orden de registro.
 *
 * <h2>Por que hay tres metodos y no uno</h2>
 *
 * <p>Los dos con cargador existen para <b>cortar recursiones</b>, no por comodidad. Un cargador que
 * esta en el repositorio y que ante un fallo le pregunta al repositorio se llamaria a si mismo para
 * siempre. Con {@link #loadClassWithout} se excluye a si mismo y con {@link #loadClassBefore} se
 * excluye ademas a todos los que vienen despues.
 *
 * <p>La diferencia entre los dos importa: {@code loadClassWithout} sigue consultando a los
 * <b>posteriores</b>, asi que dos cargadores que se preguntan entre si todavia pueden colgarse.
 * {@code loadClassBefore} no puede, porque cada llamada mira un prefijo estrictamente mas corto de
 * la lista. Por eso es la que conviene cuando el que pregunta es parte del repositorio.
 */
public interface ClassLoaderRepository {

    /**
     * Busca la clase en todos los cargadores, en orden de registro.
     *
     * @throws ClassNotFoundException si ninguno la tiene
     */
    Class<?> loadClass(String className) throws ClassNotFoundException;

    /**
     * Igual, salteando ese cargador.
     *
     * @param exclude el que no se consulta; ver la nota de la clase
     * @throws ClassNotFoundException si ninguno de los demas la tiene
     */
    Class<?> loadClassWithout(ClassLoader exclude, String className) throws ClassNotFoundException;

    /**
     * Igual, pero solo con los que estan <b>antes</b> que ese.
     *
     * <p>La busqueda se detiene al llegar a {@code stop}, que no se consulta.
     *
     * @throws ClassNotFoundException si ninguno de los anteriores la tiene
     */
    Class<?> loadClassBefore(ClassLoader stop, String className) throws ClassNotFoundException;
}
