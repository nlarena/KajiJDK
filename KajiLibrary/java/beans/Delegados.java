package java.beans;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

// Los delegados de persistencia incorporados. No es una clase del JDK: alla lo mismo vive en
// `com.sun.beans.metadata`/`MetaData`, que es interno y no forma parte del contrato. Aca se lo
// junta en un solo lugar para que `Encoder.getPersistenceDelegate` sea una tabla legible.
//
// Cada uno responde a una forma de objeto que el delegado por defecto no sabria rehacer:
// un arreglo no tiene constructor sin argumentos, un enum no se construye sino que se busca, una
// Class no se instancia, y una coleccion guarda su contenido en ningun campo publico.
final class Delegados {

    private Delegados() {
    }

    // Lo que hace el codificador cuando algo falla y nadie puso un oyente: avisa y sigue. Seguir es
    // deliberado —un grafo con una propiedad ilegible se guarda igual, sin esa propiedad—.
    static final ExceptionListener LISTENER_POR_DEFECTO = new PorDefecto();

    static final PersistenceDelegate NULO = new Nulo();
    static final PersistenceDelegate CADENA = new Cadena();
    static final PersistenceDelegate PRIMITIVO = new Primitivo();
    static final PersistenceDelegate ARREGLO = new Arreglo();
    static final PersistenceDelegate ENUM = new DeEnum();
    static final PersistenceDelegate CLASE = new DeClase();
    static final PersistenceDelegate CAMPO = new DeCampo();
    static final PersistenceDelegate METODO = new DeMetodo();
    static final PersistenceDelegate COLECCION = new DeColeccion();
    static final PersistenceDelegate MAPA = new DeMapa();
    static final PersistenceDelegate POR_DEFECTO = new DefaultPersistenceDelegate();

    // El orden importa: un enum es tambien un objeto con propiedades, y un arreglo tambien tiene
    // `getClass`. Se prueba de lo mas especifico a lo mas general.
    static PersistenceDelegate para(Class<?> type) {
        PersistenceDelegate d;
        if (type == null) {
            d = NULO;
        } else if (type == String.class) {
            d = CADENA;
        } else if (Statement.primitivoDelEnvoltorio(type) != null) {
            d = PRIMITIVO;
        } else if (type.isArray()) {
            d = ARREGLO;
        } else if (Enum.class.isAssignableFrom(type)) {
            d = ENUM;
        } else if (type == Class.class) {
            d = CLASE;
        } else if (Field.class.isAssignableFrom(type)) {
            d = CAMPO;
        } else if (Method.class.isAssignableFrom(type)) {
            d = METODO;
        } else if (Collection.class.isAssignableFrom(type)) {
            d = COLECCION;
        } else if (Map.class.isAssignableFrom(type)) {
            d = MAPA;
        } else {
            d = POR_DEFECTO;
        }
        return d;
    }

    private static final class PorDefecto implements ExceptionListener {
        public void exceptionThrown(Exception e) {
            System.err.println(e);
            System.err.println("Continuing ...");
        }
    }

