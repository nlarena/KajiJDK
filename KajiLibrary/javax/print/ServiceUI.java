package javax.print;

import java.awt.GraphicsConfiguration;
import java.awt.HeadlessException;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * KajiLibrary's javax.print.ServiceUI -- the print dialog.
 *
 * <p>A class with a single static method. It shows the box where the user chooses printer and
 * options, and returns the one chosen or null if they cancelled.
 *
 * <h2>The set of attributes is both input and output</h2>
 *
 * <p>It is the only thing to know about this method. The {@link PrintRequestAttributeSet} passed is
 * used to <b>fill in</b> the dialog, and then <b>modified in place</b> with what the user chose.
 * Passing a shared set, or reusing the same one between two dialogs, produces surprises.
 *
 * <p>Returning null and having modified the set is not contradictory: if the user cancels, the
 * documentation does not promise the set stays intact.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This VM is always headless: {@link java.awt.GraphicsEnvironment#isHeadless()} returns true, so
 * it is always in the case the JDK calls headless. (The note said this library has no Swing and no
 * windows; the Swing classes are here, but there is no screen.) There the JDK itself throws {@link
 * HeadlessException} <b>before</b> looking at the arguments --checked against JDK 25 with {@code
 * -Djava.awt.headless=true}--, so that is exactly what we do: the validation of {@code services}
 * and {@code attributes} never gets to run, just as there.
 */
public class ServiceUI {

    /** Public because the JDK left it public; the class has no state. */
    public ServiceUI() {
    }

    /**
     * Shows the dialog and returns the chosen printer, or null if it was cancelled.
     *
     * <p>See the class note: {@code attributes} is modified in place.
     *
     * @param gc on which screen, or null for the main one
     * @param x position of the corner
     * @param y likewise
     * @param services among which to choose; it cannot be null or empty
     * @param defaultService which one comes selected, or null
     * @param flavor the format that is going to be printed, or null
     * @param attributes comes in with what was asked for and goes out with what was chosen
     * @throws IllegalArgumentException if {@code services} is null or empty, or if
     *     {@code defaultService} is not among them -- in the JDK with a screen; here the
     *     {@code HeadlessException} wins
     * @throws HeadlessException always: there is no screen. See the class note
     */
    public static PrintService printDialog(GraphicsConfiguration gc, int x, int y,
                                           PrintService[] services, PrintService defaultService,
                                           DocFlavor flavor, PrintRequestAttributeSet attributes)
        throws HeadlessException {
        throw new HeadlessException();
    }
}
