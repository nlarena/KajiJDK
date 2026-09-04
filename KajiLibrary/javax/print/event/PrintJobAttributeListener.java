package javax.print.event;

/**
 * KajiLibrary's javax.print.event.PrintJobAttributeListener -- escucha cambios de atributos de un
 * trabajo.
 *
 * <p>Se registra con {@code DocPrintJob.addPrintJobAttributeListener}, que ademas recibe el conjunto
 * de atributos que interesan. Sin ese filtro un trabajo largo genera un evento por cada cambio de
 * cualquier atributo.
 */
public interface PrintJobAttributeListener {

    /** Cambio algo de lo que se pidio escuchar. */
    void attributeUpdate(PrintJobAttributeEvent pjae);
}
