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
 * The translation between a Java type and its <b>open type</b>, which is what defines an MXBean.
 *
 * <p>A normal MBean sends the Java objects down the wire as they are, which is why it only works if
 * the client has the same classes. An MXBean does not: it converts everything to the types of
 * {@link javax.management.openmbean} --numbers, strings, {@link CompositeData},
 * {@link TabularData}-- which any client can read without knowing a single class of the server.
 * That conversion is this class.
 *
 * <h2>What is mapped, and what is not</h2>
 *
 * <p>What the specification calls mappable and this tree can resolve is mapped:
 *
 * <ul>
 *   <li>the <b>simple types</b> --primitives and their wrappers, {@code String}, {@code
 *       BigDecimal}, {@code BigInteger}, {@code Date}, {@link ObjectName}-- which map to
 *       themselves;
 *   <li>the types that <b>are already open</b> ({@code CompositeData}, {@code TabularData}), also
 *       to themselves;
 *   <li><b>enumerations</b>, which go and come back as the constant's name;
 *   <li><b>arrays</b>, element by element and in any dimension;
 *   <li><b>interfaces and classes with getters</b>, which go as {@code CompositeData} with one item
 *       per property.
 * </ul>
 *
 * <p><b>{@code List<E>} and {@code Map<K,V>} are not mapped</b>, and the reason is not that the
 * rule is hard --{@code List<E>} is an array of E, {@code Map<K,V>} is a {@code TabularData}-- but
 * that <b>you need to know what E is</b>, and on this VM you cannot: {@code getGenericReturnType()}
 * returns the <b>raw</b> type, so a {@code List<String>} arrives indistinguishable from a {@code
 * List<ObjectName>}. Guessing would mean sending garbage on the first element that is not of the
 * assumed type.
 *
 * <p>That is why it is rejected with {@link IllegalArgumentException} <b>when the proxy is
 * built</b> and not when it is used: it is the same moment the JDK rejects an interface that is not
 * a valid MXBean, and it is the moment when whoever writes the code can do something about it. A
 * made-up value is never returned.
 *
 * <h2>How a {@code CompositeData} is rebuilt</h2>
 *
 * <p>Going out is easy --each getter is called-- and coming back there are three paths, tried in
 * the order the specification dictates: a {@code public static T from(CompositeData)}, a
 * constructor annotated with {@link ConstructorProperties}, or --if the type is an
 * <b>interface</b>-- a proxy over the {@code CompositeData}, which is what the JDK does and what
 * {@link CompositeDataInvocationHandler} exists for.
 */
abstract class MXMapping {

    /** The open type that corresponds to it. */
    abstract OpenType<?> openType();

    /** From Java to open. */
    abstract Object toOpen(Object v) throws OpenDataException;

    /** From open to Java. */
    abstract Object toJava(Object v) throws OpenDataException;

    /** Whether the conversion is the identity; it serves to skip it altogether. */
    boolean isIdentity() {
        return false;
    }

    // The mappings already resolved. A composite type looks at itself when it has a property of its
    // own type, so the table also cuts that recursion.
    private static final Map<Class<?>, MXMapping> CACHE = new HashMap<Class<?>, MXMapping>();

    /**
     * The mapping of that type.
     *
     * @throws IllegalArgumentException if the type cannot be mapped; the message says which and why
     */
    static synchronized MXMapping de(Class<?> c) {
        MXMapping m = CACHE.get(c);
        if (m != null) {
            return m;
        }
        m = construct(c);
        CACHE.put(c, m);
        return m;
    }

