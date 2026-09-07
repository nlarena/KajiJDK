package javax.swing.plaf.basic;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;

import javax.swing.ComboBoxEditor;
import javax.swing.JTextField;
import javax.swing.border.Border;

/**
 * El campo de texto de un combo editable.
 *
 * <h2>Devolver el tipo, no el texto</h2>
 *
 * <p>Es lo unico dificil de esta clase. El campo guarda texto, pero el combo puede tener items que
 * no son cadenas -- numeros, fechas, colores --, y si al editar uno el combo recibiera de vuelta una
 * cadena, el modelo terminaria con tipos mezclados.
 *
 * <p>Asi que {@link #getItem} se acuerda del ultimo valor que le pusieron y de que clase era. Si el
 * texto no cambio, devuelve <em>ese mismo objeto</em>. Y si cambio, busca por reflexion un
 * {@code valueOf(String)} en la clase del valor viejo y lo usa para armar uno nuevo del mismo tipo.
 * Si no lo encuentra, o si falla, devuelve el texto: es peor, pero es mejor que romper.
 *
 * <h2>El campo sin borde</h2>
 *
 * <p>El campo va adentro del combo, que ya tiene su propio borde; uno mas se veria como un marco
 * dentro de otro. Por eso {@link #createEditorComponent} le saca el borde de entrada, y por eso el
 * campo no vuelve a escribir el texto si es el mismo que ya tiene: reescribirlo moveria el cursor
 * mientras alguien escribe.
 *
 * <p>La clase se llama {@code BorderlessTextField} y su {@code setBorder} <em>parece</em> filtrar
 * los bordes del aspecto, pero no filtra ninguno; ver la nota de ese metodo.
 */
public class BasicComboBoxEditor implements ComboBoxEditor, FocusListener {

    protected JTextField editor;
    private Object oldValue;

    public BasicComboBoxEditor() {
        editor = createEditorComponent();
    }

    public Component getEditorComponent() {
        return editor;
    }

    /** Ver la nota de la clase: nueve columnas y sin borde. */
    protected JTextField createEditorComponent() {
        JTextField campo = new BorderlessTextField("", 9);
        campo.setBorder(null);
        return campo;
    }

    /**
     * Pone ese valor en el campo.
     *
     * <p>Un valor nulo deja el campo vacio y <em>no</em> olvida el valor anterior: el tipo del
     * anterior es lo que {@link #getItem} necesita para devolver algo del tipo correcto.
     */
    public void setItem(Object anObject) {
        String text;
        if (anObject != null) {
            if (anObject instanceof String) {
                text = (String) anObject;
            } else {
                text = anObject.toString();
            }
            if (text == null) {
                text = "";
            }
            oldValue = anObject;
        } else {
            text = "";
        }
        if (!text.equals(editor.getText())) {
            editor.setText(text);
        }
    }

    /** Ver la nota de la clase. */
    public Object getItem() {
        Object newValue = editor.getText();
        if (oldValue != null && !(oldValue instanceof String)) {
            if (newValue.equals(oldValue.toString())) {
                return oldValue;
            }
            Class<?> cls = oldValue.getClass();
            try {
                Method method = cls.getMethod("valueOf", new Class<?>[] {String.class});
                newValue = method.invoke(oldValue, new Object[] {editor.getText()});
            } catch (Exception ex) {
                // Sin `valueOf` no hay manera de recuperar el tipo, y devolver el texto es lo
                // unico que queda. Ver la nota de la clase.
            }
        }
        return newValue;
    }

    public void selectAll() {
        editor.selectAll();
        editor.requestFocus();
    }

    /** No hace nada: el combo se entera del foco por su cuenta. */
    public void focusGained(FocusEvent e) {
    }

    /** Idem. */
    public void focusLost(FocusEvent e) {
    }

    public void addActionListener(ActionListener l) {
        editor.addActionListener(l);
    }

    public void removeActionListener(ActionListener l) {
        editor.removeActionListener(l);
    }

    /** Ver la nota de la clase. */
    static class BorderlessTextField extends JTextField {

        public BorderlessTextField(String value, int n) {
            super(value, n);
        }

        /** No reescribe el mismo texto: hacerlo moveria el cursor mientras alguien escribe. */
        public void setText(String s) {
            if (getText().equals(s)) {
                return;
            }
            super.setText(s);
        }

        /**
         * Acepta cualquier borde, y la comprobacion que parece rechazar alguno no rechaza ninguno.
         *
         * <p>La intencion del JDK era rechazar los bordes que pone el aspecto -- de ahi el nombre
         * de la clase --, y la comprobacion dice {@code b instanceof UIResource}. Pero adentro de
         * {@link BasicComboBoxEditor} el nombre {@code UIResource} no es
         * {@link javax.swing.plaf.UIResource}: es {@link BasicComboBoxEditor.UIResource}, la clase
         * anidada de aca al lado. Un borde nunca es una instancia de <em>esa</em>, asi que la
         * comprobacion siempre pasa y el borde siempre se pone.
         *
         * <p>Esta medido -- un {@code EmptyBorderUIResource} entra sin problema -- y se copia con
         * el mismo tipo, no con el que la intencion pedia: cambiarlo dejaria a los combos de esta
         * biblioteca sin el borde que el JDK si les pone.
         */
        public void setBorder(Border b) {
            if (!(b instanceof BasicComboBoxEditor.UIResource)) {
                super.setBorder(b);
            }
        }
    }

    /**
     * El mismo editor, marcado como puesto por el aspecto.
     *
     * <p>Ver {@link BasicComboBoxRenderer.UIResource}.
     */
    public static class UIResource extends BasicComboBoxEditor
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
