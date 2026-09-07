package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.io.InputStream;
import java.text.ParseException;

import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicLookAndFeel;

/**
 * El aspecto grafico que no dibuja nada por su cuenta: lo describe otro.
 *
 * <h2>Que problema resuelve</h2>
 *
 * <p>Escribir un aspecto grafico para Swing significaba escribir cien clases. Synth invierte eso:
 * las cien clases ya estan --son las {@code SynthXxxUI}-- y lo que cambia es un archivo que dice de
 * que color es cada parte y con que imagen se dibuja. Un disenador puede cambiar el aspecto sin
 * escribir Java.
 *
 * <p>{@link #load} es donde entra esa descripcion. {@link javax.swing.plaf.nimbus.NimbusLookAndFeel}
 * es el otro camino: en vez de leer un archivo, trae la descripcion escrita en codigo.
 *
 * <h2>La fabrica de estilos</h2>
 *
 * <p>{@link #setStyleFactory} es el punto de union. Cada {@code SynthXxxUI} le pide a la fabrica el
 * estilo de su region, y la fabrica es lo unico que sabe de donde salio la descripcion. Es estatica
 * --y no un campo de la instancia-- porque las interfaces graficas la consultan sin tener a mano el
 * aspecto.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>El registro de la fabrica, las regiones y el ciclo de vida funcionan. Lo que no hay son las
 * {@code SynthXxxUI}, que son cuarenta clases de dibujo y son casi todo el paquete; por eso
 * {@link #createUI} no tiene que devolver y {@link #load} no tiene que leer. Lo que si esta
 * completo es lo que {@code javax.swing.plaf.nimbus} necesita.
 *
 * @since 1.5
 */
public class SynthLookAndFeel extends BasicLookAndFeel {

    private static SynthStyleFactory fabrica;

    /** Uno. */
    public SynthLookAndFeel() {
    }

    /**
     * Fija de donde salen los estilos.
     *
     * <p>Es estatica porque las interfaces graficas la consultan sin tener a mano el aspecto. La
     * consecuencia es que hay una sola por proceso, que es coherente con que haya un solo aspecto
     * grafico por proceso.
     *
     * @param cache la fabrica, o {@code null} para no tener ninguna
     */
    public static void setStyleFactory(SynthStyleFactory cache) {
        synchronized (SynthLookAndFeel.class) {
            fabrica = cache;
        }
    }

    /**
     * De donde salen los estilos.
     *
     * @return la fabrica, o {@code null}
     */
    public static SynthStyleFactory getStyleFactory() {
        synchronized (SynthLookAndFeel.class) {
            return fabrica;
        }
    }

    /**
     * El estilo de esa region de ese componente.
     *
     * @param c el componente
     * @param region la region
     * @return el estilo, o {@code null} si no hay fabrica
     */
    public static SynthStyle getStyle(JComponent c, Region region) {
        final SynthStyleFactory f = getStyleFactory();
        return f == null ? null : f.getStyle(c, region);
    }

    /**
     * Vuelve a pedir el estilo de ese componente y de todos los que tenga adentro.
     *
     * <p>Hace falta cuando cambia algo que la fabrica mira para decidir: el nombre de un componente,
     * una propiedad, el aspecto entero. Sin esto, los componentes que ya estaban en pantalla se
     * quedarian con el estilo viejo.
     *
     * @param c el componente desde el que bajar
     */
    public static void updateStyles(Component c) {
        if (c instanceof JComponent) {
            ((JComponent) c).updateUI();
        }
        if (c instanceof Container) {
            final Container cont = (Container) c;
            for (int i = 0; i < cont.getComponentCount(); i++) {
                updateStyles(cont.getComponent(i));
            }
        }
    }

    /**
     * La region que le corresponde a ese componente.
     *
     * @param c el componente
     * @return la region, o {@code null} si su identificador no corresponde a ninguna
     */
    public static Region getRegion(JComponent c) {
        return Region.porUI(c.getUIClassID());
    }

    /**
     * La interfaz grafica de ese componente.
     *
     * @param c el componente
     * @return la interfaz grafica, o {@code null}
     * @throws UnsupportedOperationException en esta biblioteca, que no tiene las {@code SynthXxxUI}
     */
    /**
     * La interfaz grafica de Synth para ese componente.
     *
     * <p>El nombre de la clase sale del identificador del componente: un {@code JLabel} dice
     * {@code "LabelUI"} y de ahi sale {@code javax.swing.plaf.synth.SynthLabelUI}. Esa regla es la
     * que hace que el paquete no tenga una tabla de cuarenta entradas que mantener.
     *
     * @param c el componente
     * @return la interfaz grafica
     * @throws Error si no hay una clase de Synth para ese componente
     */
    public static ComponentUI createUI(JComponent c) {
        final String nombre = "javax.swing.plaf.synth.Synth" + c.getUIClassID();
        try {
            final Class<?> k = Class.forName(nombre);
            final java.lang.reflect.Method m = k.getMethod("createUI", JComponent.class);
            return (ComponentUI) m.invoke(null, c);
        } catch (Exception e) {
            throw new Error("no hay una clase de synth para " + c.getUIClassID(), e);
        }
    }