    // null no se construye ni se inicializa: escribirlo es no hacer nada. El `<null/>` del XML lo
    // pone el codificador al imprimir, no este delegado.
    private static final class Nulo extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return null;
        }

        public void writeObject(Object oldInstance, Encoder out) {
        }

        protected void initialize(Class<?> type, Object o, Object n, Encoder out) {
        }
    }

    // Una cadena es su propio valor: `Encoder.get` ya la devuelve tal cual, asi que no hay nada
    // que rehacer. Sin este delegado se intentaria introspeccionarla como bean, que no rompe pero
    // recorre media clase String para no emitir nada.
    private static final class Cadena extends PersistenceDelegate {
        protected Expression instantiate(Object oldInstance, Encoder out) {
            return null;
        }

        public void writeObject(Object oldInstance, Encoder out) {
        }
    }

    // Envoltorios: se rehacen desde su texto, `new Integer("7")`. Y son iguales por valor, asi que
    // el que ya haya sirve si vale lo mismo.
    private static final class Primitivo extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return newInstance != null && oldInstance != null
                && oldInstance.getClass() == newInstance.getClass()
                && oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, oldInstance.getClass(), "new",
                                  new Object[] { oldInstance.toString() });
        }
    }

    // Un arreglo se crea con `Array.newInstance(tipo, largo)` y se llena elemento por elemento.
    // El objetivo tiene que ser literalmente `Array.class`: XMLEncoder reconoce esa forma y la
    // imprime como `<array class=... length=.../>` en vez de como una llamada.
    private static final class Arreglo extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return newInstance != null
                && oldInstance.getClass() == newInstance.getClass()
                && Statement.largoDeArreglo(oldInstance) == Statement.largoDeArreglo(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            return new Expression(oldInstance, Array.class, "newInstance",
                new Object[] { oldInstance.getClass().getComponentType(),
                               Integer.valueOf(Statement.largoDeArreglo(oldInstance)) });
        }

        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            int n = Statement.largoDeArreglo(oldInstance);
            for (int i = 0; i < n; i++) {
                Object indice = Integer.valueOf(i);
                Expression viejo = new Expression(oldInstance, "get", new Object[] { indice });
                Expression nuevo = new Expression(newInstance, "get", new Object[] { indice });
                try {
                    Object oldValue = viejo.getValue();
                    Object newValue = nuevo.getValue();
                    out.writeExpression(viejo);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        DefaultPersistenceDelegate.invocar(oldInstance, "set",
                            new Object[] { indice, oldValue }, out);
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }

    // Una constante de enum no se construye: se la busca por nombre. Y dos constantes son la misma
    // solo si son el mismo objeto, que es toda la garantia que da un enum.
    private static final class DeEnum extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance == newInstance;
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Enum<?> e = (Enum<?>) oldInstance;
            return new Expression(oldInstance, e.getDeclaringClass(), "valueOf",
                                  new Object[] { e.name() });
        }
    }

    // Las clases se recuperan por nombre. Las primitivas no tienen nombre que `forName` acepte:
    // se las saca del campo `TYPE` de su envoltorio, que es donde el JDK las guarda.
    private static final class DeClase extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Class<?> c = (Class<?>) oldInstance;
            Expression e = null;
            if (c.isPrimitive()) {
                try {
                    Class<?> envoltorio = Statement.envoltorioDe(c);
                    Field campo = envoltorio.getDeclaredField("TYPE");
                    e = new Expression(oldInstance, campo, "get", new Object[] { null });
                } catch (Exception ex) {
                    e = null;
                }
            }
            // Ojo con la recursion que esconde esta linea: `Class.forName("X")` es una llamada cuyo
            // OBJETIVO es `Class.class`, que es a su vez una Class. Si el codificador tuviera que
            // describir ese objetivo, para `Class.class` pediria `Class.forName("java.lang.Class")`
            // —cuyo objetivo vuelve a ser `Class.class`— y el grafo se muerde la cola.
            //
            // Quien corta el nudo es `Encoder.writeObject1`, que trata a una Class como su propia
            // contraparte y por lo tanto nunca pide describirla. Es cierto: una clase no se
            // reconstruye, `forName` devuelve el mismo objeto de siempre. El JDK corta en otro
            // lado —hace que `Class.class` salga de `String.class.getClass()`— y aca no se puede,
            // porque `Method.invoke` sobre `Object.getClass` voltea esta VM (ver el encabezado de
            // XMLEncoder). Con el corte en writeObject1 no hace falta ningun caso especial aca.
            if (e == null) {
                e = new Expression(oldInstance, Class.class, "forName", new Object[] { c.getName() });
            }
            return e;
        }
    }

    // Un Field aparece en el grafo porque el delegado por defecto describe los campos publicos con
    // `campo.get(objeto)`. Se lo recupera preguntandoselo a su clase.
    private static final class DeCampo extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Field f = (Field) oldInstance;
            return new Expression(oldInstance, f.getDeclaringClass(), "getField",
                                  new Object[] { f.getName() });
        }
    }

    private static final class DeMetodo extends PersistenceDelegate {
        protected boolean mutatesTo(Object oldInstance, Object newInstance) {
            return oldInstance.equals(newInstance);
        }

        protected Expression instantiate(Object oldInstance, Encoder out) {
            Method m = (Method) oldInstance;
            return new Expression(oldInstance, m.getDeclaringClass(), "getMethod",
                                  new Object[] { m.getName(), m.getParameterTypes() });
        }
    }

    // El contenido de una coleccion no esta en ninguna propiedad: hay que enumerarlo. Se vacia lo
    // que el objeto nuevo traiga de fabrica y se agrega lo que tiene el viejo, en orden.
    private static final class DeColeccion extends DefaultPersistenceDelegate {
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            Collection<?> vieja = (Collection<?>) oldInstance;
            Collection<?> nueva = (Collection<?>) newInstance;
            if (nueva != null && nueva.size() != 0) {
                invocar(oldInstance, "clear", new Object[0], out);
            }
            Iterator<?> it = vieja.iterator();
            while (it.hasNext()) {
                invocar(oldInstance, "add", new Object[] { it.next() }, out);
            }
        }
    }

    // Un mapa se recorre por clave y se compara valor a valor, como las propiedades de un bean:
    // asi un mapa que ya trae entradas correctas no las vuelve a escribir.
    private static final class DeMapa extends DefaultPersistenceDelegate {
        protected void initialize(Class<?> type, Object oldInstance, Object newInstance, Encoder out) {
            Map<?, ?> viejo = (Map<?, ?>) oldInstance;
            Map<?, ?> nuevo = (Map<?, ?>) newInstance;
            if (nuevo != null) {
                Object[] claves = nuevo.keySet().toArray();
                for (int i = 0; i < claves.length; i++) {
                    if (!viejo.containsKey(claves[i])) {
                        invocar(oldInstance, "remove", new Object[] { claves[i] }, out);
                    }
                }
            }
            Iterator<?> it = viejo.keySet().iterator();
            while (it.hasNext()) {
                Object clave = it.next();
                Expression eViejo = new Expression(oldInstance, "get", new Object[] { clave });
                Expression eNuevo = new Expression(newInstance, "get", new Object[] { clave });
                try {
                    Object oldValue = eViejo.getValue();
                    Object newValue = eNuevo.getValue();
                    out.writeExpression(eViejo);
                    if (!Objects.equals(newValue, out.get(oldValue))) {
                        invocar(oldInstance, "put", new Object[] { clave, oldValue }, out);
                    }
                } catch (Exception e) {
                    out.getExceptionListener().exceptionThrown(e);
                }
            }
        }
    }
}
