package javax.print.attribute.standard;

import java.net.URI;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;
import javax.print.attribute.PrintRequestAttribute;
import javax.print.attribute.URISyntax;

/*
 * FAMILY HEADER -- this package's {@code URISyntax} attributes.
 *
 * <p>A URI and nothing more. The mechanism is in {@link javax.print.attribute.URISyntax URISyntax}:
 * null is an error, {@code equals()} delegates to {@link java.net.URI}'s and {@code toString()}
 * prints the URI.
 *
 * <p>None of these classes opens anything. They keep an address; going to fetch it is
 * {@code javax.print}'s business, and which schemes can be fetched is said by
 * {@link ReferenceUriSchemesSupported}.
 */

/**
 * Where to send the output instead of to paper.
 *
 * <p>The IPP name does not match the class's --it is {@code "spool-data-destination"}-- and that
 * difference is observable from {@code getName()}. Typically a {@code file:} to dump the PostScript
 * to disk. If the scheme is not supported, the job fails.
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
