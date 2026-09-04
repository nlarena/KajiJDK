package jdk.javadoc.doclet;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

/**
 * El complemento que genera el HTML de javadoc, y que es un complemento como cualquier otro.
 *
 * <h2>Por que esta clase es publica</h2>
 *
 * <p>Para poder extenderla. Quien quiera el HTML de siempre mas una cosa propia no tiene que
 * reimplementar nada: hereda de aca, delega en {@code super} lo que no cambia y agrega sus opciones
 * a las que {@link #getSupportedOptions} devuelve. {@link #getLocale} y {@link #getReporter} existen
 * justamente para eso — son lo que {@link #init} guardo, expuesto para la subclase.
 *
 * <p>Que el generador por omision sea un complemento normal, sin acceso privilegiado, es lo que
 * hace creible la separacion: si necesitara algo que la interfaz no da, la interfaz estaria mal.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>La estructura esta completa: {@link #init} guarda lo que recibe, los accesores lo devuelven,
 * {@link #getName} y {@link #getSupportedSourceVersion} contestan lo que corresponde. Lo que no
 * hay es el <strong>generador de HTML</strong>, que en el JDK son decenas de clases internas y no
 * es API — no se puede escribir mirando la firma.
 *
 * <p>Por eso {@link #run} informa el problema por el {@link Reporter} que le dieron y devuelve
 * {@code false}, que es la forma que el contrato tiene para decir que no se pudo. No tira una
 * excepcion: una subclase que herede de aca y haga su propio trabajo antes de llamar a
 * {@code super.run} no deberia perder lo que ya hizo.
 *
 * <p>{@link #getSupportedOptions} devuelve el conjunto vacio, y eso tambien es la verdad: no hay
 * generador, asi que no hay opciones que lo configuren. Anunciar {@code -d} o {@code -windowtitle}
 * seria prometer que hacen algo.
 *
 * @since 9
 */
public class StandardDoclet implements Doclet {

    private Locale locale;
    private Reporter reporter;

    /** Para javadoc, que lo instancia por reflexion, y para las subclases. */
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
     * <p>Vacio: sin generador de HTML no hay opciones que lo configuren.
     */
    public Set<? extends Option> getSupportedOptions() {
        // El testigo <Option> es el rodeo de #502: con `--emit`, un `Collections.emptySet()`
        // inferido hacia un destino con comodin acotado se rechaza. Sacarlo cuando se cierre.
        return Collections.<Option>emptySet();
    }

    /** {@inheritDoc} */
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Informa que el generador de HTML no esta y devuelve {@code false}.
     */
    public boolean run(final DocletEnvironment environment) {
        if (reporter != null) {
            reporter.print(Diagnostic.Kind.ERROR,
                    "el generador de HTML de StandardDoclet no esta implementado en esta "
                    + "biblioteca; un complemento propio que herede de esta clase tiene que "
                    + "generar su salida sin llamar a super.run");
        }
        return false;
    }

    /**
     * El idioma que {@link #init} recibio, para las subclases.
     *
     * @return el idioma, o {@code null} si no se dio ninguno o {@code init} no corrio todavia
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * El destino de diagnosticos que {@link #init} recibio, para las subclases.
     *
     * @return el reporter, o {@code null} si {@code init} no corrio todavia
     */
    public Reporter getReporter() {
        return reporter;
    }
}
