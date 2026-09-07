package javax.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

/**
 * El registro global del aspecto grafico: quien manda y con que valores.
 *
 * <h2>Que guarda</h2>
 *
 * <p>Tres cosas. El aspecto grafico actual, su tabla de valores, y la lista de aspectos auxiliares
 * --los que no dibujan pero quieren enterarse de todo, como un lector de pantalla--.
 *
 * <p>Todo es estatico porque un proceso tiene un aspecto grafico y no varios: dos ventanas de la
 * misma aplicacion con botones de distinto aspecto seria un error, no una funcionalidad.
 *
 * <h2>Por que casi todos los metodos son atajos</h2>
 *
 * <p>{@link #getColor}, {@link #getFont} y compania son lo mismo que preguntarle a
 * {@link #getDefaults}. Estan porque el codigo que los usa los usa mucho, y
 * {@code UIManager.getColor("Button.background")} se lee mejor que la version larga.
 *
 * <h2>Los aspectos auxiliares</h2>
 *
 * <p>Se agregan con {@link #addAuxiliaryLookAndFeel} y a partir de ahi cada componente recibe una
 * interfaz grafica que reparte entre el principal y ellos; ver {@code javax.swing.plaf.multi}. La
 * lista arranca en {@code null} y no en vacia, y {@link #getAuxiliaryLookAndFeels} devuelve
 * {@code null} mientras no haya ninguno: es lo que le permite a quien pregunta saltearse el trabajo
 * de multiplexar en el caso normal, que es que no haya.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>El registro funciona: se puede fijar un aspecto, leer sus valores, agregar auxiliares y
 * escuchar los cambios. Lo que no hay es ningun aspecto grafico implementado, asi que
 * {@link #setLookAndFeel(String)} con cualquiera de los nombres que devuelve
 * {@link #getInstalledLookAndFeels} falla al cargar la clase. Esos nombres son los del JDK y son
 * correctos como nombres; lo que falta son las clases.
 *
 * @since 1.2
 */
public class UIManager implements Serializable {

    private static final long serialVersionUID = -5547977484831201933L;

    private static final PropertyChangeSupport CAMBIOS = new PropertyChangeSupport(UIManager.class);

