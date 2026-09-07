package javax.swing.text;

import java.text.AttributedCharacterIterator;
import java.text.Format;
import java.text.Format$Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JFormattedTextField;

/**
 * Un formateador que delega en un {@link Format} de {@code java.text}.
 *
 * <h2>Que agrega sobre {@link DefaultFormatter}</h2>
 *
 * <p>Tres cosas. Primero, la conversion la hace el {@code Format}, que sabe de idioma: separadores
 * de miles, nombres de meses, signos de moneda. Segundo, un rango: {@link #setMinimum} y
 * {@link #setMaximum} rechazan valores fuera de el. Tercero, y es lo interesante, sabe <em>que
 * parte</em> del texto es que: {@link #getFields} dice si la posicion 3 cae en el mes o en el ano.
 *
 * <h2>Para que sirve saber los campos</h2>
 *
 * <p>Para moverse por campos con las flechas y para subir y bajar el valor de uno solo. Sin eso,
 * un campo de fecha seria una cadena cualquiera y las flechas no podrian hacer nada mejor que
 * mover el cursor una letra.
 */
public class InternationalFormatter extends DefaultFormatter {

    private static final Format$Field[] EMPTY_FIELD_ARRAY = new Format$Field[0];

    private Format format;
    private Comparable<?> min;
    private Comparable<?> max;
    private transient Map<Format$Field, Object> literalMask;
    private transient AttributedCharacterIterator iterator;
    private transient boolean validMask;
    private transient String string;

    /** Un formateador sin formato; se comporta como el de siempre hasta que le pongan uno. */
    public InternationalFormatter() {
        setOverwriteMode(false);
    }

    /** Un formateador que usa ese formato. */
    public InternationalFormatter(Format format) {
        this();
        setFormat(format);
    }

