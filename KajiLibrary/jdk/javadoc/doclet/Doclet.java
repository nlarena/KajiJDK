package jdk.javadoc.doclet;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;

/**
 * What javadoc runs: it receives a model of the already analysed code and produces whatever it
 * wants.
 *
 * <h2>Why the documentation is a plug-in and not part of the tool</h2>
 *
 * <p>Because analysing the code and generating HTML are two jobs that do not have to go together.
 * The first one is expensive and difficult --one has to compile for real in order to know what type
 * each thing has-- and the second one is a decision of format. By separating them, anybody can make
 * use of the analysis to generate something else: an index, a JSON, a comparison of API between two
 * versions.
 *
 * <p>The HTML javadoc generates by default is, in this architecture, one more plug-in
 * ({@link StandardDoclet}) and not a privilege of the tool.
 *
 * <h2>The order of the calls</h2>
 *
 * <p>First {@link #init}, with the language and where to report. Then
 * {@link #getSupportedOptions} and {@link #getSupportedSourceVersion}, which javadoc consults
 * <strong>before</strong> processing the command line --it has to know which options to accept--.
 * Only then {@link #run}, a single time, with the whole model.
 *
 * @since 9
 */
public interface Doclet {

    /**
     * The first notice: with which language and where to report.
     *
     * @param locale the language for the messages, or {@code null} if there is no preference
     * @param reporter where to emit diagnostics
     */
    void init(Locale locale, Reporter reporter);

    /**
     * The name for the messages of the tool.
     *
     * @return the name
     */
    String getName();

    /**
     * The command line options this plug-in understands.
     *
     * <p>It is consulted before processing the arguments: javadoc cannot decide whether {@code
     * -foo} is an error without asking first.
     *
     * @return the options, possibly empty
     */
    Set<? extends Option> getSupportedOptions();

    /**
     * The version of the language this plug-in supports.
     *
     * @return the version
     */
    SourceVersion getSupportedSourceVersion();

    /**
     * It does the work, a single time, with the complete model.
     *
     * @param environment the model of the analysed code
     * @return whether it ended well
     */
    boolean run(DocletEnvironment environment);

    /**
     * A command line option the plug-in adds.
     *
     * <p>Each option describes itself --how many arguments it takes, what it is called, what it
     * does-- and it also knows how to process itself. It is what allows javadoc to validate options
     * it does not know and show them in the help without knowing anything about them.
     */
    interface Option {

        /**
         * How many arguments it takes after the name.
         *
         * @return the number, zero if it is a flag
         */
        int getArgumentCount();

        /**
         * What it does, for the help.
         *
         * @return the description
         */
        String getDescription();

        /**
         * How visible it is in the help.
         *
         * @return the kind of option
         */
        Kind getKind();

        /**
         * Every way of writing it, the preferred one first.
         *
         * <p>It is a list and not a name because one same option usually has a long form and a
         * short one, and because javadoc needs to recognise them all.
         *
         * @return the names
         */
        List<String> getNames();

        /**
         * How the arguments are written in the help, for example {@code "<directory>"}.
         *
         * @return the template of arguments
         */
        String getParameters();

        /**
         * It processes one appearance of the option.
         *
         * @param option the name as it appeared
         * @param arguments the arguments, as many as {@link #getArgumentCount} said
         * @return whether the option was accepted
         */
        boolean process(String option, List<String> arguments);

        /** How visible an option is in the help. */
        enum Kind {
            /** It is shown only with the extended help. */
            EXTENDED,
            /** It is shown in the common help. */
            STANDARD,
            /** It is not shown. */
            OTHER
        }
    }
}
