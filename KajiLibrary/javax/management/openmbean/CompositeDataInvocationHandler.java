package javax.management.openmbean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * El manejador que hace que un {@link CompositeData} se pueda usar a traves de una interfaz de
 * getters.
 *
 * <p>Es lo que resuelve el problema del lado del cliente. Un `CompositeData` se consulta por cadenas
 * --`datos.get("nombre")`-- y eso no lo revisa el compilador: una falta de ortografia en la clave
 * aparece en ejecucion. Con esto, uno declara una interfaz `Persona` con `getNombre()` y
 * `getEdad()`, envuelve el dato en un proxy con este manejador, y a partir de ahi el compilador
 * comprueba los nombres y los tipos.
 *
 * <p>La traduccion de metodo a item es la convencion de beans: `getFoo()` lee el item `foo` y
 * `isFoo()` tambien --el prefijo `is` solo se acepta para `boolean`, que es donde Java lo permite--.
 * La primera letra se pasa a minuscula salvo que las dos primeras sean mayusculas, que es la regla
 * de `Introspector` y la que hace que `getURL()` lea el item `URL` y no `uRL`.
 *
 * <p>Solo se aceptan getters sin argumentos. Un metodo con parametros no puede corresponder a un
 * item, y llamarlo es {@link IllegalArgumentException} en vez de un item inventado.
 */
public class CompositeDataInvocationHandler implements InvocationHandler {

    private final CompositeData compositeData;

    /**
     * Un manejador sobre ese dato.
     *
     * @throws IllegalArgumentException si el dato es nulo
     */
    public CompositeDataInvocationHandler(CompositeData compositeData) {
        if (compositeData == null) {
            throw new IllegalArgumentException("el dato compuesto no puede ser nulo");
        }
        this.compositeData = compositeData;
    }

    /** El dato que este manejador expone. */
    public CompositeData getCompositeData() {
        return this.compositeData;
    }

    /**
     * Contesta la llamada leyendo el item que corresponde.
     *
     * <p>`equals`, `hashCode` y `toString` se atienden aparte y no van a los items: un proxy que
     * buscara un item llamado `toString` seria inusable con cualquier herramienta que imprima
     * objetos.
     *
     * @throws IllegalArgumentException si el metodo no es un getter, o si no hay item con ese nombre
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name = method.getName();
        if (args == null || args.length == 0) {
            if (name.equals("toString")) {
                return "Proxy[" + this.compositeData.toString() + "]";
            }
            if (name.equals("hashCode")) {
                return Integer.valueOf(this.compositeData.hashCode());
            }
        }
        if (name.equals("equals") && args != null && args.length == 1
                && method.getParameterTypes()[0] == Object.class) {
            return Boolean.valueOf(this.equalsProxy(proxy, args[0]));
        }

        String item = itemOf(name, method);
        if (args != null && args.length > 0) {
            throw new IllegalArgumentException(
                    "un getter de un dato compuesto no lleva argumentos: " + name);
        }
        if (!this.compositeData.containsKey(item)) {
            throw new IllegalArgumentException(
                    "no hay un item llamado " + item + " en este dato compuesto");
        }
        return this.compositeData.get(item);
    }

    // Dos proxies son iguales si sus datos lo son. Comparar los proxies con `equals` los mandaria de
    // vuelta a este mismo metodo y no terminaria nunca.
    private boolean equalsProxy(Object proxy, Object other) {
        if (other == null) {
            return false;
        }
        if (proxy == other) {
            return true;
        }
        if (!Proxy.isProxyClass(other.getClass())) {
            return false;
        }
        InvocationHandler h = Proxy.getInvocationHandler(other);
        if (!(h instanceof CompositeDataInvocationHandler)) {
            return false;
        }
        return this.compositeData.equals(
                ((CompositeDataInvocationHandler) h).getCompositeData());
    }

    private static String itemOf(String name, Method method) {
        String resto;
        if (name.startsWith("get") && name.length() > 3) {
            resto = name.substring(3);
        } else if (name.startsWith("is") && name.length() > 2
                && method.getReturnType() == Boolean.TYPE) {
            resto = name.substring(2);
        } else {
            throw new IllegalArgumentException(name + " no es un getter");
        }
        // La regla de `Introspector`: `getURL` da `URL`, `getNombre` da `nombre`. Sin ella, un item
        // cuyo nombre empieza con una sigla no se encontraria nunca.
        if (resto.length() > 1 && Character.isUpperCase(resto.charAt(1))) {
            return resto;
        }
        return Character.toLowerCase(resto.charAt(0)) + resto.substring(1);
    }
}
