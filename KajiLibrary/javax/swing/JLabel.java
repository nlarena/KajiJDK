package javax.swing;

import java.awt.Component;

import javax.swing.plaf.LabelUI;
import javax.swing.plaf.basic.BasicLabelUI;

/**
 * Un texto, un icono, o los dos: el componente mas simple que muestra algo.
 *
 * <h2>Seis perillas para una cosa</h2>
 *
 * <p>Una etiqueta tiene dos alineaciones y dos posiciones, y la diferencia es la que confunde:
 * la <em>alineacion</em> dice donde va el conjunto icono-texto dentro del area de la etiqueta; la
 * <em>posicion</em> del texto dice donde va el texto <em>respecto del icono</em>. Con las cuatro
 * mas la separacion ({@link #setIconTextGap}) se describe cualquier disposicion, y quien la
 * resuelve es {@code SwingUtilities.layoutCompoundLabel}, el mismo para botones y celdas.
 *
 * <p>Por omision: alineada al principio ({@code LEADING}), centrada verticalmente, y el texto
 * despues del icono ({@code TRAILING}) — o sea icono a la izquierda y texto a la derecha en un
 * idioma que se lee asi.
 *
 * <h2>Lo que hace de {@code UIManager}</h2>
 *
 * <p>{@link #updateUI} instala {@link BasicLabelUI} directamente. En el JDK le pregunta al
 * {@code UIManager}, que puede contestar con el aspecto de Metal, de Windows o del que este
 * instalado; aca hay un solo aspecto, y preguntar seria una ceremonia con una sola respuesta.
 *
 * <h2>Lo que no esta</h2>
 *
 * <p>Tres cosas, y ninguna es un olvido. {@code getDisabledIcon} no fabrica una version gris del
 * icono cuando no se fijo una: eso pide una imagen que filtrar, y un {@link Icon} no promete tener
 * una. {@code imageUpdate} no esta porque su unica razon es repintar cuando una imagen asincronica
 * termina de cargar, y aca las imagenes no se cargan asincronicamente. Y la accesibilidad, que es
 * un subsistema propio.
 */
public class JLabel extends JComponent implements SwingConstants {

    private static final long serialVersionUID = 5296049245363908046L;

    /** La propiedad de cliente con la que un componente sabe que etiqueta lo nombra. */
    static final String LABELED_BY_PROPERTY = "labeledBy";

    /** El componente que esta etiqueta describe, o {@code null}. */
    protected Component labelFor;

    private int mnemonic = '\0';
    private int mnemonicIndex = -1;
    private String text = "";
    private Icon defaultIcon;
    private Icon disabledIcon;
    private boolean disabledIconSet;
    private int verticalAlignment = CENTER;
    private int horizontalAlignment = LEADING;
    private int verticalTextPosition = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int iconTextGap = 4;

    /** Con texto, icono y alineacion horizontal. */
    public JLabel(String text, Icon icon, int horizontalAlignment) {
        setText(text);
        setIcon(icon);
        setHorizontalAlignment(horizontalAlignment);
        updateUI();
        setAlignmentX(LEFT_ALIGNMENT);
    }

    /** Con texto y alineacion horizontal. */
    public JLabel(String text, int horizontalAlignment) {
        this(text, null, horizontalAlignment);
    }

    /** Con texto, alineado al principio. */
    public JLabel(String text) {
        this(text, null, LEADING);
    }

    /** Con icono y alineacion horizontal. */
    public JLabel(Icon image, int horizontalAlignment) {
        this(null, image, horizontalAlignment);
    }

    /** Con icono, centrado. */
    public JLabel(Icon image) {
        this(null, image, CENTER);
    }

    /** Vacia. */
    public JLabel() {
        this("", null, LEADING);
    }

    /** El aspecto instalado. */
    public LabelUI getUI() {
        return (LabelUI) this.ui;
    }

    /** Instala un aspecto de etiqueta. */
    public void setUI(LabelUI ui) {
        super.setUI(ui);
    }

    /** Instala el aspecto basico; ver la nota de la clase sobre {@code UIManager}. */
    public void updateUI() {
        setUI((LabelUI) BasicLabelUI.createUI(this));
    }

    public String getUIClassID() {
        return "LabelUI";
    }

    /** El texto, o {@code null}. */
    public String getText() {
        return this.text;
    }

