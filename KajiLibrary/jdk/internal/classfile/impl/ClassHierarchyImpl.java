package jdk.internal.classfile.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassHierarchyResolver;
import java.lang.classfile.ClassHierarchyResolver.ClassHierarchyInfo;
import java.lang.classfile.ClassModel;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.constant.ClassDesc;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The implementations of {@link ClassHierarchyResolver}.
 *
 * <p>Five ways of answering the same question --what this class inherits from-- plus two
 * combinators. The one thing worth keeping in mind when reading them: **`null` means "I do not
 * know", not "it inherits from nothing"**. It is the difference that lets {@link
 * ClassHierarchyResolver#orElse} chain: if a resolver answered `ofClass(null)` for what it does not
 * know, the next one in the chain would never be consulted, and `java.lang.Object` --which does
 * have a `null` superclass-- would be indistinguishable from an unknown type.
 */
public final class ClassHierarchyImpl {

    private ClassHierarchyImpl() {
    }

    /** The information of a class with that superclass. */
    public static ClassHierarchyInfo infoOfClass(ClassDesc superClass) {
        return new Info(superClass, false);
    }

    /** The information of an interface. */
    public static ClassHierarchyInfo infoOfInterface() {
        return Info.INTERFACE;
    }

    /** The first one, and what it does not know it asks the second. */
    public static ClassHierarchyResolver orElse(ClassHierarchyResolver first,
            ClassHierarchyResolver second) {
        return new OrElse(first, second);
    }

    /** That one, with memory in the map the supplier gives. */
    public static ClassHierarchyResolver cached(ClassHierarchyResolver base,
            Supplier<Map<ClassDesc, ClassHierarchyInfo>> cache) {
        return new Cached(base, cache.get());
    }

    /** The platform's one. See the note of {@link ClassHierarchyResolver#defaultResolver}. */
    public static ClassHierarchyResolver defaultResolver() {
        return DEFAULT;
    }

    /** The one that answers with that table. */
    public static ClassHierarchyResolver ofTable(Collection<ClassDesc> interfaces,
            Map<ClassDesc, ClassDesc> classToSuperClass) {
        return new Table(interfaces, classToSuperClass);
    }

    /** The one that loads the classes with that loader. */
    public static ClassHierarchyResolver ofClassLoading(ClassLoader loader) {
        return new Loading(loader);
    }

    /** The one that loads the classes with that `Lookup`. */
    public static ClassHierarchyResolver ofLookup(MethodHandles.Lookup lookup) {
        return new Loading(lookup.lookupClass().getClassLoader());
    }

    /** The one that reads the `.class` files as resources of that loader. */
    public static ClassHierarchyResolver ofResourceParsing(ClassLoader loader) {
        return new Parsing(new LoaderStreams(loader));
    }

    /** The one that reads the `.class` files from the stream that function gives. */
    public static ClassHierarchyResolver ofStreams(Function<ClassDesc, InputStream> streams) {
        return new Parsing(streams);
    }

    // The default is the platform's `ofResourceParsing`, with memory. See why it is not
    // `ofClassLoading` in the javadoc of `ClassHierarchyResolver.defaultResolver`.
    private static final ClassHierarchyResolver DEFAULT =
            new Cached(new Parsing(new LoaderStreams(ClassLoader.getPlatformClassLoader())),
                    new HashMap<ClassDesc, ClassHierarchyInfo>());

    /** The binary name (`java.lang.String`) of that descriptor. */
    static String binaryName(ClassDesc d) {
        String s = d.descriptorString();
        if (s.length() > 2 && s.charAt(0) == 'L' && s.charAt(s.length() - 1) == ';') {
            return s.substring(1, s.length() - 1).replace('/', '.');
        }
        // An array or a primitive: `Class.forName` names them with the descriptor as it stands.
        return s.replace('/', '.');
    }

