package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.URISyntax;

/*
 * CABECERA DE FAMILIA -- los atributos {@code URISyntax} de este paquete.
 *
 * <p>Una URI y nada mas. El mecanismo esta en {@link javax.print.attribute.URISyntax URISyntax}:
 * null es error, {@code equals()} delega en el de {@link java.net.URI} y {@code toString()}
 * imprime la URI.
 *
 * <p>Ninguna de estas clases abre nada. Guardan una direccion; ir a buscarla es cosa de
 * {@code javax.print}, y que esquemas se pueden ir a buscar lo dice
 * {@link ReferenceUriSchemesSupported}.
 */

/**
 * A donde mandar la salida en vez de al papel.
 *
 * <p>El nombre IPP no coincide con el de la clase --es {@code "spool-data-destination"}-- y esa
 * diferencia es observable desde {@code getName()}. Tipicamente un {@code file:} para volcar el
 * PostScript a disco. Si el esquema no esta soportado, el trabajo falla.
 */
public final class Destination extends URISyntax implements PrintJobAttribute, PrintRequestAttribute {

    private static final long serialVersionUID = 6776739171700415321L;

    public Destination(URI uri) {
        super(uri);
    }

    public boolean equals(Object object) {
        return super.equals(object) && object instanceof Destination;
    }

    public final Class<? extends Attribute> getCategory() {
        return Destination.class;
    }

    public final String getName() {
        return "spool-data-destination";
    }
}
