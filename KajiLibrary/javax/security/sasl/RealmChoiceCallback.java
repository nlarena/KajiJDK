package javax.security.sasl;

import javax.security.auth.callback.ChoiceCallback;

/**
 * KajiLibrary's javax.security.sasl.RealmChoiceCallback -- elegir entre varios dominios.
 *
 * <p>Como {@link RealmCallback} pero cuando el servidor ofrece una <b>lista</b> en vez de esperar que
 * el cliente sepa el nombre. Es la forma comoda: quien se autentica elige de lo que hay en vez de
 * escribir un identificador que tiene que acertar exacto.
 *
 * <p>Hereda de {@link ChoiceCallback} y, como aquel, permite elegir varios si se lo construye asi.
 * Para dominios eso casi nunca tiene sentido --se entra en uno-- pero el tipo lo permite porque la
 * clase base lo permite.
 */
public class RealmChoiceCallback extends ChoiceCallback {

    private static final long serialVersionUID = -8588141348846281332L;

    /**
     * @param choices los dominios que el servidor ofrece
     * @param defaultChoice el indice del que viene marcado
     * @param multiple si se puede elegir mas de uno
     */
    public RealmChoiceCallback(String prompt, String[] choices, int defaultChoice,
                               boolean multiple) {
        super(prompt, choices, defaultChoice, multiple);
    }
}
