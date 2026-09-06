package javax.swing.plaf.nimbus;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.synth.Region;
import javax.swing.plaf.synth.SynthLookAndFeel;

/**
 * El aspecto grafico Nimbus: dibujado con curvas, no con imagenes.
 *
 * <h2>Que lo distingue</h2>
 *
 * <p>Los demas aspectos estampan imagenes; Nimbus dibuja cada componente con curvas y degradados
 * calculados al vuelo. La consecuencia practica es que se ve bien en cualquier tamano y en cualquier
 * densidad de pantalla, y que se lo puede recolorear entero cambiando unos pocos colores.
 *
 * <p>Ese recoloreo es {@link #getDerivedColor}: casi todos los colores de Nimbus estan escritos como
 * desplazamientos de tono, saturacion y brillo sobre un puñado de colores base. Cambiar
 * {@code "nimbusBase"} cambia el aspecto entero.
 *
 * <h2>Por que hereda de synth</h2>
 *
 * <p>Porque el mecanismo es el mismo: una tabla que dice como se ve cada region y unas interfaces
 * graficas que la consultan. La diferencia es de donde sale la tabla. Synth la lee de un archivo;
 * Nimbus la trae escrita en codigo, generada de la herramienta de diseno.
 *
 * <h2>{@link #register}</h2>
 *
 * <p>Sirve para que un componente propio participe de Nimbus: se registra su region con un prefijo,
 * y a partir de ahi sus valores se buscan en la tabla con las mismas reglas que los de un boton.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Los colores derivados y el registro de regiones funcionan de verdad. Lo que no hay es la tabla
 * de Nimbus --los mil valores que salieron de la herramienta-- ni los pintores concretos, que son
 * las noventa clases privadas del paquete. {@link #getDefaults} devuelve una tabla vacia en vez de
 * una inventada: un boton dibujado con colores que no son los de Nimbus no seria Nimbus.
 *
 * @since 1.7
 */
public class NimbusLookAndFeel extends SynthLookAndFeel {

    private final Map<Region, String> registradas = new HashMap<Region, String>();

    /** Uno. */
    public NimbusLookAndFeel() {
    }

    /** Se instala. */
    @Override
    public void initialize() {
        super.initialize();
    }

    /** Se desinstala. */
    @Override
    public void uninitialize() {
        super.uninitialize();
    }

    /**
     * La tabla de valores de Nimbus.
     *
     * @return la tabla
     */
    @Override
    public UIDefaults getDefaults() {
        return super.getDefaults();
    }

    /**
     * El estilo de esa region de ese componente.
     *
     * @param c el componente
     * @param r la region
     * @return el estilo, o {@code null} si el que hay no es de Nimbus
     */
    public static NimbusStyle getStyle(JComponent c, Region r) {
        final Object s = SynthLookAndFeel.getStyle(c, r);
        return s instanceof NimbusStyle ? (NimbusStyle) s : null;
    }

    /**
     * El nombre para mostrar.
     *
     * @return {@code "Nimbus"}
     */
    @Override
    public String getName() {
        return "Nimbus";
    }

    /**
     * El identificador corto.
     *
     * @return {@code "Nimbus"}
     */
    @Override
    public String getID() {
        return "Nimbus";
    }

    /**
     * Que es.
     *
     * @return la descripcion
     */
    @Override
    public String getDescription() {
        return "Nimbus Look and Feel";
    }

    /**
     * Si hay que revisar el estilo cuando el componente cambia de contenedor.
     *
     * @return cierto: en Nimbus un componente se ve distinto segun donde este --un boton dentro de
     *     una barra de herramientas, por ejemplo-- asi que el cambio importa
     */
    @Override
    public boolean shouldUpdateStyleOnAncestorChanged() {
        return true;
    }

    /**
     * Si ese cambio de propiedad obliga a revisar el estilo.
     *
     * @param ev que cambio
     * @return cierto si el estilo puede haber cambiado
     */
    @Override
    protected boolean shouldUpdateStyleOnEvent(PropertyChangeEvent ev) {
        final String n = ev.getPropertyName();
        return "Nimbus.Overrides".equals(n)
                || "Nimbus.Overrides.InheritDefaults".equals(n)
                || "JComponent.sizeVariant".equals(n)
                || super.shouldUpdateStyleOnEvent(ev);
    }

