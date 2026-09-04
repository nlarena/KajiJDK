package javax.management;

import java.beans.ConstructorProperties;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataInvocationHandler;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;

/**
 * La traduccion entre un tipo Java y su <b>tipo abierto</b>, que es lo que define a un MXBean.
 *
 * <p>Un MBean normal manda por el cable los objetos Java tal cual, y por eso solo sirve si el cliente
 * tiene las mismas clases. Un MXBean no: convierte todo a los tipos de
 * {@link javax.management.openmbean} --numeros, cadenas, {@link CompositeData}, {@link TabularData}--
 * que cualquier cliente puede leer sin conocer una sola clase del servidor. Esa conversion es esta
 * clase.
 *
 * <h2>Lo que se mapea, y lo que no</h2>
 *
 * <p>Se mapea lo que la especificacion llama mapeable y este arbol puede resolver:
 *
 * <ul>
 *   <li>los <b>tipos simples</b> --primitivos y sus envoltorios, {@code String}, {@code BigDecimal},
 *       {@code BigInteger}, {@code Date}, {@link ObjectName}-- que se mapean a si mismos;
 *   <li>los tipos que <b>ya son abiertos</b> ({@code CompositeData}, {@code TabularData}), tambien a
 *       si mismos;
 *   <li>las <b>enumeraciones</b>, que van y vuelven como el nombre de la constante;
 *   <li>los <b>arreglos</b>, elemento por elemento y en cualquier dimension;
 *   <li>las <b>interfaces y clases con getters</b>, que van como {@code CompositeData} con un item
 *       por propiedad.
 * </ul>
 *
 * <p><b>{@code List<E>} y {@code Map<K,V>} no se mapean</b>, y el motivo no es que la regla sea
 * dificil --{@code List<E>} es un arreglo de E, {@code Map<K,V>} es una {@code TabularData}-- sino
 * que <b>hace falta saber quien es E</b>, y en esta VM no se puede: {@code getGenericReturnType()}
 * devuelve el tipo <b>crudo</b>, asi que un {@code List<String>} llega indistinguible de un
 * {@code List<ObjectName>}. Adivinar seria mandar basura al primer elemento que no fuera del tipo
 * supuesto.
 *
 * <p>Por eso se rechaza con {@link IllegalArgumentException} <b>al construir el proxy</b> y no al
 * usarlo: es el mismo momento en que el JDK rechaza una interfaz que no es un MXBean valido, y es el
 * momento en que quien escribe el codigo puede hacer algo al respecto. Nunca se devuelve un valor
 * inventado.
 *
 * <h2>Como se reconstruye un {@code CompositeData}</h2>
 *
 * <p>De ida es facil --se llama a cada getter-- y de vuelta hay tres caminos, que se prueban en el
 * orden que manda la especificacion: un {@code public static T from(CompositeData)}, un constructor
 * anotado con {@link ConstructorProperties}, o --si el tipo es una <b>interfaz</b>-- un proxy sobre
 * el {@code CompositeData}, que es lo que hace el JDK y para lo que existe
 * {@link CompositeDataInvocationHandler}.
 */
abstract class MXMapeo {

    /** El tipo abierto que le corresponde. */
    abstract OpenType<?> tipoAbierto();

    /** De Java a abierto. */
    abstract Object aAbierto(Object v) throws OpenDataException;

    /** De abierto a Java. */
    abstract Object aJava(Object v) throws OpenDataException;

    /** Si la conversion es la identidad; sirve para saltearla entera. */
    boolean esIdentidad() {
        return false;
    }

    // Los mapeos ya resueltos. Un tipo compuesto se mira a si mismo cuando tiene una propiedad de su
    // propio tipo, asi que la tabla ademas corta esa recursion.
    private static final Map<Class<?>, MXMapeo> CACHE = new HashMap<Class<?>, MXMapeo>();

    /**
     * El mapeo de ese tipo.
     *
     * @throws IllegalArgumentException si el tipo no se puede mapear; el mensaje dice cual y por que
     */
    static synchronized MXMapeo de(Class<?> c) {
        MXMapeo m = CACHE.get(c);
        if (m != null) {
            return m;
        }
        m = construir(c);
        CACHE.put(c, m);
        return m;
    }

