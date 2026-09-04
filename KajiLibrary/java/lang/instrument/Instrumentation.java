package java.lang.instrument;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * KajiLibrary's java.lang.instrument.Instrumentation -- lo que un agente puede hacerle a la maquina
 * virtual.
 *
 * <p>No se construye ni se busca: la maquina virtual se lo <b>pasa</b> al metodo {@code premain} o
 * {@code agentmain} de un agente. Si un programa no es un agente, no hay forma de conseguir uno, y
 * eso es a proposito -- lo que esta interfaz permite hacer no deberia estar al alcance de cualquier
 * codigo.
 *
 * <h2>Redefinir y retransformar no son lo mismo</h2>
 *
 * <p>Los dos reemplazan los bytes de una clase ya cargada, y se diferencian en de donde salen:
 *
 * <ul>
 *   <li>{@link #redefineClasses} recibe los bytes nuevos ya armados. Los transformadores
 *       registrados <b>no</b> los vuelven a ver;
 *   <li>{@link #retransformClasses} no recibe bytes: le pide a los transformadores registrados que
 *       vuelvan a procesar la clase, empezando de los bytes originales.
 * </ul>
 *
 * <p>Retransformar es lo que permite que dos agentes convivan: cada uno agrega su transformador y
 * los dos se aplican. Redefinir pisa todo, incluido lo que otro agente haya puesto.
 *
 * <p>Los dos tienen un limite duro: no se puede cambiar la <b>forma</b> de la clase. Agregar o quitar
 * metodos o campos, cambiar firmas, cambiar la jerarquia -- nada de eso se puede. Solo cambian los
 * cuerpos de los metodos.
 *
 * <h2>El prefijo de metodos nativos</h2>
 *
 * <p>{@link #setNativeMethodPrefix} resuelve un problema que no se ve hasta que aparece: un metodo
 * nativo no tiene bytecode que reescribir, asi que un transformador no puede envolverlo. Con un
 * prefijo, el transformador convierte {@code foo} en un metodo Java normal y renombra el nativo a
 * {@code prefijo$foo}; la maquina virtual busca el nativo con el prefijo puesto y el enlace cierra.
 *
 * <h2>{@link #getObjectSize} es una estimacion</h2>
 *
 * <p>La documentacion lo dice y conviene repetirlo: es una aproximacion, no incluye lo que el objeto
 * referencia, y puede cambiar entre corridas. Sirve para comparar, no para contar.
 */
public interface Instrumentation {

    /**
     * Registra un transformador.
     *
     * @param canRetransform si sus transformaciones se pueden rehacer con
     *     {@link #retransformClasses}; ver la nota de la clase
     */
    void addTransformer(ClassFileTransformer transformer, boolean canRetransform);

    /** Idem, sin poder retransformar. */
    void addTransformer(ClassFileTransformer transformer);

    /**
     * Lo da de baja.
     *
     * <p>Las clases que ya transformo <b>quedan como estan</b>: sacar el transformador no deshace lo
     * hecho. Para volver atras hay que retransformar.
     *
     * @return si estaba registrado
     */
    boolean removeTransformer(ClassFileTransformer transformer);

    /** Si esta maquina virtual soporta retransformar. */
    boolean isRetransformClassesSupported();

    /**
     * Vuelve a pasar esas clases por los transformadores registrados.
     *
     * @throws UnmodifiableClassException si alguna no se puede tocar
     * @throws UnsupportedOperationException si esta maquina virtual no lo soporta
     */
    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    /** Si esta maquina virtual soporta redefinir. */
    boolean isRedefineClassesSupported();

    /**
     * Reemplaza los bytes de esas clases, todas juntas.
     *
     * <p>Ver la nota de la clase: los transformadores no ven estos bytes, y no se puede cambiar la
     * forma de la clase.
     *
     * @throws ClassNotFoundException si alguna no esta cargada
     * @throws UnmodifiableClassException si alguna no se puede tocar
     */
    void redefineClasses(ClassDefinition... definitions)
        throws ClassNotFoundException, UnmodifiableClassException;

    /** Si esa clase se puede redefinir o retransformar. */
    boolean isModifiableClass(Class<?> theClass);

    /**
     * Todas las clases cargadas.
     *
     * <p>Es un arreglo crudo y sin tipar --{@code Class[]} y no {@code Class<?>[]}-- porque la firma
     * es de 2003 y cambiarla rompe a los agentes compilados contra ella.
     */
    @SuppressWarnings("rawtypes")
    Class[] getAllLoadedClasses();

    /** Las que ese cargador inicio, incluidas las que delego. */
    @SuppressWarnings("rawtypes")
    Class[] getInitiatedClasses(ClassLoader loader);

    /** Cuanto ocupa, aproximadamente. Ver la nota de la clase. */
    long getObjectSize(Object objectToSize);

    /**
     * Agrega ese jar a la busqueda del cargador de arranque.
     *
     * <p>Es lo que permite que las clases de apoyo de un agente sean visibles desde clases de la
     * plataforma que el agente transformo. Sin esto, el codigo inyectado no puede llamar a nada del
     * agente.
     */
    void appendToBootstrapClassLoaderSearch(JarFile jarfile);

    /** Idem, para el cargador del sistema. */
    void appendToSystemClassLoaderSearch(JarFile jarfile);

    /** Si esta maquina virtual soporta el prefijo de metodos nativos. */
    boolean isNativeMethodPrefixSupported();

    /** Fija el prefijo de ese transformador. Ver la nota de la clase. */
    void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);

    /**
     * Le agrega lecturas, exportaciones, aperturas, usos y proveedores a un modulo.
     *
     * <p>Solo agrega: no hay forma de sacar nada. Es la decision de diseno que hace que esto sea
     * seguro -- un agente puede abrir un modulo para poder instrumentarlo, y no puede cerrarle a
     * nadie lo que ya tenia.
     *
     * @throws UnmodifiableModuleException si ese modulo no se puede modificar
     */
    void redefineModule(Module module, Set<Module> extraReads,
                        Map<String, Set<Module>> extraExports,
                        Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses,
                        Map<Class<?>, List<Class<?>>> extraProvides);

    /** Si ese modulo se puede modificar. */
    boolean isModifiableModule(Module module);
}
