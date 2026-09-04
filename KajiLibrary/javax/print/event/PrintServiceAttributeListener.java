package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintServiceAttributeListener -- escucha cambios de la impresora.
 *
 * <p>Se registra con {@code PrintService.addPrintServiceAttributeListener}. A diferencia del de
 * trabajos, este no lleva filtro: llegan todos.
 */
public interface PrintServiceAttributeListener {

    /** Cambio algo de la impresora. */
    void attributeUpdate(PrintServiceAttributeEvent psae);
}