    private static LookAndFeelInfo[] instalados = {
        new LookAndFeelInfo("Metal", "javax.swing.plaf.metal.MetalLookAndFeel"),
        new LookAndFeelInfo("Nimbus", "javax.swing.plaf.nimbus.NimbusLookAndFeel"),
        new LookAndFeelInfo("CDE/Motif", "com.sun.java.swing.plaf.motif.MotifLookAndFeel"),
        new LookAndFeelInfo("Windows", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel"),
        new LookAndFeelInfo("Windows Classic",
                "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel"),
    };

    private static LookAndFeel actual;
    private static UIDefaults valores = new UIDefaults();
    private static List<LookAndFeel> auxiliares;

    /** Un registro; todo lo util es estatico. */
    public UIManager() {
    }

    /**
     * Los aspectos graficos que se pueden elegir.
     *
     * @return un arreglo nuevo con los que hay
     */
    public static LookAndFeelInfo[] getInstalledLookAndFeels() {
        return instalados.clone();
    }

    /**
     * Reemplaza la lista de aspectos disponibles.
     *
     * @param infos los aspectos
     * @throws NullPointerException si el arreglo o alguno de sus elementos es {@code null}
     */
    public static void setInstalledLookAndFeels(LookAndFeelInfo[] infos) {
        if (infos == null) {
            throw new NullPointerException("infos");
        }
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] == null) {
                throw new NullPointerException("infos[" + i + "]");
            }
        }
        instalados = infos.clone();
    }

    /**
     * Agrega un aspecto a la lista de disponibles.
     *
     * @param info el aspecto
     */
    public static void installLookAndFeel(LookAndFeelInfo info) {
        final LookAndFeelInfo[] nuevos = new LookAndFeelInfo[instalados.length + 1];
        System.arraycopy(instalados, 0, nuevos, 0, instalados.length);
        nuevos[instalados.length] = info;
        instalados = nuevos;
    }

    /**
     * Agrega un aspecto a la lista de disponibles.
     *
     * @param name el nombre para mostrar
     * @param className la clase que lo implementa
     */
    public static void installLookAndFeel(String name, String className) {
        installLookAndFeel(new LookAndFeelInfo(name, className));
    }

    /**
     * El aspecto grafico actual.
     *
     * @return el aspecto, o {@code null} si no se fijo ninguno
     */
    public static LookAndFeel getLookAndFeel() {
        return actual;
    }

    /**
     * Construye el aspecto grafico de esa clase.
     *
     * @param className la clase
     * @return el aspecto
     * @throws UnsupportedLookAndFeelException si no se pudo construir
     */
    public static LookAndFeel createLookAndFeel(String className)
            throws UnsupportedLookAndFeelException {
        try {
            return (LookAndFeel) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new UnsupportedLookAndFeelException(className + ": " + e);
        }
    }

    /**
     * Fija el aspecto grafico.
     *
     * <p>El orden es el que importa: primero se desinstala el anterior, despues se instala el nuevo,
     * despues se toma su tabla de valores, y recien al final se avisa. Avisar antes haria que quien
     * escucha redibujara con la tabla vieja.
     *
     * @param newLookAndFeel el aspecto, o {@code null} para dejar de tener uno
     * @throws UnsupportedLookAndFeelException si ese aspecto no sirve en esta plataforma
     */
    public static void setLookAndFeel(LookAndFeel newLookAndFeel)
            throws UnsupportedLookAndFeelException {
        if (newLookAndFeel != null && !newLookAndFeel.isSupportedLookAndFeel()) {
            throw new UnsupportedLookAndFeelException(newLookAndFeel + " no esta soportado");
        }
        final LookAndFeel anterior = actual;
        if (anterior != null) {
            anterior.uninitialize();
        }
        actual = newLookAndFeel;
        if (newLookAndFeel != null) {
            newLookAndFeel.initialize();
            valores = newLookAndFeel.getDefaults();
        } else {
            valores = new UIDefaults();
        }
        CAMBIOS.firePropertyChange("lookAndFeel", anterior, newLookAndFeel);
    }

    /**
     * Fija el aspecto grafico por el nombre de su clase.
     *
     * @param className la clase
     * @throws ClassNotFoundException si no esta la clase
     * @throws InstantiationException si no se pudo construir
     * @throws IllegalAccessException si no se pudo acceder al constructor
     * @throws UnsupportedLookAndFeelException si ese aspecto no sirve en esta plataforma
     */
    public static void setLookAndFeel(String className) throws ClassNotFoundException,
            InstantiationException, IllegalAccessException, UnsupportedLookAndFeelException {
        final Class<?> c = Class.forName(className);
        final Object o;
        try {
            o = c.getDeclaredConstructor().newInstance();
        } catch (InstantiationException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Exception e) {
            throw new InstantiationException(className + ": " + e);
        }
        setLookAndFeel((LookAndFeel) o);
    }

    /**
     * El aspecto grafico propio de esta plataforma.
     *
     * @return el nombre de la clase
     */
    public static String getSystemLookAndFeelClassName() {
        final String so = System.getProperty("os.name");
        if (so != null && so.startsWith("Windows")) {
            return "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        }
        return getCrossPlatformLookAndFeelClassName();
    }

    /**
     * El aspecto grafico que se ve igual en todas las plataformas.
     *
     * @return el nombre de la clase
     */
    public static String getCrossPlatformLookAndFeelClassName() {
        return "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    /**
     * La tabla de valores en uso.
     *
     * @return la tabla
     */
    public static UIDefaults getDefaults() {
        return valores;
    }

    /**
     * La tabla de valores del aspecto grafico actual.
     *
     * @return la tabla
     */
    public static UIDefaults getLookAndFeelDefaults() {
        return valores;
    }

    /**
     * La tipografia de esa clave.
     *
     * @param key la clave
     * @return la tipografia, o {@code null}
     */
    public static Font getFont(Object key) {
        return getDefaults().getFont(key);
    }

    /**
     * La tipografia de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return la tipografia, o {@code null}
     */
    public static Font getFont(Object key, Locale l) {
        return getDefaults().getFont(key, l);
    }

    /**
     * El color de esa clave.
     *
     * @param key la clave
     * @return el color, o {@code null}
     */
    public static Color getColor(Object key) {
        return getDefaults().getColor(key);
    }

    /**
     * El color de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el color, o {@code null}
     */
    public static Color getColor(Object key, Locale l) {
        return getDefaults().getColor(key, l);
    }

    /**
     * El icono de esa clave.
     *
     * @param key la clave
     * @return el icono, o {@code null}
     */
    public static Icon getIcon(Object key) {
        return getDefaults().getIcon(key);
    }

    /**
     * El icono de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el icono, o {@code null}
     */
    public static Icon getIcon(Object key, Locale l) {
        return getDefaults().getIcon(key, l);
    }

    /**
     * El borde de esa clave.
     *
     * @param key la clave
     * @return el borde, o {@code null}
     */
    public static Border getBorder(Object key) {
        return getDefaults().getBorder(key);
    }

    /**
     * El borde de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el borde, o {@code null}
     */
    public static Border getBorder(Object key, Locale l) {
        return getDefaults().getBorder(key, l);
    }

    /**
     * El texto de esa clave.
     *
     * @param key la clave
     * @return el texto, o {@code null}
     */
    public static String getString(Object key) {
        return getDefaults().getString(key);
    }

    /**
     * El texto de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el texto, o {@code null}
     */
    public static String getString(Object key, Locale l) {
        return getDefaults().getString(key, l);
    }

    /**
     * El numero entero de esa clave.
     *
     * @param key la clave
     * @return el numero, o cero
     */
    public static int getInt(Object key) {
        return getDefaults().getInt(key);
    }

    /**
     * El numero entero de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el numero, o cero
     */
    public static int getInt(Object key, Locale l) {
        return getDefaults().getInt(key, l);
    }

    /**
     * El valor de verdad de esa clave.
     *
     * @param key la clave
     * @return el valor, o falso
     */
    public static boolean getBoolean(Object key) {
        return getDefaults().getBoolean(key);
    }

    /**
     * El valor de verdad de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el valor, o falso
     */
    public static boolean getBoolean(Object key, Locale l) {
        return getDefaults().getBoolean(key, l);
    }

    /**
     * Los margenes de esa clave.
     *
     * @param key la clave
     * @return los margenes, o {@code null}
     */
    public static Insets getInsets(Object key) {
        return getDefaults().getInsets(key);
    }

    /**
     * Los margenes de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return los margenes, o {@code null}
     */
    public static Insets getInsets(Object key, Locale l) {
        return getDefaults().getInsets(key, l);
    }

    /**
     * El tamano de esa clave.
     *
     * @param key la clave
     * @return el tamano, o {@code null}
     */
    public static Dimension getDimension(Object key) {
        return getDefaults().getDimension(key);
    }

    /**
     * El tamano de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el tamano, o {@code null}
     */
    public static Dimension getDimension(Object key, Locale l) {
        return getDefaults().getDimension(key, l);
    }

    /**
     * El valor de esa clave.
     *
     * @param key la clave
     * @return el valor, o {@code null}
     */
    public static Object get(Object key) {
        return getDefaults().get(key);
    }

    /**
     * El valor de esa clave en ese idioma.
     *
     * @param key la clave
     * @param l el idioma
     * @return el valor, o {@code null}
     */
    public static Object get(Object key, Locale l) {
        return getDefaults().get(key, l);
    }

    /**
     * Guarda un valor.
     *
     * @param key la clave
     * @param value el valor
     * @return el valor anterior, o {@code null}
     */
    public static Object put(Object key, Object value) {
        return getDefaults().put(key, value);
    }

    /**
     * La interfaz grafica que le toca a ese componente.
     *
     * @param target el componente
     * @return la interfaz grafica, o {@code null}
     */
    public static ComponentUI getUI(JComponent target) {
        return getDefaults().getUI(target);
    }

    /**
     * Agrega un aspecto auxiliar.
     *
     * <p>Un auxiliar no dibuja: recibe las mismas llamadas que el principal para poder enterarse.
     * De eso viven los lectores de pantalla y las ayudas contextuales.
     *
     * @param laf el aspecto auxiliar
     */
    public static void addAuxiliaryLookAndFeel(LookAndFeel laf) {
        if (laf == null) {
            return;
        }
        if (!laf.isSupportedLookAndFeel()) {
            return;
        }
        synchronized (UIManager.class) {
            if (auxiliares == null) {
                auxiliares = new ArrayList<LookAndFeel>();
            }
            if (!auxiliares.contains(laf)) {
                auxiliares.add(laf);
                laf.initialize();
            }
        }
    }

    /**
     * Saca un aspecto auxiliar.
     *
     * @param laf el aspecto auxiliar
     * @return cierto si estaba
     */
    public static boolean removeAuxiliaryLookAndFeel(LookAndFeel laf) {
        synchronized (UIManager.class) {
            if (auxiliares == null || !auxiliares.remove(laf)) {
                return false;
            }
            laf.uninitialize();
            if (auxiliares.isEmpty()) {
                // Vuelve a null y no queda vacia: `getAuxiliaryLookAndFeels` promete null cuando no
                // hay ninguno, y eso es lo que le permite a quien pregunta saltearse el
                // multiplexado en el caso normal.
                auxiliares = null;
            }
            return true;
        }
    }

    /**
     * Los aspectos auxiliares.
     *
     * @return un arreglo nuevo, o {@code null} si no hay ninguno
     */
    public static LookAndFeel[] getAuxiliaryLookAndFeels() {
        synchronized (UIManager.class) {
            if (auxiliares == null || auxiliares.isEmpty()) {
                return null;
            }
            return auxiliares.toArray(new LookAndFeel[auxiliares.size()]);
        }
    }

    /**
     * Registra un oyente de los cambios del registro.
     *
     * @param listener el oyente
     */
    public static void addPropertyChangeListener(PropertyChangeListener listener) {
        CAMBIOS.addPropertyChangeListener(listener);
    }

    /**
     * Saca un oyente.
     *
     * @param listener el oyente
     */
    public static void removePropertyChangeListener(PropertyChangeListener listener) {
        CAMBIOS.removePropertyChangeListener(listener);
    }

    /**
     * Los oyentes registrados.
     *
     * @return los oyentes
     */
    public static PropertyChangeListener[] getPropertyChangeListeners() {
        return CAMBIOS.getPropertyChangeListeners();
    }

    /**
     * El nombre y la clase de un aspecto grafico disponible.
     *
     * <p>Guarda el <strong>nombre de la clase</strong> y no la clase: la lista se arma al arrancar y
     * cargar todos los aspectos para poder ofrecerlos costaria mucho mas de lo que vale ofrecer los
     * que nadie va a elegir.
     *
     * @since 1.2
     */
    public static class LookAndFeelInfo {

        private final String name;
        private final String className;

        /**
         * Con ese nombre y esa clase.
         *
         * @param name el nombre para mostrar
         * @param className la clase que lo implementa
         */
        public LookAndFeelInfo(String name, String className) {
            this.name = name;
            this.className = className;
        }

        /**
         * El nombre para mostrar.
         *
         * @return el nombre
         */
        public String getName() {
            return name;
        }

        /**
         * La clase que lo implementa.
         *
         * @return el nombre de la clase
         */
        public String getClassName() {
            return className;
        }

        /**
         * Una descripcion, para el registro.
         *
         * @return el nombre y la clase
         */
        @Override
        public String toString() {
            return getClass().getName() + "[" + getName() + " " + getClassName() + "]";
        }
    }
}
