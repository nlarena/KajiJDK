package javax.swing;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

/**
 * Decide cuando aparece y cuando se va un cartel de ayuda.
 *
 * <h2>Tres demoras, y las tres tienen un motivo</h2>
 *
 * <p>{@link #setInitialDelay} es cuanto hay que quedarse quieto sobre algo para que aparezca el
 * cartel: sin esa espera, mover el mouse por la pantalla llenaria todo de carteles.
 * {@link #setDismissDelay} es cuanto se queda antes de irse solo, porque un cartel que no se va tapa
 * lo que esta debajo. {@link #setReshowDelay} es la ventana durante la cual pasar a otro componente
 * muestra su cartel <em>sin</em> volver a esperar -- es lo que permite recorrer una barra de
 * herramientas leyendo cada boton sin detenerse en cada uno.
 *
 * <h2>Uno solo para toda la aplicacion</h2>
 *
 * <p>{@link #sharedInstance} es la unica forma de conseguirlo, y el constructor no es publico. Tiene
 * que ser uno: dos carteles a la vez no tienen sentido, y las tres demoras son una decision de la
 * aplicacion entera.
 *
 * <h2>Sin pantalla no hay cartel</h2>
 *
 * <p>El estado -- las demoras, si esta prendido, que componentes estan registrados -- se guarda y se
 * lee. Lo que no ocurre es la aparicion: mostrar un cartel necesita una ventana y un mouse que se
 * mueva, y sin ninguno de los dos no hay nada que mostrar. {@link #registerComponent} conecta los
 * oyentes igual, asi que en cuanto haya pantalla funciona.
 */
public final class ToolTipManager extends MouseAdapter implements MouseMotionListener {

    Timer enterTimer;
    Timer exitTimer;
    Timer insideTimer;
    String toolTipText;
    Point preferredLocation;
    JComponent insideComponent;
    MouseEvent mouseEvent;
    boolean showImmediately;
    transient Popup tipWindow;
    JToolTip tip;
    boolean enabled = true;

    /** Si el cartel puede dibujarse adentro de la ventana en vez de en una propia. */
    protected boolean lightWeightPopupEnabled = true;

    /** Si se permite una ventana del sistema para el cartel. */
    protected boolean heavyWeightPopupEnabled = false;

    private static final ToolTipManager UNICO = new ToolTipManager();

    /** No es publico; ver la nota de la clase. */
    ToolTipManager() {
        enterTimer = new Timer(750, new AlEntrar(this));
        enterTimer.setRepeats(false);
        exitTimer = new Timer(500, new AlSalir(this));
        exitTimer.setRepeats(false);
        insideTimer = new Timer(4000, new AlQuedarse(this));
        insideTimer.setRepeats(false);
    }