    /**
     * Cambia el texto.
     *
     * <p>Si habia un mnemonico, se vuelve a buscar en el texto nuevo: la letra subrayada tiene que
     * seguir siendo la del mnemonico, no la que estaba en esa posicion.
     */
    public void setText(String text) {
        String viejo = this.text;
        this.text = text;
        firePropertyChange("text", viejo, text);
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(text, getDisplayedMnemonic()));
        if (text == null || viejo == null || !text.equals(viejo)) {
            revalidate();
            repaint();
        }
    }

    /** El icono, o {@code null}. */
    public Icon getIcon() {
        return this.defaultIcon;
    }

    /** Cambia el icono; pide relayout solo si cambio de tamano. */
    public void setIcon(Icon icon) {
        Icon viejo = this.defaultIcon;
        this.defaultIcon = icon;
        firePropertyChange("icon", viejo, icon);
        if (viejo != icon) {
            if (viejo == null || icon == null
                    || viejo.getIconWidth() != icon.getIconWidth()
                    || viejo.getIconHeight() != icon.getIconHeight()) {
                revalidate();
            }
            repaint();
        }
    }

    /** El icono para cuando esta deshabilitada, o {@code null}; ver la nota de la clase. */
    public Icon getDisabledIcon() {
        return this.disabledIcon;
    }

    /** Fija el icono para cuando esta deshabilitada. */
    public void setDisabledIcon(Icon disabledIcon) {
        Icon viejo = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        this.disabledIconSet = disabledIcon != null;
        firePropertyChange("disabledIcon", viejo, disabledIcon);
        if (disabledIcon != viejo) {
            if (disabledIcon == null || viejo == null
                    || disabledIcon.getIconWidth() != viejo.getIconWidth()
                    || disabledIcon.getIconHeight() != viejo.getIconHeight()) {
                revalidate();
            }
            if (!isEnabled()) {
                repaint();
            }
        }
    }

    /**
     * Fija el mnemonico como codigo de tecla, y busca que letra subrayar.
     *
     * <p>Es {@code int} y no {@code char} porque es un codigo de {@code KeyEvent}, que no siempre
     * corresponde a un caracter.
     */
    public void setDisplayedMnemonic(int key) {
        int viejo = this.mnemonic;
        this.mnemonic = key;
        firePropertyChange("displayedMnemonic", viejo, this.mnemonic);
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(getText(), this.mnemonic));
        if (key != viejo) {
            revalidate();
            repaint();
        }
    }

    /** Fija el mnemonico como caracter; se guarda en mayuscula, que es el codigo de tecla. */
    public void setDisplayedMnemonic(char aChar) {
        setDisplayedMnemonic((int) Character.toUpperCase(aChar));
    }

    /** El mnemonico, como codigo de tecla; {@code 0} si no hay. */
    public int getDisplayedMnemonic() {
        return this.mnemonic;
    }

    /**
     * Fija que caracter se subraya, por posicion.
     *
     * <p>Para cuando la busqueda automatica elige mal: en {@code "Save As"} con mnemonico
     * {@code A}, la primera {@code A} es la de {@code As}, y quiza se queria la de {@code Save}.
     *
     * @throws IllegalArgumentException si la posicion no cae en el texto, salvo {@code -1}
     */
    public void setDisplayedMnemonicIndex(int index) {
        int viejo = this.mnemonicIndex;
        if (index == -1) {
            this.mnemonicIndex = -1;
        } else {
            String t = getText();
            int largo = t == null ? 0 : t.length();
            if (index < -1 || index >= largo) {
                throw new IllegalArgumentException("index == " + String.valueOf(index));
            }
            this.mnemonicIndex = index;
        }
        firePropertyChange("displayedMnemonicIndex", viejo, index);
        if (index != viejo) {
            revalidate();
            repaint();
        }
    }

    /** La posicion del caracter subrayado, o {@code -1}. */
    public int getDisplayedMnemonicIndex() {
        return this.mnemonicIndex;
    }

    /**
     * Valida una clave horizontal.
     *
     * @throws IllegalArgumentException con el mensaje dado si no es una de las cinco validas
     */
    protected int checkHorizontalKey(int key, String message) {
        if (key == LEFT || key == CENTER || key == RIGHT || key == LEADING || key == TRAILING) {
            return key;
        }
        throw new IllegalArgumentException(message);
    }

    /**
     * Valida una clave vertical.
     *
     * @throws IllegalArgumentException con el mensaje dado si no es una de las tres validas
     */
    protected int checkVerticalKey(int key, String message) {
        if (key == TOP || key == CENTER || key == BOTTOM) {
            return key;
        }
        throw new IllegalArgumentException(message);
    }

    /** Los pixeles entre el icono y el texto. */
    public int getIconTextGap() {
        return this.iconTextGap;
    }

    /** Cambia la separacion entre icono y texto. */
    public void setIconTextGap(int iconTextGap) {
        int viejo = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        firePropertyChange("iconTextGap", viejo, iconTextGap);
        if (iconTextGap != viejo) {
            revalidate();
            repaint();
        }
    }

    /** Donde va el conjunto verticalmente: {@code TOP}, {@code CENTER} o {@code BOTTOM}. */
    public int getVerticalAlignment() {
        return this.verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment == this.verticalAlignment) {
            return;
        }
        int viejo = this.verticalAlignment;
        this.verticalAlignment = checkVerticalKey(alignment, "verticalAlignment");
        firePropertyChange("verticalAlignment", viejo, this.verticalAlignment);
        repaint();
    }

    /** Donde va el conjunto horizontalmente. */
    public int getHorizontalAlignment() {
        return this.horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment == this.horizontalAlignment) {
            return;
        }
        int viejo = this.horizontalAlignment;
        this.horizontalAlignment = checkHorizontalKey(alignment, "horizontalAlignment");
        firePropertyChange("horizontalAlignment", viejo, this.horizontalAlignment);
        repaint();
    }

    /** Donde va el texto respecto del icono, verticalmente. */
    public int getVerticalTextPosition() {
        return this.verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition == this.verticalTextPosition) {
            return;
        }
        int viejo = this.verticalTextPosition;
        this.verticalTextPosition = checkVerticalKey(textPosition, "verticalTextPosition");
        firePropertyChange("verticalTextPosition", viejo, this.verticalTextPosition);
        revalidate();
        repaint();
    }

    /** Donde va el texto respecto del icono, horizontalmente. */
    public int getHorizontalTextPosition() {
        return this.horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        int viejo = this.horizontalTextPosition;
        this.horizontalTextPosition = checkHorizontalKey(textPosition, "horizontalTextPosition");
        firePropertyChange("horizontalTextPosition", viejo, this.horizontalTextPosition);
        revalidate();
        repaint();
    }

    protected String paramString() {
        String textoStr = this.text != null ? this.text : "";
        String iconoStr = this.defaultIcon != null && this.defaultIcon != this ? this.defaultIcon.toString() : "";
        String desIconoStr = this.disabledIcon != null && this.disabledIcon != this ? this.disabledIcon.toString() : "";
        String labelForStr = this.labelFor != null ? this.labelFor.toString() : "";
        return super.paramString()
                + ",defaultIcon=" + iconoStr
                + ",disabledIcon=" + desIconoStr
                + ",horizontalAlignment=" + nombreHorizontal(this.horizontalAlignment)
                + ",horizontalTextPosition=" + nombreHorizontal(this.horizontalTextPosition)
                + ",iconTextGap=" + String.valueOf(this.iconTextGap)
                + ",labelFor=" + labelForStr
                + ",text=" + textoStr
                + ",verticalAlignment=" + nombreVertical(this.verticalAlignment)
                + ",verticalTextPosition=" + nombreVertical(this.verticalTextPosition);
    }

    private static String nombreHorizontal(int k) {
        if (k == LEFT) {
            return "LEFT";
        }
        if (k == CENTER) {
            return "CENTER";
        }
        if (k == RIGHT) {
            return "RIGHT";
        }
        if (k == LEADING) {
            return "LEADING";
        }
        if (k == TRAILING) {
            return "TRAILING";
        }
        return "";
    }

    private static String nombreVertical(int k) {
        if (k == TOP) {
            return "TOP";
        }
        if (k == CENTER) {
            return "CENTER";
        }
        if (k == BOTTOM) {
            return "BOTTOM";
        }
        return "";
    }

    /** El componente que esta etiqueta describe, o {@code null}. */
    public Component getLabelFor() {
        return this.labelFor;
    }

    /**
     * Dice a que componente describe esta etiqueta.
     *
     * <p>El componente se entera por la propiedad de cliente {@code labeledBy}: es como un lector
     * de pantalla, o el mnemonico, encuentran el campo a partir de su etiqueta.
     */
    public void setLabelFor(Component c) {
        Component viejo = this.labelFor;
        this.labelFor = c;
        firePropertyChange("labelFor", viejo, c);
        if (viejo instanceof JComponent) {
            ((JComponent) viejo).putClientProperty(LABELED_BY_PROPERTY, null);
        }
        if (c instanceof JComponent) {
            ((JComponent) c).putClientProperty(LABELED_BY_PROPERTY, this);
        }
    }
}
