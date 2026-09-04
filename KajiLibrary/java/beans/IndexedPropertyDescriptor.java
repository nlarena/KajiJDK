package java.beans;

import java.lang.reflect.Method;

// Una propiedad a la que ademas se le puede acceder elemento por elemento: `getDatos()` devuelve
// el arreglo entero y `getDatos(int)` un solo elemento.
//
// El detalle que casi todo el mundo implementa mal, y que aca esta verificado contra el JDK real:
// **una propiedad puramente indexada tiene propertyType null**. Si el bean declara `getSoloIdx(int)`
// y `setSoloIdx(int, String)` pero ningun accesor de arreglo, el descriptor sale con
// getPropertyType() == null, getReadMethod() == null y getWriteMethod() == null, y solo la mitad
// indexada poblada. Devolver String, o String[], seria mentir sobre un accesor que no existe.
//
// Cuando SI estan los dos pares, el tipo indexado tiene que ser el componente del tipo arreglo:
// `int[]` contra `int`. Si no cierran, la propiedad no es valida.
public class IndexedPropertyDescriptor extends PropertyDescriptor {

    private Class<?> indexedPropertyType;
    private Method indexedReadMethod;
    private Method indexedWriteMethod;

    // Busca los cuatro accesores por convencion: get/is + Nombre, set + Nombre, y las variantes
    // con indice que llevan el mismo nombre pero un argumento int adelante.
    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass)
            throws IntrospectionException {
        this(propertyName, beanClass,
             "get" + capitalizar(propertyName),
             "set" + capitalizar(propertyName),
             "get" + capitalizar(propertyName),
             "set" + capitalizar(propertyName));
    }

    public IndexedPropertyDescriptor(String propertyName, Class<?> beanClass,
                                     String readMethodName, String writeMethodName,
                                     String indexedReadMethodName, String indexedWriteMethodName)
            throws IntrospectionException {
        super(propertyName, beanClass, readMethodName, writeMethodName);
        Method ir = buscarIndexado(beanClass, indexedReadMethodName, 1);
        if (ir != null) {
            this.setIndexedReadMethod(ir);
        }
        Method iw = buscarIndexado(beanClass, indexedWriteMethodName, 2);
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

    // Constructor interno de Introspector: los metodos ya vienen validados del descubrimiento.
    IndexedPropertyDescriptor(String propertyName, Method readMethod, Method writeMethod,
                              Method indexedReadMethod, Method indexedWriteMethod, boolean sinChequear) {
        super(propertyName, readMethod, writeMethod, sinChequear);
        this.indexedReadMethod = indexedReadMethod;
        this.indexedWriteMethod = indexedWriteMethod;
        if (indexedReadMethod != null) {
            this.indexedPropertyType = indexedReadMethod.getReturnType();
        } else if (indexedWriteMethod != null) {
            this.indexedPropertyType = indexedWriteMethod.getParameterTypes()[1];
        }
    }

    // El tipo de UN elemento, no el del arreglo.
    public synchronized Class<?> getIndexedPropertyType() {
        return this.indexedPropertyType;
    }

    public synchronized Method getIndexedReadMethod() {
        return this.indexedReadMethod;
    }

    // Exige la forma exacta del lector indexado: un unico argumento int y un retorno que no sea
    // void. `getPorClave(String)` no califica, y por eso no es una propiedad indexada.
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
            this.chequearContraElArreglo();
        }
    }

    public synchronized Method getIndexedWriteMethod() {
        return this.indexedWriteMethod;
    }

    // El escritor indexado lleva (int, valor): dos argumentos, el primero int.
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
            this.chequearContraElArreglo();
        }
    }

    // Si ademas hay accesores de arreglo, el tipo indexado tiene que ser el componente del tipo
    // del arreglo.
    private void chequearContraElArreglo() throws IntrospectionException {
        Class<?> arreglo = this.getPropertyType();
        if (arreglo != null && this.indexedPropertyType != null) {
            if (!arreglo.isArray() || arreglo.getComponentType() != this.indexedPropertyType) {
                throw new IntrospectionException(
                    "type mismatch between indexed and non-indexed methods");
            }
        }
    }

    // Los indexados se buscan sin exigir que exista el par de arreglo: la propiedad puede ser solo
    // indexada, y en ese caso el super ya dejo propertyType en null.
    private static Method buscarIndexado(Class<?> c, String nombre, int cantidadArgs) {
        Method m = buscarMetodo(c, nombre, cantidadArgs);
        Method bueno = null;
        if (m != null) {
            Class<?>[] args = m.getParameterTypes();
            if (args.length == cantidadArgs && args[0] == int.class) {
                bueno = m;
            }
        }
        return bueno;
    }

    public boolean equals(Object obj) {
        boolean igual = this == obj;
        if (!igual && obj instanceof IndexedPropertyDescriptor) {
            IndexedPropertyDescriptor otro = (IndexedPropertyDescriptor) obj;
            igual = super.equals(obj)
                 && this.indexedPropertyType == otro.indexedPropertyType
                 && mismoMetodo(this.indexedReadMethod, otro.indexedReadMethod)
                 && mismoMetodo(this.indexedWriteMethod, otro.indexedWriteMethod);
        }
        return igual;
    }

    public int hashCode() {
        int h = super.hashCode();
        h = 37 * h + (this.indexedPropertyType == null ? 0 : this.indexedPropertyType.hashCode());
        return h;
    }

    private static boolean mismoMetodo(Method a, Method b) {
        return a == null ? b == null : a.equals(b);
    }
}
