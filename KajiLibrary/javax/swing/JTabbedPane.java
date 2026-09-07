package javax.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.Vector;

import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.TabbedPaneUI;

/**
 * Un grupo de paneles con solapas, del que se ve uno a la vez.
 *
 * <h2>Los datos de una solapa no estan en su componente</h2>
 *
 * <p>El titulo, el icono, el color y si esta habilitada viven en la solapa, no en el componente que
 * muestra. Es lo que permite poner el mismo componente en dos solapas con nombres distintos, y lo
 * que explica que exista un metodo {@code ...At(int)} por cada cosa.
 *
 * <h2>Todos los componentes son hijos, aunque no se vean</h2>
 *
 * <p>Agregar una solapa agrega su componente como hijo del panel; el aspecto muestra el de la
 * solapa elegida y esconde los demas. No se arman al elegirlos: estan desde el principio.
 *
 * <p>Eso importa al medir. Un panel con solapas pide el tamano del mas grande de todos, no el del
 * que se ve, porque cambiar de solapa no deberia cambiar el tamano de la ventana.
 *
 * <h2>Que pasa cuando las solapas no entran</h2>
 *
 * <p>Con {@link #WRAP_TAB_LAYOUT} se acomodan en varias filas; con {@link #SCROLL_TAB_LAYOUT} se
 * quedan en una y aparecen flechas. La primera muestra todas y mueve el contenido hacia abajo cada
 * vez que se agrega una fila; la segunda deja el contenido quieto y esconde solapas. No hay una
 * buena: hay que elegir cual molesta menos.
 */
