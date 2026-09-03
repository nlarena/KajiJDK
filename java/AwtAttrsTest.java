import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.util.Arrays;

/**
 * `java.awt.JobAttributes` y `java.awt.PageAttributes`: la configuracion de un trabajo de impresion.
 *
 * <p>El mismo archivo compila y da -1 con el JDK 25 corriendo **sus** clases, asi que las reglas que
 * se comprueban aca las dicta el JDK y no yo. Varias no estan en el javadoc y salieron de sondearlo:
 * el reparto entre `fromPage`/`toPage` y `pageRanges`, los codigos IPP de orientacion y calidad, y
 * que los alias de `MediaType` sean la **misma instancia** y no una equivalente.
 *
 * <p>Lo que NO se comprueba aca es el papel por omision: depende del pais del locale, asi que una
 * afirmacion fija fallaria en la mitad del mundo. Se comprueba la regla, no el resultado.
 */
public class AwtAttrsTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- JobAttributes: los valores por omision
        JobAttributes j = new JobAttributes();
        ok("una copia por omision", j.getCopies() == 1);
        ok("todo el documento", j.getDefaultSelection() == JobAttributes.DefaultSelectionType.ALL);
        ok("a la impresora", j.getDestination() == JobAttributes.DestinationType.PRINTER);
        ok("dialogo nativo", j.getDialog() == JobAttributes.DialogType.NATIVE);
        ok("sin archivo", j.getFileName() == null);
        ok("sin impresora", j.getPrinter() == null);
        ok("minPage 1", j.getMinPage() == 1);
        ok("maxPage al tope", j.getMaxPage() == Integer.MAX_VALUE);
        ok("un solo lado", j.getSides() == JobAttributes.SidesType.ONE_SIDED);
        ok("sin intercalar", j.getMultipleDocumentHandling()
                == JobAttributes.MultipleDocumentHandlingType
                        .SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);

        // ---- El reparto entre fromPage/toPage y pageRanges
        //
        // Los tres campos son independientes y los accesores tienen una preferencia asimetrica:
        // `getToPage` mira `fromPage` antes que los rangos, y `getFromPage` no mira `toPage`.
        ok("recien creado, de la 1 a la 1", j.getFromPage() == 1 && j.getToPage() == 1);
        ok("y getPageRanges sintetiza uno",
                Arrays.deepEquals(j.getPageRanges(), new int[][] { { 1, 1 } }));

        JobAttributes soloTo = new JobAttributes();
        soloTo.setToPage(5);
        ok("con solo toPage, from cae al minimo",
                soloTo.getFromPage() == 1 && soloTo.getToPage() == 5);

        JobAttributes soloFrom = new JobAttributes();
        soloFrom.setFromPage(3);
        ok("con solo fromPage, to lo sigue",
                soloFrom.getFromPage() == 3 && soloFrom.getToPage() == 3);

        JobAttributes ambos = new JobAttributes();
        ambos.setFromPage(3);
        ambos.setToPage(7);
        ok("con los dos, cada uno el suyo",
                ambos.getFromPage() == 3 && ambos.getToPage() == 7);

        JobAttributes rangos = new JobAttributes();
        rangos.setPageRanges(new int[][] { { 2, 4 } });
        ok("con solo rangos, from y to salen de ellos",
                rangos.getFromPage() == 2 && rangos.getToPage() == 4);

        // Y la asimetria: con `fromPage` puesto, los rangos NO afectan a `getToPage`.
        JobAttributes mezcla = new JobAttributes();
        mezcla.setFromPage(3);
        mezcla.setPageRanges(new int[][] { { 8, 9 } });
        ok("fromPage gana sobre los rangos para getToPage",
                mezcla.getFromPage() == 3 && mezcla.getToPage() == 3);
        ok("y los rangos quedan intactos",
                Arrays.deepEquals(mezcla.getPageRanges(), new int[][] { { 8, 9 } }));

        JobAttributes varios = new JobAttributes();
        varios.setPageRanges(new int[][] { { 1, 3 }, { 5, 7 } });
        ok("con varios rangos, to es el final del ultimo", varios.getToPage() == 7);
        ok("y from el comienzo del primero", varios.getFromPage() == 1);

        JobAttributes minAlto = new JobAttributes();
        minAlto.setMinPage(5);
        ok("sin nada mas, from y to caen al minimo",
                minAlto.getFromPage() == 5 && minAlto.getToPage() == 5);

        // ---- Validacion de JobAttributes
        ok("cero copias no vale", tira(new Runnable() {
            public void run() {
                new JobAttributes().setCopies(0);
            }
        }));
        ok("pagina cero no vale", tira(new Runnable() {
            public void run() {
                new JobAttributes().setFromPage(0);
            }
        }));
        ok("maxPage cero no vale", tira(new Runnable() {
            public void run() {
                new JobAttributes().setMaxPage(0);
            }
        }));
        ok("minPage por encima de maxPage no vale", tira(new Runnable() {
            public void run() {
                JobAttributes k = new JobAttributes();
                k.setMinPage(5);
                k.setMaxPage(3);
            }
        }));
        ok("toPage por debajo de fromPage no vale", tira(new Runnable() {
            public void run() {
                JobAttributes k = new JobAttributes();
                k.setFromPage(7);
                k.setToPage(3);
            }
        }));
        ok("rangos vacios no valen", tira(new Runnable() {
            public void run() {
                new JobAttributes().setPageRanges(new int[0][]);
            }
        }));
        ok("rangos desordenados no valen", tira(new Runnable() {
            public void run() {
                new JobAttributes().setPageRanges(new int[][] { { 5, 7 }, { 1, 3 } });
            }
        }));
        ok("un rango que retrocede no vale", tira(new Runnable() {
            public void run() {
                new JobAttributes().setPageRanges(new int[][] { { 7, 5 } });
            }
        }));

        // ---- Copia e igualdad
        JobAttributes orig = new JobAttributes();
        orig.setCopies(3);
        orig.setPageRanges(new int[][] { { 2, 4 } });
        JobAttributes copia = (JobAttributes) orig.clone();
        ok("clone es igual", orig.equals(copia));
        ok("y comparte hashCode", orig.hashCode() == copia.hashCode());
        ok("pero no es el mismo objeto", orig != copia);
        copia.setCopies(9);
        ok("y es independiente", orig.getCopies() == 3);

        // El arreglo que devuelve `getPageRanges` es una copia: tocarlo no toca al objeto.
        int[][] sacados = orig.getPageRanges();
        sacados[0][0] = 99;
        ok("getPageRanges devuelve una copia", orig.getPageRanges()[0][0] == 2);

        // ---- PageAttributes: omisiones
        PageAttributes p = new PageAttributes();
        ok("blanco y negro por omision", p.getColor() == PageAttributes.ColorType.MONOCHROME);
        ok("vertical", p.getOrientationRequested()
                == PageAttributes.OrientationRequestedType.PORTRAIT);
        ok("origen fisico", p.getOrigin() == PageAttributes.OriginType.PHYSICAL);
        ok("calidad normal", p.getPrintQuality() == PageAttributes.PrintQualityType.NORMAL);
        ok("72 puntos por pulgada",
                Arrays.equals(p.getPrinterResolution(), new int[] { 72, 72, 3 }));
        // El papel por omision depende del pais; lo que si vale siempre es que sea uno de los dos.
        ok("el papel por omision es carta o A4",
                p.getMedia() == PageAttributes.MediaType.NA_LETTER
                        || p.getMedia() == PageAttributes.MediaType.ISO_A4);

        // ---- Los codigos IPP
        PageAttributes ipp = new PageAttributes();
        ipp.setOrientationRequested(3);
        ok("3 es vertical", ipp.getOrientationRequested()
                == PageAttributes.OrientationRequestedType.PORTRAIT);
        ipp.setOrientationRequested(4);
        ok("4 es apaisada", ipp.getOrientationRequested()
                == PageAttributes.OrientationRequestedType.LANDSCAPE);
        ok("2 no es una orientacion", tira(new Runnable() {
            public void run() {
                new PageAttributes().setOrientationRequested(2);
            }
        }));
        ok("5 tampoco", tira(new Runnable() {
            public void run() {
                new PageAttributes().setOrientationRequested(5);
            }
        }));

        ipp.setPrintQuality(3);
        ok("3 es borrador", ipp.getPrintQuality() == PageAttributes.PrintQualityType.DRAFT);
        ipp.setPrintQuality(4);
        ok("4 es normal", ipp.getPrintQuality() == PageAttributes.PrintQualityType.NORMAL);
        ipp.setPrintQuality(5);
        ok("5 es alta", ipp.getPrintQuality() == PageAttributes.PrintQualityType.HIGH);
        ok("2 no es una calidad", tira(new Runnable() {
            public void run() {
                new PageAttributes().setPrintQuality(2);
            }
        }));
        ok("6 tampoco", tira(new Runnable() {
            public void run() {
                new PageAttributes().setPrintQuality(6);
            }
        }));

        // ---- La resolucion
        PageAttributes res = new PageAttributes();
        res.setPrinterResolution(300);
        ok("el atajo pone los dos ejes y la unidad por pulgada",
                Arrays.equals(res.getPrinterResolution(), new int[] { 300, 300, 3 }));
        res.setPrinterResolution(new int[] { 600, 300, 4 });
        ok("el arreglo se guarda tal cual",
                Arrays.equals(res.getPrinterResolution(), new int[] { 600, 300, 4 }));
        ok("resolucion cero no vale", tira(new Runnable() {
            public void run() {
                new PageAttributes().setPrinterResolution(0);
            }
        }));
        ok("un arreglo de dos no vale", tira(new Runnable() {
            public void run() {
                new PageAttributes().setPrinterResolution(new int[] { 1, 2 });
            }
        }));
        ok("una unidad que no es 3 ni 4 no vale", tira(new Runnable() {
            public void run() {
                new PageAttributes().setPrinterResolution(new int[] { 1, 2, 5 });
            }
        }));

        // ---- MediaType: los alias son la MISMA instancia
        ok("A4 es ISO_A4", PageAttributes.MediaType.A4 == PageAttributes.MediaType.ISO_A4);
        ok("LETTER es NA_LETTER",
                PageAttributes.MediaType.LETTER == PageAttributes.MediaType.NA_LETTER);
        ok("ENV_10 es el sobre numero 10",
                PageAttributes.MediaType.ENV_10
                        == PageAttributes.MediaType.NA_NUMBER_10_ENVELOPE);
        ok("MONARCH es su sobre",
                PageAttributes.MediaType.MONARCH
                        == PageAttributes.MediaType.MONARCH_ENVELOPE);
        ok("y dos tamanos distintos no son el mismo",
                PageAttributes.MediaType.ISO_A4 != PageAttributes.MediaType.ISO_A3);

        // Los nombres que salen por `toString`.
        ok("ISO_A4 se llama iso-a4",
                "iso-a4".equals(PageAttributes.MediaType.ISO_A4.toString()));
        ok("NA_LETTER se llama na-letter",
                "na-letter".equals(PageAttributes.MediaType.NA_LETTER.toString()));
        ok("el sobre numero 10 se llama na-number-10-envelope",
                "na-number-10-envelope".equals(
                        PageAttributes.MediaType.NA_NUMBER_10_ENVELOPE.toString()));
        ok("el hashCode es el indice y no se repite",
                PageAttributes.MediaType.ISO_4A0.hashCode() == 0
                        && PageAttributes.MediaType.ISO_2A0.hashCode() == 1);

        // ---- Los nombres de los otros tipos anidados
        ok("all", "all".equals(JobAttributes.DefaultSelectionType.ALL.toString()));
        ok("printer", "printer".equals(JobAttributes.DestinationType.PRINTER.toString()));
        ok("native", "native".equals(JobAttributes.DialogType.NATIVE.toString()));
        ok("one-sided", "one-sided".equals(JobAttributes.SidesType.ONE_SIDED.toString()));
        ok("two-sided-long-edge", "two-sided-long-edge".equals(
                JobAttributes.SidesType.TWO_SIDED_LONG_EDGE.toString()));
        ok("separate-documents-collated-copies", "separate-documents-collated-copies".equals(
                JobAttributes.MultipleDocumentHandlingType
                        .SEPARATE_DOCUMENTS_COLLATED_COPIES.toString()));
        ok("monochrome", "monochrome".equals(PageAttributes.ColorType.MONOCHROME.toString()));
        ok("landscape", "landscape".equals(
                PageAttributes.OrientationRequestedType.LANDSCAPE.toString()));
        ok("printable", "printable".equals(PageAttributes.OriginType.PRINTABLE.toString()));
        ok("draft", "draft".equals(PageAttributes.PrintQualityType.DRAFT.toString()));

        // ---- PageAttributes: copia e igualdad
        PageAttributes pa = new PageAttributes();
        pa.setColor(PageAttributes.ColorType.COLOR);
        pa.setPrinterResolution(600);
        PageAttributes pb = (PageAttributes) pa.clone();
        ok("clone de PageAttributes es igual", pa.equals(pb));
        ok("y comparte hashCode", pa.hashCode() == pb.hashCode());
        pb.setPrinterResolution(300);
        ok("y es independiente", pa.getPrinterResolution()[0] == 600);
        ok("y ya no son iguales", !pa.equals(pb));

        // ---- toString: el formato exacto del JDK
        ok("el toString de un JobAttributes recien creado",
                ("copies=1,defaultSelection=all,destination=printer,dialog=native,fileName=null,"
                        + "fromPage=1,maxPage=2147483647,minPage=1,"
                        + "multiple-document-handling=separate-documents-uncollated-copies,"
                        + "page-ranges=[1:1],printer=null,sides=one-sided,toPage=1")
                        .equals(new JobAttributes().toString()));

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    /** Si `r` tira `IllegalArgumentException`. */
    static boolean tira(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("AwtAttrsTest " + AwtAttrsTest.run());
    }
}
