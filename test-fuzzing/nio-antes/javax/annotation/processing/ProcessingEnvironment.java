package javax.annotation.processing;

import javax.lang.model.SourceVersion;

import java.util.Locale;
import java.util.Map;

// The only channel a processor talks to the tool running it through (JSR 269). `Processor.init(env)`
// hands it over exactly once, and every service comes out of it: the `Filer` to generate, the
// `Messager` to report, the options and the language.
//
// `getElementUtils()` and `getTypeUtils()` used to be missing here, and not by decision: they return
// `javax.lang.model.util.Elements` and `javax.lang.model.util.Types`, and that package did not exist
// in KajiLibrary. A method whose return type has to be substituted for another is a different method
// with the right name put on top of it, so they stayed undeclared. The package exists now and so do
// they.
//
// This project's implementor is `ProcessingEnvironmentImpl`.
public interface ProcessingEnvironment {

    /**
     * The `-Akey=value` options the tool received. An option with no `=` maps to `null`, which is not
     * the same as absent: the difference between "-Adebug" and not passing it.
     */
    Map<String, String> getOptions();

    /** Where to report. */
    Messager getMessager();

    /** Where to generate. */
    Filer getFiler();

    /** The language version of this run's sources. */
    SourceVersion getSourceVersion();

    /** The language the messages had best be written in, or `null` if there is none. */
    Locale getLocale();

    /**
     * Whether the run has preview features enabled.
     *
     * <p>`default` and not abstract in the contract: it was added after implementations existed, and
     * "no" is the correct conservative answer for anyone who does not know.
     */
    default boolean isPreviewEnabled() {
        return false;
    }

    /**
     * The utilities for querying **elements**.
     *
     * <p>See the note at the top of the file for why this was out until now.
     *
     * <p>What the compiler hands over here is its own business; the interface only says that it hands
     * it over.
     */
    javax.lang.model.util.Elements getElementUtils();

    /** The utilities for querying **types**. The same case as {@link #getElementUtils()}. */
    javax.lang.model.util.Types getTypeUtils();
}
