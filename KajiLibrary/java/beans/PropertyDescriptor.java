package java.beans;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

// Una propiedad simple de un bean: un nombre, un tipo, y hasta dos metodos —el que lee y el que
// escribe—. Cualquiera de los dos puede faltar: sin escritor es de solo lectura, sin lector es de
// solo escritura, y ambas cosas son propiedades legitimas.
//
// El tipo NO se declara, se deduce: sale del retorno del lector o del unico parametro del
// escritor. Cuando estan los dos tienen que coincidir, y si no coinciden la propiedad no es
// valida — es el chequeo que hace que `setDesparejo(int)` no se enganche a `getDesparejo():String`.
//
// Sobre `bound`: el constructor que recibe la clase del bean lo prende solo si esa clase tiene
// addPropertyChangeListener. Esta comprobado contra el JDK real (un bean sin ese metodo da
// bound=false, uno con el da bound=true) y no es cosa de Introspector: pasa en el constructor.
// `constrained`, en cambio, NO se deduce de addVetoableChangeListener — tambien comprobado.
public class PropertyDescriptor extends FeatureDescriptor {

    private Class<?> propertyType;
    private Method readMethod;
    private Method writeMethod;
    private boolean bound;
    private boolean constrained;
    private Class<?> propertyEditorClass;

    // La clase del bean, para poder resolver el otro accesor cuando recien viene uno.
    private Class<?> class0;

    // Busca `isNombre`/`getNombre` y `setNombre` en la clase.
    public PropertyDescriptor(String propertyName, Class<?> beanClass) throws IntrospectionException {
        this(propertyName, beanClass,
             "is" + capitalizar(propertyName),
             "set" + capitalizar(propertyName));
    }

    // Igual, pero con los nombres de los metodos dados. Un nombre null significa "esta propiedad
    // no tiene ese accesor".
    public PropertyDescriptor(String propertyName, Class<?> beanClass,
                              String readMethodName, String writeMethodName)
            throws IntrospectionException {
        if (beanClass == null) {
            throw new IntrospectionException("Target Bean class is null");
        }
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        this.setName(propertyName);
        this.class0 = beanClass;

        String lectura = readMethodName;
        if (lectura != null && lectura.length() == 0) {
            lectura = null;
        }
        String escritura = writeMethodName;
        if (escritura != null && escritura.length() == 0) {
            escritura = null;
        }

        Method r = null;
        if (lectura != null) {
            r = buscarMetodo(beanClass, lectura, 0);
            if (r == null) {
                // El primer intento asume boolean (`isX`); si no esta, se prueba `getX`. Que el
                // mensaje de error conserve el nombre ORIGINAL es lo que hace el JDK.
                String alternativo = "get" + capitalizar(propertyName);
                r = buscarMetodo(beanClass, alternativo, 0);
            }
            if (r == null) {
                throw new IntrospectionException("Method not found: " + lectura);
            }
        }
        if (r != null) {
            this.setReadMethod(r);
        }

        if (escritura != null) {
            Method w = buscarMetodo(beanClass, escritura, 1);
            if (w == null && lectura == null) {
                throw new IntrospectionException("Method not found: " + escritura);
            }
            if (w != null) {
                this.setWriteMethod(w);
            }
        }

        this.bound = buscarMetodo(beanClass, "addPropertyChangeListener", 1) != null;
    }

