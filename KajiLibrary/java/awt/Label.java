package java.awt;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un texto de una sola línea que no se puede editar ni seleccionar.
 *
 * <p>Es el componente más pasivo de AWT: muestra un texto y nada más. No recibe el foco, no genera
 * eventos propios y no tiene estado más allá de lo que dice y cómo lo alinea.
 */
public class Label extends Component implements Accessible {

    private static final long serialVersionUID = 3094126758329070636L;

    /** Alinea el texto a la izquierda. */
    public static final int LEFT = 0;

    /** Lo centra. */
    public static final int CENTER = 1;

    /** Lo alinea a la derecha. */
    public static final int RIGHT = 2;

    private static int labelCounter = 0;

    /** Lo que dice. */
    String text;

    /** Cómo lo alinea. */
    int alignment = LEFT;

    /** Una etiqueta vacía, alineada a la izquierda. */
    public Label() throws HeadlessException {
        this("", LEFT);
    }

    /** Una etiqueta con ese texto, alineada a la izquierda. */
    public Label(String text) throws HeadlessException {
        this(text, LEFT);
    }

    /**
     * Una etiqueta con ese texto y esa alineación.
     *
     * @throws IllegalArgumentException si la alineación no es {@link #LEFT}, {@link #CENTER} ni
     *     {@link #RIGHT}
     */
    public Label(String text, int alignment) throws HeadlessException {
        this.text = text;
        this.setAlignment(alignment);
    }

    String constructComponentName() {
        synchronized (Label.class) {
            String n = "label" + labelCounter;
            labelCounter = labelCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Cómo alinea el texto. */
    public int getAlignment() {
        return this.alignment;
    }

    /**
     * Cambia la alineación.
     *
     * @throws IllegalArgumentException si no es una de las tres constantes
     */
    public synchronized void setAlignment(int alignment) {
        if (alignment != LEFT && alignment != CENTER && alignment != RIGHT) {
            throw new IllegalArgumentException("improper alignment: " + alignment);
        }
        this.alignment = alignment;
    }

    /**
     * Lo que dice.
     *
     * @return el texto, o `null` si nunca se le puso ninguno
     */
    public String getText() {
        return this.text;
    }

    /**
     * Cambia lo que dice.
     *
     * <p>Poner el mismo texto no hace nada: la comparación evita invalidar la distribución al pedo
     * cuando algo refresca la etiqueta en un ciclo.
     */
    public void setText(String text) {
        boolean cambio;
        synchronized (this) {
            cambio = text != this.text && (this.text == null || !this.text.equals(text));
            if (cambio) {
                this.text = text;
            }
        }
        if (cambio) {
            this.invalidate();
        }
    }

    protected String paramString() {
        String alineacion = "left";
        if (this.alignment == CENTER) {
            alineacion = "center";
        } else if (this.alignment == RIGHT) {
            alineacion = "right";
        }
        return super.paramString() + ",align=" + alineacion + ",text=" + this.text;
    }

    /** La accesibilidad de la etiqueta. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTLabel();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de una etiqueta.
     *
     * <p>Su nombre accesible es **el texto**, no el nombre del componente: para quien la lee con un
     * lector de pantalla, la etiqueta es lo que dice.
     */
    protected class AccessibleAWTLabel extends AccessibleAWTComponent {

        /** Para las subclases. */
        protected AccessibleAWTLabel() {
        }

        public String getAccessibleName() {
            if (Label.this.getText() == null) {
                return super.getAccessibleName();
            }
            return Label.this.getText();
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }
    }
}
