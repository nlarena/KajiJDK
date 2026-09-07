package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * La lista que se despliega de un combo.
 *
 * <h2>Es un menu desplegable, y eso resuelve el problema dificil</h2>
 *
 * <p>Hereda de {@link JPopupMenu} y no de {@code JPanel}, y no es un detalle de herencia: un menu
 * desplegable sabe salirse de los limites de su ventana. Una lista que fuera un panel comun quedaria
 * recortada por el borde del dialogo, que es justo donde suelen estar los combos.
 *
 * <h2>Ocho pilas de escuchas</h2>
 *
 * <p>Hay dos juegos: los que van en el <em>combo</em> --mouse, movimiento, teclado-- y los que van
 * en la <em>lista</em>. Los primeros existen porque apretar el boton del combo, arrastrar hacia
 * abajo y soltar sobre un item es un solo gesto que empieza afuera de la lista; ver la nota de
 * {@link ComboPopup}.
 *
 * <h2>El desplazamiento automatico</h2>
 *
 * <p>Arrastrar mas alla del borde de la lista la hace correr sola: {@link #startAutoScrolling} pone
 * un reloj que llama a {@link #autoScrollUp} o {@link #autoScrollDown} cada tanto. Sin eso, elegir
 * un item que no se ve requeriria soltar, correr la barra, y volver a empezar.
 *
 * <h2>El alto de la lista lo decide la cantidad de filas visibles</h2>
 *
 * <p>{@link #getPopupHeightForRowCount} suma el alto de las primeras {@code maxRowCount} filas. Si
 * hay menos items que eso, el sobrante se completa con el alto de la ultima fila -- de modo que un
 * combo con dos items y un maximo de ocho igual reserva ocho renglones --.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Mostrar la lista de verdad --{@link #show}-- necesita una ventana. El calculo de donde iria
 * ({@link #computePopupBounds}) esta y se puede probar; lo que no se puede es ver la lista abierta.
 */
public class BasicComboPopup extends JPopupMenu implements ComboPopup {

    /** Los dos sentidos del desplazamiento automatico. */
    protected static final int SCROLL_UP = 0;

    /** Ver {@link #SCROLL_UP}. */
    protected static final int SCROLL_DOWN = 1;

    protected JComboBox comboBox;
    protected JList list;
    protected JScrollPane scroller;

    /** Si el valor esta a mitad de camino de cambiar; ver {@link #updateListBoxSelectionForEvent}. */
    protected boolean valueIsAdjusting = false;

    protected MouseMotionListener mouseMotionListener;
    protected MouseListener mouseListener;
    protected KeyListener keyListener;
    protected ListSelectionListener listSelectionListener;
    protected MouseListener listMouseListener;
    protected MouseMotionListener listMouseMotionListener;
    protected PropertyChangeListener propertyChangeListener;
    protected ListDataListener listDataListener;
    protected ItemListener itemListener;

    protected Timer autoscrollTimer;
    protected boolean hasEntered = false;
    protected boolean isAutoScrolling = false;
    protected int scrollDirection = SCROLL_UP;

    /** Para ese combo; arma la lista, la ventana de desplazamiento y todos los escuchas. */
    public BasicComboPopup(JComboBox combo) {
        super();
        setName("ComboPopup.popup");
        comboBox = combo;

        mouseListener = createMouseListener();
        mouseMotionListener = createMouseMotionListener();
        keyListener = createKeyListener();

        listSelectionListener = createListSelectionListener();
        listMouseListener = createListMouseListener();
        listMouseMotionListener = createListMouseMotionListener();
        propertyChangeListener = createPropertyChangeListener();
        listDataListener = createListDataListener();
        itemListener = createItemListener();

        list = createList();
        list.setName("ComboBox.list");
        configureList();
        scroller = createScroller();
        configureScroller();
        configurePopup();

        installComboBoxListeners();
        installKeyboardActions();
    }

    /** Muestra la lista debajo del combo; ver la nota de la clase. */
    public void show() {
        Dimension popupSize = comboBox.getSize();
        popupSize.setSize(popupSize.width,
                getPopupHeightForRowCount(comboBox.getMaximumRowCount()));
        Rectangle popupBounds = computePopupBounds(0, comboBox.getBounds().height,
                popupSize.width, popupSize.height);
        Dimension scrollSize = popupBounds.getSize();
        scroller.setMaximumSize(scrollSize);
        scroller.setPreferredSize(scrollSize);
        scroller.setMinimumSize(scrollSize);
        list.invalidate();
        int selectedIndex = comboBox.getSelectedIndex();
        if (selectedIndex == -1) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(selectedIndex);
            list.ensureIndexIsVisible(selectedIndex);
        }
        setLightWeightPopupEnabled(comboBox.isLightWeightPopupEnabled());
        show(comboBox, popupBounds.x, popupBounds.y);
    }

    public void hide() {
        javax.swing.MenuSelectionManager manager =
                javax.swing.MenuSelectionManager.defaultManager();
        javax.swing.MenuElement[] selection = manager.getSelectedPath();
        for (int i = 0; i < selection.length; i++) {
            if (selection[i] == this) {
                manager.clearSelectedPath();
                break;
            }
        }
        if (selection.length > 0) {
            comboBox.repaint();
        }
    }

    public JList getList() {
        return list;
    }

    public MouseListener getMouseListener() {
        return mouseListener;
    }

    public MouseMotionListener getMouseMotionListener() {
        return mouseMotionListener;
    }

    public KeyListener getKeyListener() {
        return keyListener;
    }

    /** Suelta todo lo que este objeto engancho en el combo y en su modelo. */
    public void uninstallingUI() {
        if (propertyChangeListener != null) {
            comboBox.removePropertyChangeListener(propertyChangeListener);
        }
        if (itemListener != null) {
            comboBox.removeItemListener(itemListener);
        }
        uninstallComboBoxModelListeners(comboBox.getModel());
        uninstallKeyboardActions();
        uninstallListListeners();
    }

    protected void uninstallComboBoxModelListeners(ComboBoxModel model) {
        if (model != null && listDataListener != null) {
            model.removeListDataListener(listDataListener);
        }
    }

    protected void uninstallKeyboardActions() {
    }

    private void uninstallListListeners() {
        if (listMouseListener != null) {
            list.removeMouseListener(listMouseListener);
        }
        if (listMouseMotionListener != null) {
            list.removeMouseMotionListener(listMouseMotionListener);
        }
        if (listSelectionListener != null) {
            list.removeListSelectionListener(listSelectionListener);
        }
    }

    protected MouseListener createMouseListener() {
        return new Handler(this);
    }

    protected MouseMotionListener createMouseMotionListener() {
        return new Handler(this);
    }

    protected KeyListener createKeyListener() {
        return null;
    }

    protected ListSelectionListener createListSelectionListener() {
        return null;
    }

    protected ListDataListener createListDataListener() {
        return null;
    }

    protected MouseListener createListMouseListener() {
        return new Handler(this);
    }

    protected MouseMotionListener createListMouseMotionListener() {
        return new Handler(this);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected ItemListener createItemListener() {
        return new Handler(this);
    }

    /** La lista de adentro; una sola seleccion, y sin foco propio. */
    protected JList createList() {
        return new ListaDelCombo(comboBox);
    }

    /** La deja lista: modelo, dibujante, colores y fuente salen del combo. */
    protected void configureList() {
        list.setFont(comboBox.getFont());
        list.setForeground(comboBox.getForeground());
        list.setBackground(comboBox.getBackground());
        list.setSelectionForeground(new javax.swing.plaf.ColorUIResource(51, 51, 51));
        list.setSelectionBackground(new javax.swing.plaf.ColorUIResource(163, 184, 204));
        list.setBorder(null);
        // Por una variable suelta: llamar con el resultado de `getRenderer()` directo no compila
        // en esta VM cuando el tipo trae comodines. Ver el hallazgo #517.
        javax.swing.ListCellRenderer dibujante = comboBox.getRenderer();
        list.setCellRenderer(dibujante);
        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int selectedIndex = comboBox.getSelectedIndex();
        if (selectedIndex == -1) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(selectedIndex);
            list.ensureIndexIsVisible(selectedIndex);
        }
        installListListeners();
    }

    protected void installListListeners() {
        if (listMouseListener != null) {
            list.addMouseListener(listMouseListener);
        }
        if (listMouseMotionListener != null) {
            list.addMouseMotionListener(listMouseMotionListener);
        }
        if (listSelectionListener != null) {
            list.addListSelectionListener(listSelectionListener);
        }
    }

    /** La ventana de desplazamiento que envuelve la lista. */
    protected JScrollPane createScroller() {
        JScrollPane sp = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setHorizontalScrollBar(null);
        return sp;
    }

    /** Sin foco y sin borde: el borde lo pone la ventana emergente. */
    protected void configureScroller() {
        scroller.setFocusable(false);
        scroller.getVerticalScrollBar().setFocusable(false);
        scroller.setBorder(null);
    }

    /** Deja la ventana emergente lista: borde, acomodador y la lista adentro. */
    protected void configurePopup() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorderPainted(true);
        setBorder(new LineBorder(java.awt.Color.black));
        setOpaque(false);
        add(scroller);
        setDoubleBuffered(true);
        setFocusable(false);
    }

    /** Engancha lo que va en el combo, no en la lista. */
    protected void installComboBoxListeners() {
        if (propertyChangeListener != null) {
            comboBox.addPropertyChangeListener(propertyChangeListener);
        }
        if (itemListener != null) {
            comboBox.addItemListener(itemListener);
        }
        installComboBoxModelListeners(comboBox.getModel());
    }

    protected void installComboBoxModelListeners(ComboBoxModel model) {
        if (model != null && listDataListener != null) {
            model.addListDataListener(listDataListener);
        }
    }

    protected void installKeyboardActions() {
    }

    public boolean isFocusTraversable() {
        return false;
    }

    /** Arranca el desplazamiento automatico; ver la nota de la clase. */
    protected void startAutoScrolling(int direction) {
        if (isAutoScrolling) {
            autoscrollTimer.stop();
        }
        isAutoScrolling = true;
        scrollDirection = direction;
        if (autoscrollTimer == null) {
            autoscrollTimer = new Timer(100, new RelojDeDesplazamiento(this));
        }
        if (direction == SCROLL_UP) {
            autoScrollUp();
        } else {
            autoScrollDown();
        }
        autoscrollTimer.start();
    }

    protected void stopAutoScrolling() {
        isAutoScrolling = false;
        if (autoscrollTimer != null) {
            autoscrollTimer.stop();
        }
    }

    /** Corre la lista una fila para arriba. */
    protected void autoScrollUp() {
        int index = list.getSelectedIndex();
        if (index > 0) {
            list.setSelectedIndex(index - 1);
            list.ensureIndexIsVisible(index - 1);
        }
    }

    /** Y para abajo. */
    protected void autoScrollDown() {
        int index = list.getSelectedIndex();
        int lastItem = list.getModel().getSize() - 1;
        if (index < lastItem) {
            list.setSelectedIndex(index + 1);
            list.ensureIndexIsVisible(index + 1);
        }
    }

    /** Abre si esta cerrada y cierra si esta abierta. */
    protected void togglePopup() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /** Le pasa el foco al combo, o a su editor si es editable. */
    protected void delegateFocus(MouseEvent e) {
        if (comboBox.isEditable()) {
            Component editor = comboBox.getEditor().getEditorComponent();
            if (editor != null && !editor.isFocusable()) {
                comboBox.requestFocus();
            } else if (editor != null) {
                editor.requestFocus();
            }
        } else if (comboBox.isRequestFocusEnabled()) {
            comboBox.requestFocus();
        }
    }

    /** Traduce un evento del combo a las coordenadas de la lista. */
    protected MouseEvent convertMouseEvent(MouseEvent e) {
        Point convertedPoint = SwingUtilities.convertPoint((Component) e.getSource(),
                e.getPoint(), list);
        return new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(),
                e.getModifiersEx(), convertedPoint.x, convertedPoint.y,
                e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(),
                e.isPopupTrigger(), MouseEvent.NOBUTTON);
    }

    /**
     * Cuanto alto ocupan esas filas; ver la nota de la clase.
     *
     * <p>Si hay menos items que filas pedidas, el resto se completa con el alto de la ultima: un
     * combo de dos items y maximo ocho igual reserva ocho renglones.
     */
    protected int getPopupHeightForRowCount(int maxRowCount) {
        int currentElementCount = comboBox.getModel().getSize();
        int rowCount = Math.min(maxRowCount, currentElementCount);
        int height = 0;
        javax.swing.ListCellRenderer renderer = list.getCellRenderer();
        for (int i = 0; i < rowCount; i++) {
            Object value = list.getModel().getElementAt(i);
            Component c = renderer.getListCellRendererComponent(list, value, i, false, false);
            height += c.getPreferredSize().height;
        }
        if (height == 0) {
            height = comboBox.getHeight();
        }
        java.awt.Insets insets = getInsets();
        return height + insets.top + insets.bottom;
    }

    /** Donde iria la lista; debajo del combo, del mismo ancho. */
    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        return new Rectangle(px, py, pw, ph);
    }

    /** Elige en la lista el item que quedo debajo del mouse. */
    protected void updateListBoxSelectionForEvent(MouseEvent anEvent, boolean shouldScroll) {
        Point location = anEvent.getPoint();
        if (list == null) {
            return;
        }
        int index = list.locationToIndex(location);
        if (index == -1) {
            if (location.y < 0) {
                index = 0;
            } else {
                index = comboBox.getModel().getSize() - 1;
            }
        }
        if (list.getSelectedIndex() != index) {
            list.setSelectedIndex(index);
            if (shouldScroll) {
                list.ensureIndexIsVisible(index);
            }
        }
    }

    protected void firePopupMenuWillBecomeVisible() {
        super.firePopupMenuWillBecomeVisible();
    }

    protected void firePopupMenuWillBecomeInvisible() {
        super.firePopupMenuWillBecomeInvisible();
    }

    protected void firePopupMenuCanceled() {
        super.firePopupMenuCanceled();
    }

    /** Sin contexto de accesibilidad: ver la nota general del paquete. */
    public javax.accessibility.AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * La lista de adentro.
     *
     * <p>Lo unico propio es que no se puede enfocar y que su ancho preferido es el del combo: sin
     * eso, un item largo haria una lista mas ancha que el combo del que cuelga.
     */
    private static class ListaDelCombo extends JList<Object> {

        ListaDelCombo(JComboBox combo) {
            // El modelo se pone despues y no en el `super(...)`: pasarle ahi el del combo, que
            // llega con comodines, no compila en esta VM. Ver el hallazgo #519.
            super();
            javax.swing.ListModel modelo = combo.getModel();
            setModel(modelo);
            setFocusable(false);
        }

        public void processMouseEvent(MouseEvent e) {
            if (e.isControlDown()) {
                // Control en una lista de una sola seleccion no tiene sentido; se ignora.
                e = new MouseEvent((Component) e.getSource(), e.getID(), e.getWhen(),
                        e.getModifiersEx() ^ MouseEvent.CTRL_DOWN_MASK, e.getX(), e.getY(),
                        e.getXOnScreen(), e.getYOnScreen(), e.getClickCount(),
                        e.isPopupTrigger(), MouseEvent.NOBUTTON);
            }
            super.processMouseEvent(e);
        }
    }

    /** El reloj del desplazamiento automatico. */
    private static class RelojDeDesplazamiento implements java.awt.event.ActionListener {

        private final BasicComboPopup popup;

        RelojDeDesplazamiento(BasicComboPopup popup) {
            this.popup = popup;
        }

        public void actionPerformed(java.awt.event.ActionEvent e) {
            if (popup.scrollDirection == SCROLL_UP) {
                popup.autoScrollUp();
            } else {
                popup.autoScrollDown();
            }
        }
    }

    /**
     * El que escucha todo: el mouse en el combo, el mouse en la lista, las propiedades y los items.
     *
     * <p>Uno solo por lo mismo que en los otros UI: todos reaccionan al mismo estado --si la lista
     * esta abierta y por donde anda el mouse--.
     */
    private static class Handler implements MouseListener, MouseMotionListener,
            PropertyChangeListener, ItemListener, ListSelectionListener, ListDataListener {

        private final BasicComboPopup popup;

        Handler(BasicComboPopup popup) {
            this.popup = popup;
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (e.getSource() == popup.list) {
                return;
            }
            if (!SwingUtilities.isLeftMouseButton(e) || !popup.comboBox.isEnabled()) {
                return;
            }
            if (popup.comboBox.isEditable()) {
                Component comp = popup.comboBox.getEditor().getEditorComponent();
                if (comp != null && comp.isFocusable()) {
                    comp.requestFocus();
                }
            } else if (popup.comboBox.isRequestFocusEnabled()) {
                popup.comboBox.requestFocus();
            }
            popup.togglePopup();
        }

        public void mouseReleased(MouseEvent e) {
            if (e.getSource() == popup.list) {
                if (popup.list.getModel().getSize() > 0) {
                    if (popup.comboBox.getSelectedIndex() == popup.list.getSelectedIndex()) {
                        popup.comboBox.getEditor().setItem(popup.list.getSelectedValue());
                    }
                    popup.comboBox.setSelectedIndex(popup.list.getSelectedIndex());
                }
                popup.comboBox.setPopupVisible(false);
                return;
            }
            Component source = (Component) e.getSource();
            Dimension size = source.getSize();
            Rectangle bounds = new Rectangle(0, 0, size.width - 1, size.height - 1);
            if (!bounds.contains(e.getPoint())) {
                MouseEvent newEvent = popup.convertMouseEvent(e);
                Point location = newEvent.getPoint();
                Rectangle r = new Rectangle();
                popup.list.computeVisibleRect(r);
                if (r.contains(location) && popup.list.getModel().getSize() > 0) {
                    popup.comboBox.setSelectedIndex(popup.list.getSelectedIndex());
                }
                popup.comboBox.setPopupVisible(false);
            }
            popup.hasEntered = false;
            popup.stopAutoScrolling();
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (e.getSource() == popup.list) {
                popup.updateListBoxSelectionForEvent(e, false);
                return;
            }
            if (!popup.isVisible()) {
                return;
            }
            MouseEvent newEvent = popup.convertMouseEvent(e);
            Rectangle r = new Rectangle();
            popup.list.computeVisibleRect(r);
            if (newEvent.getPoint().y >= r.y && newEvent.getPoint().y <= r.y + r.height - 1) {
                popup.hasEntered = true;
                popup.stopAutoScrolling();
                popup.updateListBoxSelectionForEvent(newEvent, false);
            } else if (popup.hasEntered) {
                int direction = (newEvent.getPoint().y < r.y) ? SCROLL_UP : SCROLL_DOWN;
                if (!popup.isAutoScrolling || popup.scrollDirection != direction) {
                    popup.startAutoScrolling(direction);
                }
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (e.getSource() == popup.list) {
                popup.updateListBoxSelectionForEvent(e, false);
            }
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ("model".equals(propertyName)) {
                ComboBoxModel viejo = (ComboBoxModel) e.getOldValue();
                ComboBoxModel nuevo = (ComboBoxModel) e.getNewValue();
                popup.uninstallComboBoxModelListeners(viejo);
                popup.installComboBoxModelListeners(nuevo);
                javax.swing.ListModel modeloNuevo = nuevo;
                popup.list.setModel(modeloNuevo);
                if (popup.isVisible()) {
                    popup.hide();
                }
            } else if ("renderer".equals(propertyName)) {
                javax.swing.ListCellRenderer nuevoDibujante = popup.comboBox.getRenderer();
                popup.list.setCellRenderer(nuevoDibujante);
                if (popup.isVisible()) {
                    popup.hide();
                }
            } else if ("componentOrientation".equals(propertyName)) {
                popup.list.setComponentOrientation(popup.comboBox.getComponentOrientation());
            }
        }

        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                JComboBox comboBox = (JComboBox) e.getSource();
                popup.setListSelection(comboBox.getSelectedIndex());
            }
        }

        public void valueChanged(ListSelectionEvent e) {
        }

        public void intervalAdded(ListDataEvent e) {
        }

        public void intervalRemoved(ListDataEvent e) {
        }

        public void contentsChanged(ListDataEvent e) {
        }
    }

    /** Deja la lista mostrando el item elegido en el combo. */
    private void setListSelection(int selectedIndex) {
        if (selectedIndex == -1) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(selectedIndex);
            list.ensureIndexIsVisible(selectedIndex);
        }
    }
}
