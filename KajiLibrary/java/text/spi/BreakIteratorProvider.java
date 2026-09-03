package java.text.spi;

import java.text.BreakIterator;
import java.util.Locale;
import java.util.spi.LocaleServiceProvider;

/**
 * KajiLibrary's java.text.spi.BreakIteratorProvider -- donde se puede cortar un texto.
 *
 * <p>Los cuatro metodos son cuatro preguntas distintas, y la que casi siempre se necesita es la que
 * menos se usa:
 *
 * <ul>
 *   <li><b>character</b> -- limites de <b>caracter percibido</b>, que no es lo mismo que un
 *       {@code char} ni que un punto de codigo: una vocal con tilde combinante, o un emoji con
 *       modificador de tono de piel, son varios puntos de codigo y un solo caracter para quien lee.
 *       Es el que hay que usar para mover un cursor o cortar una cadena sin partir un simbolo.
 *   <li><b>word</b> -- limites de palabra. En japones o tailandes no hay espacios, asi que esto es
 *       de verdad dependiente del idioma y no se puede aproximar con un {@code split}.
 *   <li><b>line</b> -- donde se <b>permite</b> cortar una linea. No corta: dice donde se podria.
 *   <li><b>sentence</b> -- limites de oracion, que no es "cortar en el punto": un punto en una
 *       abreviatura no termina nada.
 * </ul>
 *
 * <p>Un proveedor que devuelva null para un local que declaro soportar rompe el contrato; para los
 * que no soporta, el runtime ni siquiera lo llama.
 */
public abstract class BreakIteratorProvider extends LocaleServiceProvider {

    protected BreakIteratorProvider() {
    }

    /** Limites de palabra. Ver la nota de la clase: en varios idiomas no hay espacios. */
    public abstract BreakIterator getWordInstance(Locale locale);

    /** Donde se <b>permite</b> cortar una linea. No corta por si mismo. */
    public abstract BreakIterator getLineInstance(Locale locale);

    /** Limites de caracter percibido, que no es un {@code char}. Ver la nota de la clase. */
    public abstract BreakIterator getCharacterInstance(Locale locale);

    /** Limites de oracion, teniendo en cuenta las abreviaturas. */
    public abstract BreakIterator getSentenceInstance(Locale locale);
}