    private static MXMapeo construir(Class<?> c) {
        if (c == null) {
            throw new IllegalArgumentException("El tipo no puede ser null");
        }
        SimpleType<?> s = MXMapeo.simple(c);
        if (s != null) {
            return new Identidad(s);
        }
        if (CompositeData.class.isAssignableFrom(c) || TabularData.class.isAssignableFrom(c)) {
            // Ya es un tipo abierto: no hay nada que convertir. El tipo abierto exacto no se conoce
            // sin un valor, y no hace falta -- esta rama nunca describe un item de un CompositeType.
            return new Identidad(null);
        }
        if (c.isEnum()) {
            return new DeEnum(c);
        }
        if (c.isArray()) {
            return new DeArreglo(c);
        }
        if (Map.class.isAssignableFrom(c) || List.class.isAssignableFrom(c)
                || java.util.Set.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException(
                    c.getName() + " no se puede mapear: haria falta saber el tipo de sus elementos,"
                    + " y esta VM no expone los argumentos de tipo (getGenericReturnType devuelve el"
                    + " tipo crudo). Ver la nota de MXMapeo.");
        }
        if (c.isInterface() && JMX.isMXBeanInterface(c)) {
            // La especificacion dice que una interfaz MXBean nombrada dentro de otra es una
            // **referencia**: viaja como el `ObjectName` del MBean que la implementa, no como sus
            // datos. Convertirla a `CompositeData` seria mandar una copia donde el contrato promete
            // un puntero, y quien la reciba creeria estar mirando el objeto vivo.
            //
            // Resolver un `ObjectName` en las dos direcciones necesita el registro del servidor, que
            // un proxy no tiene. Se rechaza en vez de aproximarse; se comprobo contra el JDK 25 que
            // el mapeo correcto es el `ObjectName`.
            throw new IllegalArgumentException(c.getName()
                    + " es una interfaz MXBean, o sea una referencia a otro MBean: viaja como su"
                    + " ObjectName, y resolverlo necesita el registro del servidor que un proxy no"
                    + " tiene");
        }
        return DeCompuesto.crear(c);
    }

    /** El {@link SimpleType} de ese tipo, o null si no es uno de los simples. */
    private static SimpleType<?> simple(Class<?> c) {
        if (c == Boolean.TYPE || c == Boolean.class) {
            return SimpleType.BOOLEAN;
        }
        if (c == Character.TYPE || c == Character.class) {
            return SimpleType.CHARACTER;
        }
        if (c == Byte.TYPE || c == Byte.class) {
            return SimpleType.BYTE;
        }
        if (c == Short.TYPE || c == Short.class) {
            return SimpleType.SHORT;
        }
        if (c == Integer.TYPE || c == Integer.class) {
            return SimpleType.INTEGER;
        }
        if (c == Long.TYPE || c == Long.class) {
            return SimpleType.LONG;
        }
        if (c == Float.TYPE || c == Float.class) {
            return SimpleType.FLOAT;
        }
        if (c == Double.TYPE || c == Double.class) {
            return SimpleType.DOUBLE;
        }
        if (c == Void.TYPE || c == Void.class) {
            return SimpleType.VOID;
        }
        if (c == String.class) {
            return SimpleType.STRING;
        }
        if (c == BigDecimal.class) {
            return SimpleType.BIGDECIMAL;
        }
        if (c == BigInteger.class) {
            return SimpleType.BIGINTEGER;
        }
        if (c == Date.class) {
            return SimpleType.DATE;
        }
        if (c == ObjectName.class) {
            return SimpleType.OBJECTNAME;
        }
        return null;
    }

    // ---- las cuatro formas -----------------------------------------------------------------

    /** El valor viaja tal cual. */
    private static final class Identidad extends MXMapeo {

        private final OpenType<?> tipo;

        Identidad(OpenType<?> tipo) {
            this.tipo = tipo;
        }

        OpenType<?> tipoAbierto() {
            return this.tipo;
        }

        Object aAbierto(Object v) {
            return v;
        }

        Object aJava(Object v) {
            return v;
        }

        boolean esIdentidad() {
            return true;
        }
    }

