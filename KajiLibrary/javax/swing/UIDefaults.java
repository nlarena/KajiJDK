package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * La tabla donde un aspecto grafico guarda todos sus valores.
 *
 * <h2>Que hay adentro</h2>
 *
 * <p>Colores, tipografias, bordes, iconos, margenes y --lo mas importante-- que clase implementa la
 * interfaz grafica de cada componente. Las claves son textos como {@code "Button.background"} o
 * {@code "ButtonUI"}.
 *
 * <h2>Los tres tipos de valor</h2>
 *
 * <p>Un valor puede ser el objeto directamente, o uno de dos envoltorios que lo fabrican recien
 * cuando se lo pide.
 *
 * <p>{@link LazyValue} lo fabrica <strong>una vez</strong> y reemplaza la entrada por el resultado.
 * Existe por una razon concreta: un aspecto grafico define miles de entradas y una sesion usa unas
 * pocas. Construir todos los iconos y todas las tipografias al arrancar seria pagar por lo que no se
 * va a mirar.
 *
 * <p>{@link ActiveValue} lo fabrica <strong>cada vez</strong> y no se guarda. Es para lo que no se
 * puede compartir: si dos componentes reciben el mismo objeto de estado, uno le pisa el estado al
 * otro.
 *
 * <p>La diferencia entre los dos no es de rendimiento sino de correccion, y confundirlos da errores
 * que aparecen recien cuando hay dos componentes del mismo tipo en pantalla.
 *
 * <h2>El idioma</h2>
 *
 * <p>Cada operacion tiene una version con {@link Locale}. Los textos que ve el usuario --los nombres
 * de los botones de un dialogo, por ejemplo-- salen de aqui, y una aplicacion puede tener abiertas
 * dos ventanas en dos idiomas. Sin el parametro habria un solo idioma por proceso.
 *
 * <p>Los textos que no estan en la tabla se buscan en los {@link ResourceBundle} que se hayan
 * agregado, del ultimo al primero: el que se agrega despues tapa al anterior, que es lo que permite
 * a una aplicacion cambiar un texto sin reescribir el paquete entero.
 *
 * @since 1.2
 */
public class UIDefaults extends Hashtable<Object, Object> {

    private static final long serialVersionUID = 7341222528856548117L;

    private final PropertyChangeSupport cambios = new PropertyChangeSupport(this);
    private final List<String> paquetes = new ArrayList<String>();

    private Locale idiomaPorOmision = Locale.getDefault();

    /** Una tabla vacia. */
    public UIDefaults() {
        super(700, 0.75f);
    }

