import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.print.attribute.Attribute;
import javax.print.attribute.EnumSyntax;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.*;

/**
 * javax.print.attribute.standard: los 72 atributos de impresion.
 *
 * <p>Casi todo este paquete son valores fijos -- nombres, numeros de enumeracion, tablas de tamaños
 * de papel -- y por eso la prueba es sobre todo una lista de igualdades. No es trivial igual: los
 * numeros no son consecutivos (Finishings salta de 9 a 20, JobState arranca en 0 y sigue en 3), los
 * textos estan especificados al caracter, y equivocarse en uno es un atributo que se serializa mal
 * contra una impresora de verdad.
 *
 * <p>Las expectativas salieron de preguntarle al JDK 25, no de leer el RFC 2911. Las cuatro que no
 * son obvias y por las que vale la pena tener la prueba: la categoria de <b>cualquier</b> medio es
 * Media y no la clase concreta, PageRanges normaliza fusionando rangos pegados y no solo los
 * solapados, los tamaños se guardan en micrometros y el toString los muestra crudos, y los conjuntos
 * de razones tiran ClassCastException -- no una excepcion propia -- ante un elemento del tipo
 * equivocado.
 */
public class PrintStdTest {

    /** Comprueba el numero y el texto de un valor de enumeracion de una sola vez. */
    private static boolean isEnum(EnumSyntax v, int value, String text) {
        return v.getValue() == value && v.toString().equals(text);
    }

    /** Comprueba la categoria y el nombre de un atributo. */
    private static boolean labels(Attribute a, Class<?> categoria, String name) {
        return a.getCategory() == categoria && a.getName().equals(name);
    }

