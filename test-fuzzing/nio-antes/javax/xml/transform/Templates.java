package javax.xml.transform;

import java.util.Properties;

/**
 * KajiLibrary's javax.xml.transform.Templates -- an already compiled stylesheet.
 *
 * <p>It is the answer to a very concrete problem: compiling a stylesheet is expensive --it has to
 * be parsed, its `import`s resolved, the template tree built, the XPath patterns compiled-- and a
 * {@link Transformer} is **not reusable in parallel**, because it has state (the parameters, the
 * output properties). Without this interface one would have to choose between paying for
 * compilation on every transformation or sharing an object that cannot be shared.
 *
 * <p>`Templates` splits that in two: the expensive result of compiling, **immutable and
 * thread-safe**, and the cheap transformers that come out of it. The usage pattern is to compile
 * once at startup and ask for one {@link Transformer} per job.
 *
 * <p>Immutability is the reason {@link #getOutputProperties} returns a **copy**: if it handed out
 * the internal table, anybody who modified it would be changing the stylesheet for all the threads
 * sharing it.
 */
public interface Templates {

    /**
     * A new transformer, ready to use, with this stylesheet's properties.
     *
     * <p>The object it returns has a single owner: it is not shared between threads. What is shared
     * is this `Templates`.
     *
     * @return a freshly made transformer
     * @throws TransformerConfigurationException if it cannot be built
     */
    Transformer newTransformer() throws TransformerConfigurationException;

    /**
     * A copy of the output properties the stylesheet declares.
     *
     * <p>A copy, not a view: see the header note. The ones the stylesheet does not declare appear
     * as the output method's default values in the table's {@link Properties#defaults}, not as
     * entries of their own -- that is how "the stylesheet said it" is told apart from "the spec put
     * it", which is just what one needs to know to decide whether to override it.
     *
     * @return the properties, with the defaults underneath
     */
    Properties getOutputProperties();
}
