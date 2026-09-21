package java.lang.classfile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import jdk.internal.classfile.impl.ClassHierarchyImpl;

/**
 * Whoever answers what a class inherits from and whether it is an interface.
 *
 * <p>It is needed for a single thing, and saying it is worthwhile because it explains the whole
 * interface: **computing a `StackMapTable` demands knowing the common supertype of two types**, and
 * that cannot be worked out from the `.class` being written -- it is in the other `.class` files. A
 * writer therefore needs a source for that information, and where it comes from is a decision for
 * whoever uses it.
 *
 * <p>Hence the five factories, which are five answers to "where do I get the hierarchy from":
 *
 * <ul>
 * <li>{@link #of} -- from a table one hands it. The only one touching nothing outside.</li>
 * <li>{@link #ofClassLoading} -- by **loading** the classes. It is the most exact and the most
 *     expensive, and it has a side effect one sometimes does not want: it runs static
 *     initialisers.</li>
 * <li>{@link #ofResourceParsing} -- by reading the `.class` files as resources, without loading them.
 *     It avoids the previous side effect.</li>
 * <li>{@link #defaultResolver} -- the platform's.</li>
 * </ul>
 *
 * <p>{@link #cached} and {@link #orElse} compose: resolvers are chained and the result is given a
 * memory.
 */
public interface ClassHierarchyResolver {

    /** What is known about a class: whether it is an interface, and what its superclass is. */
    public interface ClassHierarchyInfo {

        /** The superclass, or `null` if it is an interface or if it is `Object`. */
        ClassDesc superClass();

        /** Whether it is an interface. */
        boolean isInterface();

        /** The information of a class with that superclass. */
        public static ClassHierarchyInfo ofClass(ClassDesc superClass) {
            return ClassHierarchyImpl.infoOfClass(superClass);
        }

        /** An interface's information. */
        public static ClassHierarchyInfo ofInterface() {
            return ClassHierarchyImpl.infoOfInterface();
        }
    }

    /** What is known about that class, or `null` if this resolver does not know it. */
    ClassHierarchyInfo getClassInfo(ClassDesc classDesc);

    /**
     * This one, and whatever this one does not know it asks that other one.
     *
     * <p>It is what allows combining a small, exact table with a general source: what one declared is
     * consulted first and it falls back to loading classes only for what is missing.
     */
    default ClassHierarchyResolver orElse(ClassHierarchyResolver other) {
        return ClassHierarchyImpl.orElse(this, other);
    }

    /**
     * This one, with a memory.
     *
     * <p>It is nearly always worth it: computing a `StackMapTable` asks about the same types many
     * times, and with `ofClassLoading` or `ofResourceParsing` each question is real work.
     */
    default ClassHierarchyResolver cached() {
        return ClassHierarchyImpl.cached(this, new HashMapSupplier());
    }

    /** This one, with a memory in the map that supplier gives. */
    default ClassHierarchyResolver cached(Supplier<Map<ClassDesc, ClassHierarchyInfo>> cache) {
        return ClassHierarchyImpl.cached(this, cache);
    }

    /**
     * The platform's: it reads the system loader's `.class` files without loading the classes.
     *
     * <p>It is `ofResourceParsing` on the platform loader, and not `ofClassLoading`, for the same
     * reason the note above gives: the one in place by default should not run anybody's static
     * initialisers.
     */
    public static ClassHierarchyResolver defaultResolver() {
        return ClassHierarchyImpl.defaultResolver();
    }

    /**
     * The one answering with that table and nothing else.
     *
     * @param interfaces the ones that are interfaces
     * @param classToSuperClass each class's superclass
     */
    public static ClassHierarchyResolver of(Collection<ClassDesc> interfaces,
            Map<ClassDesc, ClassDesc> classToSuperClass) {
        return ClassHierarchyImpl.ofTable(interfaces, classToSuperClass);
    }

    /**
     * The one **loading** the classes with that loader.
     *
     * <p>Exact and with a side effect: loading a class runs its static initialiser. If that is a
     * bother, {@link #ofResourceParsing}.
     */
    public static ClassHierarchyResolver ofClassLoading(ClassLoader loader) {
        return ClassHierarchyImpl.ofClassLoading(loader);
    }

    /** The one loading the classes with that `Lookup`, respecting its access. */
    public static ClassHierarchyResolver ofClassLoading(MethodHandles.Lookup lookup) {
        return ClassHierarchyImpl.ofLookup(lookup);
    }

    /** The one reading the `.class` files as resources of that loader, without loading the classes. */
    public static ClassHierarchyResolver ofResourceParsing(ClassLoader loader) {
        return ClassHierarchyImpl.ofResourceParsing(loader);
    }

    /** The one reading the `.class` files from the stream that function gives. */
    public static ClassHierarchyResolver ofResourceParsing(
            Function<ClassDesc, InputStream> classStreamResolver) {
        return ClassHierarchyImpl.ofStreams(classStreamResolver);
    }
}

// `cached()`'s default map supplier. A named class and not a lambda: see the note on
// `ClassBuilder`.
final class HashMapSupplier
        implements Supplier<Map<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo>> {

    public Map<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo> get() {
        return new HashMap<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo>();
    }
}