public class JTabbedPane extends JComponent implements Serializable,
        javax.accessibility.Accessible, SwingConstants {

    private static final String uiClassID = "TabbedPaneUI";

    /** Las solapas que no entran pasan a otra fila. */
    public static final int WRAP_TAB_LAYOUT = 0;

    /** Las solapas se quedan en una fila y se desplazan. */
    public static final int SCROLL_TAB_LAYOUT = 1;

    /** De que lado van las solapas. */
    protected int tabPlacement = TOP;

    /** Cual esta elegida. */
    protected SingleSelectionModel model;

    /** El puente entre el modelo y quien escucha al panel. */
    protected ChangeListener changeListener = null;

    /** El unico evento de cambio; no lleva datos, asi que se reusa. */
    protected transient ChangeEvent changeEvent = null;

    private int tabLayoutPolicy;
    private Vector<Solapa> pages = new Vector<Solapa>();
    private AccessibleContext accessibleContext;

    /** Un panel con las solapas arriba. */
    public JTabbedPane() {
        this(TOP, WRAP_TAB_LAYOUT);
    }

    /** Un panel con las solapas de ese lado. */
    public JTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    /**
     * Un panel con las solapas de ese lado y esa politica.
     *
     * @throws IllegalArgumentException si el lado o la politica no existen.
     */
    public JTabbedPane(int tabPlacement, int tabLayoutPolicy) {
        setTabPlacement(tabPlacement);
        setTabLayoutPolicy(tabLayoutPolicy);
        setModel(new DefaultSingleSelectionModel());
        updateUI();
    }

    public TabbedPaneUI getUI() {
        return (TabbedPaneUI) ui;
    }

    public void setUI(TabbedPaneUI ui) {
        super.setUI(ui);
        revalidate();
        repaint();
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    protected ChangeListener createChangeListener() {
        return new PuenteDeCambio(this);
    }

    /** Reenvia el aviso del modelo a quien escucha al panel. */
    static class PuenteDeCambio implements ChangeListener, Serializable {

        private final JTabbedPane panel;

        PuenteDeCambio(JTabbedPane panel) {
            this.panel = panel;
        }

        public void stateChanged(ChangeEvent e) {
            panel.fireStateChanged();
        }
    }

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public SingleSelectionModel getModel() {
        return model;
    }

    /** Cambia el modelo, llevandose el puente al nuevo. */
    public void setModel(SingleSelectionModel newModel) {
        SingleSelectionModel oldModel = getModel();
        if (oldModel != null) {
            oldModel.removeChangeListener(changeListener);
            changeListener = null;
        }
        model = newModel;
        if (newModel != null) {
            changeListener = createChangeListener();
            newModel.addChangeListener(changeListener);
        }
        firePropertyChange("model", oldModel, newModel);
        repaint();
    }

    public int getTabPlacement() {
        return tabPlacement;
    }

    /**
     * De que lado van las solapas.
     *
     * @throws IllegalArgumentException si no es uno de los cuatro lados.
     */
    public void setTabPlacement(int tabPlacement) {
        if (tabPlacement != TOP && tabPlacement != LEFT && tabPlacement != BOTTOM
                && tabPlacement != RIGHT) {
            throw new IllegalArgumentException("illegal tab placement: must be "
                    + "TOP, BOTTOM, LEFT, or RIGHT");
        }
        if (this.tabPlacement != tabPlacement) {
            int oldValue = this.tabPlacement;
            this.tabPlacement = tabPlacement;
            firePropertyChange("tabPlacement", oldValue, tabPlacement);
            revalidate();
            repaint();
        }
    }

    public int getTabLayoutPolicy() {
        return tabLayoutPolicy;
    }

    /**
     * Que hacer cuando las solapas no entran; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si no es una de las dos.
     */
    public void setTabLayoutPolicy(int tabLayoutPolicy) {
        if (tabLayoutPolicy != WRAP_TAB_LAYOUT && tabLayoutPolicy != SCROLL_TAB_LAYOUT) {
            throw new IllegalArgumentException("illegal tab layout policy: must be "
                    + "WRAP_TAB_LAYOUT or SCROLL_TAB_LAYOUT");
        }
        if (this.tabLayoutPolicy != tabLayoutPolicy) {
            int oldValue = this.tabLayoutPolicy;
            this.tabLayoutPolicy = tabLayoutPolicy;
            firePropertyChange("tabLayoutPolicy", oldValue, tabLayoutPolicy);
            revalidate();
            repaint();
        }
    }

    public int getSelectedIndex() {
        return model.getSelectedIndex();
    }

    /**
     * Elige la solapa numero tal.
     *
     * @throws IndexOutOfBoundsException si no existe.
     */
    public void setSelectedIndex(int index) {
        if (index >= getTabCount() || index < -1) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: "
                    + getTabCount());
        }
        model.setSelectedIndex(index);
    }

    /** El componente de la solapa elegida, o nulo. */
    public Component getSelectedComponent() {
        int index = getSelectedIndex();
        if (index == -1) {
            return null;
        }
        return getComponentAt(index);
    }

    /**
     * Elige la solapa de ese componente.
     *
     * @throws IllegalArgumentException si el componente no esta en ninguna solapa.
     */
    public void setSelectedComponent(Component c) {
        int index = indexOfComponent(c);
        if (index != -1) {
            setSelectedIndex(index);
        } else {
            throw new IllegalArgumentException("component not found in tabbed pane");
        }
    }

    /**
     * Agrega una solapa en esa posicion.
     *
     * <p>Si era la primera queda elegida: un panel con solapas y ninguna elegida no mostraria
     * nada.
     */
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        int newIndex = index;
        if (newIndex > pages.size()) {
            newIndex = pages.size();
        }
        Solapa p = new Solapa(this, title != null ? title : "", icon, null, component, tip);
        pages.insertElementAt(p, newIndex);
        if (component != null) {
            addImpl(component, null, -1);
            component.setVisible(false);
        }
        if (pages.size() == 1) {
            setSelectedIndex(0);
        } else if (newIndex <= getSelectedIndex()) {
            // Insertar antes de la elegida la corre un lugar. Sin esto, agregar una solapa al
            // principio cambiaria en silencio cual esta abierta.
            setSelectedIndex(getSelectedIndex() + 1);
        }
        revalidate();
        repaint();
    }

    public void addTab(String title, Icon icon, Component component, String tip) {
        insertTab(title, icon, component, tip, pages.size());
    }

    public void addTab(String title, Icon icon, Component component) {
        insertTab(title, icon, component, null, pages.size());
    }

    public void addTab(String title, Component component) {
        insertTab(title, null, component, null, pages.size());
    }

    /** Agrega una solapa cuyo titulo es el nombre del componente. */
    public Component add(Component component) {
        if (!(component instanceof javax.swing.JComponent)
                || ((javax.swing.JComponent) component).getClientProperty(
                        "__index_to_remove__") == null) {
            addTab(component.getName(), component);
        }
        return component;
    }

    public Component add(String title, Component component) {
        addTab(title, component);
        return component;
    }

    public Component add(Component component, int index) {
        insertTab(component.getName(), null, component, null,
                index == -1 ? getTabCount() : index);
        return component;
    }

    /** Agrega una solapa; si la restriccion es texto o icono, es el titulo. */
    public void add(Component component, Object constraints) {
        if (constraints instanceof String) {
            addTab((String) constraints, component);
        } else if (constraints instanceof Icon) {
            addTab(null, (Icon) constraints, component);
        } else {
            add(component);
        }
    }

    public void add(Component component, Object constraints, int index) {
        Icon icon = constraints instanceof Icon ? (Icon) constraints : null;
        String title = constraints instanceof String ? (String) constraints : null;
        insertTab(title, icon, component, null, index == -1 ? getTabCount() : index);
    }

    /**
     * Saca la solapa numero tal.
     *
     * <p>Si era la elegida, queda elegida la anterior; si era la primera, la que quedo primera. Un
     * panel con solapas nunca queda sin elegida mientras le quede alguna.
     */
    public void removeTabAt(int index) {
        checkIndex(index);
        Component component = getComponentAt(index);
        int selected = getSelectedIndex();
        pages.removeElementAt(index);
        sacarHijo(component);
        int nuevas = getTabCount();
        if (nuevas == 0) {
            model.setSelectedIndex(-1);
        } else if (index < selected) {
            // Se fue una de las de antes: la elegida sigue siendo la misma, un lugar mas atras.
            setSelectedIndex(selected - 1);
        } else if (index == selected) {
            // Se fue la elegida: queda la que ocupo su lugar, o la ultima si era la ultima.
            setSelectedIndex(Math.min(selected, nuevas - 1));
        }
        revalidate();
        repaint();
    }

    public void remove(Component component) {
        int index = indexOfComponent(component);
        if (index != -1) {
            removeTabAt(index);
        } else {
            sacarHijo(component);
        }
    }

    public void remove(int index) {
        removeTabAt(index);
    }

    public void removeAll() {
        setSelectedIndex(-1);
        int tabCount = getTabCount();
        while (tabCount-- > 0) {
            removeTabAt(tabCount);
        }
    }

    public int getTabCount() {
        return pages.size();
    }

    /** En cuantas filas quedaron las solapas; lo contesta el aspecto. */
    public int getTabRunCount() {
        if (ui != null) {
            return getUI().getTabRunCount(this);
        }
        return 0;
    }

    public String getTitleAt(int index) {
        return solapa(index).titulo;
    }

    public Icon getIconAt(int index) {
        return solapa(index).icono;
    }

    /** El icono que se ve cuando la solapa esta deshabilitada. */
    public Icon getDisabledIconAt(int index) {
        return solapa(index).iconoApagado;
    }

    public String getToolTipTextAt(int index) {
        return solapa(index).ayuda;
    }

    public Color getBackgroundAt(int index) {
        Color c = solapa(index).fondo;
        return (c == null) ? getBackground() : c;
    }

    public Color getForegroundAt(int index) {
        Color c = solapa(index).frente;
        return (c == null) ? getForeground() : c;
    }

    public boolean isEnabledAt(int index) {
        return solapa(index).habilitada;
    }

    public Component getComponentAt(int index) {
        return solapa(index).componente;
    }

    /** La letra con la que se salta a esta solapa desde el teclado. */
    public int getMnemonicAt(int index) {
        return solapa(index).mnemonico;
    }

    /** Que letra del titulo va subrayada. */
    public int getDisplayedMnemonicIndexAt(int index) {
        return solapa(index).indiceMnemonico;
    }

    /** El rectangulo de la solapa; lo contesta el aspecto. */
    public Rectangle getBoundsAt(int index) {
        checkIndex(index);
        if (ui != null) {
            return getUI().getTabBounds(this, index);
        }
        return null;
    }

    public void setTitleAt(int index, String title) {
        String oldTitle = solapa(index).titulo;
        solapa(index).titulo = title;
        if (oldTitle != title) {
            revalidate();
            repaint();
        }
    }

    public void setIconAt(int index, Icon icon) {
        solapa(index).icono = icon;
        revalidate();
        repaint();
    }

    public void setDisabledIconAt(int index, Icon disabledIcon) {
        solapa(index).iconoApagado = disabledIcon;
        repaint();
    }

    public void setToolTipTextAt(int index, String toolTipText) {
        solapa(index).ayuda = toolTipText;
    }

    public void setBackgroundAt(int index, Color background) {
        solapa(index).fondo = background;
        repaint();
    }

    public void setForegroundAt(int index, Color foreground) {
        solapa(index).frente = foreground;
        repaint();
    }

    public void setEnabledAt(int index, boolean enabled) {
        solapa(index).habilitada = enabled;
        repaint();
    }

    /** Cambia el componente de una solapa sin tocar su titulo ni su icono. */
    public void setComponentAt(int index, Component component) {
        Solapa p = solapa(index);
        if (component != p.componente) {
            sacarHijo(p.componente);
            p.componente = component;
            if (component != null) {
                component.setVisible(index == getSelectedIndex());
                addImpl(component, null, -1);
            }
            revalidate();
            repaint();
        }
    }

    public void setDisplayedMnemonicIndexAt(int tabIndex, int mnemonicIndex) {
        Solapa p = solapa(tabIndex);
        if (mnemonicIndex != -1) {
            String title = p.titulo;
            if (title == null || mnemonicIndex < 0 || mnemonicIndex >= title.length()) {
                throw new IllegalArgumentException("Invalid mnemonic index: " + mnemonicIndex);
            }
        }
        p.indiceMnemonico = mnemonicIndex;
        repaint();
    }

    /**
     * La letra que salta a esta solapa.
     *
     * <p>Ademas busca esa letra en el titulo para subrayarla. Si no esta, no se subraya nada: el
     * atajo sigue andando, solo que no se ve.
     */
    public void setMnemonicAt(int tabIndex, int mnemonic) {
        Solapa p = solapa(tabIndex);
        p.mnemonico = mnemonic;
        String title = p.titulo;
        if (title != null && mnemonic != 0) {
            int i = title.toUpperCase(java.util.Locale.ROOT)
                    .indexOf(Character.toUpperCase((char) mnemonic));
            p.indiceMnemonico = i;
        }
        repaint();
    }

    /** La primera solapa con ese titulo, o -1. */
    public int indexOfTab(String title) {
        for (int i = 0; i < getTabCount(); i++) {
            String t = getTitleAt(i);
            if ((t == null && title == null) || (t != null && t.equals(title))) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfTab(Icon icon) {
        for (int i = 0; i < getTabCount(); i++) {
            Icon ic = getIconAt(i);
            if ((ic != null && ic.equals(icon)) || (ic == null && ic == icon)) {
                return i;
            }
        }
        return -1;
    }

    public int indexOfComponent(Component component) {
        for (int i = 0; i < getTabCount(); i++) {
            Component c = getComponentAt(i);
            if ((c == null && c == component) || (c != null && c.equals(component))) {
                return i;
            }
        }
        return -1;
    }

    /** Que solapa cae en ese punto, o -1; lo contesta el aspecto. */
    public int indexAtLocation(int x, int y) {
        if (ui != null) {
            return getUI().tabForCoordinate(this, x, y);
        }
        return -1;
    }

    /** El texto de ayuda de la solapa que esta bajo el mouse. */
    public String getToolTipText(MouseEvent event) {
        if (ui != null) {
            int index = getUI().tabForCoordinate(this, event.getX(), event.getY());
            if (index != -1) {
                return solapa(index).ayuda;
            }
        }
        return super.getToolTipText(event);
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * Un componente que reemplaza al titulo de la solapa.
     *
     * <p>Es lo que permite una solapa con un boton de cerrar, o con dos lineas de texto: en lugar
     * de un titulo y un icono, se dibuja el componente que se le ponga.
     */
    public void setTabComponentAt(int index, Component component) {
        Solapa p = solapa(index);
        Component vieja = p.componenteSolapa;
        p.componenteSolapa = component;
        sacarHijo(vieja);
        if (component != null) {
            addImpl(component, null, -1);
        }
        revalidate();
        repaint();
    }

    public Component getTabComponentAt(int index) {
        return solapa(index).componenteSolapa;
    }

    public int indexOfTabComponent(Component tabComponent) {
        for (int i = 0; i < getTabCount(); i++) {
            if (getTabComponentAt(i) == tabComponent) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Saca un componente de la lista de hijos sin pasar por {@link #removeTabAt}.
     *
     * <p>Parece un rodeo y es necesario: {@code Container.remove(Component)} busca el indice del
     * hijo y llama a {@code remove(int)}, que en esta clase saca una <em>solapa</em>. Sacar el
     * hijo numero tres cuando hay cuatro hijos y dos solapas termina en un indice invalido, o peor,
     * en sacar la solapa equivocada.
     *
     * <p>Los indices de hijo y de solapa no son el mismo numero: un panel con solapas tiene ademas
     * los componentes que el aspecto agregue.
     */
    private void sacarHijo(Component comp) {
        if (comp == null) {
            return;
        }
        int n = getComponentCount();
        for (int i = 0; i < n; i++) {
            if (getComponent(i) == comp) {
                super.remove(i);
                return;
            }
        }
    }

    private Solapa solapa(int index) {
        checkIndex(index);
        return pages.elementAt(index);
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= pages.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: "
                    + pages.size());
        }
    }

    /**
     * Todo lo que se sabe de una solapa.
     *
     * <p>Es privada en el JDK y aca tambien: quien use el panel habla por indice, y esta clase es
     * lo que hace que el titulo y el componente puedan cambiar por separado.
     */
    static class Solapa implements Serializable {

        String titulo;
        Icon icono;
        Icon iconoApagado;
        Component componente;
        String ayuda;
        Color fondo;
        Color frente;
        boolean habilitada = true;
        int mnemonico = -1;
        int indiceMnemonico = -1;
        Component componenteSolapa;

        Solapa(JTabbedPane panel, String titulo, Icon icono, Icon iconoApagado,
                Component componente, String ayuda) {
            this.titulo = titulo;
            this.icono = icono;
            this.iconoApagado = iconoApagado;
            this.componente = componente;
            this.ayuda = ayuda;
        }
    }
}