    /**
     * Una tabla vacia con esa capacidad.
     *
     * @param initialCapacity cuantas entradas se esperan
     * @param loadFactor cuanto se llena antes de agrandarse
     */
    public UIDefaults(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Una tabla con esos pares clave-valor.
     *
     * <p>El arreglo va plano: clave, valor, clave, valor. Es incomodo de leer y es como esta escrita
     * la definicion de todos los aspectos graficos, que son listas de cientos de pares.
     *
     * @param keyValueList los pares, alternados
     */
    public UIDefaults(Object[] keyValueList) {
        super(keyValueList.length / 2 + 1, 0.75f);
        putDefaults(keyValueList);
    }

    /**
     * El valor de esa clave, con el idioma por omision.
     *
     * @param key la clave
     * @return el valor, o {@code null}
     */
    @Override
    public Object get(Object key) {
        return get(key, getDefaultLocale());
    }

    /**
     * El valor de esa clave en ese idioma.
     *
     * <p>Aca es donde se resuelven los dos envoltorios: un {@link LazyValue} se fabrica y se guarda
     * en su lugar, y un {@link ActiveValue} se fabrica y no se guarda.
     *
     * @param key la clave
     * @param l el idioma, o {@code null} para el de la tabla
     * @return el valor, o {@code null}
     */
    public Object get(Object key, Locale l) {
        Object v = super.get(key);
        if (v == null) {
            v = deLosPaquetes(key, l);
        }
        if (v instanceof LazyValue) {
            final Object hecho = ((LazyValue) v).createValue(this);
            // Se reemplaza la entrada, que es lo que hace que se fabrique una sola vez. Si el
            // fabricante devolvio null se saca la clave: dejar el envoltorio haria que se lo intente
            // fabricar en cada consulta, sin exito, para siempre.
            if (hecho == null) {
                super.remove(key);
            } else {
                super.put(key, hecho);
            }
            return hecho;
        }
        if (v instanceof ActiveValue) {
            return ((ActiveValue) v).createValue(this);
        }
        return v;
    }

    /**
     * Guarda un valor y avisa del cambio.
     *
     * <p>Con valor {@code null} se saca la clave. No es lo mismo que guardar {@code null}: una
     * tabla de dispersion de las de este tipo no admite valores nulos, y ademas "no hay valor" es
     * justamente lo que se quiere decir.
     *
     * @param key la clave
     * @param value el valor, o {@code null} para sacarla
     * @return el valor anterior, o {@code null}
     */
    @Override
    public Object put(Object key, Object value) {
        final Object viejo = value == null ? super.remove(key) : super.put(key, value);
        if (key instanceof String) {
            cambios.firePropertyChange((String) key, viejo, value);
        }
        return viejo;
    }

    /**
     * Guarda varios pares de una.
     *
     * <p>Los avisos de cambio salen todos al final, no uno por par: quien escucha suele redibujar, y
     * con un aviso por entrada redibujaria cientos de veces para el mismo cambio.
     *
     * @param keyValueList los pares, alternados
     */
    public void putDefaults(Object[] keyValueList) {
        for (int i = 0; i < keyValueList.length - 1; i += 2) {
            final Object k = keyValueList[i];
            final Object v = keyValueList[i + 1];
            if (v == null) {
                super.remove(k);
            } else {
                super.put(k, v);
            }
        }
        cambios.firePropertyChange("UIDefaults", null, null);
    }

    /**
     * La tipografia de esa clave.
     *
     * @param key la clave
     * @return la tipografia, o {@code null} si no hay o no es una
     */
    public Font getFont(Object key) {
        return getFont(key, getDefaultLocale());
    }

    /**
     * La tipografia de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return la tipografia, o {@code null}
     */
    public Font getFont(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Font ? (Font) v : null;
    }

    /**
     * El color de esa clave.
     *
     * @param key la clave
     * @return el color, o {@code null}
     */
    public Color getColor(Object key) {
        return getColor(key, getDefaultLocale());
    }

    /**
     * El color de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el color, o {@code null}
     */
    public Color getColor(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Color ? (Color) v : null;
    }

    /**
     * El icono de esa clave.
     *
     * @param key la clave
     * @return el icono, o {@code null}
     */
    public Icon getIcon(Object key) {
        return getIcon(key, getDefaultLocale());
    }

    /**
     * El icono de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el icono, o {@code null}
     */
    public Icon getIcon(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Icon ? (Icon) v : null;
    }

    /**
     * El borde de esa clave.
     *
     * @param key la clave
     * @return el borde, o {@code null}
     */
    public Border getBorder(Object key) {
        return getBorder(key, getDefaultLocale());
    }

    /**
     * El borde de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el borde, o {@code null}
     */
    public Border getBorder(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Border ? (Border) v : null;
    }

    /**
     * El texto de esa clave.
     *
     * @param key la clave
     * @return el texto, o {@code null}
     */
    public String getString(Object key) {
        return getString(key, getDefaultLocale());
    }

    /**
     * El texto de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el texto, o {@code null}
     */
    public String getString(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof String ? (String) v : null;
    }

    /**
     * El numero entero de esa clave.
     *
     * @param key la clave
     * @return el numero, o cero si no hay
     */
    public int getInt(Object key) {
        return getInt(key, getDefaultLocale());
    }

    /**
     * El numero entero de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el numero, o cero si no hay
     */
    public int getInt(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Integer ? ((Integer) v).intValue() : 0;
    }

    /**
     * El valor de verdad de esa clave.
     *
     * @param key la clave
     * @return el valor, o falso si no hay
     */
    public boolean getBoolean(Object key) {
        return getBoolean(key, getDefaultLocale());
    }

    /**
     * El valor de verdad de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el valor, o falso si no hay
     */
    public boolean getBoolean(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Boolean && ((Boolean) v).booleanValue();
    }

    /**
     * Los margenes de esa clave.
     *
     * @param key la clave
     * @return los margenes, o {@code null}
     */
    public Insets getInsets(Object key) {
        return getInsets(key, getDefaultLocale());
    }

    /**
     * Los margenes de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return los margenes, o {@code null}
     */
    public Insets getInsets(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Insets ? (Insets) v : null;
    }

    /**
     * El tamano de esa clave.
     *
     * @param key la clave
     * @return el tamano, o {@code null}
     */
    public Dimension getDimension(Object key) {
        return getDimension(key, getDefaultLocale());
    }

    /**
     * El tamano de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el tamano, o {@code null}
     */
    public Dimension getDimension(Object key, Locale l) {
        final Object v = get(key, l);
        return v instanceof Dimension ? (Dimension) v : null;
    }

    /**
     * La clase que implementa la interfaz grafica de ese componente.
     *
     * @param uiClassID el identificador, como {@code "ButtonUI"}
     * @param uiClassLoader el cargador con el que buscarla, o {@code null}
     * @return la clase, o {@code null} si no se pudo encontrar
     */
    @SuppressWarnings("unchecked")
    public Class<? extends ComponentUI> getUIClass(String uiClassID, ClassLoader uiClassLoader) {
        final Object v = get(uiClassID);
        if (!(v instanceof String)) {
            return null;
        }
        final String nombre = (String) v;
        // La clase resuelta se guarda bajo su propio nombre. No es solo por velocidad: la tabla es
        // publica, y el JDK deja ahi el `Class` para que quien quiera pueda mirarlo.
        final Object cacheada = get(nombre);
        if (cacheada instanceof Class) {
            return (Class<? extends ComponentUI>) cacheada;
        }
        // Sin cargador se usa el del contexto del hilo, no el de esta clase. La diferencia no es
        // teorica: la clase de la interfaz grafica la trae el aspecto grafico, que vive donde vive
        // la aplicacion, y el cargador de esta biblioteca no llega ahi.
        ClassLoader cl = uiClassLoader;
        if (cl == null) {
            cl = Thread.currentThread().getContextClassLoader();
        }
        try {
            final Class<?> c = cl == null ? Class.forName(nombre) : cl.loadClass(nombre);
            put(nombre, c);
            // La conversion va sin comprobar, y eso es a proposito: lo unico que se le pide a esta
            // clase es tener un `createUI` estatico. Exigirle ademas ser un ComponentUI dejaria
            // afuera a las fabricas, que es una forma legitima --y usada-- de escribir un aspecto.
            return (Class<? extends ComponentUI>) c;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * La clase que implementa la interfaz grafica de ese componente.
     *
     * @param uiClassID el identificador
     * @return la clase, o {@code null}
     */
    public Class<? extends ComponentUI> getUIClass(String uiClassID) {
        return getUIClass(uiClassID, null);
    }

    /**
     * Aviso de que no se pudo encontrar o construir una interfaz grafica.
     *
     * <p>Se lo puede redefinir para registrarlo en otro lado. Que no lance nada es a proposito: un
     * componente sin interfaz grafica se ve mal, y una aplicacion que se cae se ve peor.
     *
     * @param msg que paso
     */
    protected void getUIError(String msg) {
        System.err.println("UIDefaults.getUI() failed: " + msg);
    }

    /**
     * Construye la interfaz grafica de ese componente.
     *
     * <p>Busca la clase por el identificador que el componente declara y le pide su
     * {@code createUI}. Que la fabricacion pase por un metodo estatico y no por el constructor es lo
     * que permite a una implementacion devolver una instancia compartida entre componentes, que es
     * lo que hacen casi todas.
     *
     * @param target el componente
     * @return la interfaz grafica, o {@code null} si no se pudo
     */
    public ComponentUI getUI(JComponent target) {
        final String id = target.getUIClassID();
        final Class<? extends ComponentUI> clase = getUIClass(id, null);
        if (clase == null) {
            getUIError("no hay clase para " + id);
            return null;
        }
        try {
            final Method m = clase.getMethod("createUI", new Class<?>[] {JComponent.class});
            return (ComponentUI) m.invoke(null, new Object[] {target});
        } catch (Exception e) {
            getUIError("no se pudo crear " + clase.getName() + ": " + e);
            return null;
        }
    }

    /**
     * Registra un oyente de los cambios de la tabla.
     *
     * @param listener el oyente
     */
    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        cambios.addPropertyChangeListener(listener);
    }

    /**
     * Saca un oyente.
     *
     * @param listener el oyente
     */
    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        cambios.removePropertyChangeListener(listener);
    }

    /**
     * Los oyentes registrados.
     *
     * @return los oyentes
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        return cambios.getPropertyChangeListeners();
    }

    /**
     * Avisa de un cambio.
     *
     * @param propertyName la clave que cambio
     * @param oldValue lo que habia
     * @param newValue lo que hay
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        cambios.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Agrega un paquete de textos donde buscar lo que no este en la tabla.
     *
     * @param bundleName el nombre del paquete
     */
    public synchronized void addResourceBundle(String bundleName) {
        if (bundleName != null && !paquetes.contains(bundleName)) {
            paquetes.add(bundleName);
        }
    }

    /**
     * Saca un paquete de textos.
     *
     * @param bundleName el nombre del paquete
     */
    public synchronized void removeResourceBundle(String bundleName) {
        paquetes.remove(bundleName);
    }

    /**
     * Fija el idioma que se usa cuando no se pasa ninguno.
     *
     * @param l el idioma
     */
    public void setDefaultLocale(Locale l) {
        idiomaPorOmision = l;
    }

    /**
     * El idioma que se usa cuando no se pasa ninguno.
     *
     * @return el idioma
     */
    public Locale getDefaultLocale() {
        return idiomaPorOmision;
    }

    /**
     * Busca la clave en los paquetes de textos, del ultimo agregado al primero.
     *
     * <p>Del ultimo al primero para que el que se agrega despues tape al anterior: es lo que permite
     * a una aplicacion cambiar un texto sin reescribir el paquete entero.
     */
    private synchronized Object deLosPaquetes(Object key, Locale l) {
        if (!(key instanceof String)) {
            return null;
        }
        for (int i = paquetes.size() - 1; i >= 0; i--) {
            try {
                final ResourceBundle b = ResourceBundle.getBundle(paquetes.get(i),
                        l == null ? getDefaultLocale() : l);
                return b.getObject((String) key);
            } catch (MissingResourceException e) {
                // Ni el paquete ni la clave: se sigue con el anterior. Que falte es lo normal --por
                // eso hay varios-- y solo importa si no esta en ninguno.
                continue;
            }
        }
        return null;
    }

    /**
     * Un valor que se fabrica cada vez que se lo pide y no se guarda.
     *
     * <p>Es para lo que no se puede compartir entre componentes: si dos reciben el mismo objeto con
     * estado, uno le pisa el estado al otro.
     *
     * @since 1.2
     */
    public interface ActiveValue {

        /**
         * Fabrica el valor.
         *
         * @param table la tabla que lo pide
         * @return el valor
         */
        Object createValue(UIDefaults table);
    }

    /**
     * Un valor que se fabrica la primera vez que se lo pide y despues queda guardado.
     *
     * <p>Un aspecto grafico define miles de entradas y una sesion usa unas pocas: construirlas todas
     * al arrancar seria pagar por lo que no se va a mirar.
     *
     * @since 1.2
     */
    public interface LazyValue {

        /**
         * Fabrica el valor.
         *
         * @param table la tabla que lo pide
         * @return el valor
         */
        Object createValue(UIDefaults table);
    }

    /**
     * Un {@link LazyValue} que fabrica su valor llamando a un metodo por reflexion.
     *
     * <p>Sirve para nombrar en una tabla de datos algo que hay que construir con codigo, sin que la
     * tabla tenga que cargar la clase para poder nombrarla. Esa es la parte que importa: cargarla
     * seria justamente lo que se quiere postergar.
     *
     * @since 1.2
     */
    public static class ProxyLazyValue implements LazyValue {

        private final String className;
        private final String methodName;
        private final Object[] args;

        /**
         * Construye con el constructor sin argumentos de esa clase.
         *
         * @param c el nombre de la clase
         */
        public ProxyLazyValue(String c) {
            this(c, (String) null, null);
        }

        /**
         * Llama a ese metodo estatico sin argumentos.
         *
         * @param c el nombre de la clase
         * @param m el nombre del metodo
         */
        public ProxyLazyValue(String c, String m) {
            this(c, m, null);
        }

        /**
         * Construye con el constructor que acepte esos argumentos.
         *
         * @param c el nombre de la clase
         * @param o los argumentos
         */
        public ProxyLazyValue(String c, Object[] o) {
            this(c, null, o);
        }

        /**
         * Llama a ese metodo estatico con esos argumentos.
         *
         * @param c el nombre de la clase
         * @param m el nombre del metodo, o {@code null} para el constructor
         * @param o los argumentos
         */
        public ProxyLazyValue(String c, String m, Object[] o) {
            this.className = c;
            this.methodName = m;
            this.args = o == null ? null : o.clone();
        }

        /**
         * Fabrica el valor.
         *
         * @param table la tabla que lo pide
         * @return el valor, o {@code null} si no se pudo fabricar
         */
        @Override
        public Object createValue(UIDefaults table) {
            try {
                final Class<?> c = Class.forName(className);
                final Class<?>[] tipos = tiposDe(args);
                if (methodName == null) {
                    return c.getConstructor(tipos).newInstance(args == null ? new Object[0] : args);
                }
                return c.getMethod(methodName, tipos)
                        .invoke(null, args == null ? new Object[0] : args);
            } catch (Exception e) {
                return null;
            }
        }

        private static Class<?>[] tiposDe(Object[] o) {
            if (o == null) {
                return new Class<?>[0];
            }
            final Class<?>[] t = new Class<?>[o.length];
            for (int i = 0; i < o.length; i++) {
                t[i] = o[i] == null ? Object.class : o[i].getClass();
            }
            return t;
        }
    }

    /**
     * Un {@link LazyValue} que arma un mapa de teclas.
     *
     * <p>Los atajos de teclado de un aspecto grafico son cientos y casi ninguno se usa en una sesion
     * dada; armarlos al pedido es la diferencia entre arrancar rapido y no.
     *
     * @since 1.3
     */
    public static class LazyInputMap implements LazyValue {

        private final Object[] bindings;

        /**
         * Con esos pares de tecla y accion, alternados.
         *
         * @param bindings los pares
         */
        public LazyInputMap(Object[] bindings) {
            this.bindings = bindings == null ? null : bindings.clone();
        }

        /**
         * Arma el mapa.
         *
         * @param table la tabla que lo pide
         * @return el mapa, o {@code null} si no hay ataduras
         */
        @Override
        public Object createValue(UIDefaults table) {
            if (bindings == null) {
                return null;
            }
            final InputMap mapa = new InputMap();
            for (int i = 0; i < bindings.length - 1; i += 2) {
                // La tecla puede venir ya resuelta o como texto: la tabla de un aspecto grafico se
                // escribe con textos --"ctrl C"-- porque asi se lee, y se resuelven al armarla.
                final Object k = bindings[i];
                final KeyStroke tecla = k instanceof KeyStroke
                        ? (KeyStroke) k : KeyStroke.getKeyStroke(String.valueOf(k));
                if (tecla != null) {
                    mapa.put(tecla, bindings[i + 1]);
                }
            }
            return mapa;
        }
    }
}