    /** Una enumeracion viaja como el nombre de su constante. */
    private static final class DeEnum extends MXMapeo {

        private final Class<?> clase;

        DeEnum(Class<?> clase) {
            this.clase = clase;
        }

        OpenType<?> tipoAbierto() {
            return SimpleType.STRING;
        }

        Object aAbierto(Object v) {
            return v == null ? null : ((Enum<?>) v).name();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Object aJava(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            try {
                return Enum.valueOf((Class<Enum>) this.clase, (String) v);
            } catch (IllegalArgumentException e) {
                // Un nombre que la enumeracion no tiene. Es un error del dato, no del mapeo, y por
                // eso sale como `OpenDataException` y no como el `IllegalArgumentException` que
                // significa "este tipo no se puede mapear".
                throw new OpenDataException("'" + v + "' no es una constante de "
                        + this.clase.getName());
            }
        }
    }

    /** Un arreglo viaja como un arreglo de lo que sea que viajen sus elementos. */
    private static final class DeArreglo extends MXMapeo {

        private final Class<?> componente;
        private final MXMapeo mapeoDelComponente;
        private final OpenType<?> tipo;

        DeArreglo(Class<?> arreglo) {
            this.componente = arreglo.getComponentType();
            this.mapeoDelComponente = MXMapeo.de(this.componente);
            OpenType<?> elem = this.mapeoDelComponente.tipoAbierto();
            OpenType<?> t = null;
            try {
                if (elem instanceof SimpleType) {
                    t = new javax.management.openmbean.ArrayType<Object>(
                            (SimpleType<?>) elem, this.componente.isPrimitive());
                } else if (elem != null) {
                    t = new javax.management.openmbean.ArrayType<Object>(1, elem);
                }
            } catch (OpenDataException e) {
                throw new IllegalArgumentException(
                        arreglo.getName() + " no se puede mapear: " + e.getMessage());
            }
            this.tipo = t;
        }

        OpenType<?> tipoAbierto() {
            return this.tipo;
        }

        // Los dos recorridos se leen y se escriben como `Object[]`, y eso **no** es un atajo: si la
        // conversion no es la identidad, el componente es una enumeracion, un compuesto o un
        // arreglo, y los tres son tipos de referencia. Un componente primitivo siempre mapea a si
        // mismo y sale por el `esIdentidad` de arriba sin tocar nada. Un `Array.get`/`Array.set`
        // andaria igual --ya andan-- pero pasarian por un boxeo por elemento que aca no hace falta.
        Object aAbierto(Object v) throws OpenDataException {
            if (v == null || this.mapeoDelComponente.esIdentidad()) {
                return v;
            }
            Object[] entrada = (Object[]) v;
            // El arreglo de salida es del tipo que el elemento convertido necesita, no del de
            // entrada: una enumeracion entra como `Color[]` y sale como `String[]`.
            Object[] salida = (Object[]) Array.newInstance(
                    MXMapeo.claseAbierta(this.componente), entrada.length);
            for (int i = 0; i < entrada.length; i++) {
                salida[i] = this.mapeoDelComponente.aAbierto(entrada[i]);
            }
            return salida;
        }

        Object aJava(Object v) throws OpenDataException {
            if (v == null || this.mapeoDelComponente.esIdentidad()) {
                return v;
            }
            Object[] entrada = (Object[]) v;
            Object[] salida = (Object[]) Array.newInstance(this.componente, entrada.length);
            for (int i = 0; i < entrada.length; i++) {
                salida[i] = this.mapeoDelComponente.aJava(entrada[i]);
            }
            return salida;
        }
    }

    /**
     * La clase Java del lado <b>abierto</b> de un tipo, para poder alocar el arreglo destino.
     *
     * <p>Recursiva por los arreglos de arreglos: el lado abierto de un {@code Color[][]} es un
     * {@code String[][]}, y se arma alocando uno de largo cero y preguntandole su clase, que es la
     * unica forma de nombrar un tipo de arreglo que no se conoce al compilar.
     */
    private static Class<?> claseAbierta(Class<?> javaType) {
        if (javaType.isArray()) {
            return Array.newInstance(MXMapeo.claseAbierta(javaType.getComponentType()), 0)
                    .getClass();
        }
        MXMapeo m = MXMapeo.de(javaType);
        if (m.esIdentidad()) {
            return javaType;
        }
        if (m instanceof DeEnum) {
            return String.class;
        }
        return CompositeData.class;
    }

