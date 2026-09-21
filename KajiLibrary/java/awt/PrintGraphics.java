package java.awt;

/**
 * The mark a {@link Graphics} carries when it is drawing **onto a print**.
 *
 * <p>It is how a component finds out it is being printed rather than painted on screen, without
 * looking at the concrete class: if the `Graphics` it received is a `PrintGraphics`, it goes to
 * paper. It serves to avoid drawing what makes no sense printed —a cursor, a selection highlight—.
 */
public interface PrintGraphics {

    /** The print job it belongs to. */
    PrintJob getPrintJob();
}
