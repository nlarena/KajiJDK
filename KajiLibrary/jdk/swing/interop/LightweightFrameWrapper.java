package jdk.swing.interop;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowFocusListener;

/**
 * La ventana que no se ve, dentro de la que Swing dibuja cuando lo hospeda otro juego de
 * herramientas graficas.
 *
 * <h2>Para que una ventana que no se muestra</h2>
 *
 * <p>Swing no sabe dibujar sin una ventana: la jerarquia de componentes tiene que terminar en una,
 * de ahi salen el foco, el repintado y el despacho de eventos. Cuando quien muestra la interfaz es
 * otro juego de herramientas, la ventana igual tiene que existir --si no, nada de Swing anda-- pero
 * nunca aparece en pantalla. Se dibuja en memoria y el hospedador copia el resultado.
 *
 * <h2>Los eventos al reves</h2>
 *
 * <p>Los cuatro {@code create...Event} van en la direccion contraria a la habitual: el mouse y el
 * teclado los recibe el hospedador, no AWT, asi que hay que fabricar el evento de AWT que
 * corresponde e inyectarlo. {@link #createUngrabEvent} es el que avisa que un menu emergente tiene
 * que cerrarse porque el usuario hizo clic afuera.
 *
 * <p>{@link #emulateActivation} es del mismo tipo: la ventana nunca recibe el foco del sistema
 * porque no esta en pantalla, asi que hay que decirle que lo tiene para que Swing dibuje el cursor
 * de texto y los bordes de foco.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>No se puede construir uno. El constructor arma una ventana, y en una maquina sin pantalla eso
 * tira {@link HeadlessException} --tambien en el JDK--, y esta implementacion no habla con ningun
 * sistema de ventanas. Los metodos estan porque la clase los declara; ninguno es alcanzable, y todos
 * tiran lo mismo por si algun dia dejara de ser cierto.
 *
 * @since 9
 */
public class LightweightFrameWrapper {

    /**
     * Uno.
     *
     * @throws HeadlessException siempre: hace falta una ventana, y no hay pantalla
     */
    public LightweightFrameWrapper() {
        throw new HeadlessException();
    }

    /**
     * Avisa que cambio la densidad de la pantalla.
     *
     * @param scale la escala nueva
     */
    public void notifyDisplayChanged(int scale) {
        throw new HeadlessException();
    }

    /**
     * Avisa que cambio la densidad de la pantalla, con escalas distintas por eje.
     *
     * @param scaleX la escala horizontal
     * @param scaleY la escala vertical
     */
    public void notifyDisplayChanged(double scaleX, double scaleY) {
        throw new HeadlessException();
    }

    /**
     * Le dice donde esta la ventana del hospedador.
     *
     * <p>Hace falta para los menus emergentes, que son ventanas de verdad y tienen que aparecer en
     * el lugar correcto de la pantalla, no del componente.
     *
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void setHostBounds(int x, int y, int w, int h) {
        throw new HeadlessException();
    }

    /** La cierra y suelta lo que tenga tomado. */
    public void dispose() {
        throw new HeadlessException();
    }

    /**
     * Agrega quien quiera enterarse de que la ventana gano o perdio el foco.
     *
     * @param listener a quien avisarle
     */
    public void addWindowFocusListener(WindowFocusListener listener) {
        throw new HeadlessException();
    }

    /**
     * La muestra o la esconde.
     *
     * @param visible si se muestra
     */
    public void setVisible(boolean visible) {
        throw new HeadlessException();
    }

    /**
     * La mueve y la redimensiona.
     *
     * @param x la esquina izquierda
     * @param y la esquina de arriba
     * @param w el ancho
     * @param h el alto
     */
    public void setBounds(int x, int y, int w, int h) {
        throw new HeadlessException();
    }

    /**
     * Le pone el contenido que va a hospedar.
     *
     * @param content el contenido
     */
    public void setContent(LightweightContentWrapper content) {
        throw new HeadlessException();
    }

    /**
     * Le hace creer que gano o perdio el foco del sistema.
     *
     * @param activate si se la da por activa
     */
    public void emulateActivation(boolean activate) {
        throw new HeadlessException();
    }

    /**
     * Fabrica un evento de mouse para inyectarlo.
     *
     * @param frame la ventana
     * @param id que evento es
     * @param when cuando paso
     * @param modifiers las teclas y botones apretados
     * @param x donde, respecto de la ventana
     * @param y donde, respecto de la ventana
     * @param xAbs donde, en pantalla
     * @param yAbs donde, en pantalla
     * @param clickCount cuantos clics seguidos
     * @param popupTrigger si es el gesto que abre el menu contextual
     * @param button que boton
     * @return el evento
     */
    public MouseEvent createMouseEvent(LightweightFrameWrapper frame, int id, long when,
            int modifiers, int x, int y, int xAbs, int yAbs, int clickCount, boolean popupTrigger,
            int button) {
        throw new HeadlessException();
    }

    /**
     * Fabrica un evento de rueda para inyectarlo.
     *
     * @param frame la ventana
     * @param scrollType si el desplazamiento es por unidades o por pantallas
     * @param scrollAmount cuantas unidades
     * @param wheelRotation cuanto giro la rueda
     * @param clickCount cuantos clics seguidos
     * @return el evento
     */
    public MouseWheelEvent createMouseWheelEvent(LightweightFrameWrapper frame, int scrollType,
            int scrollAmount, int wheelRotation, int clickCount) {
        throw new HeadlessException();
    }

    /**
     * Fabrica un evento de teclado para inyectarlo.
     *
     * @param frame la ventana
     * @param id que evento es
     * @param when cuando paso
     * @param modifiers las teclas apretadas
     * @param keyCode que tecla
     * @param keyChar que caracter produjo
     * @return el evento
     */
    public KeyEvent createKeyEvent(LightweightFrameWrapper frame, int id, long when, int modifiers,
            int keyCode, char keyChar) {
        throw new HeadlessException();
    }

    /**
     * Fabrica el evento que cierra los menus emergentes.
     *
     * @param frame la ventana
     * @return el evento
     */
    public AWTEvent createUngrabEvent(LightweightFrameWrapper frame) {
        throw new HeadlessException();
    }

    /**
     * Que componente cae en ese punto.
     *
     * @param frame la ventana
     * @param x donde, respecto de la ventana
     * @param y donde, respecto de la ventana
     * @param ignoreEnabled si tambien cuentan los componentes deshabilitados
     * @return el componente
     */
    public Component findComponentAt(LightweightFrameWrapper frame, int x, int y,
            boolean ignoreEnabled) {
        throw new HeadlessException();
    }

    /**
     * Si ese componente es esta ventana.
     *
     * @param comp el componente
     * @param frame la ventana
     * @return cierto si son el mismo
     */
    public boolean isCompEqual(Component comp, LightweightFrameWrapper frame) {
        throw new HeadlessException();
    }
}
