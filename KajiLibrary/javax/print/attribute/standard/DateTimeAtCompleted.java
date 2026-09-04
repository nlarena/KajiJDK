package javax.print.attribute.standard;

import java.util.Date;
import javax.print.attribute.Attribute;
import javax.print.attribute.DateTimeSyntax;
import javax.print.attribute.PrintJobAttribute;

/*
 * CABECERA DE FAMILIA -- los atributos {@code DateTimeSyntax} de este paquete.
 *
 * <p>Un instante. El mecanismo esta en {@link javax.print.attribute.DateTimeSyntax DateTimeSyntax},
 * que copia el {@link java.util.Date} al entrar y al salir para que el atributo sea inmutable
 * aunque {@code Date} no lo sea.
 *
 * <p>Tres de las cuatro son marcas de tiempo que reporta el trabajo --cuando se creo, cuando
 * empezo, cuando termino-- y solo {@link JobHoldUntil} se pide.
 */

/**
 * Cuando el trabajo llego a un estado terminal: {@code COMPLETED}, {@code CANCELED} o {@code
 * ABORTED}.
 *
 * <p>El atributo no existe hasta que el trabajo termina, y no distingue como termino --eso lo dice
 * {@link JobState}.
 */
public final class DateTimeAtCompleted extends DateTimeSyntax implements PrintJobAttribute {

    private static final long serialVersionUID = 6497399708058490000L;

    public DateTimeAtCompleted(Date dateTimeAtCompleted) {
        super(dateTimeAtCompleted);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof DateTimeAtCompleted;
    }

    public final Class<? extends Attribute> getCategory() {
        return DateTimeAtCompleted.class;
    }

    public final String getName() {
        return "date-time-at-completed";
    }
}
