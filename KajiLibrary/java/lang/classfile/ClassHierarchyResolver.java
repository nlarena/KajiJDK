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
 * Quien contesta de qué hereda una clase y si es una interfaz.
 *
 * <p>Hace falta para una sola cosa, y conviene decirla porque explica toda la interfaz: **calcular
 * un `StackMapTable` exige saber el supertipo común de dos tipos**, y eso no se puede deducir del
 * `.class` que se está escribiendo — está en los `.class` de los otros. Un escritor necesita
 * entonces una fuente de esa información, y de dónde sale es una decisión de quien lo usa.
 *
 * <p>De ahí las cinco fábricas, que son cinco respuestas a "de dónde saco la jerarquía":
 *
 * <ul>
 * <li>{@link #of} — de una tabla que uno le da. La única que no toca nada de afuera.</li>
 * <li>{@link #ofClassLoading} — **cargando** las clases. Es la más exacta y la más cara, y tiene un
 *     efecto de lado que a veces no se quiere: corre los inicializadores estáticos.</li>
 * <li>{@link #ofResourceParsing} — leyendo los `.class` como recursos, sin cargarlos. Evita el
 *     efecto de lado anterior.</li>
 * <li>{@link #defaultResolver} — la de la plataforma.</li>
 * </ul>
 *
 * <p>{@link #cached} y {@link #orElse} componen: se encadenan resolvedores y se le pone memoria al
 * resultado.
 */
public interface ClassHierarchyResolver {

    /** Lo que se sabe de una clase: si es interfaz, y cuál es su superclase. */
    public interface ClassHierarchyInfo {

        /** La superclase, o `null` si es una interfaz o si es `Object`. */
        ClassDesc superClass();

        /** Si es una interfaz. */
        boolean isInterface();

        /** La información de una clase con esa superclase. */
        public static ClassHierarchyInfo ofClass(ClassDesc superClass) {
            return ClassHierarchyImpl.infoOfClass(superClass);
        }

        /** La información de una interfaz. */
        public static ClassHierarchyInfo ofInterface() {
            return ClassHierarchyImpl.infoOfInterface();
        }
    }

    /** Lo que se sabe de esa clase, o `null` si este resolvedor no la conoce. */
    ClassHierarchyInfo getClassInfo(ClassDesc classDesc);

    /**
     * Éste, y lo que éste no sepa se lo pregunta a ese otro.
     *
     * <p>Es lo que permite combinar una tabla chica y exacta con una fuente general: se consulta
     * primero lo que uno declaró y se cae a cargar clases sólo para lo que falte.
     */
    default ClassHierarchyResolver orElse(ClassHierarchyResolver other) {
        return ClassHierarchyImpl.orElse(this, other);
    }

    /**
     * Éste, con memoria.
     *
     * <p>Vale la pena casi siempre: calcular un `StackMapTable` pregunta por los mismos tipos muchas
     * veces, y con `ofClassLoading` u `ofResourceParsing` cada pregunta es trabajo de verdad.
     */
    default ClassHierarchyResolver cached() {
        return ClassHierarchyImpl.cached(this, new HashMapSupplier());
    }

    /** Éste, con memoria en el mapa que dé ese proveedor. */
    default ClassHierarchyResolver cached(Supplier<Map<ClassDesc, ClassHierarchyInfo>> cache) {
        return ClassHierarchyImpl.cached(this, cache);
    }

    /**
     * El de la plataforma: lee los `.class` del cargador del sistema sin cargar las clases.
     *
     * <p>Es `ofResourceParsing` del cargador de la plataforma, y no `ofClassLoading`, por lo mismo
     * que dice la nota de arriba: el que está por omisión no debería correr inicializadores
     * estáticos de nadie.
     */
    public static ClassHierarchyResolver defaultResolver() {
        return ClassHierarchyImpl.defaultResolver();
    }

    /**
     * El que contesta con esa tabla y nada más.
     *
     * @param interfaces las que son interfaces
     * @param classToSuperClass de cada clase, su superclase
     */
    public static ClassHierarchyResolver of(Collection<ClassDesc> interfaces,
            Map<ClassDesc, ClassDesc> classToSuperClass) {
        return ClassHierarchyImpl.ofTable(interfaces, classToSuperClass);
    }

    /**
     * El que **carga** las clases con ese cargador.
     *
     * <p>Exacto y con efecto de lado: cargar una clase corre su inicializador estático. Si eso
     * molesta, {@link #ofResourceParsing}.
     */
    public static ClassHierarchyResolver ofClassLoading(ClassLoader loader) {
        return ClassHierarchyImpl.ofClassLoading(loader);
    }

    /** El que carga las clases con ese `Lookup`, respetando su acceso. */
    public static ClassHierarchyResolver ofClassLoading(MethodHandles.Lookup lookup) {
        return ClassHierarchyImpl.ofLookup(lookup);
    }

    /** El que lee los `.class` como recursos de ese cargador, sin cargar las clases. */
    public static ClassHierarchyResolver ofResourceParsing(ClassLoader loader) {
        return ClassHierarchyImpl.ofResourceParsing(loader);
    }

    /** El que lee los `.class` del flujo que dé esa función. */
    public static ClassHierarchyResolver ofResourceParsing(
            Function<ClassDesc, InputStream> classStreamResolver) {
        return ClassHierarchyImpl.ofStreams(classStreamResolver);
    }
}

// El proveedor de mapa por omisión de `cached()`. Clase nombrada y no lambda: ver la nota de
// `ClassBuilder`.
final class HashMapSupplier
        implements Supplier<Map<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo>> {

    public Map<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo> get() {
        return new HashMap<ClassDesc, ClassHierarchyResolver.ClassHierarchyInfo>();
    }
}
