package java.beans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

// El delegado que se usa cuando nadie dijo otra cosa: sirve para cualquier clase que respete el
// contrato de bean —constructor sin argumentos y pares get/set—.
//
// Su trabajo son dos preguntas:
//
//   1. Como se construye. Por defecto, `new Foo()`. Si la clase es inmutable y su estado va por el
//      constructor, se le pasan al constructor los nombres de las propiedades que lo alimentan:
//      `new DefaultPersistenceDelegate(new String[] { "x", "y" })` produce `new Punto(getX(), getY())`.
//   2. Que hay que ajustar despues. Se recorren los campos publicos y las propiedades, se compara
//      contra el objeto recien creado y **solo se emite lo que difiere**. Por eso guardar un bean
//      con todo en su valor por defecto no produce ninguna llamada.
//
// El `mutatesTo` tambien cambia respecto del de la superclase, pero solo para el caso 1 de arriba:
// si el objeto se arma desde sus propiedades y define `equals`, dos instancias iguales describen el
// mismo valor y no hace falta construir otra. Para el caso corriente —constructor sin argumentos,
// objeto mutable— se usa la regla de la superclase; ver el comentario de `mutatesTo`.
public class DefaultPersistenceDelegate extends PersistenceDelegate {

    private String[] constructor;

    public DefaultPersistenceDelegate() {
        this.constructor = new String[0];
    }

    public DefaultPersistenceDelegate(String[] constructorPropertyNames) {
        this.constructor = constructorPropertyNames == null ? new String[0] : constructorPropertyNames;
    }

    // Si la clase escribio su propio `equals`, es ella la que sabe cuando dos instancias valen lo
    // mismo. Si no lo escribio, `equals` es identidad y no dice nada util.
    private static boolean defineEquals(Class<?> type) {
        boolean r = false;
        Method[] ms = type.getDeclaredMethods();
        for (int i = 0; i < ms.length; i++) {
            Class<?>[] ps = ms[i].getParameterTypes();
            if (!r && ms[i].getName().equals("equals") && ps.length == 1 && ps[0] == Object.class) {
                r = true;
            }
        }
        return r;
    }

    // Preguntarle a `equals` solo vale cuando el objeto se construye desde sus propiedades, o sea
    // cuando este delegado tiene nombres de constructor. La condicion no es un detalle: si el
    // objeto tiene constructor sin argumentos, es mutable, y para uno mutable `equals` responde
    // otra pregunta —"¿valen lo mismo AHORA?"— que no es la que se esta haciendo.
    //
    // Sin la condicion, una lista con elementos nunca es igual a la lista vacia recien creada, asi
    // que `writeObject` decide una y otra vez que hay que crearla de nuevo, y vuelve a crearla, y
    // vuelve: guardar cualquier coleccion no vacia desborda la pila. Para un objeto mutable la
    // pregunta correcta es la de la superclase —¿es del mismo molde?— y las diferencias las arregla
    // despues `initialize`.
    protected boolean mutatesTo(Object oldInstance, Object newInstance) {
        boolean r;
        if (this.constructor.length != 0 && oldInstance != null
                && defineEquals(oldInstance.getClass())) {
            r = oldInstance.equals(newInstance);
        } else {
            r = super.mutatesTo(oldInstance, newInstance);
        }
        return r;
    }

    protected Expression instantiate(Object oldInstance, Encoder out) {
        int n = this.constructor.length;
        Class<?> type = oldInstance.getClass();
        Object[] args = new Object[n];
        for (int i = 0; i < n; i++) {
            try {
                Method lector = lectorDe(type, this.constructor[i]);
                args[i] = lector.invoke(oldInstance);
            } catch (Exception e) {
                out.getExceptionListener().exceptionThrown(e);
            }
        }
        return new Expression(oldInstance, type, "new", args);
    }

