package javax.imageio;

/**
 * KajiLibrary's javax.imageio.IIOParamController -- asks the user to fill in the parameters.
 *
 * <p>A single method. It is attached to an {@link IIOParam} and, when someone calls
 * {@code activateController()}, this object shows whatever it likes --a dialog, a form-- and
 * <b>modifies the parameter itself</b> with what the user picks.
 *
 * <p>Returning false means the user cancelled, and then the parameter should be left <b>as it
 * was</b>. The JDK's contract only says to return false on cancel; leaving the parameter untouched
 * is the sensible reading, because a controller that modifies and then returns false leaves the
 * parameter half changed.
 *
 * <p>The interface does not mention a graphical interface anywhere, and that is on purpose: a
 * controller can read from a configuration file or from the command line just as well.
 */
public interface IIOParamController {

    /**
     * Fills in that parameter.
     *
     * @return whether the user accepted; false should leave the parameter untouched
     */
    boolean activate(IIOParam param);
}
