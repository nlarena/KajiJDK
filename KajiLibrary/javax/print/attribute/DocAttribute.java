package javax.print.attribute;

/**
 * KajiLibrary's javax.print.attribute.DocAttribute -- an attribute that applies to **one
 * document**.
 *
 * <h2>The family of the four markers, explained once</h2>
 *
 * <p>{@code DocAttribute}, {@link PrintRequestAttribute}, {@link PrintJobAttribute} and
 * {@link PrintServiceAttribute} declare not a single member. The only thing they do is say **which
 * scope** an attribute belongs to, and that scope becomes a real type restriction in the sets:
 * {@link DocAttributeSet} only accepts {@code DocAttribute}s, {@link PrintJobAttributeSet} only
 * {@code PrintJobAttribute}s, and so on. A misplaced attribute does not compile --or blows up with
 * {@code ClassCastException} if it is put in through the raw interface.
 *
 * <p>The same attribute usually belongs to several. {@code Sides} is the first three: it can be
 * asked for per document, per request and it can be reported on the job already put together. What
 * does **not** exist is an attribute that is both a request one and a service one: the first is
 * chosen by whoever prints, the second is reported by the printer.
 *
 * <p>This is the smallest scope: it holds for **one** piece to print. In a job of several documents
 * each one may bring its own --one in A4 and another in legal within the same job--, which is
 * exactly what a request attribute cannot express.
 */
public interface DocAttribute extends Attribute {
}
