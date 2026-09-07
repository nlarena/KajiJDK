package javax.swing.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;

/**
 * Dibuja una fila de arbol: un icono y un texto.
 *
 * <h2>Un solo componente para todas las filas</h2>
 *
 * <p>El arbol no tiene un componente por fila: tiene <em>este</em>, y lo configura y lo dibuja una
 * vez por fila. De ahi que casi todos los metodos que en un componente normal disparan un
 * repintado -- {@link #revalidate}, {@link #repaint}, {@link #invalidate},
 * {@code firePropertyChange} -- esten <strong>vaciados a proposito</strong>: un dibujante que pide
 * repintarse mientras lo estan dibujando haria que el arbol se repinte en bucle.
 *
 * <p>Es la razon de que la lista de metodos de esta clase sea tan larga y tan aburrida. No es
 * codigo de mas; es la lista de cosas que hay que apagar.
 *
 * <h2>Tres iconos, no uno</h2>
 *
 * <p>Hoja, carpeta abierta y carpeta cerrada. Los elige {@link #getTreeCellRendererComponent} segun
 * lo que le diga el arbol, y no segun lo que el nodo sepa de si mismo: si una carpeta esta abierta o
 * cerrada es de la vista, no del modelo.
 *
 * <h2>Los colores salen del aspecto</h2>
 *
 * <p>El constructor los pide por {@link UIManager}. Sin un aspecto instalado quedan nulos, y
 * entonces el arbol se dibuja con los colores del componente. Los seis {@code setXxx} los cambian a
 * mano y funcionan siempre.
 */
public class DefaultTreeCellRenderer extends JLabel implements TreeCellRenderer {

    /** Si la fila que se esta dibujando esta elegida. */
    protected boolean selected;

    /** Si la fila que se esta dibujando tiene el foco. */
    protected boolean hasFocus;

    private boolean drawsFocusBorderAroundIcon;
    private boolean drawDashedFocusIndicator;

    /** El icono de una carpeta cerrada. */
    protected transient Icon closedIcon;

    /** El icono de una hoja. */
    protected transient Icon leafIcon;

    /** El icono de una carpeta abierta. */
    protected transient Icon openIcon;

    /** El color del texto elegido. */
    protected Color textSelectionColor;

    /** El color del texto no elegido. */
    protected Color textNonSelectionColor;

    /** El fondo del texto elegido. */
    protected Color backgroundSelectionColor;

    /** El fondo del texto no elegido. */
    protected Color backgroundNonSelectionColor;

    /** El color del recuadro alrededor de lo elegido. */
    protected Color borderSelectionColor;

    private transient JTree tree;

    /** Un dibujante con los iconos y colores del aspecto. */
    public DefaultTreeCellRenderer() {
        inicializar();
    }

    private void inicializar() {
        setHorizontalAlignment(JLabel.LEFT);
        setLeafIcon(UIManager.getIcon("Tree.leafIcon"));
        setClosedIcon(UIManager.getIcon("Tree.closedIcon"));
        setOpenIcon(UIManager.getIcon("Tree.openIcon"));
        setTextSelectionColor(UIManager.getColor("Tree.selectionForeground"));
        setTextNonSelectionColor(UIManager.getColor("Tree.textForeground"));
        setBackgroundSelectionColor(UIManager.getColor("Tree.selectionBackground"));
        setBackgroundNonSelectionColor(UIManager.getColor("Tree.textBackground"));
        setBorderSelectionColor(UIManager.getColor("Tree.selectionBorderColor"));
        Object value = UIManager.get("Tree.drawsFocusBorderAroundIcon");
        drawsFocusBorderAroundIcon = (value != null && ((Boolean) value).booleanValue());
        value = UIManager.get("Tree.drawDashedFocusIndicator");
        drawDashedFocusIndicator = (value != null && ((Boolean) value).booleanValue());
    }

