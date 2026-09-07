package java.beans;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// It discovers a bean's shape by looking at its public methods: which properties it has, which
// events it fires, and which methods are worth exposing.
//
// Every rule below is CHECKED against the real JDK (H:/jdk-25.0.2) with a bean touching each
// doubtful case at once; they are not deductions from the written contract. The ones most often
// implemented wrongly:
//
//  - **decapitalize does NOT always lower the first letter.** If the first two are capitals the name
//    is left as it stands: `getURL` gives the property "URL", not "uRL". `getXCoord` gives "XCoord",
//    because X and C are both capitals. `getX` does give "x".
//  - **`is` counts only for the primitive boolean.** `isWrapped()` returning `Boolean` is NOT a
//    getter; the property ends up write-only if it has a setter.
//  - **The setter has to return void.** `String setOdd(String)` does not hook on, and the property
//    "odd" does not exist at all.
//  - **A setter whose type does not match is discarded, and the getter wins.**
//    `getMismatched():String` with `setMismatched(int)` gives a read-only String property, not two
//    properties and not an error.
//  - **The static ones do not count.** A `public static String getStatic()` produces no property.
//  - **A purely indexed property has a null type.** See IndexedPropertyDescriptor.
//  - **The properties come out sorted by name.**
//  - **The "class" property appears** (from Object.getClass) unless it is cut off with a stopClass.
//  - `bound` is turned on when the bean has addPropertyChangeListener; `constrained` is NOT worked
//    out from addVetoableChangeListener.
//
// **The annotations are NOT read.** `@BeanProperty`, `@JavaBean` and `@Transient` are declared in
// the package, but this Introspector does not consult them, and it is deliberate: in this tree the
// javac loses `@Retention(RUNTIME)` when the annotation type comes from the classpath, so at run
// time they are not seen; and besides, `Method.invoke` on an annotation's instance topples the VM.
// An Introspector that claimed to read them and could not would give incomplete descriptors without
// saying so, which is worse than not reading them and saying so.
public class Introspector {

    public static final int USE_ALL_BEANINFO = 1;
    public static final int IGNORE_IMMEDIATE_BEANINFO = 2;
    public static final int IGNORE_ALL_BEANINFO = 3;

    private static String[] searchPath = new String[] { "sun.beans.infos" };

    // A cache of what has already been introspected, with the key including stopClass and flags:
    // the same bean cut off at a different place gives a different result.
    private static Map<String, BeanInfo> cache = new HashMap<String, BeanInfo>();

    private Introspector() {
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass) throws IntrospectionException {
        return getBeanInfo(beanClass, null, USE_ALL_BEANINFO);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, int flags) throws IntrospectionException {
        return getBeanInfo(beanClass, null, flags);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass)
            throws IntrospectionException {
        return getBeanInfo(beanClass, stopClass, USE_ALL_BEANINFO);
    }

