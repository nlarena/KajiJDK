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
 * A group of panes with tabs, of which one is seen at a time.
 *
 * <h2>A tab's data are not in its component</h2>
 *
 * <p>The title, the icon, the colour and whether it is enabled live in the tab, not in the
 * component it shows. It is what allows the same component to be put in two tabs with
 * different names, and what explains why there is a {@code ...At(int)} method for each
 * thing.
 *
 * <h2>Every component is a child, even though they are not seen</h2>
 *
 * <p>Adding a tab adds its component as a child of the pane; the look and feel shows the chosen
 * tab's and hides the others. They are not built on being chosen: they are there from the
 * start.
 *
 * <p>That matters when measuring. A tabbed pane asks for the size of the largest of them all,
 * not that of the one that is seen, because changing tab should not change the window's size.
 *
 * <h2>What happens when the tabs do not fit</h2>
 *
 * <p>With {@link #WRAP_TAB_LAYOUT} they are laid out in several rows; with
 * {@link #SCROLL_TAB_LAYOUT} they stay in one and arrows appear. The first shows them all and
 * moves the content downwards every time a row is added; the second leaves the content still
 * and hides tabs. There is no good one: one has to choose which is less of a nuisance.
 */
public class JTabbedPane extends JComponent implements Serializable,
        javax.accessibility.Accessible, SwingConstants {

    private static final String uiClassID = "TabbedPaneUI";

    /** The tabs that do not fit go to another row. */
    public static final int WRAP_TAB_LAYOUT = 0;

    /** The tabs stay in one row and are scrolled. */
    public static final int SCROLL_TAB_LAYOUT = 1;

    /** Which side the tabs go on. */
    protected int tabPlacement = TOP;

    /** Which one is chosen. */
    protected SingleSelectionModel model;

    /** The bridge between the model and whoever listens to the pane. */
    protected ChangeListener changeListener = null;

    /** The single change event; it carries no data, so it is reused. */
    protected transient ChangeEvent changeEvent = null;

    private int tabLayoutPolicy;
    private Vector<Tab> pages = new Vector<Tab>();
    private AccessibleContext accessibleContext;

    /** A pane with the tabs on top. */
    public JTabbedPane() {
        this(TOP, WRAP_TAB_LAYOUT);
    }

    /** A pane with the tabs on that side. */
    public JTabbedPane(int tabPlacement) {
        this(tabPlacement, WRAP_TAB_LAYOUT);
    }

    /**
     * A pane with the tabs on that side and that policy.
     *
     * @throws IllegalArgumentException if the side or the policy do not exist.
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
        return new ChangeBridge(this);
    }

    /** It forwards the model's notice to whoever listens to the pane. */
    static class ChangeBridge implements ChangeListener, Serializable {

        private final JTabbedPane panel;

        ChangeBridge(JTabbedPane panel) {
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

    /** It changes the model, taking the bridge along to the new one. */
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
     * Which side the tabs go on.
     *
     * @throws IllegalArgumentException if it is not one of the four sides.
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
     * What to do when the tabs do not fit; see the class note.
     *
     * @throws IllegalArgumentException if it is not one of the two.
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
     * It chooses tab number such-and-such.
     *
     * @throws IndexOutOfBoundsException if it does not exist.
     */
    public void setSelectedIndex(int index) {
        if (index >= getTabCount() || index < -1) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Tab count: "
                    + getTabCount());
        }
        model.setSelectedIndex(index);
    }

    /** The chosen tab's component, or null. */
    public Component getSelectedComponent() {
        int index = getSelectedIndex();
        if (index == -1) {
            return null;
        }
        return getComponentAt(index);
    }

    /**
     * It chooses that component's tab.
     *
     * @throws IllegalArgumentException if the component is in no tab.
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
     * It adds a tab at that position.
     *
     * <p>If it was the first it is left chosen: a tabbed pane with no tab chosen would show
     * nothing.
     */
    public void insertTab(String title, Icon icon, Component component, String tip, int index) {
        int newIndex = index;
        if (newIndex > pages.size()) {
            newIndex = pages.size();
        }
        Tab p = new Tab(this, title != null ? title : "", icon, null, component, tip);
        pages.insertElementAt(p, newIndex);
        if (component != null) {
            addImpl(component, null, -1);
            component.setVisible(false);
        }
        if (pages.size() == 1) {
            setSelectedIndex(0);
        } else if (newIndex <= getSelectedIndex()) {
            // Inserting before the chosen one shifts it one place. Without this, adding a tab at
            // the
                        // beginning would silently change which one is open.
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

    /** It adds a tab whose title is the component's name. */
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

    /** It adds a tab; if the constraint is text or an icon, it is the title. */
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
     * It removes tab number such-and-such.
     *
     * <p>If it was the chosen one, the previous one is left chosen; if it was the first, the one
     * that ended up first. A tabbed pane is never left with none chosen while it has any left.
     */
    public void removeTabAt(int index) {
        checkIndex(index);
        Component component = getComponentAt(index);
        int selected = getSelectedIndex();
        pages.removeElementAt(index);
        removeChild(component);
        int added = getTabCount();
        if (added == 0) {
            model.setSelectedIndex(-1);
        } else if (index < selected) {
            // One of the earlier ones left: the chosen one is still the same, one place back.
            setSelectedIndex(selected - 1);
        } else if (index == selected) {
            // The chosen one left: the one that took its place is left, or the last if it was the
            // last.
            setSelectedIndex(Math.min(selected, added - 1));
        }
        revalidate();
        repaint();
    }

    public void remove(Component component) {
        int index = indexOfComponent(component);
        if (index != -1) {
            removeTabAt(index);
        } else {
            removeChild(component);
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

    /** How many rows the tabs ended up in; the look and feel answers it. */
    public int getTabRunCount() {
        if (ui != null) {
            return getUI().getTabRunCount(this);
        }
        return 0;
    }

    public String getTitleAt(int index) {
        return tab(index).title;
    }

    public Icon getIconAt(int index) {
        return tab(index).icon;
    }

    /** The icon that is seen when the tab is disabled. */
    public Icon getDisabledIconAt(int index) {
        return tab(index).disabledIcon;
    }

    public String getToolTipTextAt(int index) {
        return tab(index).tip;
    }

    public Color getBackgroundAt(int index) {
        Color c = tab(index).background;
        return (c == null) ? getBackground() : c;
    }

    public Color getForegroundAt(int index) {
        Color c = tab(index).foreground;
        return (c == null) ? getForeground() : c;
    }

    public boolean isEnabledAt(int index) {
        return tab(index).enabled;
    }

    public Component getComponentAt(int index) {
        return tab(index).component;
    }

    /** The letter this tab is jumped to with from the keyboard. */
    public int getMnemonicAt(int index) {
        return tab(index).mnemonic;
    }

    /** Which letter of the title goes underlined. */
    public int getDisplayedMnemonicIndexAt(int index) {
        return tab(index).mnemonicIndex;
    }

    /** The tab's rectangle; the look and feel answers it. */
    public Rectangle getBoundsAt(int index) {
        checkIndex(index);
        if (ui != null) {
            return getUI().getTabBounds(this, index);
        }
        return null;
    }

    public void setTitleAt(int index, String title) {
        String oldTitle = tab(index).title;
        tab(index).title = title;
        if (oldTitle != title) {
            revalidate();
            repaint();
        }
    }

    public void setIconAt(int index, Icon icon) {
        tab(index).icon = icon;
        revalidate();
        repaint();
    }

    public void setDisabledIconAt(int index, Icon disabledIcon) {
        tab(index).disabledIcon = disabledIcon;
        repaint();
    }

    public void setToolTipTextAt(int index, String toolTipText) {
        tab(index).tip = toolTipText;
    }

    public void setBackgroundAt(int index, Color background) {
        tab(index).background = background;
        repaint();
    }

    public void setForegroundAt(int index, Color foreground) {
        tab(index).foreground = foreground;
        repaint();
    }

    public void setEnabledAt(int index, boolean enabled) {
        tab(index).enabled = enabled;
        repaint();
    }

    /** It changes a tab's component without touching its title or its icon. */
    public void setComponentAt(int index, Component component) {
        Tab p = tab(index);
        if (component != p.component) {
            removeChild(p.component);
            p.component = component;
            if (component != null) {
                component.setVisible(index == getSelectedIndex());
                addImpl(component, null, -1);
            }
            revalidate();
            repaint();
        }
    }

    public void setDisplayedMnemonicIndexAt(int tabIndex, int mnemonicIndex) {
        Tab p = tab(tabIndex);
        if (mnemonicIndex != -1) {
            String title = p.title;
            if (title == null || mnemonicIndex < 0 || mnemonicIndex >= title.length()) {
                throw new IllegalArgumentException("Invalid mnemonic index: " + mnemonicIndex);
            }
        }
        p.mnemonicIndex = mnemonicIndex;
        repaint();
    }

    /**
     * The letter that jumps to this tab.
     *
     * <p>It also looks that letter up in the title in order to underline it. If it is not there,
     * nothing is underlined: the shortcut goes on working, only it is not seen.
     */
    public void setMnemonicAt(int tabIndex, int mnemonic) {
        Tab p = tab(tabIndex);
        p.mnemonic = mnemonic;
        String title = p.title;
        if (title != null && mnemonic != 0) {
            int i = title.toUpperCase(java.util.Locale.ROOT)
                    .indexOf(Character.toUpperCase((char) mnemonic));
            p.mnemonicIndex = i;
        }
        repaint();
    }

    /** The first tab with that title, or -1. */
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

    /** Which tab falls at that point, or -1; the look and feel answers it. */
    public int indexAtLocation(int x, int y) {
        if (ui != null) {
            return getUI().tabForCoordinate(this, x, y);
        }
        return -1;
    }

    /** The tool tip text of the tab that is under the mouse. */
    public String getToolTipText(MouseEvent event) {
        if (ui != null) {
            int index = getUI().tabForCoordinate(this, event.getX(), event.getY());
            if (index != -1) {
                return tab(index).tip;
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
     * A component that replaces the tab's title.
     *
     * <p>It is what allows a tab with a close button, or with two lines of text: instead of a
     * title and an icon, the component that is set is drawn.
     */
    public void setTabComponentAt(int index, Component component) {
        Tab p = tab(index);
        Component old = p.tabComponent;
        p.tabComponent = component;
        removeChild(old);
        if (component != null) {
            addImpl(component, null, -1);
        }
        revalidate();
        repaint();
    }

    public Component getTabComponentAt(int index) {
        return tab(index).tabComponent;
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
     * It removes a component from the list of children without going through
     * {@link #removeTabAt}.
     *
     * <p>It looks like a detour and it is necessary: {@code Container.remove(Component)} looks the
     * child's index up and calls {@code remove(int)}, which in this class removes a <em>tab</em>.
     * Removing child number three when there are four children and two tabs ends in an invalid
     * index, or worse, in removing the wrong tab.
     *
     * <p>The child and tab indices are not the same number: a tabbed pane also has the components
     * the look and feel adds.
     */
    private void removeChild(Component comp) {
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

    private Tab tab(int index) {
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
     * Everything that is known about a tab.
     *
     * <p>It is private in the JDK and here too: whoever uses the pane talks by index, and this
     * class is what allows the title and the component to change separately.
     */
    static class Tab implements Serializable {

        String title;
        Icon icon;
        Icon disabledIcon;
        Component component;
        String tip;
        Color background;
        Color foreground;
        boolean enabled = true;
        int mnemonic = -1;
        int mnemonicIndex = -1;
        Component tabComponent;

        Tab(JTabbedPane panel, String title, Icon icon, Icon disabledIcon,
                Component component, String tip) {
            this.title = title;
            this.icon = icon;
            this.disabledIcon = disabledIcon;
            this.component = component;
            this.tip = tip;
        }
    }
}