    /** Vuelve a pedirle al aspecto los iconos y colores que no se pusieron a mano. */
    public void updateUI() {
        super.updateUI();
        if (closedIcon instanceof UIResource) {
            closedIcon = null;
        }
        if (openIcon instanceof UIResource) {
            openIcon = null;
        }
        if (leafIcon instanceof UIResource) {
            leafIcon = null;
        }
        inicializar();
    }

    /** El icono de carpeta abierta que dice el aspecto. */
    public Icon getDefaultOpenIcon() {
        return UIManager.getIcon("Tree.openIcon");
    }

    /** El icono de carpeta cerrada que dice el aspecto. */
    public Icon getDefaultClosedIcon() {
        return UIManager.getIcon("Tree.closedIcon");
    }

    /** El icono de hoja que dice el aspecto. */
    public Icon getDefaultLeafIcon() {
        return UIManager.getIcon("Tree.leafIcon");
    }

    public void setOpenIcon(Icon newIcon) {
        openIcon = newIcon;
    }

    public Icon getOpenIcon() {
        return openIcon;
    }

    public void setClosedIcon(Icon newIcon) {
        closedIcon = newIcon;
    }

    public Icon getClosedIcon() {
        return closedIcon;
    }

    public void setLeafIcon(Icon newIcon) {
        leafIcon = newIcon;
    }

    public Icon getLeafIcon() {
        return leafIcon;
    }

    public void setTextSelectionColor(Color newColor) {
        textSelectionColor = newColor;
    }

    public Color getTextSelectionColor() {
        return textSelectionColor;
    }

    public void setTextNonSelectionColor(Color newColor) {
        textNonSelectionColor = newColor;
    }

    public Color getTextNonSelectionColor() {
        return textNonSelectionColor;
    }

    public void setBackgroundSelectionColor(Color newColor) {
        backgroundSelectionColor = newColor;
    }

    public Color getBackgroundSelectionColor() {
        return backgroundSelectionColor;
    }

    public void setBackgroundNonSelectionColor(Color newColor) {
        backgroundNonSelectionColor = newColor;
    }

    public Color getBackgroundNonSelectionColor() {
        return backgroundNonSelectionColor;
    }

    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    /**
     * Cambia la tipografia.
     *
     * <p>Una tipografia que venga del aspecto se descarta: ver {@link #getFont}.
     */
    public void setFont(Font font) {
        if (font instanceof javax.swing.plaf.FontUIResource) {
            font = null;
        }
        super.setFont(font);
    }

    /**
     * La tipografia; si no tiene una propia, la del arbol.
     *
     * <p>Es lo que hace que cambiarle la tipografia al arbol se note en las filas sin tocar el
     * dibujante.
     */
    public Font getFont() {
        Font font = super.getFont();
        if (font == null && tree != null) {
            font = tree.getFont();
        }
        return font;
    }

    /**
     * Cambia el fondo.
     *
     * <p>Un color que venga del aspecto se descarta, por lo mismo que en {@link #setFont}.
     */
    public void setBackground(Color color) {
        if (color instanceof javax.swing.plaf.ColorUIResource) {
            color = null;
        }
        super.setBackground(color);
    }