    // Con los metodos ya en la mano. Cualquiera de los dos puede ser null.
    public PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod)
            throws IntrospectionException {
        if (propertyName == null || propertyName.length() == 0) {
            throw new IntrospectionException("bad property name");
        }
        this.setName(propertyName);
        this.setReadMethod(readMethod);
        this.setWriteMethod(writeMethod);
    }

    // Constructor interno de Introspector, que ya valido todo al descubrir los metodos y no
    // necesita que se lo revalide.
    PropertyDescriptor(String propertyName, Method readMethod, Method writeMethod, boolean sinChequear) {
        this.setName(propertyName);
        this.readMethod = readMethod;
        this.writeMethod = writeMethod;
        if (readMethod != null) {
            this.propertyType = readMethod.getReturnType();
        } else if (writeMethod != null) {
            this.propertyType = writeMethod.getParameterTypes()[0];
        }
    }

    // El tipo de la propiedad, o null cuando no hay ni lector ni escritor no indexados — que es
    // exactamente el caso de una propiedad puramente indexada.
    public synchronized Class<?> getPropertyType() {
        return this.propertyType;
    }

    // Para que IndexedPropertyDescriptor pueda dejarlo en null sin repetir la deduccion.
    void fijarTipo(Class<?> t) {
        this.propertyType = t;
    }

    public synchronized Method getReadMethod() {
        return this.readMethod;
    }

    // Acepta el metodo lector si es un getter de verdad: sin parametros y devolviendo algo. Si ya
    // habia un escritor, los tipos tienen que cerrar.
    public synchronized void setReadMethod(Method readMethod) throws IntrospectionException {
        if (readMethod == null) {
            this.readMethod = null;
            if (this.writeMethod == null) {
                this.propertyType = null;
            }
        } else {
            if (readMethod.getParameterTypes().length != 0) {
                throw new IntrospectionException("bad read method arg count");
            }
            Class<?> t = readMethod.getReturnType();
            if (t == void.class) {
                throw new IntrospectionException("read method returns void");
            }
            if (this.propertyType != null && this.propertyType != t) {
                throw new IntrospectionException("type mismatch between read and write methods");
            }
            this.readMethod = readMethod;
            this.propertyType = t;
            if (this.class0 == null) {
                this.class0 = readMethod.getDeclaringClass();
            }
        }
    }

    public synchronized Method getWriteMethod() {
        return this.writeMethod;
    }

    // Acepta el metodo escritor si toma exactamente un argumento, y si ese argumento es del tipo
    // que ya tiene la propiedad.
    public synchronized void setWriteMethod(Method writeMethod) throws IntrospectionException {
        if (writeMethod == null) {
            this.writeMethod = null;
            if (this.readMethod == null) {
                this.propertyType = null;
            }
        } else {
            Class<?>[] args = writeMethod.getParameterTypes();
            if (args.length != 1) {
                throw new IntrospectionException("bad write method arg count");
            }
            if (this.propertyType != null && this.propertyType != args[0]) {
                throw new IntrospectionException("type mismatch between read and write methods");
            }
            this.writeMethod = writeMethod;
            this.propertyType = args[0];
            if (this.class0 == null) {
                this.class0 = writeMethod.getDeclaringClass();
            }
        }
    }

    // Si al cambiar dispara un PropertyChangeEvent.
    public boolean isBound() {
        return this.bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    // Si un oyente puede vetar el cambio.
    public boolean isConstrained() {
        return this.constrained;
    }

    public void setConstrained(boolean constrained) {
        this.constrained = constrained;
    }

    public void setPropertyEditorClass(Class<?> propertyEditorClass) {
        this.propertyEditorClass = propertyEditorClass;
    }

    public Class<?> getPropertyEditorClass() {
        return this.propertyEditorClass;
    }

    // Instancia el editor declarado. Se prueba primero el constructor que recibe el bean —el que
    // usan los editores que necesitan contexto— y se cae al de cero argumentos.
    public PropertyEditor createPropertyEditor(Object bean) {
        PropertyEditor ed = null;
        if (this.propertyEditorClass != null) {
            try {
                Constructor<?> c = null;
                try {
                    c = this.propertyEditorClass.getConstructor(Object.class);
                } catch (Exception sinEseCtor) {
                    c = null;
                }
                Object o;
                if (c != null) {
                    o = c.newInstance(bean);
                } else {
                    o = this.propertyEditorClass.newInstance();
                }
                ed = (PropertyEditor) o;
            } catch (Exception e) {
                ed = null;
            }
        }
        return ed;
    }

    public boolean equals(Object obj) {
        boolean igual = this == obj;
        if (!igual && obj instanceof PropertyDescriptor) {
            PropertyDescriptor otro = (PropertyDescriptor) obj;
            igual = mismos(this.getReadMethod(), otro.getReadMethod())
                 && mismos(this.getWriteMethod(), otro.getWriteMethod())
                 && this.getPropertyType() == otro.getPropertyType()
                 && this.getPropertyEditorClass() == otro.getPropertyEditorClass()
                 && this.bound == otro.bound
                 && this.constrained == otro.constrained;
        }
        return igual;
    }

    public int hashCode() {
        int h = 7;
        h = 37 * h + (this.getName() == null ? 0 : this.getName().hashCode());
        h = 37 * h + (this.propertyType == null ? 0 : this.propertyType.hashCode());
        return h;
    }

    private static boolean mismos(Object a, Object b) {
        return a == null ? b == null : a.equals(b);
    }

    // --- ayudantes compartidos con el resto del paquete -------------------------------

    // "nombre" -> "Nombre". No es decapitalize al reves: aca alcanza con subir la primera letra.
    static String capitalizar(String s) {
        String r = s;
        if (s != null && s.length() > 0) {
            r = s.substring(0, 1).toUpperCase() + s.substring(1);
        }
        return r;
    }

    // El primer metodo publico y NO estatico con ese nombre y esa cantidad de argumentos.
    // Introspector ignora los estaticos, y esta comprobado contra el JDK real: un `getEstatico()`
    // publico no produce ninguna propiedad.
    static Method buscarMetodo(Class<?> c, String nombre, int cantidadArgs) {
        Method encontrado = null;
        if (c != null && nombre != null) {
            Method[] ms = c.getMethods();
            for (int i = 0; i < ms.length; i++) {
                Method m = ms[i];
                if (encontrado == null
                        && m.getName().equals(nombre)
                        && m.getParameterTypes().length == cantidadArgs
                        && !Modifier.isStatic(m.getModifiers())) {
                    encontrado = m;
                }
            }
        }
        return encontrado;
    }
}