    /**
     * El estado de ese componente, como lo entiende Synth.
     *
     * <p>Tres casos y nada mas: apagado, encendido, y encendido con el foco. Los otros estados de
     * {@link SynthConstants} -- apretado, con el cursor encima, elegido -- los agrega cada
     * interfaz grafica mirando su propio modelo, porque un componente sin botones no tiene como
     * estar apretado.
     *
     * @param c el componente
     * @return la combinacion de banderas
     */
    static int estadoDe(java.awt.Component c) {
        if (c == null || !c.isEnabled()) {
            return SynthConstants.DISABLED;
        }
        if (c.isFocusOwner()) {
            return SynthConstants.ENABLED | SynthConstants.FOCUSED;
        }
        return SynthConstants.ENABLED;
    }

    /**
     * Le pide a la fabrica el estilo de ese contexto y se lo instala al componente.
     *
     * <p>Es lo que hace cada {@code SynthXxxUI} al instalarse y cada vez que algo cambia. Si no hay
     * fabrica revienta con {@code NullPointerException}, y <strong>eso esta medido</strong>: en el
     * JDK, instalar cualquier interfaz grafica de Synth sin haber cargado antes un archivo de
     * estilos tira exactamente esa excepcion. No es un descuido de esta biblioteca; es que Synth no
     * tiene un aspecto por omision, y ese es justamente el punto del paquete.
     *
     * @param context el contexto
     * @return el estilo nuevo
     */
    static SynthStyle actualizar(SynthContext context) {
        final SynthStyleFactory f = getStyleFactory();
        final SynthStyle estilo = f.getStyle(context.getComponent(), context.getRegion());
        if (estilo != null) {
            estilo.installDefaults(new SynthContext(context.getComponent(),
                    context.getRegion(), estilo, context.getComponentState()));
        }
        return estilo;
    }

    /**
     * Lee la descripcion del aspecto de un archivo.
     *
     * @param input de donde leer
     * @param resourceBase la clase respecto de la cual resolver los recursos que el archivo nombre
     * @throws ParseException si el archivo no se entiende
     * @throws UnsupportedOperationException en esta biblioteca, que no tiene el lector
     */
    public void load(InputStream input, Class<?> resourceBase) throws ParseException {
        throw new UnsupportedOperationException("esta biblioteca no tiene el lector de "
                + "descripciones de synth");
    }

    /** Se instala. */
    @Override
    public void initialize() {
        super.initialize();
    }

    /** Se desinstala; suelta la fabrica de estilos. */
    @Override
    public void uninitialize() {
        setStyleFactory(null);
        super.uninitialize();
    }

    /**
     * La tabla de valores.
     *
     * @return la tabla
     */
    @Override
    public UIDefaults getDefaults() {
        return super.getDefaults();
    }

    /**
     * Si sirve en esta plataforma.
     *
     * @return cierto: no depende de la plataforma
     */
    @Override
    public boolean isSupportedLookAndFeel() {
        return true;
    }

    /**
     * Si es el aspecto propio de la plataforma.
     *
     * @return falso
     */
    @Override
    public boolean isNativeLookAndFeel() {
        return false;
    }

    /**
     * Que es.
     *
     * @return la descripcion
     */
    @Override
    public String getDescription() {
        return "Synth Look and Feel";
    }

    /**
     * El nombre para mostrar.
     *
     * @return {@code "Synth Look and Feel"}
     */
    @Override
    public String getName() {
        return "Synth Look and Feel";
    }

    /**
     * El identificador corto.
     *
     * @return {@code "Synth"}
     */
    @Override
    public String getID() {
        return "Synth";
    }

    /**
     * Si hay que revisar el estilo cuando el componente cambia de contenedor.
     *
     * <p>Con {@code false} se ahorra trabajo; con {@code true} un aspecto puede decidir el estilo
     * segun donde este el componente --un boton dentro de una barra de herramientas se ve
     * distinto--. Por omision no, porque la mayoria de los aspectos no lo necesita y revisarlo
     * cuesta en cada agregado.
     *
     * @return falso
     */
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return false;
    }

    /**
     * Si ese cambio de propiedad obliga a revisar el estilo.
     *
     * @param ev que cambio
     * @return cierto si el estilo puede haber cambiado
     */
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        final String n = ev.getPropertyName();
        return "name".equals(n) || "componentOrientation".equals(n)
                || "ancestor".equals(n) && shouldUpdateStyleOnAncestorChanged();
    }
}
