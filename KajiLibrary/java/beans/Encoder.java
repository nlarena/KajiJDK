package java.beans;

import java.util.IdentityHashMap;
import java.util.Map;

// El motor de la persistencia de beans. No escribe nada: lo que hace es **rearmar el objeto en
// paralelo**. Por cada llamada que se le describe, ejecuta la llamada equivalente sobre una copia
// que va construyendo, y guarda la correspondencia "objeto viejo -> expresion que lo produce".
//
// Esa copia es la clave de todo. Es lo que permite que `initialize` pregunte "¿el nuevo ya tiene
// este valor?" y no emita nada cuando la respuesta es si. Sin la copia, un bean con veinte
// propiedades en su valor por defecto saldria con veinte llamadas inutiles.
//
// Escribir es problema de las subclases: XMLEncoder redefine `writeStatement`/`writeExpression`
// para ademas anotar lo que pasa y despues imprimirlo.
//
// El mapa de enlaces es de **identidad**, no de igualdad: dos listas iguales pero distintas son dos
// objetos del grafo y tienen que salir dos veces. Con un HashMap comun se fusionarian y el grafo
// reconstruido tendria aliasing que el original no tenia.
public class Encoder {

    // El registro de delegados es estatico, como en el JDK: `setPersistenceDelegate` cambia como
    // se guarda ese tipo para todos los codificadores, no para este.
    private static final Map<Class<?>, PersistenceDelegate> registro =
        new java.util.HashMap<Class<?>, PersistenceDelegate>();

    private final Map<Object, Expression> enlaces = new IdentityHashMap<Object, Expression>();

    private ExceptionListener exceptionListener;

    public Encoder() {
    }

    // Escribe un objeto: busca quien sabe rehacerlo y le pasa la posta.
    protected void writeObject(Object o) {
        if (o != this) {
            PersistenceDelegate info = this.getPersistenceDelegate(o == null ? null : o.getClass());
            info.writeObject(o, this);
        }
    }

    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    // Nunca devuelve null: si nadie puso uno, el de por defecto imprime el problema y sigue. Que
    // siga es a proposito — un grafo con una propiedad que no se puede leer se guarda igual, sin
    // esa propiedad, en vez de no guardarse.
    public ExceptionListener getExceptionListener() {
        return this.exceptionListener != null ? this.exceptionListener : Delegados.LISTENER_POR_DEFECTO;
    }

    // Quien sabe rehacer ese tipo. El orden es: lo que se registro a mano, lo que diga el BeanInfo
    // del tipo, y por ultimo la regla incorporada que corresponda a su forma.
    public PersistenceDelegate getPersistenceDelegate(Class<?> type) {
        PersistenceDelegate d = leerRegistro(type);
        if (d == null) {
            d = delegadoDelBeanInfo(type);
        }
        if (d == null) {
            d = Delegados.para(type);
        }
        return d;
    }

    public void setPersistenceDelegate(Class<?> type, PersistenceDelegate delegate) {
        escribirRegistro(type, delegate);
    }

    private static synchronized PersistenceDelegate leerRegistro(Class<?> type) {
        return type == null ? null : registro.get(type);
    }

    private static synchronized void escribirRegistro(Class<?> type, PersistenceDelegate d) {
        if (type != null) {
            if (d == null) {
                registro.remove(type);
            } else {
                registro.put(type, d);
            }
        }
    }

    // Un BeanInfo puede traer su propio delegado en el atributo "persistenceDelegate" de su
    // BeanDescriptor. Es como el JDK deja que una clase diga como se guarda sin tocar el Encoder.
    private static PersistenceDelegate delegadoDelBeanInfo(Class<?> type) {
        PersistenceDelegate d = null;
        if (type != null && !type.isPrimitive() && !type.isArray()) {
            try {
                BeanDescriptor bd = Introspector.getBeanInfo(type).getBeanDescriptor();
                if (bd != null) {
                    Object v = bd.getValue("persistenceDelegate");
                    if (v instanceof PersistenceDelegate) {
                        d = (PersistenceDelegate) v;
                    }
                }
            } catch (Exception e) {
                d = null;
            }
        }
        return d;
    }

    // Saca el objeto del mapa y devuelve lo que valia. Lo usa writeObject del delegado cuando
    // decide que la copia que hay no sirve y hay que crear una nueva.
    public Object remove(Object oldInstance) {
        Expression exp = this.enlaces.remove(oldInstance);
        return this.valorDe(exp);
    }