    public static int run() {
        int i = 0;

        // ======================================================================================
        // los numeros y los textos de las enumeraciones
        // ======================================================================================
        if (!isEnum(Chromaticity.MONOCHROME, 0, "monochrome")) { return i; } i++;
        if (!isEnum(Chromaticity.COLOR, 1, "color")) { return i; } i++;
        if (!isEnum(ColorSupported.NOT_SUPPORTED, 0, "not-supported")) { return i; } i++;
        if (!isEnum(ColorSupported.SUPPORTED, 1, "supported")) { return i; } i++;
        if (!isEnum(Compression.NONE, 0, "none")) { return i; } i++;
        if (!isEnum(Compression.DEFLATE, 1, "deflate")) { return i; } i++;
        if (!isEnum(Compression.GZIP, 2, "gzip")) { return i; } i++;
        if (!isEnum(Compression.COMPRESS, 3, "compress")) { return i; } i++;
        // Fidelity dice "true"/"false" y no "fidelity-true": el texto no sigue al nombre del campo.
        if (!isEnum(Fidelity.FIDELITY_TRUE, 0, "true")) { return i; } i++;
        if (!isEnum(Fidelity.FIDELITY_FALSE, 1, "false")) { return i; } i++;

        // Finishings: los numeros NO son consecutivos. Salta de 9 a 20.
        if (!isEnum(Finishings.NONE, 3, "none")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE, 4, "staple")) { return i; } i++;
        if (!isEnum(Finishings.COVER, 6, "cover")) { return i; } i++;
        if (!isEnum(Finishings.BIND, 7, "bind")) { return i; } i++;
        if (!isEnum(Finishings.SADDLE_STITCH, 8, "saddle-stitch")) { return i; } i++;
        if (!isEnum(Finishings.EDGE_STITCH, 9, "edge-stitch")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_TOP_LEFT, 20, "staple-top-left")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_BOTTOM_LEFT, 21, "staple-bottom-left")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_TOP_RIGHT, 22, "staple-top-right")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_BOTTOM_RIGHT, 23, "staple-bottom-right")) { return i; } i++;
        if (!isEnum(Finishings.EDGE_STITCH_LEFT, 24, "edge-stitch-left")) { return i; } i++;
        if (!isEnum(Finishings.EDGE_STITCH_TOP, 25, "edge-stitch-top")) { return i; } i++;
        if (!isEnum(Finishings.EDGE_STITCH_RIGHT, 26, "edge-stitch-right")) { return i; } i++;
        if (!isEnum(Finishings.EDGE_STITCH_BOTTOM, 27, "edge-stitch-bottom")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_DUAL_LEFT, 28, "staple-dual-left")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_DUAL_TOP, 29, "staple-dual-top")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_DUAL_RIGHT, 30, "staple-dual-right")) { return i; } i++;
        if (!isEnum(Finishings.STAPLE_DUAL_BOTTOM, 31, "staple-dual-bottom")) { return i; } i++;

        if (!isEnum(JobSheets.NONE, 0, "none")) { return i; } i++;
        if (!isEnum(JobSheets.STANDARD, 1, "standard")) { return i; } i++;

        // JobState: 0 y despues 3 en adelante. El 1 y el 2 no existen.
        if (!isEnum(JobState.UNKNOWN, 0, "unknown")) { return i; } i++;
        if (!isEnum(JobState.PENDING, 3, "pending")) { return i; } i++;
        if (!isEnum(JobState.PENDING_HELD, 4, "pending-held")) { return i; } i++;
        if (!isEnum(JobState.PROCESSING, 5, "processing")) { return i; } i++;
        if (!isEnum(JobState.PROCESSING_STOPPED, 6, "processing-stopped")) { return i; } i++;
        if (!isEnum(JobState.CANCELED, 7, "canceled")) { return i; } i++;
        if (!isEnum(JobState.ABORTED, 8, "aborted")) { return i; } i++;
        if (!isEnum(JobState.COMPLETED, 9, "completed")) { return i; } i++;

        if (!isEnum(MultipleDocumentHandling.SINGLE_DOCUMENT, 0, "single-document")) { return i; } i++;
        if (!isEnum(MultipleDocumentHandling.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES, 1,
                "separate-documents-uncollated-copies")) { return i; } i++;
        if (!isEnum(MultipleDocumentHandling.SEPARATE_DOCUMENTS_COLLATED_COPIES, 2,
                "separate-documents-collated-copies")) { return i; } i++;
        if (!isEnum(MultipleDocumentHandling.SINGLE_DOCUMENT_NEW_SHEET, 3,
                "single-document-new-sheet")) { return i; } i++;

        if (!isEnum(OrientationRequested.PORTRAIT, 3, "portrait")) { return i; } i++;
        if (!isEnum(OrientationRequested.LANDSCAPE, 4, "landscape")) { return i; } i++;
        if (!isEnum(OrientationRequested.REVERSE_LANDSCAPE, 5, "reverse-landscape")) { return i; } i++;
        if (!isEnum(OrientationRequested.REVERSE_PORTRAIT, 6, "reverse-portrait")) { return i; } i++;

        if (!isEnum(PDLOverrideSupported.NOT_ATTEMPTED, 0, "not-attempted")) { return i; } i++;
        if (!isEnum(PDLOverrideSupported.ATTEMPTED, 1, "attempted")) { return i; } i++;

        // PresentationDirection: los numeros NO siguen el orden en el que se declaran los campos.
        if (!isEnum(PresentationDirection.TOBOTTOM_TORIGHT, 0, "tobottom-toright")) { return i; } i++;
        if (!isEnum(PresentationDirection.TOBOTTOM_TOLEFT, 1, "tobottom-toleft")) { return i; } i++;
        if (!isEnum(PresentationDirection.TOTOP_TORIGHT, 2, "totop-toright")) { return i; } i++;
        if (!isEnum(PresentationDirection.TOTOP_TOLEFT, 3, "totop-toleft")) { return i; } i++;
        if (!isEnum(PresentationDirection.TORIGHT_TOBOTTOM, 4, "toright-tobottom")) { return i; } i++;
        if (!isEnum(PresentationDirection.TORIGHT_TOTOP, 5, "toright-totop")) { return i; } i++;
        if (!isEnum(PresentationDirection.TOLEFT_TOBOTTOM, 6, "toleft-tobottom")) { return i; } i++;
        if (!isEnum(PresentationDirection.TOLEFT_TOTOP, 7, "toleft-totop")) { return i; } i++;

        if (!isEnum(PrintQuality.DRAFT, 3, "draft")) { return i; } i++;
        if (!isEnum(PrintQuality.NORMAL, 4, "normal")) { return i; } i++;
        if (!isEnum(PrintQuality.HIGH, 5, "high")) { return i; } i++;

        if (!isEnum(PrinterIsAcceptingJobs.NOT_ACCEPTING_JOBS, 0, "not-accepting-jobs")) { return i; } i++;
        if (!isEnum(PrinterIsAcceptingJobs.ACCEPTING_JOBS, 1, "accepting-jobs")) { return i; } i++;
        if (!isEnum(PrinterState.UNKNOWN, 0, "unknown")) { return i; } i++;
        if (!isEnum(PrinterState.IDLE, 3, "idle")) { return i; } i++;
        if (!isEnum(PrinterState.PROCESSING, 4, "processing")) { return i; } i++;
        if (!isEnum(PrinterState.STOPPED, 5, "stopped")) { return i; } i++;

        // ReferenceUriSchemesSupported: FILE es 7, no 3. Hay huecos por esquemas que se sacaron.
        if (!isEnum(ReferenceUriSchemesSupported.FTP, 0, "ftp")) { return i; } i++;
        if (!isEnum(ReferenceUriSchemesSupported.HTTP, 1, "http")) { return i; } i++;
        if (!isEnum(ReferenceUriSchemesSupported.HTTPS, 2, "https")) { return i; } i++;
        if (!isEnum(ReferenceUriSchemesSupported.FILE, 7, "file")) { return i; } i++;

        if (!isEnum(Severity.REPORT, 0, "report")) { return i; } i++;
        if (!isEnum(Severity.WARNING, 1, "warning")) { return i; } i++;
        if (!isEnum(Severity.ERROR, 2, "error")) { return i; } i++;
        if (!isEnum(SheetCollate.UNCOLLATED, 0, "uncollated")) { return i; } i++;
        if (!isEnum(SheetCollate.COLLATED, 1, "collated")) { return i; } i++;

        if (!isEnum(Sides.ONE_SIDED, 0, "one-sided")) { return i; } i++;
        if (!isEnum(Sides.TWO_SIDED_LONG_EDGE, 1, "two-sided-long-edge")) { return i; } i++;
        if (!isEnum(Sides.TWO_SIDED_SHORT_EDGE, 2, "two-sided-short-edge")) { return i; } i++;
        // DUPLEX y TUMBLE son alias: la MISMA instancia, no una copia con el mismo numero.
        if (Sides.DUPLEX != Sides.TWO_SIDED_LONG_EDGE) { return i; } i++;
        if (Sides.TUMBLE != Sides.TWO_SIDED_SHORT_EDGE) { return i; } i++;

        if (!isEnum(DialogTypeSelection.NATIVE, 0, "native")) { return i; } i++;
        if (!isEnum(DialogTypeSelection.COMMON, 1, "common")) { return i; } i++;

        if (!isEnum(JobStateReason.JOB_INCOMING, 0, "job-incoming")) { return i; } i++;
        if (!isEnum(JobStateReason.JOB_DATA_INSUFFICIENT, 1, "job-data-insufficient")) { return i; } i++;
        if (!isEnum(JobStateReason.DOCUMENT_ACCESS_ERROR, 2, "document-access-error")) { return i; } i++;
        if (!isEnum(JobStateReason.JOB_PRINTING, 13, "job-printing")) { return i; } i++;
        if (!isEnum(JobStateReason.QUEUED_IN_DEVICE, 28, "queued-in-device")) { return i; } i++;
        if (!isEnum(PrinterStateReason.OTHER, 0, "other")) { return i; } i++;
        if (!isEnum(PrinterStateReason.MEDIA_NEEDED, 1, "media-needed")) { return i; } i++;
        if (!isEnum(PrinterStateReason.PAUSED, 4, "paused")) { return i; } i++;
        if (!isEnum(PrinterStateReason.SHUTDOWN, 5, "shutdown")) { return i; } i++;

        if (!isEnum(MediaName.NA_LETTER_WHITE, 0, "na-letter-white")) { return i; } i++;
        if (!isEnum(MediaName.NA_LETTER_TRANSPARENT, 1, "na-letter-transparent")) { return i; } i++;
        if (!isEnum(MediaName.ISO_A4_WHITE, 2, "iso-a4-white")) { return i; } i++;
        if (!isEnum(MediaName.ISO_A4_TRANSPARENT, 3, "iso-a4-transparent")) { return i; } i++;
        if (!isEnum(MediaTray.TOP, 0, "top")) { return i; } i++;
        if (!isEnum(MediaTray.MIDDLE, 1, "middle")) { return i; } i++;
        if (!isEnum(MediaTray.BOTTOM, 2, "bottom")) { return i; } i++;
        if (!isEnum(MediaTray.ENVELOPE, 3, "envelope")) { return i; } i++;
        if (!isEnum(MediaTray.MANUAL, 4, "manual")) { return i; } i++;
        if (!isEnum(MediaTray.LARGE_CAPACITY, 5, "large-capacity")) { return i; } i++;
        if (!isEnum(MediaTray.MAIN, 6, "main")) { return i; } i++;
        if (!isEnum(MediaTray.SIDE, 7, "side")) { return i; } i++;
        if (!isEnum(MediaSizeName.ISO_A0, 0, "iso-a0")) { return i; } i++;
        if (!isEnum(MediaSizeName.ISO_A4, 4, "iso-a4")) { return i; } i++;
        if (!isEnum(MediaSizeName.JIS_B4, 26, "jis-b4")) { return i; } i++;
        if (!isEnum(MediaSizeName.NA_LETTER, 40, "na-letter")) { return i; } i++;
        if (!isEnum(MediaSizeName.NA_LEGAL, 41, "na-legal")) { return i; } i++;
        if (!isEnum(MediaSizeName.EXECUTIVE, 42, "executive")) { return i; } i++;

        // ======================================================================================
        // categoria y nombre
        // ======================================================================================
        if (!labels(Chromaticity.COLOR, Chromaticity.class, "chromaticity")) { return i; } i++;
        if (!labels(new Copies(3), Copies.class, "copies")) { return i; } i++;
        // El nombre NO siempre sale de la clase: Destination se llama "spool-data-destination".
        if (!labels(new Destination(java.net.URI.create("file:/x")), Destination.class,
                "spool-data-destination")) { return i; } i++;
        if (!labels(new DocumentName("d", Locale.US), DocumentName.class,
                "document-name")) { return i; } i++;
        if (!labels(Finishings.STAPLE, Finishings.class, "finishings")) { return i; } i++;
        if (!labels(new JobHoldUntil(new Date(0)), JobHoldUntil.class,
                "job-hold-until")) { return i; } i++;
        if (!labels(new JobImpressions(5), JobImpressions.class, "job-impressions")) { return i; } i++;
        if (!labels(JobState.PENDING, JobState.class, "job-state")) { return i; } i++;
        if (!labels(new JobName("n", null), JobName.class, "job-name")) { return i; } i++;
        if (!labels(new JobPriority(50), JobPriority.class, "job-priority")) { return i; } i++;
        // Esta es la que sorprende: la categoria de CUALQUIER medio es Media, la clase base, no la
        // concreta. Asi un MediaSizeName y un MediaTray compiten por la misma ranura del conjunto.
        if (!labels(MediaSizeName.ISO_A4, Media.class, "media")) { return i; } i++;
        if (!labels(MediaTray.TOP, Media.class, "media")) { return i; } i++;
        if (!labels(MediaName.ISO_A4_WHITE, Media.class, "media")) { return i; } i++;
        // MediaSize, en cambio, NO es un Media: es su propia categoria.
        if (!labels(MediaSize.ISO.A4, MediaSize.class, "media-size")) { return i; } i++;
        if (!labels(new MediaPrintableArea(0, 0, 10, 10, 1000), MediaPrintableArea.class,
                "media-printable-area")) { return i; } i++;
        if (!labels(MultipleDocumentHandling.SINGLE_DOCUMENT, MultipleDocumentHandling.class,
                "multiple-document-handling")) { return i; } i++;
        if (!labels(new NumberUp(2), NumberUp.class, "number-up")) { return i; } i++;
        if (!labels(OrientationRequested.PORTRAIT, OrientationRequested.class,
                "orientation-requested")) { return i; } i++;
        if (!labels(new PageRanges(1, 5), PageRanges.class, "page-ranges")) { return i; } i++;
        if (!labels(new PrinterName("p", null), PrinterName.class, "printer-name")) { return i; } i++;
        if (!labels(new PrinterResolution(300, 300, 100), PrinterResolution.class,
                "printer-resolution")) { return i; } i++;
        if (!labels(PrinterState.IDLE, PrinterState.class, "printer-state")) { return i; } i++;
        if (!labels(PrintQuality.HIGH, PrintQuality.class, "print-quality")) { return i; } i++;
        if (!labels(new QueuedJobCount(0), QueuedJobCount.class,
                "queued-job-count")) { return i; } i++;
        if (!labels(new RequestingUserName("u", null), RequestingUserName.class,
                "requesting-user-name")) { return i; } i++;
        if (!labels(Sides.DUPLEX, Sides.class, "sides")) { return i; } i++;
        if (!labels(new CopiesSupported(1, 9), CopiesSupported.class,
                "copies-supported")) { return i; } i++;
        if (!labels(new JobPrioritySupported(7), JobPrioritySupported.class,
                "job-priority-supported")) { return i; } i++;
        if (!labels(new NumberUpSupported(1, 4), NumberUpSupported.class,
                "number-up-supported")) { return i; } i++;
        if (!labels(new PagesPerMinute(10), PagesPerMinute.class,
                "pages-per-minute")) { return i; } i++;
        if (!labels(new PagesPerMinuteColor(10), PagesPerMinuteColor.class,
                "pages-per-minute-color")) { return i; } i++;
        if (!labels(new NumberOfDocuments(2), NumberOfDocuments.class,
                "number-of-documents")) { return i; } i++;
        if (!labels(new NumberOfInterveningJobs(3), NumberOfInterveningJobs.class,
                "number-of-intervening-jobs")) { return i; } i++;
        if (!labels(new PrinterInfo("i", null), PrinterInfo.class, "printer-info")) { return i; } i++;
        if (!labels(new PrinterLocation("l", null), PrinterLocation.class,
                "printer-location")) { return i; } i++;
        if (!labels(new PrinterMakeAndModel("m", null), PrinterMakeAndModel.class,
                "printer-make-and-model")) { return i; } i++;
        if (!labels(new PrinterMessageFromOperator("o", null), PrinterMessageFromOperator.class,
                "printer-message-from-operator")) { return i; } i++;
        if (!labels(new JobMessageFromOperator("j", null), JobMessageFromOperator.class,
                "job-message-from-operator")) { return i; } i++;
        if (!labels(new JobOriginatingUserName("jo", null), JobOriginatingUserName.class,
                "job-originating-user-name")) { return i; } i++;
        if (!labels(new OutputDeviceAssigned("od", null), OutputDeviceAssigned.class,
                "output-device-assigned")) { return i; } i++;
        if (!labels(new JobKOctets(1), JobKOctets.class, "job-k-octets")) { return i; } i++;
        if (!labels(new JobMediaSheets(1), JobMediaSheets.class,
                "job-media-sheets")) { return i; } i++;
        if (!labels(new JobKOctetsProcessed(1), JobKOctetsProcessed.class,
                "job-k-octets-processed")) { return i; } i++;
        if (!labels(new JobMediaSheetsCompleted(1), JobMediaSheetsCompleted.class,
                "job-media-sheets-completed")) { return i; } i++;
        if (!labels(new JobImpressionsCompleted(1), JobImpressionsCompleted.class,
                "job-impressions-completed")) { return i; } i++;
        if (!labels(new JobKOctetsSupported(1, 9), JobKOctetsSupported.class,
                "job-k-octets-supported")) { return i; } i++;
        if (!labels(new JobMediaSheetsSupported(1, 9), JobMediaSheetsSupported.class,
                "job-media-sheets-supported")) { return i; } i++;
        if (!labels(new JobImpressionsSupported(1, 9), JobImpressionsSupported.class,
                "job-impressions-supported")) { return i; } i++;
        if (!labels(new DateTimeAtCreation(new Date(0)), DateTimeAtCreation.class,
                "date-time-at-creation")) { return i; } i++;
        if (!labels(new DateTimeAtProcessing(new Date(0)), DateTimeAtProcessing.class,
                "date-time-at-processing")) { return i; } i++;
        if (!labels(new DateTimeAtCompleted(new Date(0)), DateTimeAtCompleted.class,
                "date-time-at-completed")) { return i; } i++;
        if (!labels(new PrinterURI(java.net.URI.create("ipp://x/y")), PrinterURI.class,
                "printer-uri")) { return i; } i++;
        if (!labels(new PrinterMoreInfo(java.net.URI.create("http://x/")), PrinterMoreInfo.class,
                "printer-more-info")) { return i; } i++;
        if (!labels(new PrinterMoreInfoManufacturer(java.net.URI.create("http://x/")),
                PrinterMoreInfoManufacturer.class,
                "printer-more-info-manufacturer")) { return i; } i++;
        if (!labels(Fidelity.FIDELITY_TRUE, Fidelity.class, "ipp-attribute-fidelity")) { return i; } i++;
        if (!labels(OutputBin.TOP, OutputBin.class, "output-bin")) { return i; } i++;
        if (!isEnum(OutputBin.TOP, 0, "top")) { return i; } i++;
        if (!labels(new DialogOwner(), DialogOwner.class, "dialog-owner")) { return i; } i++;
        // DialogOwner es el unico atributo del paquete que apunta a un objeto vivo de la interfaz
        // grafica. No se construye ninguna ventana aca a proposito: hacerlo necesita un escritorio,
        // y una prueba que anda o no segun donde corra no prueba nada. Lo que si se puede fijar es
        // que el duenio ausente sea null en los dos caminos que lo dejan ausente.
        if (new DialogOwner().getOwner() != null) { return i; } i++;
        // Construir con null vale y equivale al constructor sin argumentos: lo hace el JDK 25.
        if (new DialogOwner(null).getOwner() != null) { return i; } i++;
        if (!labels(new DialogOwner(null), DialogOwner.class, "dialog-owner")) { return i; } i++;
        if (!labels(DialogTypeSelection.NATIVE, DialogTypeSelection.class,
                "dialog-type-selection")) { return i; } i++;

        // ======================================================================================
        // PageRanges: la aritmetica
        // ======================================================================================
        if (!new PageRanges("1-3,5,7-8").toString().equals("1-3,5,7-8")) { return i; } i++;
        // Rangos solapados se funden.
        if (!new PageRanges("1-5,3-8").toString().equals("1-8")) { return i; } i++;
        // Desordenados se ordenan.
        if (!new PageRanges("7-8,1-3").toString().equals("1-3,7-8")) { return i; } i++;
        // Y los PEGADOS tambien se funden, aunque no se solapen: 3 y 4 son contiguos.
        if (!new PageRanges("1-3,4-5").toString().equals("1-5")) { return i; } i++;
        // Un rango de una pagina se escribe sin guion.
        if (!new PageRanges(4).toString().equals("4")) { return i; } i++;
        if (!new PageRanges(" 1 - 3 , 5 ").toString().equals("1-3,5")) { return i; } i++;
        if (!new PageRanges(2, 6).toString().equals("2-6")) { return i; } i++;
        if (!new PageRanges(3, 3).toString().equals("3")) { return i; } i++;

        PageRanges pr = new PageRanges("1-3,5,7-8");
        int[][] m = pr.getMembers();
        if (m.length != 3) { return i; } i++;
        if (m[0][0] != 1 || m[0][1] != 3) { return i; } i++;
        // Una pagina suelta se guarda igual como un rango de ella a ella.
        if (m[1][0] != 5 || m[1][1] != 5) { return i; } i++;
        if (m[2][0] != 7 || m[2][1] != 8) { return i; } i++;
        // getMembers devuelve una copia profunda: tocarla no cambia el atributo.
        m[0][0] = 99;
        if (pr.getMembers()[0][0] != 1) { return i; } i++;
        if (!pr.contains(2)) { return i; } i++;
        if (pr.contains(4)) { return i; } i++;
        if (!pr.contains(5)) { return i; } i++;
        if (!pr.contains(8)) { return i; } i++;
        if (pr.contains(9)) { return i; } i++;
        // next salta el hueco.
        if (pr.next(3) != 5) { return i; } i++;
        if (pr.next(5) != 7) { return i; } i++;
        // Despues de la ultima devuelve -1, no 0.
        if (pr.next(8) != -1) { return i; } i++;
        if (pr.next(0) != 1) { return i; } i++;
        // Dos textos distintos que designan el mismo conjunto son el mismo atributo.
        if (!pr.equals(new PageRanges("1-3, 5, 7-8"))) { return i; } i++;
        if (pr.hashCode() != new PageRanges("1-3, 5, 7-8").hashCode()) { return i; } i++;
        if (pr.equals(new PageRanges("1-3"))) { return i; } i++;
        if (pr.equals(null)) { return i; } i++;
        if (pr.equals("1-3,5,7-8")) { return i; } i++;

        boolean threw = false;
        try { new PageRanges(0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PageRanges(-1); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Un rango al reves es un rango vacio, y eso se rechaza.
        threw = false;
        try { new PageRanges(5, 3); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PageRanges(""); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PageRanges("a"); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PageRanges((String) null); } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new PageRanges(new int[0][]); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // MediaSize: la tabla de papeles, guardada en micrometros
        // ======================================================================================
        MediaSize a4 = MediaSize.ISO.A4;
        if (a4.getX(Size2DSyntax.MM) != 210.0f) { return i; } i++;
        if (a4.getY(Size2DSyntax.MM) != 297.0f) { return i; } i++;
        // Las unidades crudas son micrometros: MM vale 1000.
        if (Size2DSyntax.MM != 1000) { return i; } i++;
        if (a4.getX(1) != 210000.0f) { return i; } i++;
        if (a4.getY(1) != 297000.0f) { return i; } i++;
        // Y el toString muestra los numeros CRUDOS con la unidad "um", no los milimetros.
        if (!a4.toString().equals("210000x297000 um")) { return i; } i++;
        if (a4.getMediaSizeName() != MediaSizeName.ISO_A4) { return i; } i++;
        if (MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) != 8.5f) { return i; } i++;
        if (MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) != 11.0f) { return i; } i++;
        // La busqueda por nombre y la busqueda por medidas dan lo mismo.
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4) != a4) { return i; } i++;
        if (MediaSize.findMedia(210f, 297f, Size2DSyntax.MM) != MediaSizeName.ISO_A4) { return i; } i++;
        if (MediaSize.findMedia(8.5f, 11f, Size2DSyntax.INCH) != MediaSizeName.NA_LETTER) { return i; } i++;
        if (!Arrays.equals(a4.getSize(Size2DSyntax.MM), new float[] {210.0f, 297.0f})) { return i; } i++;
        if (!a4.toString(Size2DSyntax.MM, "mm").equals("210.0x297.0 mm")) { return i; } i++;
        if (!a4.equals(new MediaSize(210, 297, Size2DSyntax.MM))) { return i; } i++;
        // Un tamaño mas ancho que alto se rechaza: MediaSize obliga a que el papel venga en
        // vertical, y es la rotacion la que lo pone de lado. Un cero, en cambio, SI se acepta --
        // Size2DSyntax solo rechaza los negativos --, que es lo que uno no espera.
        threw = false;
        try { new MediaSize(300f, 10f, Size2DSyntax.MM); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new MediaSize(-1f, 10f, Size2DSyntax.MM); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        if (new MediaSize(0f, 10f, Size2DSyntax.MM).getX(Size2DSyntax.MM) != 0f) { return i; } i++;

        // ======================================================================================
        // MediaPrintableArea y PrinterResolution: las conversiones de unidad
        // ======================================================================================
        MediaPrintableArea mpa =
            new MediaPrintableArea(0.5f, 0.5f, 7.5f, 10f, MediaPrintableArea.INCH);
        if (mpa.getX(MediaPrintableArea.INCH) != 0.5f) { return i; } i++;
        if (mpa.getWidth(MediaPrintableArea.MM) != 190.5f) { return i; } i++;
        if (mpa.getHeight(MediaPrintableArea.MM) != 254.0f) { return i; } i++;
        float[] area = mpa.getPrintableArea(MediaPrintableArea.MM);
        if (area.length != 4 || area[0] != 12.7f || area[3] != 254.0f) { return i; } i++;
        if (!mpa.toString(MediaPrintableArea.MM, "mm")
                .equals("(12.7,12.7)->(190.5,254.0)mm")) { return i; } i++;
        // El toString sin argumentos ya viene en milimetros: es la excepcion a la regla de arriba.
        if (!mpa.toString().equals("(12.7,12.7)->(190.5,254.0)mm")) { return i; } i++;
        threw = false;
        try { new MediaPrintableArea(0f, 0f, 0f, 1f, MediaPrintableArea.INCH); }
        catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        PrinterResolution res = new PrinterResolution(300, 600, ResolutionSyntax.DPI);
        if (res.getCrossFeedResolution(ResolutionSyntax.DPI) != 300) { return i; } i++;
        if (res.getFeedResolution(ResolutionSyntax.DPI) != 600) { return i; } i++;
        int[] rr = res.getResolution(ResolutionSyntax.DPCM);
        // 300 dpi son 118 dpcm redondeando: la conversion es entera y pierde.
        if (rr[0] != 118 || rr[1] != 236) { return i; } i++;
        if (!res.toString().equals("30000x60000 dphi")) { return i; } i++;
        if (!res.toString(ResolutionSyntax.DPI, "dpi").equals("300x600 dpi")) { return i; } i++;
        if (!res.lessThanOrEquals(new PrinterResolution(600, 600, ResolutionSyntax.DPI))) { return i; } i++;
        if (res.lessThanOrEquals(new PrinterResolution(100, 100, ResolutionSyntax.DPI))) { return i; } i++;
        if (!res.equals(new PrinterResolution(300, 600, ResolutionSyntax.DPI))) { return i; } i++;
        if (res.equals(new PrinterResolution(600, 300, ResolutionSyntax.DPI))) { return i; } i++;

        // ======================================================================================
        // los conjuntos de razones
        // ======================================================================================
        JobStateReasons jsr = new JobStateReasons();
        jsr.add(JobStateReason.JOB_INCOMING);
        jsr.add(JobStateReason.JOB_PRINTING);
        if (jsr.size() != 2) { return i; } i++;
        if (!jsr.contains(JobStateReason.JOB_INCOMING)) { return i; } i++;
        if (jsr.getCategory() != JobStateReasons.class) { return i; } i++;
        if (!jsr.getName().equals("job-state-reasons")) { return i; } i++;
        // Un elemento del tipo equivocado: ClassCastException, no una excepcion propia. El conjunto
        // castea al agregar en vez de comprobar, y eso es observable.
        threw = false;
        try {
            Set raw = new JobStateReasons();
            raw.add("no soy una razon");
        } catch (ClassCastException e) {
            threw = true;
        }
        if (!threw) { return i; } i++;

        PrinterStateReasons psr = new PrinterStateReasons();
        psr.put(PrinterStateReason.PAUSED, Severity.WARNING);
        psr.put(PrinterStateReason.SHUTDOWN, Severity.ERROR);
        if (psr.get(PrinterStateReason.PAUSED) != Severity.WARNING) { return i; } i++;
        if (psr.getCategory() != PrinterStateReasons.class) { return i; } i++;
        if (!psr.getName().equals("printer-state-reasons")) { return i; } i++;
        // La vista por severidad es lo que hace util a este mapa: separa lo urgente de lo informativo.
        Set<PrinterStateReason> warn = psr.printerStateReasonSet(Severity.WARNING);
        if (warn.size() != 1 || !warn.contains(PrinterStateReason.PAUSED)) { return i; } i++;
        Set<PrinterStateReason> err = psr.printerStateReasonSet(Severity.ERROR);
        if (err.size() != 1 || !err.contains(PrinterStateReason.SHUTDOWN)) { return i; } i++;
        if (!psr.printerStateReasonSet(Severity.REPORT).isEmpty()) { return i; } i++;
        threw = false;
        try {
            Map raw = new PrinterStateReasons();
            raw.put(PrinterStateReason.PAUSED, "no soy una severidad");
        } catch (ClassCastException e) {
            threw = true;
        }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // los rangos que tienen que rechazar
        // ======================================================================================
        threw = false;
        try { new Copies(0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new JobPriority(0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new JobPriority(101); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new NumberUp(0); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        // Los contadores de trabajo empiezan en 0, no en 1: la diferencia importa.
        threw = false;
        try { new JobImpressions(-1); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        threw = false;
        try { new QueuedJobCount(-1); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;
        if (new JobImpressions(0).getValue() != 0) { return i; } i++;
        if (new QueuedJobCount(0).getValue() != 0) { return i; } i++;
        if (new JobPriority(1).getValue() != 1) { return i; } i++;
        if (new JobPriority(100).getValue() != 100) { return i; } i++;
        if (new Copies(1).getValue() != 1) { return i; } i++;
        threw = false;
        try { new CopiesSupported(5, 3); } catch (IllegalArgumentException e) { threw = true; }
        if (!threw) { return i; } i++;

        // ======================================================================================
        // texto, locale, fechas y URIs
        // ======================================================================================
        JobName jn = new JobName("mi trabajo", Locale.FRANCE);
        if (!jn.getValue().equals("mi trabajo")) { return i; } i++;
        if (!jn.getLocale().equals(Locale.FRANCE)) { return i; } i++;
        if (!jn.toString().equals("mi trabajo")) { return i; } i++;
        // El locale es parte de la identidad: el mismo texto en otro idioma es otro atributo.
        if (!jn.equals(new JobName("mi trabajo", Locale.FRANCE))) { return i; } i++;
        if (jn.equals(new JobName("mi trabajo", Locale.US))) { return i; } i++;
        if (jn.equals(new JobName("otro", Locale.FRANCE))) { return i; } i++;
        // Un locale null toma el del sistema, que no se puede fijar aca; alcanza con que coincida.
        if (!new JobName("x", null).getLocale().equals(Locale.getDefault())) { return i; } i++;
        threw = false;
        try { new JobName(null, Locale.US); } catch (NullPointerException e) { threw = true; }
        if (!threw) { return i; } i++;

        if (new DateTimeAtCreation(new Date(0)).getValue().getTime() != 0) { return i; } i++;
        if (!new PrinterURI(java.net.URI.create("ipp://x/y")).getURI().toString()
                .equals("ipp://x/y")) { return i; } i++;
        if (!new PrinterMoreInfo(java.net.URI.create("http://x/")).getURI().toString()
                .equals("http://x/")) { return i; } i++;
        if (!new Destination(java.net.URI.create("file:/x")).getURI().toString()
                .equals("file:/x")) { return i; } i++;

        return -1;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