    /** El formato que convierte entre valor y texto. */
    public void setFormat(Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    /**
     * El valor mas chico aceptable.
     *
     * <p>Si el valor actual es menor, se sube al minimo: dejarlo fuera de rango haria que el campo
     * mostrara algo que el mismo declara invalido.
     */
    public void setMinimum(Comparable<?> minimum) {
        if (getValueClass() == null && minimum != null) {
            setValueClass(minimum.getClass());
        }
        min = minimum;
    }

    public Comparable<?> getMinimum() {
        return min;
    }

    /** El valor mas grande aceptable. */
    public void setMaximum(Comparable<?> max) {
        if (getValueClass() == null && max != null) {
            setValueClass(max.getClass());
        }
        this.max = max;
    }

    public Comparable<?> getMaximum() {
        return max;
    }

    public void install(JFormattedTextField ftf) {
        super.install(ftf);
        updateMaskIfNecessary();
    }

    /** El texto del valor, segun el formato. */
    public String valueToString(Object value) throws ParseException {
        if (value == null) {
            return "";
        }
        Format f = getFormat();
        if (f == null) {
            return value.toString();
        }
        return f.format(value);
    }

    /**
     * El valor de ese texto, dentro del rango.
     *
     * @throws ParseException si el texto no se puede convertir o queda fuera de rango.
     */
    public Object stringToValue(String text) throws ParseException {
        Object value = stringToValue(text, getFormat());

        // Primero el tipo, despues el rango. Un Format devuelve el tipo que se le antoja (un
        // NumberFormat siempre da Long o Double), asi que comparar antes de convertir seria
        // comparar un Integer con un Long y terminar en una conversion invalida, no en un rango
        // mal.
        Class<?> vc = getValueClass();
        if (value != null && vc != null && !vc.isInstance(value)) {
            value = super.stringToValue(value.toString());
        }
        try {
            if (!isValidValue(value, true)) {
                throw new ParseException("Value not within min/max range", 0);
            }
        } catch (ClassCastException cce) {
            throw new ParseException("Class cast exception comparing values: " + cce, 0);
        }
        return value;
    }

    private Object stringToValue(String text, Format f) throws ParseException {
        if (f == null) {
            return super.stringToValue(text);
        }
        return f.parseObject(text);
    }

    /** Si el valor cae dentro del rango. */
    private boolean isValidValue(Object value, boolean wantsCCE) {
        Comparable<Object> min = (Comparable<Object>) getMinimum();
        try {
            if (min != null && min.compareTo(value) > 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }
        Comparable<Object> max = (Comparable<Object>) getMaximum();
        try {
            if (max != null && max.compareTo(value) < 0) {
                return false;
            }
        } catch (ClassCastException cce) {
            if (wantsCCE) {
                throw cce;
            }
            return false;
        }
        return true;
    }

    /**
     * Los campos del formato que cubren esa posicion del texto.
     *
     * <p>Puede haber mas de uno: los formatos anidados hacen que una posicion pertenezca a un campo
     * y al que lo contiene.
     */
    public Format$Field[] getFields(int offset) {
        if (getAllowsInvalid()) {
            // El texto puede no corresponder al formato: se rearma el recorrido.
            updateMask();
        }
        AttributedCharacterIterator iterator = getIterator();
        if (iterator != null && offset >= 0 && offset <= iterator.getEndIndex()) {
            iterator.setIndex(offset);
            Map<AttributedCharacterIterator.Attribute, Object> attrs = iterator.getAttributes();
            if (attrs != null && attrs.size() > 0) {
                List<Format$Field> al = new ArrayList<Format$Field>();
                for (AttributedCharacterIterator.Attribute key : attrs.keySet()) {
                    if (key instanceof Format$Field) {
                        al.add((Format$Field) key);
                    }
                }
                if (al.size() > 0) {
                    return al.toArray(EMPTY_FIELD_ARRAY);
                }
            }
        }
        return EMPTY_FIELD_ARRAY;
    }

    public Object clone() throws CloneNotSupportedException {
        InternationalFormatter formatter = (InternationalFormatter) super.clone();
        formatter.literalMask = null;
        formatter.iterator = null;
        formatter.validMask = false;
        formatter.string = null;
        return formatter;
    }

    /** Las acciones de subir y bajar el campo donde esta el cursor. */
    protected Action[] getActions() {
        if (getSupportsIncrement()) {
            return new Action[] {
                new IncrementAction(this, "increment", 1),
                new IncrementAction(this, "decrement", -1)
            };
        }
        return null;
    }

    /** Si tiene sentido subir y bajar el valor con las flechas. */
    boolean getSupportsIncrement() {
        return false;
    }

    /**
     * Reenvia a {@link #getFormattedTextField()} desde las clases anidadas.
     *
     * <p>Mismo motivo que en {@link DefaultFormatter}: hallazgo #512.
     */
    JFormattedTextField campo() {
        return getFormattedTextField();
    }

    AttributedCharacterIterator getIterator() {
        return iterator;
    }

    void updateMaskIfNecessary() {
        Format f = getFormat();
        if (f != null) {
            if (!validMask) {
                updateMask();
            } else {
                String text = getFormattedTextField() == null ? null
                        : getFormattedTextField().getText();
                if (text == null || !text.equals(string)) {
                    updateMask();
                }
            }
        }
    }

    /** Rearma el recorrido de campos a partir del texto actual. */
    void updateMask() {
        Format f = getFormat();
        validMask = false;
        iterator = null;
        string = null;
        if (f == null) {
            return;
        }
        JFormattedTextField ftf = getFormattedTextField();
        if (ftf == null) {
            return;
        }
        String text = ftf.getText();
        if (text == null) {
            return;
        }
        try {
            Object value = stringToValue(text, f);
            iterator = f.formatToCharacterIterator(value);
            string = text;
            validMask = true;
        } catch (Exception e) {
            // El texto no corresponde al formato: no hay campos que informar.
            iterator = null;
        }
    }

    /**
     * Sube o baja el valor.
     *
     * <p>La accion de las flechas arriba y abajo. Quien sabe como incrementar es la subclase: en
     * un numero es sumar uno, en una fecha depende del campo donde esta el cursor.
     */
    static class IncrementAction extends javax.swing.AbstractAction {

        private final InternationalFormatter fmt;
        private int direction;

        IncrementAction(InternationalFormatter fmt, String name, int direction) {
            super(name);
            this.fmt = fmt;
            this.direction = direction;
        }

        public void actionPerformed(java.awt.event.ActionEvent ae) {
            JFormattedTextField ftf = fmt.campo();
            if (ftf != null && ftf.isEditable()) {
                fmt.adjustValue(direction);
            }
        }
    }

    /** Suma esa cantidad al valor; la subclase que sepa hacerlo la sobrescribe. */
    void adjustValue(int direction) {
    }
}
