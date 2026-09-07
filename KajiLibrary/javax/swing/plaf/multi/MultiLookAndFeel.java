package javax.swing.plaf.multi;

import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.ComponentUI;

/**
 * El aspecto grafico que no dibuja: reparte cada componente entre el principal y los auxiliares.
 *
 * <h2>Para que</h2>
 *
 * <p>Para colgarle a Swing observadores que necesitan las mismas llamadas que la interfaz grafica de
 * verdad: un lector de pantalla, un registrador de lo que el usuario hace, una ayuda que sigue al
 * foco. Se los agrega con {@link UIManager#addAuxiliaryLookAndFeel} y a partir de ahi cada
 * componente recibe una interfaz de {@code javax.swing.plaf.multi} en lugar de la simple.
 *
 * <h2>Donde se decide</h2>
 *
 * <p>En {@link #createUIs}, que llaman los treinta {@code createUI} del paquete. Ahi se le pide la
 * interfaz al aspecto principal, despues a cada auxiliar, y se devuelve el multiplexor.
 *
 * <p>Salvo cuando hay uno solo: en ese caso se devuelve ese, sin envolver. No es una optimizacion
 * cosmetica --es el caso normal, el de una aplicacion sin auxiliares-- y envolverlo costaria una
 * llamada de mas en cada operacion de cada componente de la pantalla.
 *
 * <h2>Este aspecto no tiene valores propios</h2>
 *
 * <p>{@link #getDefaults} devuelve la tabla del que si dibuja. Es coherente con lo que esta clase
 * es: no aporta colores ni tipografias, solo reparte.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>Funciona: el registro de auxiliares es real y el reparto tambien. Lo que no hay es ningun
 * aspecto grafico implementado del cual repartir, asi que en la practica no hay nada que
 * multiplexar todavia. La mecanica esta y esta probada.
 *
 * @since 1.2
 */
public class MultiLookAndFeel extends LookAndFeel {

    /** Uno. */
    public MultiLookAndFeel() {
    }

    /**
     * El nombre para mostrar.
     *
     * @return {@code "Multiplexing Look and Feel"}
     */
    @Override
    public String getName() {
        return "Multiplexing Look and Feel";
    }

    /**
     * El identificador corto.
     *
     * @return {@code "Multiplex"}
     */
    @Override
    public String getID() {
        return "Multiplex";
    }

    /**
     * Que hace.
     *
     * @return la descripcion
     */
    @Override
    public String getDescription() {
        return "Allows multiple UI instances per component instance";
    }

    /**
     * Si es el aspecto propio de la plataforma.
     *
     * @return falso: este no dibuja nada
     */
    @Override
    public boolean isNativeLookAndFeel() {
        return false;
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
     * La tabla de valores.
     *
     * @return la del aspecto que si dibuja
     */
    @Override
    public UIDefaults getDefaults() {
        return UIManager.getDefaults();
    }

    /**
     * Arma la lista de interfaces graficas de un componente.
     *
     * <p>Primero la del aspecto principal y despues la de cada auxiliar, en el orden en que se los
     * agrego. Ese orden es el que decide quien contesta cuando un metodo devuelve un valor: la
     * primera, o sea el principal.
     *
     * <p>Si el principal no dio ninguna, no hay a que agregarle auxiliares y se devuelve
     * {@code null}: un componente sin interfaz grafica es un problema del aspecto principal, y
     * taparlo con un multiplexor vacio lo convertiria en un fallo mas tarde y en otro lado.
     *
     * @param mui el multiplexor que quedaria a cargo
     * @param uis la lista donde anotarlas; se la modifica
     * @param target el componente
     * @return el multiplexor, o la unica interfaz si no hay auxiliares, o {@code null}
     */
    public static ComponentUI createUIs(ComponentUI mui, Vector<ComponentUI> uis,
            JComponent target) {
        ComponentUI ui = UIManager.getDefaults().getUI(target);
        if (ui == null) {
            return null;
        }
        uis.addElement(ui);
        final LookAndFeel[] auxiliares = UIManager.getAuxiliaryLookAndFeels();
        if (auxiliares != null) {
            for (int i = 0; i < auxiliares.length; i++) {
                final UIDefaults tabla = auxiliares[i].getDefaults();
                if (tabla == null) {
                    continue;
                }
                ui = tabla.getUI(target);
                if (ui != null) {
                    uis.addElement(ui);
                }
            }
        }
        return uis.size() == 1 ? uis.elementAt(0) : mui;
    }

    /**
     * La lista de interfaces graficas como arreglo.
     *
     * @param uis la lista, o {@code null}
     * @return un arreglo nuevo; vacio si la lista era {@code null}
     */
    protected static ComponentUI[] uisToArray(Vector<? extends ComponentUI> uis) {
        if (uis == null) {
            return new ComponentUI[0];
        }
        final ComponentUI[] out = new ComponentUI[uis.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = uis.elementAt(i);
        }
        return out;
    }
}