    /** Prende o apaga los carteles de toda la aplicacion. */
    public void setEnabled(boolean flag) {
        enabled = flag;
        if (!flag) {
            hideTipWindow();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Si el cartel puede dibujarse adentro de la ventana.
     *
     * <p>Es mas barato, y no sirve cuando el cartel se sale del borde de la ventana o cuando hay un
     * componente pesado debajo que lo taparia.
     */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        lightWeightPopupEnabled = aFlag;
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /** Cuanto hay que quedarse quieto para que aparezca; ver la nota de la clase. */
    public void setInitialDelay(int milliseconds) {
        enterTimer.setInitialDelay(milliseconds);
    }

    public int getInitialDelay() {
        return enterTimer.getInitialDelay();
    }

    /** Cuanto se queda antes de irse solo. */
    public void setDismissDelay(int milliseconds) {
        insideTimer.setInitialDelay(milliseconds);
    }

    public int getDismissDelay() {
        return insideTimer.getInitialDelay();
    }

    /** La ventana para pasar de un componente a otro sin volver a esperar. */
    public void setReshowDelay(int milliseconds) {
        exitTimer.setInitialDelay(milliseconds);
    }

    public int getReshowDelay() {
        return exitTimer.getInitialDelay();
    }

    /**
     * Muestra el cartel.
     *
     * <p>Sin pantalla no hay donde mostrarlo; ver la nota de la clase. Lo que si ocurre es la parte
     * de estado: se arma el cartel y se arranca el temporizador que lo va a esconder.
     */
    void showTipWindow() {
        if (insideComponent == null || !insideComponent.isShowing()) {
            return;
        }
        if (!enabled || !insideComponent.isEnabled()) {
            return;
        }
        if (tipWindow != null) {
            return;
        }
        tip = insideComponent.createToolTip();
        tip.setTipText(toolTipText);
        insideTimer.start();
    }

    /** Esconde el cartel y para los temporizadores. */
    void hideTipWindow() {
        if (tipWindow != null) {
            tipWindow.hide();
            tipWindow = null;
        }
        tip = null;
        insideTimer.stop();
    }

    /** El unico administrador de carteles; ver la nota de la clase. */
    public static ToolTipManager sharedInstance() {
        return UNICO;
    }

    /**
     * Empieza a vigilar ese componente.
     *
     * <p>Se lo desanota primero: registrar dos veces dejaria dos oyentes y el cartel aparecería
     * dos veces.
     */
    public void registerComponent(JComponent component) {
        component.removeMouseListener(this);
        component.addMouseListener(this);
        component.removeMouseMotionListener(this);
        component.addMouseMotionListener(this);
    }

    /** Deja de vigilarlo. */
    public void unregisterComponent(JComponent component) {
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        if (component == insideComponent) {
            hideTipWindow();
            insideComponent = null;
            toolTipText = null;
            mouseEvent = null;
        }
    }

    /** El mouse entro: arranca la espera, o muestra ya si viene de otro componente. */
    public void mouseEntered(MouseEvent event) {
        initiateToolTip(event);
    }

    private void initiateToolTip(MouseEvent event) {
        if (event.getSource() == tipWindow) {
            return;
        }
        JComponent component = (JComponent) event.getSource();
        component.getToolTipText(event);
        exitTimer.stop();
        Point location = event.getPoint();
        if (location.x < 0 || location.x >= component.getWidth()
                || location.y < 0 || location.y >= component.getHeight()) {
            return;
        }
        if (insideComponent != null) {
            enterTimer.stop();
        }
        insideComponent = component;
        mouseEvent = event;
        toolTipText = component.getToolTipText(event);
        preferredLocation = component.getToolTipLocation(event);
        if (showImmediately) {
            showTipWindow();
        } else {
            enterTimer.start();
        }
    }

    /** El mouse salio: se esconde, pero queda la ventana de reaparicion. */
    public void mouseExited(MouseEvent event) {
        if (insideComponent == null) {
            return;
        }
        if (window(event) == window(mouseEvent)) {
            enterTimer.stop();
        }
        hideTipWindow();
        insideComponent = null;
        toolTipText = null;
        mouseEvent = null;
        showImmediately = false;
        exitTimer.start();
    }

    private static java.awt.Window window(MouseEvent e) {
        if (e == null || !(e.getSource() instanceof Component)) {
            return null;
        }
        return SwingUtilities.getWindowAncestor((Component) e.getSource());
    }

    /** Un clic esconde el cartel: el usuario ya no esta leyendo, esta haciendo algo. */
    public void mousePressed(MouseEvent event) {
        hideTipWindow();
        enterTimer.stop();
        showImmediately = false;
        insideComponent = null;
        mouseEvent = null;
    }

    /** Arrastrar tampoco es leer. */
    public void mouseDragged(MouseEvent event) {
    }

    /**
     * Mover el mouse dentro del mismo componente reinicia la espera.
     *
     * <p>Salvo que el texto haya cambiado -- una tabla da un texto por celda --, en cuyo caso el
     * cartel se rehace.
     */
    public void mouseMoved(MouseEvent event) {
        if (tipWindow != null) {
            return;
        }
        JComponent component = (JComponent) event.getSource();
        String newText = component.getToolTipText(event);
        Point newPreferredLocation = component.getToolTipLocation(event);
        boolean sameText = (newText == null) ? (toolTipText == null)
                : newText.equals(toolTipText);
        boolean sameLoc = (preferredLocation == null) ? (newPreferredLocation == null)
                : preferredLocation.equals(newPreferredLocation);
        if (sameText && sameLoc) {
            if (toolTipText != null) {
                if (enterTimer.isRunning()) {
                    enterTimer.restart();
                }
            }
        } else {
            toolTipText = newText;
            preferredLocation = newPreferredLocation;
            if (showImmediately) {
                hideTipWindow();
                showTipWindow();
                exitTimer.stop();
            } else {
                enterTimer.restart();
            }
        }
        mouseEvent = event;
    }

    /** La ventana del sistema que contiene a ese componente, o nulo. */
    static Frame frameForComponent(Component component) {
        Component c = component;
        while (c != null && !(c instanceof Frame)) {
            c = c.getParent();
        }
        return (Frame) c;
    }

    /** Se cumplio la espera: aparece el cartel. */
    private static class AlEntrar implements ActionListener {

        private final ToolTipManager m;

        AlEntrar(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.showImmediately = true;
            m.showTipWindow();
        }
    }

    /** Se paso la ventana de reaparicion: la proxima vez hay que volver a esperar. */
    private static class AlSalir implements ActionListener {

        private final ToolTipManager m;

        AlSalir(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.showImmediately = false;
            m.insideComponent = null;
            m.mouseEvent = null;
        }
    }

    /** El cartel lleva demasiado tiempo puesto: se va. */
    private static class AlQuedarse implements ActionListener {

        private final ToolTipManager m;

        AlQuedarse(ToolTipManager m) {
            this.m = m;
        }

        public void actionPerformed(ActionEvent e) {
            m.hideTipWindow();
            m.enterTimer.stop();
            m.showImmediately = false;
            m.insideComponent = null;
            m.mouseEvent = null;
        }
    }
}