    private static Method lectorDe(Class<?> type, String propiedad) throws Exception {
        if (propiedad == null) {
            throw new IllegalArgumentException("Property name is null");
        }
        PropertyDescriptor elegida = null;
        PropertyDescriptor[] pds = Introspector.getBeanInfo(type).getPropertyDescriptors();
        for (int i = 0; i < pds.length; i++) {
            if (elegida == null && propiedad.equals(pds[i].getName())) {
                elegida = pds[i];
            }
        }
        if (elegida == null) {
            throw new IllegalStateException("Could not find property by the name " + propiedad);
        }
        Method m = elegida.getReadMethod();
        if (m == null) {
            throw new IllegalStateException("Could not find getter for the property " + propiedad);
        }
        return m;
    }

    protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        super.initialize(type, oldInstance, newInstance, out);
        // Solo cuando la cadena de superclases llega a la clase real del objeto: initialize se
        // llama una vez por nivel y el estado se copia entero de una sola pasada, no por nivel.
        if (oldInstance.getClass() == type) {
            this.copiarEstado(type, oldInstance, newInstance, out);
        }
    }

    private void copiarEstado(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        this.copiarCampos(type, oldInstance, newInstance, out);
        BeanInfo info;
        try {
            info = Introspector.getBeanInfo(type);
        } catch (IntrospectionException e) {
            out.getExceptionListener().exceptionThrown(e);
            info = null;
        }
        if (info != null) {
            PropertyDescriptor[] pds = info.getPropertyDescriptors();
            for (int i = 0; i < pds.length; i++) {
                if (!esTransitorio(pds[i])) {
                    try {
                        this.copiarPropiedad(pds[i], oldInstance, newInstance, out);
                    } catch (Exception e) {
                        out.getExceptionListener().exceptionThrown(e);
                    }
                }
            }
        }
    }

    // Un descriptor marcado `transient` queda afuera a proposito: es como una clase dice "esto no
    // se guarda". La marca la pone la anotacion @Transient o un BeanInfo a mano.
    static boolean esTransitorio(FeatureDescriptor d) {
        return Boolean.TRUE.equals(d.getValue("transient"));
    }

    // Los campos publicos y mutables tambien son estado. Se los lee con un Expression sobre el
    // propio Field para que el codificador sepa reproducir la lectura, no solo su resultado.
    private void copiarCampos(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
        Field[] fs = type.getFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            int mod = f.getModifiers();
            if (!Modifier.isFinal(mod) && !Modifier.isStatic(mod) && !Modifier.isTransient(mod)) {
                try {
                    Expression viejo = new Expression(f, "get", new Object[] { oldInstance });
                    Expression nuevo = new Expression(f, "get", new Object[] { newInstance });
                    Object oldValue = viejo.getValue();
                    Object newValue = nuevo.getValue();
                    out.writeExpression(viejo);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        out.writeStatement(new Statement(f, "set", new Object[] { oldInstance, oldValue }));
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }

    // El corazon del ahorro: se lee la propiedad en los dos objetos y solo se emite el `set` si el
    // nuevo todavia no tiene ese valor.
    //
    // La comparacion es contra `out.get(oldValue)` y no contra `oldValue` a secas: lo que hay que
    // preguntar es si el objeto nuevo ya apunta a la CONTRAPARTE del valor viejo, no si apunta al
    // valor viejo mismo —que vive en el otro grafo y nunca va a estar ahi—.
    private void copiarPropiedad(PropertyDescriptor pd, Object oldInstance, Object newInstance,
                                 Encoder out) throws Exception {
        Method lector = pd.getReadMethod();
        Method escritor = pd.getWriteMethod();
        if (lector != null && escritor != null) {
            Expression viejo = new Expression(oldInstance, lector.getName(), new Object[0]);
            Expression nuevo = new Expression(newInstance, lector.getName(), new Object[0]);
            Object oldValue = viejo.getValue();
            Object newValue = nuevo.getValue();
            out.writeExpression(viejo);
            if (!Objects.equals(newValue, out.get(oldValue))) {
                out.writeStatement(new Statement(oldInstance, escritor.getName(),
                                                 new Object[] { oldValue }));
            }
        }
    }

    // Atajo que usan los delegados incorporados para emitir "llamale esto al objeto viejo".
    static void invocar(Object instancia, String metodo, Object[] args, Encoder out) {
        out.writeStatement(new Statement(instancia, metodo, args));
    }
}
