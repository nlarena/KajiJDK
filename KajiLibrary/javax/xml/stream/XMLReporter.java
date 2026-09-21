package javax.xml.stream;

/**
 * KajiLibrary's javax.xml.stream.XMLReporter -- where the warnings that do **not** interrupt
 * reading go.
 *
 * <p>A parser finds two kinds of problem and they are not treated the same. What prevents reading
 * on --an unclosed tag, an illegal character-- comes out as {@link XMLStreamException} and cuts the
 * walk short. What can be reported and read past --an undeclared entity resolved as empty, a
 * duplicate attribute that is discarded-- arrives here. Without this channel, the only way out
 * would be to choose between aborting over something minor or keeping quiet, and both are bad.
 *
 * <p>That the method can throw {@link XMLStreamException} is what gives control back to whoever
 * writes the reporter: a warning the application considers fatal becomes fatal by throwing from
 * inside.
 */
public interface XMLReporter {

    /**
     * Reports a non-fatal problem.
     *
     * @param message the text of the warning
     * @param errorType the kind of problem, defined by the parser implementation
     * @param relatedInformation whatever the parser has at hand about the case, or null
     * @param location where it happened, or null if not known
     * @throws XMLStreamException if the reporter decides this warning does have to cut
     */
    void report(String message, String errorType, Object relatedInformation, Location location)
            throws XMLStreamException;
}