    /**
     * Una interfaz o clase con getters viaja como {@link CompositeData}.
     *
     * <p>La lista de items sale de los getters, en orden alfabetico. No es cosmetico: dos
     * {@code CompositeType} con los mismos items en distinto orden son el mismo tipo, pero el orden
     * estable hace que el `toString` y las pruebas no dependan de en que orden vino la reflexion.
     *
     * <p>El item se llama como la <b>propiedad</b> y no como el getter: {@code getNombreLargo} da
     * {@code nombreLargo}, con la primera letra en minuscula, salvo que las dos primeras ya sean
     * mayusculas ({@code getURL} da {@code URL}). Es la regla de JavaBeans, y se comprobo contra el
     * JDK 25 -- que para un {@code MemoryUsage} entrega {@code committed}, {@code init},
     * {@code max}, {@code used}, todos en minuscula. Equivocarse aca da un {@code CompositeData}
     * que ningun cliente de verdad puede leer.
     */
    private static final class DeCompuesto extends MXMapeo {

        private final Class<?> clase;
        private final String[] nombres;
        private final Method[] getters;
        private final MXMapeo[] mapeos;
        private final CompositeType tipo;

        /** El `from` estatico, o null. */
        private final Method desde;

        /** El constructor anotado, y el orden en que toma las propiedades; null si no hay. */
        private final Constructor<?> ctor;
        private final String[] ordenDelCtor;

