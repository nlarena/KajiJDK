package javax.swing.text;

import java.io.Serializable;

import javax.swing.JFormattedTextField;
import javax.swing.JFormattedTextField$AbstractFormatter;
import javax.swing.JFormattedTextField$AbstractFormatterFactory;

/**
 * Una fabrica que elige entre cuatro formateadores segun el estado del campo.
 *
 * <h2>Por que cuatro</h2>
 *
 * <p>El texto que se muestra y el que se edita no tienen por que ser el mismo. Un importe se
 * muestra como <code>$ 1.234,50</code> y se edita como <code>1234,5</code>: con el signo de moneda
 * adelante, el usuario tendria que saltearlo con las flechas cada vez.
 *
 * <p>De ahi los cuatro: uno para cuando el campo esta en blanco, uno para cuando se lo esta
 * editando, uno para cuando solo se lo mira, y uno de reserva para cuando alguno de los otros no
 * esta puesto. La mayoria de los usos ponen solo el de reserva.
 */
public class DefaultFormatterFactory extends JFormattedTextField$AbstractFormatterFactory
        implements Serializable {

    private JFormattedTextField$AbstractFormatter defaultFormat;
    private JFormattedTextField$AbstractFormatter displayFormat;
    private JFormattedTextField$AbstractFormatter editFormat;
    private JFormattedTextField$AbstractFormatter nullFormat;

    /** Una fabrica sin ningun formateador; devuelve nulo hasta que le pongan alguno. */
    public DefaultFormatterFactory() {
    }

    /** Una fabrica con ese formateador de reserva. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat) {
        this(defaultFormat, null);
    }

    /** Una fabrica con el de reserva y el de solo mirar. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat) {
        this(defaultFormat, displayFormat, null);
    }

    /** Una fabrica con el de reserva, el de mirar y el de editar. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat,
            JFormattedTextField$AbstractFormatter editFormat) {
        this(defaultFormat, displayFormat, editFormat, null);
    }

    /** Una fabrica con los cuatro. */
    public DefaultFormatterFactory(JFormattedTextField$AbstractFormatter defaultFormat,
            JFormattedTextField$AbstractFormatter displayFormat,
            JFormattedTextField$AbstractFormatter editFormat,
            JFormattedTextField$AbstractFormatter nullFormat) {
        this.defaultFormat = defaultFormat;
        this.displayFormat = displayFormat;
        this.editFormat = editFormat;
        this.nullFormat = nullFormat;
    }

    /** El que se usa cuando ninguno de los otros corresponde. */
    public void setDefaultFormatter(JFormattedTextField$AbstractFormatter atf) {
        defaultFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getDefaultFormatter() {
        return defaultFormat;
    }

    /** El que se usa cuando el campo no tiene el foco. */
    public void setDisplayFormatter(JFormattedTextField$AbstractFormatter atf) {
        displayFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getDisplayFormatter() {
        return displayFormat;
    }

    /** El que se usa cuando el campo tiene el foco. */
    public void setEditFormatter(JFormattedTextField$AbstractFormatter atf) {
        editFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getEditFormatter() {
        return editFormat;
    }

    /** El que se usa cuando el valor es nulo. */
    public void setNullFormatter(JFormattedTextField$AbstractFormatter atf) {
        nullFormat = atf;
    }

    public JFormattedTextField$AbstractFormatter getNullFormatter() {
        return nullFormat;
    }

    /** Elige el formateador que corresponde al estado del campo. */
    public JFormattedTextField$AbstractFormatter getFormatter(JFormattedTextField source) {
        JFormattedTextField$AbstractFormatter format = null;

        if (source == null) {
            return null;
        }
        Object value = source.getValue();

        if (value == null) {
            format = getNullFormatter();
        }
        if (format == null) {
            if (source.hasFocus()) {
                format = getEditFormatter();
            } else {
                format = getDisplayFormatter();
            }
            if (format == null) {
                format = getDefaultFormatter();
            }
        }
        return format;
    }
}
