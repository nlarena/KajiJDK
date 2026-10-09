package java.beans;

import java.lang.reflect.Method;

// A property that can also be reached element by element: `getData()` returns the whole array and
// `getData(int)` a single element.
//
// The detail nearly everyone implements wrongly, and that is checked here against the real JDK: **a
// purely indexed property has a null propertyType**. If the bean declares `getIdxOnly(int)` and
// `setIdxOnly(int, String)` but no array accessor, the descriptor comes out with
// getPropertyType() == null, getReadMethod() == null and getWriteMethod() == null, and only the
// indexed half filled in. Returning String, or String[], would be lying about an accessor that does
// not exist.
//
// When both pairs ARE there, the indexed type has to be the array type's component: `int[]` against
// `int`. If they do not add up, the property is not valid.
public class IndexedPropertyDescriptor extends PropertyDescriptor {

    private Class<?> indexedPropertyType;
    private Method indexedReadMethod;
    private Method indexedWriteMethod;

    // It looks for the four accessors by convention: get/is + Name, set + Name, and the indexed
    // variants carrying the same name but an int argument in front.
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass)
            throws IntrospectionException {
        this(propertyName, beanClass,
             "get" + capitalize(propertyName),
             "set" + capitalize(propertyName),
             "get" + capitalize(propertyName),
             "set" + capitalize(propertyName));
    }

    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass,
                                     String readMethodName, String writeMethodName,
                                     String indexedReadMethodName, String indexedWriteMethodName)
            throws IntrospectionException {
        super(propertyName, beanClass, readMethodName, writeMethodName);
        Method ir = findIndexed(beanClass, indexedReadMethodName, 1);
        if (ir != null) {
            this.setIndexedReadMethod(ir);
        }
        Method iw = findIndexed(beanClass, indexedWriteMethodName, 2);
        if (iw != null) {
            this.setIndexedWriteMethod(iw);
        }
        if (this.indexedReadMethod == null && this.indexedWriteMethod == null) {
            throw new IntrospectionException("No indexed accessor for property " + propertyName);
        }
    }

    public IndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
                                     Method indexedReadMethod, Method indexedWriteMethod)
            throws IntrospectionException {
        super(propertyName, readMethod, writeMethod);
        this.setIndexedReadMethod(indexedReadMethod);
        this.setIndexedWriteMethod(indexedWriteMethod);
    }

    // Introspector's internal constructor: the methods come already validated from the
    // discovery.
    IndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
                              Method indexedReadMethod, Method indexedWriteMethod, boolean unchecked) {
        super(propertyName, readMethod, writeMethod, unchecked);
        this.indexedReadMethod = indexedReadMethod;
        this.indexedWriteMethod = indexedWriteMethod;
        if (indexedReadMethod != null) {
            this.indexedPropertyType = indexedReadMethod.getReturnType();
        } else if (indexedWriteMethod != null) {
            this.indexedPropertyType = indexedWriteMethod.getParameterTypes()[1];
        }
    }

    // The type of ONE element, not the array's.
    public synchronized Class<?> getIndexedPropertyType() {
        return this.indexedPropertyType;
    }

    public synchronized Method getIndexedReadMethod() {
        return this.indexedReadMethod;
    }

    // It demands the indexed reader's exact shape: a single int argument and a return that is not
    // void. `getByKey(String)` does not qualify, and that is why it is not an indexed property.
    public synchronized void setIndexedReadMethod(Method readMethod) throws IntrospectionException {
        if (readMethod == null) {
            this.indexedReadMethod = null;
            if (this.indexedWriteMethod == null) {
                this.indexedPropertyType = null;
            }
        } else {
            Class<?>[] args = readMethod.getParameterTypes();
            if (args.length != 1 || args[0] != int.class) {
                throw new IntrospectionException("bad indexed read method arg count");
            }
            Class<?> t = readMethod.getReturnType();
            if (t == void.class) {
                throw new IntrospectionException("indexed read method returns void");
            }
            if (this.indexedPropertyType != null && this.indexedPropertyType != t) {
                throw new IntrospectionException("type mismatch between indexed read and write methods");
            }
            this.indexedReadMethod = readMethod;
            this.indexedPropertyType = t;
            this.checkAgainstTheArray();
        }
    }

    public synchronized Method getIndexedWriteMethod() {
        return this.indexedWriteMethod;
    }

    // The indexed writer takes (int, value): two arguments, the first an int.
    public synchronized void setIndexedWriteMethod(Method writeMethod) throws IntrospectionException {
        if (writeMethod == null) {
            this.indexedWriteMethod = null;
            if (this.indexedReadMethod == null) {
                this.indexedPropertyType = null;
            }
        } else {
            Class<?>[] args = writeMethod.getParameterTypes();
            if (args.length != 2 || args[0] != int.class) {
                throw new IntrospectionException("bad indexed write method arg count");
            }
            if (this.indexedPropertyType != null && this.indexedPropertyType != args[1]) {
                throw new IntrospectionException("type mismatch between indexed read and write methods");
            }
            this.indexedWriteMethod = writeMethod;
            this.indexedPropertyType = args[1];
            this.checkAgainstTheArray();
        }
    }

    // If there are array accessors as well, the indexed type has to be the array type's
    // component.
    private void checkAgainstTheArray() throws IntrospectionException {
        Class<?> array = this.getPropertyType();
        if (array != null && this.indexedPropertyType != null) {
            if (!array.isArray() || array.getComponentType() != this.indexedPropertyType) {
                throw new IntrospectionException(
                    "type mismatch between indexed and non-indexed methods");
            }
        }
    }

    // The indexed ones are looked for without demanding that the array pair exist: the property may
    // be indexed only, and in that case the super already left propertyType null.
    private static Method findIndexed(Class<?> c, String name, int argCount) {
        Method m = findMethod(c, name, argCount);
        Method found = null;
        if (m != null) {
            Class<?>[] args = m.getParameterTypes();
            if (args.length == argCount && args[0] == int.class) {
                found = m;
            }
        }
        return found;
    }

    public boolean equals(Object obj) {
        boolean same = this == obj;
        if (!same && obj instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor other = (IndexedPropertyDescriptor) obj;
            same = super.equals(obj)
                 && this.indexedPropertyType == other.indexedPropertyType
                 && sameMethod(this.indexedReadMethod, other.indexedReadMethod)
                 && sameMethod(this.indexedWriteMethod, other.indexedWriteMethod);
        }
        return same;
    }

    public int hashCode() {
        int h = super.hashCode();
        h = 37 * h + (this.indexedPropertyType == null ? 0 : this.indexedPropertyType.hashCode());
        return h;
    }

    private static boolean sameMethod(Method a, Method b) {
        return a == null ? b == null : a.equals(b);
    }
}