    private static MXMapping construct(Class<?> c) {
        if (c == null) {
            throw new IllegalArgumentException("The type cannot be null");
        }
        SimpleType<?> s = MXMapping.simple(c);
        if (s != null) {
            return new IdentityMapping(s);
        }
        if (CompositeData.class.isAssignableFrom(c) || TabularData.class.isAssignableFrom(c)) {
            // It is already an open type: there is nothing to convert. The exact open type is not
            // known without a value, and it is not needed -- this branch never describes an item of
            // a CompositeType.
            return new IdentityMapping(null);
        }
        if (c.isEnum()) {
            return new EnumMapping(c);
        }
        if (c.isArray()) {
            return new ArrayMapping(c);
        }
        if (Map.class.isAssignableFrom(c) || List.class.isAssignableFrom(c)
                || java.util.Set.class.isAssignableFrom(c)) {
            throw new IllegalArgumentException(
                    c.getName() + " cannot be mapped: the type of its elements would be needed,"
                    + " and this VM does not expose type arguments (getGenericReturnType gives the"
                    + " raw type). See the MXMapping note.");
        }
        if (c.isInterface() && JMX.isMXBeanInterface(c)) {
            // The specification says an MXBean interface named inside another is a *reference*: it
            // travels as the `ObjectName` of the MBean that implements it, not as its data.
            // Converting it to `CompositeData` would mean sending a copy where the contract
            // promises a pointer, and whoever receives it would believe they are looking at the
            // live object.
            //
            // Resolving an `ObjectName` both ways needs the server's registry, which a proxy does
            // not have. It is rejected instead of approximated; it was checked against the JDK 25
            // that the correct mapping is the `ObjectName`.
            throw new IllegalArgumentException(c.getName()
                    + " is an MXBean interface, a reference to another MBean: it travels as its"
                    + " ObjectName, and resolving it needs the server registry, which a proxy does not"
                    + " have");
        }
        return CompositeMapping.create(c);
    }

    /** The {@link SimpleType} of that type, or null if it is not one of the simple ones. */
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

    // ---- the four forms ----------------------------------------------------------------------

    /** The value travels as is. */
    private static final class IdentityMapping extends MXMapping {

        private final OpenType<?> type;

        IdentityMapping(OpenType<?> type) {
            this.type = type;
        }

        OpenType<?> openType() {
            return this.type;
        }

        Object toOpen(Object v) {
            return v;
        }

        Object toJava(Object v) {
            return v;
        }

        boolean isIdentity() {
            return true;
        }
    }

    /** An enumeration travels as the name of its constant. */
    private static final class EnumMapping extends MXMapping {

        private final Class<?> cls;

        EnumMapping(Class<?> cls) {
            this.cls = cls;
        }

        OpenType<?> openType() {
            return SimpleType.STRING;
        }

        Object toOpen(Object v) {
            return v == null ? null : ((Enum<?>) v).name();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Object toJava(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            try {
                return Enum.valueOf((Class<Enum>) this.cls, (String) v);
            } catch (IllegalArgumentException e) {
                // A name the enumeration does not have. It is an error of the data, not of the
                // mapping, which is why it comes out as `OpenDataException` and not as the
                // `IllegalArgumentException` that means "this type cannot be mapped".
                throw new OpenDataException("'" + v + "' is not a constant of "
                        + this.cls.getName());
            }
        }
    }

    /** An array travels as an array of whatever its elements travel as. */
    private static final class ArrayMapping extends MXMapping {

        private final Class<?> component;
        private final MXMapping componentMapping;
        private final OpenType<?> type;

        ArrayMapping(Class<?> array) {
            this.component = array.getComponentType();
            this.componentMapping = MXMapping.de(this.component);
            OpenType<?> elem = this.componentMapping.openType();
            OpenType<?> t = null;
            try {
                if (elem instanceof SimpleType) {
                    t = new javax.management.openmbean.ArrayType<Object>(
                            (SimpleType<?>) elem, this.component.isPrimitive());
                } else if (elem != null) {
                    t = new javax.management.openmbean.ArrayType<Object>(1, elem);
                }
            } catch (OpenDataException e) {
                throw new IllegalArgumentException(
                        array.getName() + " cannot be mapped: " + e.getMessage());
            }
            this.type = t;
        }

        OpenType<?> openType() {
            return this.type;
        }

        // Both walks are read and written as `Object[]`, and that is *not* a shortcut: if the
        // conversion is not the identity, the component is an enumeration, a composite or an array,
        // and all three are reference types. A primitive component always maps to itself and leaves
        // through the `isIdentity` above without touching anything. `Array.get`/`Array.set` would
        // work too --they do work-- but they would go through a boxing per element not needed here.
        Object toOpen(Object v) throws OpenDataException {
            if (v == null || this.componentMapping.isIdentity()) {
                return v;
            }
            Object[] entry = (Object[]) v;
            // The output array is of the type the converted element needs, not of the input's: an
            // enumeration comes in as `Color[]` and goes out as `String[]`.
            Object[] result = (Object[]) Array.newInstance(
                    MXMapping.openClass(this.component), entry.length);
            for (int i = 0; i < entry.length; i++) {
                result[i] = this.componentMapping.toOpen(entry[i]);
            }
            return result;
        }