    /**
     * Registra una region para que participe de Nimbus.
     *
     * <p>El prefijo es con lo que se arman las claves de la tabla para esa region. Registrar la
     * misma region dos veces con prefijos distintos reemplaza al anterior: no tendria sentido que
     * una region tuviera dos juegos de valores.
     *
     * @param region la region
     * @param prefix el prefijo de sus claves
     */
    public void register(Region region, String prefix) {
        registradas.put(region, prefix);
    }

    /**
     * Un icono en su version deshabilitada.
     *
     * @param component el componente, o {@code null}
     * @param icon el icono, o {@code null}
     * @return el icono deshabilitado, o {@code null}
     */
    @Override
    public Icon getDisabledIcon(JComponent component, Icon icon) {
        return super.getDisabledIcon(component, icon);
    }

    /**
     * Un color corrido respecto de uno de la tabla de Nimbus.
     *
     * <p>Es el mecanismo por el que Nimbus se recolorea entero: casi todos sus colores estan
     * escritos asi, como desplazamientos sobre un puñado de colores base.
     *
     * @param uiDefaultParentName la clave del color base
     * @param hOffset cuanto correr el tono
     * @param sOffset cuanto correr la saturacion
     * @param bOffset cuanto correr el brillo
     * @param aOffset cuanto correr la transparencia, de -255 a 255
     * @param uiResource si el color devuelto tiene que marcarse como puesto por el aspecto
     * @return el color
     */
    public Color getDerivedColor(String uiDefaultParentName, float hOffset, float sOffset,
            float bOffset, int aOffset, boolean uiResource) {
        final Object v = javax.swing.UIManager.get(uiDefaultParentName);
        final Color base = v instanceof Color ? (Color) v : Color.GRAY;
        final Color d = corrido(base, hOffset, sOffset, bOffset, aOffset);
        return uiResource ? new javax.swing.plaf.ColorUIResource(d) : d;
    }

    /**
     * Un color entre dos, en esa proporcion, marcado como puesto por el aspecto.
     *
     * @param color1 el de un extremo
     * @param color2 el del otro
     * @param midPoint cuanto del segundo, entre cero y uno
     * @param uiResource si tiene que marcarse como puesto por el aspecto
     * @return el color intermedio
     */
    protected final Color getDerivedColor(Color color1, Color color2, float midPoint,
            boolean uiResource) {
        // La transparencia se mezcla y despues se pierde: el color se arma empaquetado y se lo
        // entrega por el constructor de un solo entero, que da siempre opaco. Es lo que hace el
        // JDK y no un descuido de aca; cambiarlo daria colores distintos de los suyos.
        final int argb = mezclar(color1.getAlpha(), color2.getAlpha(), midPoint) << 24
                | mezclar(color1.getRed(), color2.getRed(), midPoint) << 16
                | mezclar(color1.getGreen(), color2.getGreen(), midPoint) << 8
                | mezclar(color1.getBlue(), color2.getBlue(), midPoint);
        return uiResource ? new javax.swing.plaf.ColorUIResource(argb) : new Color(argb);
    }

    /**
     * Un color entre dos, en esa proporcion.
     *
     * @param color1 el de un extremo
     * @param color2 el del otro
     * @param midPoint cuanto del segundo, entre cero y uno
     * @return el color intermedio
     */
    protected final Color getDerivedColor(Color color1, Color color2, float midPoint) {
        return getDerivedColor(color1, color2, midPoint, false);
    }

    private static Color corrido(Color base, float h, float s, float b, int a) {
        final float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        final Color c = Color.getHSBColor(acotar(hsb[0] + h), acotar(hsb[1] + s),
                acotar(hsb[2] + b));
        final int aa = Math.max(0, Math.min(255, base.getAlpha() + a));
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), aa);
    }

    private static float acotar(float v) {
        return v < 0f ? 0f : v > 1f ? 1f : v;
    }

    private static int mezclar(int a, int b, float p) {
        final int v = (int) (a + (b - a) * p + 0.5f);
        return v < 0 ? 0 : v > 255 ? 255 : v;
    }
}