    /**
     * Se configura para dibujar esa fila y se devuelve a si mismo.
     *
     * <p>El texto sale de {@link JTree#convertValueToText}, no de {@code toString}: es el arbol el
     * que decide como se escribe un nodo.
     */
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        String stringValue = tree.convertValueToText(value, sel, expanded, leaf, row, hasFocus);
        this.tree = tree;
        this.hasFocus = hasFocus;
        setText(stringValue);
        Color fg;
        if (sel) {
            fg = getTextSelectionColor();
        } else {
            fg = getTextNonSelectionColor();
        }
        setForeground(fg);
        Icon icon;
        if (leaf) {
            icon = getLeafIcon();
        } else if (expanded) {
            icon = getOpenIcon();
        } else {
            icon = getClosedIcon();
        }
        if (!tree.isEnabled()) {
            setEnabled(false);
            LookAndFeel laf = UIManager.getLookAndFeel();
            Icon disabledIcon = (laf == null) ? null : laf.getDisabledIcon(tree, icon);
            if (disabledIcon != null) {
                icon = disabledIcon;
            }
            setDisabledIcon(icon);
        } else {
            setEnabled(true);
            setIcon(icon);
        }
        setComponentOrientation(tree.getComponentOrientation());
        selected = sel;
        return this;
    }

    /**
     * Dibuja el fondo de la fila y despues el texto.
     *
     * <p>El fondo solo cubre lo que ocupan el icono y el texto, no la fila entera: en un arbol la
     * seleccion se ve como una etiqueta pintada, no como una banda de lado a lado.
     */
    public void paint(Graphics g) {
        Color bColor;
        if (selected) {
            bColor = getBackgroundSelectionColor();
        } else {
            bColor = getBackgroundNonSelectionColor();
            if (bColor == null) {
                bColor = getBackground();
            }
        }
        int imageOffset = -1;
        if (bColor != null) {
            imageOffset = getLabelStart();
            g.setColor(bColor);
            if (getComponentOrientation().isLeftToRight()) {
                g.fillRect(imageOffset, 0, getWidth() - imageOffset, getHeight());
            } else {
                g.fillRect(0, 0, getWidth() - imageOffset, getHeight());
            }
        }
        if (hasFocus) {
            if (drawsFocusBorderAroundIcon) {
                imageOffset = 0;
            } else if (imageOffset == -1) {
                imageOffset = getLabelStart();
            }
            Color bsColor = getBorderSelectionColor();
            if (bsColor != null) {
                g.setColor(bsColor);
                if (getComponentOrientation().isLeftToRight()) {
                    g.drawRect(imageOffset, 0, getWidth() - imageOffset - 1, getHeight() - 1);
                } else {
                    g.drawRect(0, 0, getWidth() - imageOffset - 1, getHeight() - 1);
                }
            }
        }
        super.paint(g);
    }

    /** Donde empieza el texto: despues del icono y su separacion. */
    private int getLabelStart() {
        Icon currentI = getIcon();
        if (currentI != null && getText() != null) {
            return currentI.getIconWidth() + Math.max(0, getIconTextGap() - 1);
        }
        return 0;
    }

    /**
     * Lo que ocupa, con tres pixeles de mas a la derecha.
     *
     * <p>Los tres pixeles no son cosmeticos: sin ellos la ultima letra queda pegada al borde del
     * recuadro de seleccion.
     */
    public Dimension getPreferredSize() {
        Dimension retDimension = super.getPreferredSize();
        if (retDimension != null) {
            retDimension = new Dimension(retDimension.width + 3, retDimension.height);
        }
        return retDimension;
    }

    /** No hace nada; ver la nota de la clase. */
    public void validate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void invalidate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void revalidate() {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint(long tm, int x, int y, int width, int height) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint(Rectangle r) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void repaint() {
    }

    /**
     * Solo deja pasar el aviso de que cambio el texto.
     *
     * <p>Los demas se descartan por lo que dice la nota de la clase. El texto pasa porque el aspecto
     * lo necesita para volver a medir la fila, y eso ocurre una vez por fila, no en bucle.
     *
     * <p>El JDK deja pasar tambien la tipografia y el color <em>cuando el texto es HTML</em>, porque
     * entonces la vista de HTML tiene que rearmarse. Esa rama pide {@code BasicHTML}, que esta
     * biblioteca no trae; sin ella el texto sigue siendo texto y la rama no haria nada.
     */
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (propertyName == "text") {
            super.firePropertyChange(propertyName, oldValue, newValue);
        }
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, byte oldValue, byte newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, short oldValue, short newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, int oldValue, int newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, long oldValue, long newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, float oldValue, float newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, double oldValue, double newValue) {
    }

    /** No hace nada; ver la nota de la clase. */
    public void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
    }
}
