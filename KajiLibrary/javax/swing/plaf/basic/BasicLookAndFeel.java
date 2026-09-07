package javax.swing.plaf.basic;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;

/**
 * La base de la que heredan todos los aspectos graficos de Swing.
 *
 * <h2>Que aporta</h2>
 *
 * <p>La tabla de valores por omision: que clase dibuja cada componente, que colores tiene el
 * sistema, y los cientos de valores que un componente consulta al instalarse. Un aspecto concreto
 * hereda de aca y redefine lo que quiere cambiar, que suele ser una fraccion.
 *
 * <p>Esta clase no se usa directamente --su constructor es {@code protected}-- porque por si sola no
 * define ningun aspecto completo. Es el andamio.
 *
 * <h2>Los tres pasos de {@link #getDefaults}</h2>
 *
 * <p>Primero las clases de las interfaces graficas, despues los colores del sistema, despues todo lo
 * demas. El orden importa: los valores de {@link #initComponentDefaults} se escriben en terminos de
 * los colores del sistema, asi que esos tienen que estar antes.
 *
 * <h2>Los sonidos</h2>
 *
 * <p>Un aspecto grafico puede tener sonidos --el clic de un menu, el aviso de un dialogo-- y
 * {@link #getAudioActionMap} es donde se los declara. Estan aca y no en el aspecto concreto porque
 * el mecanismo de reproducirlos es el mismo para todos.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>La estructura esta y funciona: se puede heredar, poblar la tabla y consultarla. Lo que no hay
 * es contenido: {@link #initClassDefaults} no registra ninguna interfaz grafica porque esta
 * biblioteca todavia no tiene un aspecto concreto que las provea. Un aspecto que herede de aca y
 * llene la tabla anda.
 *
 * @since 1.2
 */
public abstract class BasicLookAndFeel extends LookAndFeel {

    /** Uno; solo para las subclases. */
    protected BasicLookAndFeel() {
    }

    /**
     * La tabla de valores de este aspecto.
     *
     * @return la tabla, ya poblada
     */
    @Override
    public UIDefaults getDefaults() {
        final UIDefaults table = new UIDefaults();
        initClassDefaults(table);
        initSystemColorDefaults(table);
        initComponentDefaults(table);
        return table;
    }

    /** Se instala. */
    @Override
    public void initialize() {
    }

    /** Se desinstala. */
    @Override
    public void uninitialize() {
    }

    /**
     * Registra que clase dibuja cada componente.
     *
     * <p>Las claves son los identificadores que devuelve {@code JComponent.getUIClassID}, como
     * {@code "ButtonUI"}, y los valores son nombres de clase. Van como texto y no como {@code Class}
     * para no cargar las cien clases de un aspecto al arrancar; ver {@link UIDefaults}.
     *
     * @param table la tabla a poblar
     */
    protected void initClassDefaults(UIDefaults table) {
    }

    /**
     * Pone los colores del sistema.
     *
     * @param table la tabla a poblar
     */
    protected void initSystemColorDefaults(UIDefaults table) {
    }

    /**
     * Carga los colores del sistema desde una lista de pares nombre-valor.
     *
     * <p>Los valores son enteros en hexadecimal escritos como texto. El interruptor decide si se
     * usan esos o los que reporte el escritorio: un aspecto que quiere verse igual en todos lados
     * usa los suyos, y uno que quiere integrarse usa los del sistema.
     *
     * @param table la tabla a poblar
     * @param systemColors los pares, alternados
     * @param useNative si hay que preferir los del escritorio
     */
    protected void loadSystemColors(UIDefaults table, String[] systemColors, boolean useNative) {
        for (int i = 0; i < systemColors.length - 1; i += 2) {
            table.put(systemColors[i], new java.awt.Color(
                    (int) Long.parseLong(systemColors[i + 1].substring(1), 16)));
        }
    }

    /**
     * Pone todo lo demas: colores, tipografias, bordes, margenes y atajos de teclado.
     *
     * @param table la tabla a poblar
     */
    protected void initComponentDefaults(UIDefaults table) {
    }

    /**
     * Los sonidos de este aspecto.
     *
     * @return el mapa de acciones de sonido, o {@code null} si no tiene
     */
    protected ActionMap getAudioActionMap() {
        return null;
    }

    /**
     * Fabrica la accion que reproduce un sonido.
     *
     * @param key la clave del sonido en la tabla
     * @return la accion, o {@code null} si no hay sonido para esa clave
     */
    protected Action createAudioAction(Object key) {
        return null;
    }

    /**
     * Reproduce el sonido de esa accion.
     *
     * <p>No hace nada si la accion es {@code null}: quien llama no tiene por que comprobar si el
     * aspecto define ese sonido, y hacerlo en cada sitio de llamada seria la misma comprobacion
     * repetida veinte veces.
     *
     * @param audioAction la accion, o {@code null}
     */
    protected void playSound(Action audioAction) {
    }
}