        static MXMapeo crear(Class<?> c) {
            if (c.isPrimitive()) {
                throw new IllegalArgumentException(c.getName() + " no se puede mapear");
            }
            List<String> nombres = new ArrayList<String>();
            List<Method> getters = new ArrayList<Method>();
            for (Method m : c.getMethods()) {
                if (m.getDeclaringClass() == Object.class || m.getParameterTypes().length != 0
                        || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                String n = m.getName();
                String prop;
                if (n.startsWith("get") && n.length() > 3 && m.getReturnType() != Void.TYPE) {
                    prop = DeCompuesto.descapitalizar(n.substring(3));
                } else if (n.startsWith("is") && n.length() > 2
                        && m.getReturnType() == Boolean.TYPE) {
                    prop = DeCompuesto.descapitalizar(n.substring(2));
                } else {
                    continue;
                }
                if (!nombres.contains(prop)) {
                    nombres.add(prop);
                    getters.add(m);
                }
            }
            if (nombres.isEmpty()) {
                throw new IllegalArgumentException(c.getName()
                        + " no se puede mapear: no es un tipo simple ni tiene getters, asi que no"
                        + " hay con que armar un CompositeData");
            }
            // Alfabetico, que es el orden que fija la especificacion.
            for (int i = 1; i < nombres.size(); i++) {
                for (int j = i; j > 0 && nombres.get(j).compareTo(nombres.get(j - 1)) < 0; j--) {
                    String sn = nombres.get(j);
                    nombres.set(j, nombres.get(j - 1));
                    nombres.set(j - 1, sn);
                    Method sm = getters.get(j);
                    getters.set(j, getters.get(j - 1));
                    getters.set(j - 1, sm);
                }
            }
            return new DeCompuesto(c, nombres, getters);
        }

        private DeCompuesto(Class<?> c, List<String> nombres, List<Method> getters) {
            this.clase = c;
            this.nombres = nombres.toArray(new String[0]);
            this.getters = getters.toArray(new Method[0]);
            this.mapeos = new MXMapeo[this.nombres.length];
            OpenType<?>[] tipos = new OpenType<?>[this.nombres.length];
            String[] descripciones = new String[this.nombres.length];
            for (int i = 0; i < this.nombres.length; i++) {
                this.mapeos[i] = MXMapeo.de(this.getters[i].getReturnType());
                tipos[i] = this.mapeos[i].tipoAbierto();
                if (tipos[i] == null) {
                    throw new IllegalArgumentException(c.getName() + "." + this.nombres[i]
                            + " no se puede mapear: su tipo no tiene un tipo abierto fijo");
                }
                descripciones[i] = this.nombres[i];
            }
            try {
                this.tipo = new CompositeType(c.getName(), c.getName(), this.nombres, descripciones,
                                              tipos);
            } catch (OpenDataException e) {
                throw new IllegalArgumentException(c.getName() + " no se puede mapear: "
                        + e.getMessage());
            }
            this.desde = DeCompuesto.buscarFrom(c);
            Constructor<?> encontrado = null;
            String[] orden = null;
            if (this.desde == null) {
                for (Constructor<?> k : c.getConstructors()) {
                    ConstructorProperties a = k.getAnnotation(ConstructorProperties.class);
                    if (a != null && a.value().length == k.getParameterTypes().length) {
                        encontrado = k;
                        orden = a.value();
                        break;
                    }
                }
            }
            this.ctor = encontrado;
            this.ordenDelCtor = orden;
            if (this.desde == null && this.ctor == null && !c.isInterface()) {
                throw new IllegalArgumentException(c.getName()
                        + " no se puede reconstruir desde un CompositeData: no tiene un"
                        + " `public static " + c.getSimpleName() + " from(CompositeData)` ni un"
                        + " constructor con @ConstructorProperties, y no es una interfaz");
            }
        }

        private static Method buscarFrom(Class<?> c) {
            try {
                Method m = c.getMethod("from", CompositeData.class);
                if (Modifier.isStatic(m.getModifiers()) && c.isAssignableFrom(m.getReturnType())) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
                // No lo tiene; se prueba el constructor anotado.
            }
            return null;
        }

        OpenType<?> tipoAbierto() {
            return this.tipo;
        }

        Object aAbierto(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            Object[] valores = new Object[this.nombres.length];
            for (int i = 0; i < this.nombres.length; i++) {
                try {
                    valores[i] = this.mapeos[i].aAbierto(this.getters[i].invoke(v));
                } catch (Exception e) {
                    throw new OpenDataException("no se pudo leer " + this.clase.getName() + "."
                            + this.nombres[i] + ": " + e);
                }
            }
            return new CompositeDataSupport(this.tipo, this.nombres, valores);
        }

        Object aJava(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            CompositeData cd = (CompositeData) v;
            if (this.desde != null) {
                try {
                    return this.desde.invoke(null, cd);
                } catch (Exception e) {
                    throw new OpenDataException(this.clase.getName() + ".from fallo: " + e);
                }
            }
            if (this.ctor != null) {
                Object[] args = new Object[this.ordenDelCtor.length];
                Class<?>[] tiposDelCtor = this.ctor.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    // `@ConstructorProperties` nombra las propiedades igual que los items: en la
                    // forma de JavaBeans. No hay nada que traducir entre las dos.
                    args[i] = MXMapeo.de(tiposDelCtor[i]).aJava(cd.get(this.ordenDelCtor[i]));
                }
                try {
                    return this.ctor.newInstance(args);
                } catch (Exception e) {
                    throw new OpenDataException("no se pudo construir " + this.clase.getName()
                            + ": " + e);
                }
            }
            // Una interfaz: un proxy sobre el CompositeData. Es lo que hace el JDK, y es lo unico
            // que se puede hacer -- no hay implementacion que instanciar.
            return Proxy.newProxyInstance(this.clase.getClassLoader(),
                                          new Class<?>[] {this.clase},
                                          new CompositeDataInvocationHandler(cd));
        }

        // La regla de JavaBeans: minuscula la primera, salvo que las dos primeras sean mayusculas
        // --que es lo que salva a los acronimos, `getURL` da `URL` y no `uRL`--.
        private static String descapitalizar(String s) {
            if (s.length() == 0) {
                return s;
            }
            char c0 = s.charAt(0);
            if (s.length() > 1) {
                char c1 = s.charAt(1);
                if (c0 >= 'A' && c0 <= 'Z' && c1 >= 'A' && c1 <= 'Z') {
                    return s;
                }
            }
            if (c0 >= 'A' && c0 <= 'Z') {
                return ("" + (char) (c0 - 'A' + 'a')) + s.substring(1);
            }
            return s;
        }
    }
}