    public static BeanInfo getBeanInfo(Class<?> beanClass, Class<?> stopClass, int flags)
            throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("null bean class");
        }
        String key = beanClass.getName() + "|" + (stopClass == null ? "-" : stopClass.getName()) + "|" + flags;
        BeanInfo bi = readCache(key);
        if (bi == null) {
            bi = analyse(beanClass, stopClass, flags);
            storeCache(key, bi);
        }
        return bi;
    }

    private static synchronized BeanInfo readCache(String key) {
        return cache.get(key);
    }

    private static synchronized void storeCache(String key, BeanInfo bi) {
        cache.put(key, bi);
    }

    public static synchronized void flushCaches() {
        cache.clear();
    }

    // It throws away a class's entry. The key carries the name in front, so every combination of
    // stopClass/flags for that class is swept away.
    public static synchronized void flushFromCaches(Class<?> clz) {
        if (clz == null) {
            throw new NullPointerException();
        }
        String prefijo = clz.getName() + "|";
        Object[] keys = cache.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            String c = (String) keys[i];
            if (c.startsWith(prefijo)) {
                cache.remove(c);
            }
        }
    }

    public static synchronized String[] getBeanInfoSearchPath() {
        String[] r = new String[searchPath.length];
        for (int i = 0; i < searchPath.length; i++) {
            r[i] = searchPath[i];
        }
        return r;
    }

    public static synchronized void setBeanInfoSearchPath(String[] path) {
        if (path == null) {
            searchPath = new String[0];
        } else {
            String[] r = new String[path.length];
            for (int i = 0; i < path.length; i++) {
                r[i] = path[i];
            }
            searchPath = r;
        }
    }

    // The naming rule nearly everyone implements wrongly. Checked against the real JDK:
    //   "URL" -> "URL"      "Name"   -> "name"    "XCoord" -> "XCoord"
    //   "X"   -> "x"        "aB"     -> "aB"      "ABc"    -> "ABc"
    // The odd case --two capitals at the start are left alone-- exists so that acronyms survive:
    // lowering only "URL"'s first letter would give "uRL", which is nobody's name.
    public static String decapitalize(String name) {
        String r = name;
        if (name != null && name.length() != 0) {
            boolean twoCapitals = name.length() > 1
                && Character.isUpperCase(name.charAt(1))
                && Character.isUpperCase(name.charAt(0));
            if (!twoCapitals) {
                char[] c = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                r = new String(c);
            }
        }
        return r;
    }

    // --- the analysis ---------------------------------------------------------------

    private static BeanInfo analyse(Class<?> beanClass, Class<?> stopClass, int flags)
            throws IntrospectionException {

        // The methods to consider: the public instance ones declared between beanClass and
        // stopClass (stopClass excluded). With no stopClass, Object's come in too, which is where
        // the "class" property comes from.
        List<Method> methods = visibleMethods(beanClass, stopClass);

        List<PropertyDescriptor> props = discoverProperties(methods, beanClass);
        List<EventSetDescriptor> eventSets = discoverEvents(methods);

        MethodDescriptor[] mds = new MethodDescriptor[methods.size()];
        for (int i = 0; i < methods.size(); i++) {
            mds[i] = new MethodDescriptor(methods.get(i));
        }

        PropertyDescriptor[] pds = new PropertyDescriptor[props.size()];
        for (int i = 0; i < props.size(); i++) {
            pds[i] = props.get(i);
        }
        EventSetDescriptor[] esds = new EventSetDescriptor[eventSets.size()];
        for (int i = 0; i < eventSets.size(); i++) {
            esds[i] = eventSets.get(i);
        }

        BeanInfo explicit = findExplicitBeanInfo(beanClass, flags);
        return new GenericBeanInfo(new BeanDescriptor(beanClass), pds, esds, mds, explicit);
    }

    // It walks the hierarchy from beanClass upwards, stopping at stopClass. The declared ones of
    // each level are taken and filtered down to the public instance ones; that way an overridden
    // method appears once only, with the most derived version.
    private static List<Method> visibleMethods(Class<?> beanClass, Class<?> stopClass) {
        List<Method> salida = new ArrayList<Method>();
        List<String> vistos = new ArrayList<String>();
        Class<?> c = beanClass;
        while (c != null && c != stopClass) {
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                int mods = m.getModifiers();
                if (Modifier.isPublic(mods) && !Modifier.isStatic(mods) && !m.isSynthetic()) {
                    String firma = signatureOf(m);
                    if (!vistos.contains(firma)) {
                        vistos.add(firma);
                        salida.add(m);
                    }
                }
            }
            c = c.getSuperclass();
        }
        return salida;
    }

    private static String signatureOf(Method m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getName()).append('(');
        Class<?>[] args = m.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            sb.append(args[i].getName()).append(',');
        }
        sb.append(')');
        return sb.toString();
    }

    // The discovery of properties: each method is classified into one of the four roles and the
    // descriptors are only built at the end, because a property's type depends on having seen them
    // all.
    private static List<PropertyDescriptor> discoverProperties(List<Method> methods, Class<?> beanClass)
            throws IntrospectionException {

        List<String> names = new ArrayList<String>();
        List<Method> readers = new ArrayList<Method>();
        List<Method> writers = new ArrayList<Method>();
        List<Method> indexedReaders = new ArrayList<Method>();
        List<Method> indexedWriters = new ArrayList<Method>();

        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);
            String methodName = m.getName();
            Class<?>[] args = m.getParameterTypes();
            Class<?> ret = m.getReturnType();

            String prop = null;
            int rol = -1;   // 0 lector, 1 escritor, 2 lector indexado, 3 escritor indexado

            if (methodName.startsWith("get") && methodName.length() > 3) {
                if (args.length == 0 && ret != void.class) {
                    prop = decapitalize(methodName.substring(3));
                    rol = 0;
                } else if (args.length == 1 && args[0] == int.class && ret != void.class) {
                    prop = decapitalize(methodName.substring(3));
                    rol = 2;
                }
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                // Only the primitive boolean. `Boolean` does NOT qualify: checked against the
                // real JDK.
                if (args.length == 0 && ret == boolean.class) {
                    prop = decapitalize(methodName.substring(2));
                    rol = 0;
                }
            } else if (methodName.startsWith("set") && methodName.length() > 3) {
                // The writer has to return void: checked against the real JDK.
                if (ret == void.class) {
                    if (args.length == 1) {
                        prop = decapitalize(methodName.substring(3));
                        rol = 1;
                    } else if (args.length == 2 && args[0] == int.class) {
                        prop = decapitalize(methodName.substring(3));
                        rol = 3;
                    }
                }
            }

            if (prop != null && prop.length() > 0) {
                int idx = names.indexOf(prop);
                if (idx < 0) {
                    names.add(prop);
                    readers.add(null);
                    writers.add(null);
                    indexedReaders.add(null);
                    indexedWriters.add(null);
                    idx = names.size() - 1;
                }
                if (rol == 0 && readers.get(idx) == null) {
                    readers.set(idx, m);
                } else if (rol == 1 && writers.get(idx) == null) {
                    writers.set(idx, m);
                } else if (rol == 2 && indexedReaders.get(idx) == null) {
                    indexedReaders.set(idx, m);
                } else if (rol == 3 && indexedWriters.get(idx) == null) {
                    indexedWriters.set(idx, m);
                }
            }
        }

        boolean ligadas = PropertyDescriptor.findMethod(beanClass, "addPropertyChangeListener", 1) != null;

        List<PropertyDescriptor> salida = new ArrayList<PropertyDescriptor>();
        for (int i = 0; i < names.size(); i++) {
            PropertyDescriptor pd = buildDescriptor(names.get(i), readers.get(i), writers.get(i),
                                          indexedReaders.get(i), indexedWriters.get(i));
            if (pd != null) {
                pd.setBound(ligadas);
                salida.add(pd);
            }
        }
        sortByName(salida);
        return salida;
    }

    // It builds a property's descriptor by reconciling the four possible accessors. The pairing
    // rules live here, which are the ones that decide whether something is a property and of what
    // type.
    private static PropertyDescriptor buildDescriptor(String propName, Method lector, Method escritor,
                                            Method indexedReader, Method indexedWriter) {
        PropertyDescriptor pd = null;

        // The non-indexed type is set by the reader if it is there; if not, by the writer.
        Method l = lector;
        Method e = escritor;
        if (l != null && e != null) {
            // Types that do not add up: the writer is discarded and the reader wins. Checked
            // against the real JDK with getMismatched():String / setMismatched(int).
            if (l.getReturnType() != e.getParameterTypes()[0]) {
                e = null;
            }
        }

        boolean hayIdx = indexedReader != null || indexedWriter != null;
        if (hayIdx) {
            Class<?> indexedType = null;
            Method li = indexedReader;
            Method ei = indexedWriter;
            if (li != null) {
                indexedType = li.getReturnType();
            }
            if (ei != null) {
                Class<?> t = ei.getParameterTypes()[1];
                if (indexedType == null) {
                    indexedType = t;
                } else if (indexedType != t) {
                    ei = null;
                }
            }
            // If there are array accessors too, the component has to match; if not, the
            // non-indexed half does not belong to this property.
            Class<?> arrayType = null;
            if (l != null) {
                arrayType = l.getReturnType();
            } else if (e != null) {
                arrayType = e.getParameterTypes()[0];
            }
            if (arrayType != null) {
                if (!arrayType.isArray() || arrayType.getComponentType() != indexedType) {
                    l = null;
                    e = null;
                }
            }
            pd = new IndexedPropertyDescriptor(propName, l, e, li, ei, true);
        } else if (l != null || e != null) {
            pd = new PropertyDescriptor(propName, l, e, true);
        }
        return pd;
    }

    // Insertion sort: there are few properties and it avoids depending on a sort of Object[],
    // which java.util.Arrays does not offer in this tree (it only has the primitives).
    private static void sortByName(List<PropertyDescriptor> l) {
        for (int i = 1; i < l.size(); i++) {
            PropertyDescriptor current = l.get(i);
            int j = i - 1;
            while (j >= 0 && l.get(j).getName().compareTo(current.getName()) > 0) {
                l.set(j + 1, l.get(j));
                j = j - 1;
            }
            l.set(j + 1, current);
        }
    }

    // The event sets: add/remove pairs taking a listener. The "Listener" suffix in the type's name
    // is compulsory -- checked against the real JDK, an `addBarOyente(BarOyente)` with BarOyente
    // extending EventListener produces nothing.
    private static List<EventSetDescriptor> discoverEvents(List<Method> methods)
            throws IntrospectionException {

        List<String> names = new ArrayList<String>();
        List<Class<?>> kinds = new ArrayList<Class<?>>();
        List<Method> adds = new ArrayList<Method>();
        List<Method> removes = new ArrayList<Method>();

        for (int i = 0; i < methods.size(); i++) {
            Method m = methods.get(i);
            String n = m.getName();
            Class<?>[] args = m.getParameterTypes();
            boolean isAdd = n.startsWith("add") && n.length() > 3;
            boolean isRemove = n.startsWith("remove") && n.length() > 6;
            if ((isAdd || isRemove) && args.length == 1 && m.getReturnType() == void.class) {
                Class<?> kind = args[0];
                if (java.util.EventListener.class.isAssignableFrom(kind)) {
                    String simple = EventSetDescriptor.simpleName(kind);
                    String methodSuffix = isAdd ? n.substring(3) : n.substring(6);
                    if (simple.endsWith("Listener") && simple.equals(methodSuffix)) {
                        String eventName = decapitalize(simple.substring(0, simple.length() - 8));
                        int idx = names.indexOf(eventName);
                        if (idx < 0) {
                            names.add(eventName);
                            kinds.add(kind);
                            adds.add(null);
                            removes.add(null);
                            idx = names.size() - 1;
                        }
                        if (isAdd) {
                            adds.set(idx, m);
                        } else {
                            removes.set(idx, m);
                        }
                    }
                }
            }
        }

        List<EventSetDescriptor> salida = new ArrayList<EventSetDescriptor>();
        for (int i = 0; i < names.size(); i++) {
            // Both are needed: being able to subscribe and not being able to unsubscribe is not a
            // usable event set.
            if (adds.get(i) != null && removes.get(i) != null) {
                Class<?> kind = kinds.get(i);
                Method[] delOyente = methodsOfListener(kind);
                salida.add(new EventSetDescriptor(names.get(i), kind, delOyente,
                                                  adds.get(i), removes.get(i)));
            }
        }
        return salida;
    }

    private static Method[] methodsOfListener(Class<?> kind) {
        Method[] all = kind.getMethods();
        int n = 0;
        for (int i = 0; i < all.length; i++) {
            if (!Modifier.isStatic(all[i].getModifiers())) {
                n = n + 1;
            }
        }
        Method[] r = new Method[n];
        int k = 0;
        for (int i = 0; i < all.length; i++) {
            if (!Modifier.isStatic(all[i].getModifiers())) {
                r[k] = all[i];
                k = k + 1;
            }
        }
        return r;
    }

    // It looks for the `<Bean>BeanInfo` class next to the bean. If it is not there --the normal
    // case-- null is returned and everything comes out of reflection.
    private static BeanInfo findExplicitBeanInfo(Class<?> beanClass, int flags) {
        BeanInfo bi = null;
        if (flags != IGNORE_ALL_BEANINFO && flags != IGNORE_IMMEDIATE_BEANINFO) {
            try {
                Class<?> c = Class.forName(beanClass.getName() + "BeanInfo");
                Object o = c.newInstance();
                if (o instanceof BeanInfo) {
                    bi = (BeanInfo) o;
                }
            } catch (Throwable noHay) {
                bi = null;
            }
        }
        return bi;
    }
}
