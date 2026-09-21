package java.lang.instrument;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * KajiLibrary's java.lang.instrument.Instrumentation -- what an agent can do to the virtual machine.
 *
 * <p>It is neither constructed nor looked up: the virtual machine <b>hands</b> it to an agent's
 * {@code premain} or {@code agentmain} method. If a program is not an agent there is no way of
 * getting one, and that is on purpose -- what this interface allows should not be within reach of
 * just any code.
 *
 * <h2>Redefining and retransforming are not the same</h2>
 *
 * <p>Both replace the bytes of an already loaded class, and they differ in where those bytes come
 * from:
 *
 * <ul>
 *   <li>{@link #redefineClasses} is given the new bytes already built. The registered transformers
 *       do <b>not</b> see them again;
 *   <li>{@link #retransformClasses} is given no bytes: it asks the registered transformers to
 *       process the class again, starting from the original bytes.
 * </ul>
 *
 * <p>Retransforming is what lets two agents coexist: each adds its transformer and both are applied.
 * Redefining overwrites everything, including whatever another agent put there.
 *
 * <p>Both have a hard limit: the class's <b>shape</b> cannot change. Adding or removing methods or
 * fields, changing signatures, changing the hierarchy -- none of that is possible. Only the method
 * bodies change.
 *
 * <h2>The native method prefix</h2>
 *
 * <p>{@link #setNativeMethodPrefix} solves a problem that is invisible until it turns up: a native
 * method has no bytecode to rewrite, so a transformer cannot wrap it. With a prefix, the transformer
 * turns {@code foo} into an ordinary Java method and renames the native one to {@code prefix$foo};
 * the virtual machine looks for the native one with the prefix on and the linking closes.
 *
 * <h2>{@link #getObjectSize} is an estimate</h2>
 *
 * <p>The documentation says so and it is worth repeating: it is an approximation, it does not
 * include what the object references, and it can change between runs. It is good for comparing, not
 * for counting.
 */
public interface Instrumentation {

    /**
     * It registers a transformer.
     *
     * @param canRetransform whether its transformations can be redone with
     *     {@link #retransformClasses}; see the class's note
     */
    void addTransformer(ClassFileTransformer transformer, boolean canRetransform);

    /** The same, with no retransforming. */
    void addTransformer(ClassFileTransformer transformer);

    /**
     * It unregisters it.
     *
     * <p>The classes it already transformed <b>stay as they are</b>: removing the transformer does
     * not undo what was done. Going back takes a retransform.
     *
     * @return whether it was registered
     */
    boolean removeTransformer(ClassFileTransformer transformer);

    /** Whether this virtual machine supports retransforming. */
    boolean isRetransformClassesSupported();

    /**
     * It puts those classes through the registered transformers again.
     *
     * @throws UnmodifiableClassException if one of them cannot be touched
     * @throws UnsupportedOperationException if this virtual machine does not support it
     */
    void retransformClasses(Class<?>... classes) throws UnmodifiableClassException;

    /** Whether this virtual machine supports redefining. */
    boolean isRedefineClassesSupported();

    /**
     * It replaces those classes' bytes, all of them together.
     *
     * <p>See the class's note: the transformers do not see these bytes, and the class's shape cannot
     * change.
     *
     * @throws ClassNotFoundException if one of them is not loaded
     * @throws UnmodifiableClassException if one of them cannot be touched
     */
    void redefineClasses(ClassDefinition... definitions)
        throws ClassNotFoundException, UnmodifiableClassException;

    /** Whether that class can be redefined or retransformed. */
    boolean isModifiableClass(Class<?> theClass);

    /**
     * Every loaded class.
     *
     * <p>It is a raw, untyped array --{@code Class[]} and not {@code Class<?>[]}-- because the
     * signature is from 2003 and changing it breaks the agents compiled against it.
     */
    @SuppressWarnings("rawtypes")
    Class[] getAllLoadedClasses();

    /** The ones that loader initiated, the ones it delegated included. */
    @SuppressWarnings("rawtypes")
    Class[] getInitiatedClasses(ClassLoader loader);

    /** How much it takes up, approximately. See the class's note. */
    long getObjectSize(Object objectToSize);

    /**
     * It adds that jar to the bootstrap loader's search.
     *
     * <p>It is what makes an agent's supporting classes visible from platform classes the agent
     * transformed. Without it, the injected code cannot call anything of the agent's.
     */
    void appendToBootstrapClassLoaderSearch(JarFile jarfile);

    /** The same, for the system loader. */
    void appendToSystemClassLoaderSearch(JarFile jarfile);

    /** Whether this virtual machine supports the native method prefix. */
    boolean isNativeMethodPrefixSupported();

    /** It sets that transformer's prefix. See the class's note. */
    void setNativeMethodPrefix(ClassFileTransformer transformer, String prefix);

    /**
     * It adds reads, exports, opens, uses and providers to a module.
     *
     * <p>It only adds: there is no way of taking anything away. That is the design decision that
     * makes this safe -- an agent can open a module in order to instrument it, and cannot close off
     * to anybody what they already had.
     *
     * @throws UnmodifiableModuleException if that module cannot be modified
     */
    void redefineModule(Module module, Set<Module> extraReads,
                        Map<String, Set<Module>> extraExports,
                        Map<String, Set<Module>> extraOpens, Set<Class<?>> extraUses,
                        Map<Class<?>, List<Class<?>>> extraProvides);

    /** Whether that module can be modified. */
    boolean isModifiableModule(Module module);
}
