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
 * Las implementaciones de {@link ClassHierarchyResolver}.
 *
 * <p>Cinco formas de contestar la misma pregunta --de qué hereda esta clase-- más dos combinadores.
 * Lo único que conviene tener presente al leerlas: **`null` significa "no sé", no "no hereda de
 * nada"**. Es la diferencia que hace que {@link ClassHierarchyResolver#orElse} pueda encadenar: si
 * un resolvedor contestara `ofClass(null)` para lo que no conoce, el siguiente de la cadena nunca se
 * consultaría, y `java.lang.Object` --que sí tiene superclase `null`-- sería indistinguible de un
 * tipo desconocido.
 */
public final class ClassHierarchyImpl {

    private ClassHierarchyImpl() {
    }

    /** La información de una clase con esa superclase. */
    public static ClassHierarchyInfo infoOfClass(ClassDesc superClass) {
        return new Info(superClass, false);
    }

    /** La información de una interfaz. */
    public static ClassHierarchyInfo infoOfInterface() {
        return Info.INTERFACE;
    }

    /** El primero, y lo que no sepa se lo pregunta al segundo. */
    public static ClassHierarchyResolver orElse(ClassHierarchyResolver first,
            ClassHierarchyResolver second) {
        return new OrElse(first, second);
    }

    /** Ése, con memoria en el mapa que dé el proveedor. */
    public static ClassHierarchyResolver cached(ClassHierarchyResolver base,
            Supplier<Map<ClassDesc, ClassHierarchyInfo>> cache) {
        return new Cached(base, cache.get());
    }

    /** El de la plataforma. Ver la nota de {@link ClassHierarchyResolver#defaultResolver}. */
    public static ClassHierarchyResolver defaultResolver() {
        return DEFAULT;
    }

    /** El que contesta con esa tabla. */
    public static ClassHierarchyResolver ofTable(Collection<ClassDesc> interfaces,
            Map<ClassDesc, ClassDesc> classToSuperClass) {
        return new Table(interfaces, classToSuperClass);
    }

    /** El que carga las clases con ese cargador. */
    public static ClassHierarchyResolver ofClassLoading(ClassLoader loader) {
        return new Loading(loader);
    }

    /** El que carga las clases con ese `Lookup`. */
    public static ClassHierarchyResolver ofLookup(MethodHandles.Lookup lookup) {
        return new Loading(lookup.lookupClass().getClassLoader());
    }

    /** El que lee los `.class` como recursos de ese cargador. */
    public static ClassHierarchyResolver ofResourceParsing(ClassLoader loader) {
        return new Parsing(new LoaderStreams(loader));
    }

    /** El que lee los `.class` del flujo que dé esa función. */
    public static ClassHierarchyResolver ofStreams(Function<ClassDesc, InputStream> streams) {
        return new Parsing(streams);
    }

    // El de omisión es `ofResourceParsing` de la plataforma, con memoria. Ver por qué no es
    // `ofClassLoading` en el javadoc de `ClassHierarchyResolver.defaultResolver`.
    private static final ClassHierarchyResolver DEFAULT =
            new Cached(new Parsing(new LoaderStreams(ClassLoader.getPlatformClassLoader())),
                    new HashMap<ClassDesc, ClassHierarchyInfo>());

    /** El nombre binario (`java.lang.String`) de ese descriptor. */
    static String binaryName(ClassDesc d) {
        String s = d.descriptorString();
        if (s.length() > 2 && s.charAt(0) == 'L' && s.charAt(s.length() - 1) == ';') {
            return s.substring(1, s.length() - 1).replace('/', '.');
        }
        // Un arreglo o un primitivo: `Class.forName` los nombra con el descriptor tal cual.
        return s.replace('/', '.');
    }

    /** El nombre interno (`java/lang/String`) de ese descriptor. */
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

    // La memoria guarda también los "no sé", y tiene que hacerlo: sin eso, un tipo que no está
    // vuelve a costar una lectura de `.class` cada vez que se pregunta por él, que es justo el caso
    // en que la respuesta es más cara y menos útil. `Info.INTERFACE` no puede usarse de centinela
    // --es una respuesta válida-- así que el mapa guarda `null` y se distingue con `containsKey`.
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
            Iterator<Map.Entry<ClassDesc, ClassDesc>> es = classToSuperClass.entrySet().iterator();
            while (es.hasNext()) {
                Map.Entry<ClassDesc, ClassDesc> e = es.next();
                this.table.put(e.getKey(), new Info(e.getValue(), false));
            }
        }

        public ClassHierarchyInfo getClassInfo(ClassDesc classDesc) {
            return this.table.get(classDesc);
        }
    }

    // Carga la clase para preguntarle. `initialize` en `false`: hace falta la jerarquía, no el
    // estado estático, y correr un inicializador ajeno por calcular un stack map sería un efecto de
    // lado que nadie pidió.
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
                // Una clase que está pero no enlaza --le falta un supertipo, o su formato es de otra
                // versión-- es tan desconocida para este propósito como una que no está.
                return null;
            }
        }
    }

    // Lee el `.class` y mira su encabezado. No carga nada, así que no corre inicializadores ni
    // resuelve supertipos: para lo que hace falta acá --el nombre de la superclase y si es
    // interfaz-- alcanza con los primeros bytes del archivo.
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
                // Un `.class` que no se puede parsear no es una respuesta: es un "no sé".
                return null;
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                    // Cerrar el flujo del que ya se leyó no puede cambiar la respuesta.
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
