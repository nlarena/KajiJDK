package jdk.javadoc.doclet;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

/**
 * The plug-in that generates the HTML of javadoc, and which is a plug-in like any other.
 *
 * <h2>Why this class is public</h2>
 *
 * <p>So that it can be extended. Whoever wants the usual HTML plus something of their own does not
 * have to reimplement anything: they inherit from here, delegate to {@code super} what does not
 * change and add their options to the ones {@link #getSupportedOptions} returns. {@link #getLocale}
 * and {@link #getReporter} exist precisely for that -- they are what {@link #init} saved, exposed
 * for the subclass.
 *
 * <p>That the default generator is an ordinary plug-in, with no privileged access, is what makes
 * the separation credible: if it needed something the interface does not give, the interface would
 * be wrong.
 *
 * <h2>State in this VM</h2>
 *
 * <p>The structure is complete: {@link #init} saves what it receives, the accessors return it,
 * {@link #getName} and {@link #getSupportedSourceVersion} answer what corresponds. What there is
 * not is the <strong>HTML generator</strong>, which in the JDK is dozens of internal classes and is
 * not API -- it cannot be written by looking at the signature.
 *
 * <p>That is why {@link #run} reports the problem through the {@link Reporter} it was given and
 * returns {@code false}, which is the way the contract has of saying that it could not be done. It
 * does not throw an exception: a subclass that inherits from here and does its own work before
 * calling {@code super.run} should not lose what it has already done.
 *
 * <p>{@link #getSupportedOptions} returns the empty set, and that is also the truth: there is no
 * generator, so there are no options that configure it. Announcing {@code -d} or
 * {@code -windowtitle} would be promising that they do something.
 *
 * @since 9
 */
public class StandardDoclet implements Doclet {

    private Locale locale;
    private Reporter reporter;

    /** For javadoc, which instantiates it by reflection, and for the subclasses. */
    public StandardDoclet() {
    }

    /** {@inheritDoc} */
    public void init(final Locale locale, final Reporter reporter) {
        this.locale = locale;
        this.reporter = reporter;
    }

    /** {@inheritDoc} */
    public String getName() {
        return "Standard";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Empty: with no HTML generator there are no options that configure it.
     */
    public Set<? extends Option> getSupportedOptions() {
        // The <Option> witness is the way round #502: with `--emit`, a `Collections.emptySet()`
        // inferred towards a destination with a bounded wildcard is rejected. Take it out when it
        // closes.
        return Collections.<Option>emptySet();
    }

    /** {@inheritDoc} */
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    /**
     * {@inheritDoc}
     *
     * <p>It reports that the HTML generator is not there and returns {@code false}.
     */
    public boolean run(final DocletEnvironment environment) {
        if (reporter != null) {
            reporter.print(Diagnostic.Kind.ERROR,
                    "the HTML generator of StandardDoclet is not implemented in this "
                    + "library; a plug-in of one's own that inherits from this class has "
                    + "to generate its output without calling super.run");
        }
        return false;
    }

    /**
     * The language {@link #init} received, for the subclasses.
     *
     * @return the language, or {@code null} if none was given or {@code init} has not run yet
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * The destination of diagnostics {@link #init} received, for the subclasses.
     *
     * @return the reporter, or {@code null} if {@code init} has not run yet
     */
    public Reporter getReporter() {
        return reporter;
    }
}