        Object toJava(Object v) throws OpenDataException {
            if (v == null || this.componentMapping.isIdentity()) {
                return v;
            }
            Object[] entry = (Object[]) v;
            Object[] result = (Object[]) Array.newInstance(this.component, entry.length);
            for (int i = 0; i < entry.length; i++) {
                result[i] = this.componentMapping.toJava(entry[i]);
            }
            return result;
        }
    }

    /**
     * The Java class of the <b>open</b> side of a type, to be able to allocate the target array.
     *
     * <p>Recursive because of arrays of arrays: the open side of a {@code Color[][]} is a {@code
     * String[][]}, and it is built by allocating one of length zero and asking its class, which is
     * the only way to name an array type not known at compile time.
     */
    private static Class<?> openClass(Class<?> javaType) {
        if (javaType.isArray()) {
            return Array.newInstance(MXMapping.openClass(javaType.getComponentType()), 0)
                    .getClass();
        }
        MXMapping m = MXMapping.de(javaType);
        if (m.isIdentity()) {
            return javaType;
        }
        if (m instanceof EnumMapping) {
            return String.class;
        }
        return CompositeData.class;
    }

    /**
     * An interface or class with getters travels as {@link CompositeData}.
     *
     * <p>The list of items comes from the getters, in alphabetical order. It is not cosmetic: two
     * {@code CompositeType}s with the same items in a different order are the same type, but the
     * stable order keeps {@code toString} and the tests from depending on the order reflection came
     * in.
     *
     * <p>The item is named after the <b>property</b> and not after the getter: {@code getLongName}
     * gives {@code longName}, with the first letter in lower case, unless the first two are already
     * upper case ({@code getURL} gives {@code URL}). It is the JavaBeans rule, and it was checked
     * against the JDK 25 -- which for a {@code MemoryUsage} gives {@code committed}, {@code init},
     * {@code max}, {@code used}, all lower case. Getting it wrong here gives a {@code
     * CompositeData} no real client can read.
     */
    private static final class CompositeMapping extends MXMapping {

        private final Class<?> cls;
        private final String[] names;
        private final Method[] getters;
        private final MXMapping[] mappings;
        private final CompositeType type;

        /** The static {@code from}, or null. */
        private final Method from;

        /**
         * The annotated constructor, and the order it takes the properties in; null if there is
         * none.
         */
        private final Constructor<?> ctor;
        private final String[] ctorOrder;

