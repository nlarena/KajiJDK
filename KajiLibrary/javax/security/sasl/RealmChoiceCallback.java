package javax.security.sasl;

import javax.security.auth.callback.ChoiceCallback;

/**
 * KajiLibrary's javax.security.sasl.RealmChoiceCallback -- choosing among several realms.
 *
 * <p>Like {@link RealmCallback} but when the server offers a <b>list</b> instead of expecting the
 * client to know the name. It is the convenient form: whoever authenticates chooses from what there
 * is instead of typing an identifier that has to be exactly right.
 *
 * <p>It inherits from {@link ChoiceCallback} and, like it, allows choosing several if it is built
 * that way. For realms that almost never makes sense --one logs into one-- but the type allows it
 * because the base class allows it.
 */
public class RealmChoiceCallback extends ChoiceCallback {

    private static final long serialVersionUID = -8588141348846281332L;

    /**
     * @param choices the realms the server offers
     * @param defaultChoice the index of the one marked by default
     * @param multiple whether more than one can be chosen
     */
    public RealmChoiceCallback(String prompt, String[] choices, int defaultChoice,
                               boolean multiple) {
        super(prompt, choices, defaultChoice, multiple);
    }
}
