package java.lang.instrument;

import java.security.ProtectionDomain;

/**
 * KajiLibrary's java.lang.instrument.ClassFileTransformer -- it rewrites a class's bytes before the
 * class is loaded.
 *
 * <p>It is the piece that makes a profiler, a tracer or a dependency injector possible without
 * touching the code: the virtual machine hands it the bytes of every class it is about to load and
 * uses whatever comes back.
 *
 * <h2>Returning null is the normal case</h2>
 *
 * <p>A transformer is applied to <b>every</b> class, the thousands of platform ones included.
 * Returning null means "I am not touching it" and is the right answer for nearly all of them;
 * returning an unchanged copy works just as well but costs one rewrite per class.
 *
 * <h2>The two methods, and which one to override</h2>
 *
 * <p>Both have a default and both are called the same, and that is the confusing part. The
 * <b>six</b>-argument one --the one taking the {@code Module}-- is the one the virtual machine
 * calls; its default delegates to the five-argument one, which is the one that existed before
 * modules.
 *
 * <p>So overriding the five-argument one is enough and is the usual thing. Overriding the
 * six-argument one is only needed when the transformer has to know which module the class is in --
 * to leave everything in {@code java.base} alone, for instance.
 *
 * <p>The five-argument default returns null: a transformer overriding neither of the two transforms
 * nothing, which is the only coherent answer.
 */
public interface ClassFileTransformer {

    /**
     * The version from before modules.
     *
     * @param loader who is loading the class; null is the bootstrap loader
     * @param className the internal name, with slashes and without {@code .class}
     * @param classBeingRedefined the class being redefined, or null on a fresh load
     * @param protectionDomain where it comes from
     * @param classfileBuffer the current bytes
     * @return the new bytes, or null to leave it alone
     * @throws IllegalClassFormatException if the bytes that arrived are no good
     */
    default byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                             ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException {
        return null;
    }

    /**
     * The version with a module, which is the one the virtual machine calls.
     *
     * <p>By default it delegates to the five-argument one; see the class's note.
     *
     * @param module the class's module
     */
    default byte[] transform(Module module, ClassLoader loader, String className,
                             Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
                             byte[] classfileBuffer) throws IllegalClassFormatException {
        return transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
    }
}