    // La contraparte nueva del objeto viejo, o null si todavia no se escribio.
    //
    // Las cadenas se devuelven a si mismas: son inmutables, asi que la "copia" de una cadena es la
    // cadena. Sin este caso, cada literal del grafo pediria una construccion aparte.
    public Object get(Object oldInstance) {
        Object r;
        if (oldInstance == null || oldInstance == this || oldInstance.getClass() == String.class) {
            r = oldInstance;
        } else {
            r = this.valorDe(this.enlaces.get(oldInstance));
        }
        return r;
    }

    // La contraparte del objeto, escribiendolo solo si todavia no la tiene.
    //
    // El "solo si" no es una optimizacion: es lo que corta la recursion. Al copiar una propiedad,
    // el delegado por defecto pide escribir la expresion `viejo.getX()`, cuyo OBJETIVO es el mismo
    // objeto que se esta escribiendo. Sin la guarda, traducir esa expresion vuelve a escribir el
    // objetivo, que vuelve a copiar sus propiedades, que vuelven a pedir `getX()`: cualquier bean
    // con una propiedad desborda la pila.
    private Object writeObject1(Object oldInstance) {
        Object o = this.get(oldInstance);
        if (o == null) {
            if (oldInstance instanceof Class) {
                // Una Class es su propia contraparte: las clases no se reconstruyen, el `forName`
                // del otro lado devuelve este mismo objeto. Decirlo aca es lo que impide que
                // describir `Class.forName("X")` —cuyo objetivo es `Class.class`— obligue a
                // describir `Class.class`, que se describiria con otro `Class.forName` sobre el
                // mismo objetivo. Sin esto, guardar cualquier objeto desborda la pila.
                o = oldInstance;
            } else {
                this.writeObject(oldInstance);
                o = this.get(oldInstance);
            }
        }
        return o;
    }

    // Traduce una llamada del mundo viejo al mundo nuevo: cada objeto que aparece como objetivo o
    // como argumento se escribe primero y se reemplaza por su contraparte.
    private Statement clonar(Statement oldExp) {
        Object newTarget = this.writeObject1(oldExp.getTarget());
        Object[] oldArgs = oldExp.getArguments();
        Object[] newArgs = new Object[oldArgs.length];
        for (int i = 0; i < oldArgs.length; i++) {
            newArgs[i] = this.writeObject1(oldArgs[i]);
        }
        return Statement.class.equals(oldExp.getClass())
            ? new Statement(newTarget, oldExp.getMethodName(), newArgs)
            : new Expression(newTarget, oldExp.getMethodName(), newArgs);
    }

    // Una llamada sin valor: se ejecuta sobre la copia y se descarta.
    public void writeStatement(Statement oldStm) {
        Statement newStm = this.clonar(oldStm);
        if (oldStm.getTarget() != this) {
            try {
                newStm.execute();
            } catch (Exception e) {
                this.getExceptionListener().exceptionThrown(
                    new Exception("Encoder: discarding statement " + newStm, e));
            }
        }
    }

    // Una llamada con valor: se anota que ese valor viejo se produce con esta expresion, y despues
    // se escribe el valor —que es lo que dispara la escritura de su estado interno—.
    //
    // El `get(oldValue) != null` de arriba corta la recursion en los ciclos del grafo: un objeto
    // que ya tiene enlace no se vuelve a describir.
    public void writeExpression(Expression oldExp) {
        Object oldValue = this.valorDe(oldExp);
        if (this.get(oldValue) == null) {
            this.enlaces.put(oldValue, (Expression) this.clonar(oldExp));
            this.writeObject(oldValue);
        }
    }

    // Evaluar una expresion no puede devolver "no se pudo": el llamador ya la esta usando como
    // valor. Se avisa al oyente y se corta.
    // Olvida las contrapartes acumuladas. Lo usa XMLEncoder al terminar un flush: cada documento
    // escrito arranca de cero, asi que un objeto que aparezca en dos flushes sucesivos se describe
    // entero las dos veces en vez de salir como una referencia a un id del documento anterior.
    void limpiarEnlaces() {
        this.enlaces.clear();
    }

    final Object valorDe(Expression exp) {
        Object r = null;
        if (exp != null) {
            try {
                r = exp.getValue();
            } catch (Exception e) {
                this.getExceptionListener().exceptionThrown(e);
                throw new RuntimeException("failed to evaluate: " + exp.toString());
            }
        }
        return r;
    }
}