    /** The internal name (`java/lang/String`) of that descriptor. */
    static String internalName(ClassDesc d) {
        String s = d.descriptorString();
        if (s.length() > 2 && s.charAt(0) == 'L' && s.charAt(s.length() - 1) == ';') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static final class Info implements ClassHierarchyInfo {

        static final Info INTERFACE = new Info(null, true);

        private final ClassDesc superClass;
        private final boolean isInterface;

        Info(ClassDesc superClass, boolean isInterface) {
            this.superClass = superClass;
            this.isInterface = isInterface;
        }

        public ClassDesc superClass() {
            return this.superClass;
        }

        public boolean isInterface() {
            return this.isInterface;
        }

        public String toString() {
            return this.isInterface ? "interface" : "class extends " + this.superClass;
        }
    }

    private static final class OrElse implements ClassHierarchyResolver {

        private final ClassHierarchyResolver first;
        private final ClassHierarchyResolver second;

        OrElse(ClassHierarchyResolver first, ClassHierarchyResolver second) {
            this.first = first;
            this.second = second;
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            ClassHierarchyInfo i = this.first.getClassInfo(classDesc);
            return i != null ? i : this.second.getClassInfo(classDesc);
        }
    }

    // The memory also keeps the "I do not know"s, and has to: without that, a type that is not
    // there costs a `.class` read again every time it is asked about, which is exactly the case in
    // which the answer is most expensive and least useful. `Info.INTERFACE` cannot be used as a
    // sentinel --it is a valid answer-- so the map keeps `null` and it is told apart with
    // `containsKey`.
    private static final class Cached implements ClassHierarchyResolver {

        private final ClassHierarchyResolver base;
        private final Map<ClassDesc, ClassHierarchyInfo> cache;

        Cached(ClassHierarchyResolver base, Map<ClassDesc, ClassHierarchyInfo> cache) {
            this.base = base;
            this.cache = cache;
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            if (this.cache.containsKey(classDesc)) {
                return this.cache.get(classDesc);
            }
            ClassHierarchyInfo i = this.base.getClassInfo(classDesc);
            this.cache.put(classDesc, i);
            return i;
        }
    }

    private static final class Table implements ClassHierarchyResolver {

        private final Map<ClassDesc, ClassHierarchyInfo> table =
                new HashMap<ClassDesc, ClassHierarchyInfo>();

        Table(Collection<ClassDesc> interfaces, Map<ClassDesc, ClassDesc> classToSuperClass) {
            Iterator<ClassDesc> it = interfaces.iterator();
            while (it.hasNext()) {
                this.table.put(it.next(), Info.INTERFACE);
            }
            Iterator<Map.Entry<ClassDesc, ClassDesc>> it = classToSuperClass.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<ClassDesc, ClassDesc> e = it.next();
                this.table.put(e.getKey(), new Info(e.getValue(), false));
            }
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            return this.table.get(classDesc);
        }
    }

    // It loads the class to ask it. `initialize` as `false`: the hierarchy is needed, not the
    // static state, and running someone else's initialiser in order to compute a stack map would be
    // a side effect nobody asked for.
    private static final class Loading implements ClassHierarchyResolver {

        private final ClassLoader loader;

        Loading(ClassLoader loader) {
            this.loader = loader;
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            try {
                Class<?> c = Class.forName(ClassHierarchyImpl.binaryName(classDesc), false,
                        this.loader);
                if (c.isInterface()) {
                    return Info.INTERFACE;
                }
                Class<?> sup = c.getSuperclass();
                return new Info(sup == null ? null : ClassDesc.of(sup.getName()), false);
            } catch (ClassNotFoundException e) {
                return null;
            } catch (LinkageError e) {
                // A class that is there but does not link --it lacks a supertype, or its format is
                // of another version-- is as unknown for this purpose as one that is not there.
                return null;
            }
        }
    }

    // It reads the `.class` and looks at its header. It loads nothing, so it runs no initialisers
    // and resolves no supertypes: for what is needed here --the name of the superclass and whether
    // it is an interface-- the first bytes of the file are enough.
    private static final class Parsing implements ClassHierarchyResolver {

        private final Function<ClassDesc, InputStream> streams;

        Parsing(Function<ClassDesc, InputStream> streams) {
            this.streams = streams;
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            InputStream in = this.streams.apply(classDesc);
            if (in == null) {
                return null;
            }
            try {
                byte[] bytes = ClassHierarchyImpl.readAll(in);
                ClassModel m = ClassFile.of().parse(bytes);
                if ((m.flags().flagsMask() & 0x0200) != 0) {
                    return Info.INTERFACE;
                }
                Optional<ClassEntry> sup = m.superclass();
                return new Info(sup.isPresent() ? sup.get().asSymbol() : null, false);
            } catch (IOException e) {
                return null;
            } catch (IllegalArgumentException e) {
                // A `.class` that cannot be parsed is not an answer: it is an "I do not know".
                return null;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // Closing the stream already read from cannot change the answer.
                }
            }
        }
    }

    private static final class LoaderStreams implements Function<ClassDesc, InputStream> {

        private final ClassLoader loader;

        LoaderStreams(ClassLoader loader) {
            this.loader = loader;
        }

        public InputStream apply(ClassDesc classDesc) {
            String resource = ClassHierarchyImpl.internalName(classDesc) + ".class";
            if (this.loader == null) {
                return ClassLoader.getSystemResourceAsStream(resource);
            }
            return this.loader.getResourceAsStream(resource);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n = in.read(buf);
        while (n > 0) {
            out.write(buf, 0, n);
            n = in.read(buf);
        }
        return out.toByteArray();
    }
}
