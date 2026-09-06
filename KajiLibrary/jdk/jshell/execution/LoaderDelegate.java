package jdk.jshell.execution;

import jdk.jshell.spi.ExecutionControl.ClassBytecodes;
import jdk.jshell.spi.ExecutionControl.ClassInstallException;
import jdk.jshell.spi.ExecutionControl.EngineTerminationException;
import jdk.jshell.spi.ExecutionControl.InternalException;
import jdk.jshell.spi.ExecutionControl.NotImplementedException;

/**
 * Quien se encarga de meter las clases en la maquina virtual que ejecuta.
 *
 * <h2>Por que es una interfaz aparte</h2>
 *
 * <p>Un motor de ejecucion hace dos cosas bien distintas: instalar el codigo y correrlo. La segunda
 * es siempre igual --buscar el metodo y llamarlo--; la primera cambia por completo segun donde este
 * el codigo, si hay que aislarlo del resto, y si se lo puede reemplazar en caliente.
 *
 * <p>Separarlas es lo que permite tener un solo {@link DirectExecutionControl} y cambiarle el
 * cargador: uno que define en un {@link ClassLoader} propio, otro que reusa el del programa, otro
 * que manda los bytes a la otra punta.
 *
 * <h2>{@link #classesRedefined} no lanza nada</h2>
 *
 * <p>Es un aviso, no una operacion: le dice al cargador que unas clases que el instalo acaban de
 * cambiar de contenido. Quien redefinio ya hizo el trabajo y no esta esperando permiso, asi que no
 * hay nada que el cargador pueda contestar.
 *
 * @since 9
 */
public interface LoaderDelegate {

    /**
     * Instala esas clases.
     *
     * @param cbcs los nombres y el bytecode de cada una
     * @throws ClassInstallException si alguna no se pudo instalar
     * @throws NotImplementedException si este cargador no sabe instalar
     * @throws EngineTerminationException si el motor ya no esta
     */
    void load(ClassBytecodes[] cbcs)
            throws ClassInstallException, NotImplementedException, EngineTerminationException;

    /**
     * Aviso de que esas clases cambiaron de contenido.
     *
     * @param cbcs las clases redefinidas
     */
    void classesRedefined(ClassBytecodes[] cbcs);

    /**
     * Agrega una entrada al camino de busqueda de clases.
     *
     * @param path la entrada
     * @throws EngineTerminationException si el motor ya no esta
     * @throws InternalException si no se pudo agregar
     */
    void addToClasspath(String path) throws EngineTerminationException, InternalException;

    /**
     * Busca una clase por nombre.
     *
     * @param name el nombre completo
     * @return la clase
     * @throws ClassNotFoundException si no esta
     */
    Class<?> findClass(String name) throws ClassNotFoundException;
}