        static MXMapping create(Class<?> c) {
            if (c.isPrimitive()) {
                throw new IllegalArgumentException(c.getName() + " cannot be mapped");
            }
            List<String> names = new ArrayList<String>();
            List<Method> getters = new ArrayList<Method>();
            for (Method m : c.getMethods()) {
                if (m.getDeclaringClass() == Object.class || m.getParameterTypes().length != 0
                        || Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                String n = m.getName();
                String prop;
                if (n.startsWith("get") && n.length() > 3 && m.getReturnType() != Void.TYPE) {
                    prop = CompositeMapping.decapitalize(n.substring(3));
                } else if (n.startsWith("is") && n.length() > 2
                        && m.getReturnType() == Boolean.TYPE) {
                    prop = CompositeMapping.decapitalize(n.substring(2));
                } else {
                    continue;
                }
                if (!names.contains(prop)) {
                    names.add(prop);
                    getters.add(m);
                }
            }
            if (names.isEmpty()) {
                throw new IllegalArgumentException(c.getName()
                        + " cannot be mapped: not a simple type and no getters, so there is"
                        + " nothing to build a CompositeData from");
            }
            // Alphabetical, which is the order the specification fixes.
            for (int i = 1; i < names.size(); i++) {
                for (int j = i; j > 0 && names.get(j).compareTo(names.get(j - 1)) < 0; j--) {
                    String sn = names.get(j);
                    names.set(j, names.get(j - 1));
                    names.set(j - 1, sn);
                    Method sm = getters.get(j);
                    getters.set(j, getters.get(j - 1));
                    getters.set(j - 1, sm);
                }
            }
            return new CompositeMapping(c, names, getters);
        }

        private CompositeMapping(Class<?> c, List<String> names, List<Method> getters) {
            this.cls = c;
            this.names = names.toArray(new String[0]);
            this.getters = getters.toArray(new Method[0]);
            this.mappings = new MXMapping[this.names.length];
            OpenType<?>[] types = new OpenType<?>[this.names.length];
            String[] descriptions = new String[this.names.length];
            for (int i = 0; i < this.names.length; i++) {
                this.mappings[i] = MXMapping.de(this.getters[i].getReturnType());
                types[i] = this.mappings[i].openType();
                if (types[i] == null) {
                    throw new IllegalArgumentException(c.getName() + "." + this.names[i]
                            + " cannot be mapped: its type has no fixed open type");
                }
                descriptions[i] = this.names[i];
            }
            try {
                this.type = new CompositeType(c.getName(), c.getName(), this.names, descriptions,
                                              types);
            } catch (OpenDataException e) {
                throw new IllegalArgumentException(c.getName() + " cannot be mapped: "
                        + e.getMessage());
            }
            this.from = CompositeMapping.findFrom(c);
            Constructor<?> found = null;
            String[] orden = null;
            if (this.from == null) {
                for (Constructor<?> k : c.getConstructors()) {
                    ConstructorProperties a = k.getAnnotation(ConstructorProperties.class);
                    if (a != null && a.value().length == k.getParameterTypes().length) {
                        found = k;
                        orden = a.value();
                        break;
                    }
                }
            }
            this.ctor = found;
            this.ctorOrder = orden;
            if (this.from == null && this.ctor == null && !c.isInterface()) {
                throw new IllegalArgumentException(c.getName()
                        + " cannot be rebuilt from a CompositeData: it has no"
                        + " `public static " + c.getSimpleName() + " from(CompositeData)` nor a"
                        + " constructor with @ConstructorProperties, and it is not an interface");
            }
        }

        private static Method findFrom(Class<?> c) {
            try {
                Method m = c.getMethod("from", CompositeData.class);
                if (Modifier.isStatic(m.getModifiers()) && c.isAssignableFrom(m.getReturnType())) {
                    return m;
                }
            } catch (NoSuchMethodException e) {
                // It does not have one; try the annotated constructor.
            }
            return null;
        }

        OpenType<?> openType() {
            return this.type;
        }

        Object toOpen(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            Object[] values = new Object[this.names.length];
            for (int i = 0; i < this.names.length; i++) {
                try {
                    values[i] = this.mappings[i].toOpen(this.getters[i].invoke(v));
                } catch (Exception e) {
                    throw new OpenDataException("could not read " + this.cls.getName() + "."
                            + this.names[i] + ": " + e);
                }
            }
            return new CompositeDataSupport(this.type, this.names, values);
        }

        Object toJava(Object v) throws OpenDataException {
            if (v == null) {
                return null;
            }
            CompositeData cd = (CompositeData) v;
            if (this.from != null) {
                try {
                    return this.from.invoke(null, cd);
                } catch (Exception e) {
                    throw new OpenDataException(this.cls.getName() + ".from fallo: " + e);
                }
            }
            if (this.ctor != null) {
                Object[] args = new Object[this.ctorOrder.length];
                Class<?>[] ctorTypes = this.ctor.getParameterTypes();
                for (int i = 0; i < args.length; i++) {
                    // `@ConstructorProperties` names the properties the same way as the items: in
                    // the JavaBeans form. There is nothing to translate between the two.
                    args[i] = MXMapping.de(ctorTypes[i]).toJava(cd.get(this.ctorOrder[i]));
                }
                try {
                    return this.ctor.newInstance(args);
                } catch (Exception e) {
                    throw new OpenDataException("could not build " + this.cls.getName()
                            + ": " + e);
                }
            }
            // An interface: a proxy over the CompositeData. It is what the JDK does, and it is the
            // only thing that can be done -- there is no implementation to instantiate.
            return Proxy.newProxyInstance(this.cls.getClassLoader(),
                                          new Class<?>[] {this.cls},
                                          new CompositeDataInvocationHandler(cd));
        }

        // The JavaBeans rule: lower case the first, unless the first two are upper case --which is
        // what saves acronyms, `getURL` gives `URL` and not `uRL`.
        private static String decapitalize(String s) {
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
