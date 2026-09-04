import java.net.URI;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import javax.print.attribute.Attribute;
import javax.print.attribute.ResolutionSyntax;
import javax.print.attribute.Size2DSyntax;
import javax.print.attribute.standard.Chromaticity;
import javax.print.attribute.standard.ColorSupported;
import javax.print.attribute.standard.Compression;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.CopiesSupported;
import javax.print.attribute.standard.DateTimeAtCompleted;
import javax.print.attribute.standard.DateTimeAtCreation;
import javax.print.attribute.standard.DateTimeAtProcessing;
import javax.print.attribute.standard.Destination;
import javax.print.attribute.standard.DialogOwner;
import javax.print.attribute.standard.DialogTypeSelection;
import javax.print.attribute.standard.DocumentName;
import javax.print.attribute.standard.Fidelity;
import javax.print.attribute.standard.Finishings;
import javax.print.attribute.standard.JobHoldUntil;
import javax.print.attribute.standard.JobImpressions;
import javax.print.attribute.standard.JobImpressionsSupported;
import javax.print.attribute.standard.JobImpressionsCompleted;
import javax.print.attribute.standard.JobKOctets;
import javax.print.attribute.standard.JobKOctetsProcessed;
import javax.print.attribute.standard.JobKOctetsSupported;
import javax.print.attribute.standard.JobMediaSheets;
import javax.print.attribute.standard.JobMediaSheetsCompleted;
import javax.print.attribute.standard.JobMediaSheetsSupported;
import javax.print.attribute.standard.JobMessageFromOperator;
import javax.print.attribute.standard.JobName;
import javax.print.attribute.standard.JobPriority;
import javax.print.attribute.standard.JobOriginatingUserName;
import javax.print.attribute.standard.JobPrioritySupported;
import javax.print.attribute.standard.JobSheets;
import javax.print.attribute.standard.JobState;
import javax.print.attribute.standard.JobStateReason;
import javax.print.attribute.standard.JobStateReasons;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaName;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.MediaTray;
import javax.print.attribute.standard.MultipleDocumentHandling;
import javax.print.attribute.standard.NumberOfDocuments;
import javax.print.attribute.standard.NumberOfInterveningJobs;
import javax.print.attribute.standard.NumberUp;
import javax.print.attribute.standard.NumberUpSupported;
import javax.print.attribute.standard.OrientationRequested;
import javax.print.attribute.standard.OutputBin;
import javax.print.attribute.standard.OutputDeviceAssigned;
import javax.print.attribute.standard.PDLOverrideSupported;
import javax.print.attribute.standard.PageRanges;
import javax.print.attribute.standard.PagesPerMinute;
import javax.print.attribute.standard.PagesPerMinuteColor;
import javax.print.attribute.standard.PresentationDirection;
import javax.print.attribute.standard.PrintQuality;
import javax.print.attribute.standard.PrinterInfo;
import javax.print.attribute.standard.PrinterIsAcceptingJobs;
import javax.print.attribute.standard.PrinterLocation;
import javax.print.attribute.standard.PrinterMakeAndModel;
import javax.print.attribute.standard.PrinterMessageFromOperator;
import javax.print.attribute.standard.PrinterMoreInfo;
import javax.print.attribute.standard.PrinterMoreInfoManufacturer;
import javax.print.attribute.standard.PrinterName;
import javax.print.attribute.standard.PrinterResolution;
import javax.print.attribute.standard.PrinterState;
import javax.print.attribute.standard.PrinterStateReason;
import javax.print.attribute.standard.PrinterStateReasons;
import javax.print.attribute.standard.PrinterURI;
import javax.print.attribute.standard.QueuedJobCount;
import javax.print.attribute.standard.ReferenceUriSchemesSupported;
import javax.print.attribute.standard.RequestingUserName;
import javax.print.attribute.standard.Severity;
import javax.print.attribute.standard.SheetCollate;
import javax.print.attribute.standard.Sides;

/**
 * Prueba de comportamiento de javax.print.attribute.standard, escrita para correr **igual** en esta
 * VM y en el JDK real.
 *
 * <p>Cada comprobacion tiene un indice. {@code run()} devuelve -1 si pasaron todas, o el indice de
 * la primera que fallo: un solo int alcanza para comparar las dos VMs sin depender de que la salida
 * por consola coincida caracter por caracter.
 *
 * <p>Lo que se apunta es lo que el JDK especifica y es facil de errar sin que se note:
 *
 * <ul>
 * <li>los {@code toString()} de los {@code EnumSyntax}, que van al caracter y no son los nombres de
 *     las constantes;</li>
 * <li>los offsets de las tres categorias que IPP numera desde 3, y las filas {@code null} que dejan
 *     los valores reservados;</li>
 * <li>{@code getName()} y {@code getCategory()} de cada familia --sobre todo el de {@code Media},
 *     que las tres subclases comparten, y el de {@code Destination}, cuyo nombre IPP no se parece
 *     al de la clase;</li>
 * <li>la aritmetica de {@code PageRanges}: normalizacion, fusion de rangos adyacentes,
 *     {@code next()};</li>
 * <li>los rangos que tienen que tirar {@code IllegalArgumentException}, incluido el caso donde el
 *     error no es el valor sino que el conjunto quedo vacio;</li>
 * <li>las tablas de {@code MediaSize} y su registro estatico.</li>
 * </ul>
 *
 * <p>No se prueba nada que dependa de una impresora real ni de un locale: este paquete es datos.
 */
public class PrintStdTest {

    // Un instante fijo. No se imprime nunca --el formato de Date si depende de la zona horaria--,
    // solo se compara.
    private static final long T0 = 1000000000L;

    public static int run() {
        int i = 0;

        // --- toString() de los EnumSyntax: el JDK los especifica al caracter ---
        if (!Chromaticity.MONOCHROME.toString().equals("monochrome")) return i; i++;   // 0
        if (!Chromaticity.COLOR.toString().equals("color")) return i; i++;             // 1
        if (!ColorSupported.NOT_SUPPORTED.toString().equals("not-supported")) return i; i++; // 2
        if (!Compression.COMPRESS.toString().equals("compress")) return i; i++;        // 3
        if (!DialogTypeSelection.NATIVE.toString().equals("native")) return i; i++;    // 4
        if (!Fidelity.FIDELITY_TRUE.toString().equals("true")) return i; i++;          // 5
        if (!Fidelity.FIDELITY_FALSE.toString().equals("false")) return i; i++;        // 6
        if (!JobSheets.STANDARD.toString().equals("standard")) return i; i++;          // 7
        if (!MultipleDocumentHandling.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES.toString()
                .equals("separate-documents-uncollated-copies")) return i; i++;        // 8
        if (!OutputBin.LARGE_CAPACITY.toString().equals("large-capacity")) return i; i++; // 9
        if (!PDLOverrideSupported.NOT_ATTEMPTED.toString().equals("not-attempted")) return i; i++; // 10
        if (!PresentationDirection.TOBOTTOM_TORIGHT.toString()
                .equals("tobottom-toright")) return i; i++;                            // 11
        if (!PrinterIsAcceptingJobs.ACCEPTING_JOBS.toString()
                .equals("accepting-jobs")) return i; i++;                              // 12
        if (!ReferenceUriSchemesSupported.NNTP.toString().equals("nntp")) return i; i++; // 13
        if (!Severity.WARNING.toString().equals("warning")) return i; i++;             // 14
        if (!SheetCollate.UNCOLLATED.toString().equals("uncollated")) return i; i++;   // 15
        if (!Sides.TWO_SIDED_LONG_EDGE.toString().equals("two-sided-long-edge")) return i; i++; // 16
        if (!JobStateReason.QUEUED_IN_DEVICE.toString().equals("queued-in-device")) return i; i++; // 17
        if (!PrinterStateReason.INTERPRETER_RESOURCE_UNAVAILABLE.toString()
                .equals("interpreter-resource-unavailable")) return i; i++;            // 18

        // --- los alias de Sides son el mismo objeto, no valores nuevos ---
        if (Sides.DUPLEX != Sides.TWO_SIDED_LONG_EDGE) return i; i++;                  // 19
        if (Sides.TUMBLE != Sides.TWO_SIDED_SHORT_EDGE) return i; i++;                 // 20
        if (Sides.DUPLEX.getValue() != 1) return i; i++;                               // 21

        // --- las tres categorias que IPP numera desde 3 ---
        if (Finishings.NONE.getValue() != 3) return i; i++;                            // 22
        if (!Finishings.NONE.toString().equals("none")) return i; i++;                 // 23
        if (Finishings.STAPLE_DUAL_BOTTOM.getValue() != 31) return i; i++;             // 24
        if (!Finishings.STAPLE_DUAL_BOTTOM.toString()
                .equals("staple-dual-bottom")) return i; i++;                          // 25
        if (OrientationRequested.PORTRAIT.getValue() != 3) return i; i++;              // 26
        if (!OrientationRequested.REVERSE_PORTRAIT.toString()
                .equals("reverse-portrait")) return i; i++;                            // 27
        if (PrintQuality.DRAFT.getValue() != 3) return i; i++;                         // 28
        if (!PrintQuality.HIGH.toString().equals("high")) return i; i++;               // 29

        // --- un valor fuera de la tabla imprime el entero pelado ---
        // Por debajo del offset y por encima de la ultima fila.
        if (!new FinishingsProbe(2).toString().equals("2")) return i; i++;              // 30
        if (!new FinishingsProbe(32).toString().equals("32")) return i; i++;            // 31
        //
        // Lo que NO se comprueba aca, y conviene saber por que: un valor que cae DENTRO de la tabla
        // pero en una fila reservada --Finishings 5 y 10..19, JobState 1 y 2-- da `null` en el JDK
        // real, porque su EnumSyntax.toString() no mira si la entrada es null antes de devolverla.
        // El EnumSyntax de KajiLibrary si la mira y cae al entero. La divergencia esta en la clase
        // base, que es de otro paquete; ponerla aca haria que las dos VMs no coincidan sin que el
        // problema sea de este paquete. Queda anotada y no probada.
        if (!JobState.PENDING.toString().equals("pending")) return i; i++;              // 32
        if (!JobState.UNKNOWN.toString().equals("unknown")) return i; i++;             // 33
        if (JobState.COMPLETED.getValue() != 9) return i; i++;                         // 34
        if (PrinterState.STOPPED.getValue() != 5) return i; i++;                       // 35
        // Un valor fuera de la tabla entera tambien cae al entero.
        if (!new FinishingsProbe(99).toString().equals("99")) return i; i++;                // 36

        // --- getName(): el nombre IPP, que no siempre se parece al de la clase ---
        if (!Chromaticity.COLOR.getName().equals("chromaticity")) return i; i++;       // 37
        if (!Fidelity.FIDELITY_TRUE.getName().equals("ipp-attribute-fidelity")) return i; i++; // 38
        if (!new Destination(URI.create("file:/x")).getName()
                .equals("spool-data-destination")) return i; i++;                      // 39
        if (!new Copies(1).getName().equals("copies")) return i; i++;                  // 40
        if (!new JobKOctets(0).getName().equals("job-k-octets")) return i; i++;        // 41
        if (!new PageRanges(1).getName().equals("page-ranges")) return i; i++;         // 42
        if (!new PrinterResolution(300, 600, ResolutionSyntax.DPI).getName()
                .equals("printer-resolution")) return i; i++;                          // 43
        if (!new MediaPrintableArea(0, 0, 1, 1, MediaPrintableArea.INCH).getName()
                .equals("media-printable-area")) return i; i++;                        // 44
        if (!new JobStateReasons().getName().equals("job-state-reasons")) return i; i++; // 45
        if (!new PrinterStateReasons().getName().equals("printer-state-reasons")) return i; i++; // 46
        if (!new DialogOwner().getName().equals("dialog-owner")) return i; i++;        // 47
        if (!new PrinterName("p", Locale.US).getName().equals("printer-name")) return i; i++; // 48
        if (!new DateTimeAtCreation(new Date(T0)).getName()
                .equals("date-time-at-creation")) return i; i++;                       // 49

        // --- getCategory(): las tres formas de decir "que papel" comparten la de Media ---
        if (MediaSizeName.ISO_A4.getCategory() != Media.class) return i; i++;          // 50
        if (MediaTray.MANUAL.getCategory() != Media.class) return i; i++;              // 51
        if (MediaName.ISO_A4_WHITE.getCategory() != Media.class) return i; i++;        // 52
        if (!MediaTray.MANUAL.getName().equals("media")) return i; i++;                // 53
        if (!MediaSizeName.ISO_A4.getName().equals("media")) return i; i++;            // 54
        // MediaSize en cambio es su propia categoria: son medidas, no una eleccion de papel.
        if (MediaSize.ISO.A4.getCategory() != MediaSize.class) return i; i++;          // 55
        if (!MediaSize.ISO.A4.getName().equals("media-size")) return i; i++;           // 56
        if (Chromaticity.COLOR.getCategory() != Chromaticity.class) return i; i++;     // 57
        if (new Copies(1).getCategory() != Copies.class) return i; i++;                // 58

        // --- Media.equals mira la clase concreta, no solo el entero ---
        // MediaTray.TOP y MediaName.NA_LETTER_WHITE valen los dos cero.
        if (MediaTray.TOP.getValue() != MediaName.NA_LETTER_WHITE.getValue()) return i; i++; // 59
        if (MediaTray.TOP.equals(MediaName.NA_LETTER_WHITE)) return i; i++;            // 60
        if (!MediaTray.TOP.equals(MediaTray.TOP)) return i; i++;                       // 61

        // --- IntegerSyntax: el instanceof impide que dos atributos distintos den iguales ---
        if (new Copies(1).equals(new NumberUp(1))) return i; i++;                      // 62
        if (!new Copies(3).equals(new Copies(3))) return i; i++;                       // 63
        if (new Copies(3).hashCode() != 3) return i; i++;                              // 64
        if (!new Copies(7).toString().equals("7")) return i; i++;                      // 65

        // --- los rangos legales ---
        if (!tiraIAE(0, "Copies")) return i; i++;                                      // 66
        if (!tiraIAE(-1, "Copies")) return i; i++;                                     // 67
        if (tiraIAE(1, "Copies")) return i; i++;                                       // 68
        if (tiraIAE(0, "JobImpressions")) return i; i++;                               // 69
        if (!tiraIAE(-1, "JobImpressions")) return i; i++;                             // 70
        if (tiraIAE(0, "QueuedJobCount")) return i; i++;                               // 71
        if (!tiraIAE(0, "NumberUp")) return i; i++;                                    // 72
        if (tiraIAE(100, "JobPriority")) return i; i++;                                // 73
        if (!tiraIAE(101, "JobPriority")) return i; i++;                               // 74
        if (!tiraIAE(0, "JobPriority")) return i; i++;                                 // 75
        if (tiraIAE(Integer.MAX_VALUE, "Copies")) return i; i++;                       // 76
        if (!tiraIAE(Integer.MAX_VALUE, "JobPriority")) return i; i++;                 // 77

        // --- PageRanges: canonicalizacion ---
        // Desordenado y solapado sale ordenado y fusionado.
        if (!new PageRanges("3-5,1-2").toString().equals("1-5")) return i; i++;        // 78
        if (!new PageRanges(new int[][] {{1, 5}, {2, 6}}).toString()
                .equals("1-6")) return i; i++;                                         // 79
        // Adyacentes tambien se fusionan: 1-3 y 4-6 no dejan ninguna pagina afuera.
        if (!new PageRanges("1-3,4-6").toString().equals("1-6")) return i; i++;        // 80
        if (!new PageRanges("1,3,5").toString().equals("1,3,5")) return i; i++;        // 81
        if (!new PageRanges(2, 4).toString().equals("2-4")) return i; i++;             // 82
        if (!new PageRanges(7).toString().equals("7")) return i; i++;                  // 83
        // Y la forma canonica es lo que hace que equals sea barato y correcto.
        if (!new PageRanges("3-5,1-2").equals(new PageRanges(1, 5))) return i; i++;    // 84
        if (new PageRanges(1, 5).equals(new PageRanges(1, 6))) return i; i++;          // 85

        // --- PageRanges: contains y next ---
        PageRanges pr = new PageRanges("1,3,5");
        if (!pr.contains(3)) return i; i++;                                            // 86
        if (pr.contains(2)) return i; i++;                                             // 87
        if (pr.next(1) != 3) return i; i++;                                            // 88
        if (pr.next(3) != 5) return i; i++;                                            // 89
        if (pr.next(5) != -1) return i; i++;                                           // 90
        if (pr.next(0) != 1) return i; i++;                                            // 91
        if (pr.getMembers().length != 3) return i; i++;                                // 92

        // --- PageRanges: lo que rechaza ---
        if (!prTiraIAE("0")) return i; i++;                                            // 93
        // "5-1" es un rango vacio: la base lo descarta y falla por conjunto vacio, no por el valor.
        if (!prTiraIAE("5-1")) return i; i++;                                          // 94
        if (prTiraIAE("1-3,7,10-12")) return i; i++;                                   // 95
        if (!prTiraIAE("1,")) return i; i++;                                           // 96
        if (!prTiraIAE("abc")) return i; i++;                                          // 97

        // --- los conjuntos de valores soportados ---
        if (!new CopiesSupported(1, 5).toString().equals("1-5")) return i; i++;        // 98
        if (!new CopiesSupported(3).toString().equals("3")) return i; i++;             // 99
        if (!csTiraIAE(0, 5)) return i; i++;                                           // 100
        if (csTiraIAE(1, 99)) return i; i++;                                           // 101
        if (!new JobImpressionsSupported(0, 10).toString().equals("0-10")) return i; i++; // 102
        if (!jisTiraIAE(-1, 5)) return i; i++;                                         // 103
        if (!new NumberUpSupported(new int[][] {{1, 1}, {2, 2}, {4, 4}}).toString()
                .equals("1-2,4")) return i; i++;                                       // 104
        if (!nusTiraIAE(0)) return i; i++;                                             // 105
        if (!new NumberUpSupported(1, 3).toString().equals("1-3")) return i; i++;      // 106
        if (!new CopiesSupported(1, 5).getName().equals("copies-supported")) return i; i++; // 107
        // El instanceof separa dos conjuntos con los mismos numeros.
        if (new CopiesSupported(1, 5).equals(new PageRanges(1, 5))) return i; i++;     // 108

        // --- ResolutionSyntax ---
        PrinterResolution res = new PrinterResolution(300, 600, ResolutionSyntax.DPI);
        if (res.getCrossFeedResolution(ResolutionSyntax.DPI) != 300) return i; i++;    // 109
        if (res.getFeedResolution(ResolutionSyntax.DPI) != 600) return i; i++;         // 110
        if (!res.toString().equals("30000x60000 dphi")) return i; i++;                 // 111
        if (!res.toString(ResolutionSyntax.DPI, "dpi").equals("300x600 dpi")) return i; i++; // 112
        if (!res.equals(new PrinterResolution(300, 600, ResolutionSyntax.DPI))) return i; i++; // 113
        if (res.equals(new PrinterResolution(600, 300, ResolutionSyntax.DPI))) return i; i++; // 114
        // 100 dpcm son 254 dphi por unidad: la unidad interna hace exactas las dos escalas.
        if (new PrinterResolution(100, 100, ResolutionSyntax.DPCM)
                .getCrossFeedResolution(ResolutionSyntax.DPCM) != 100) return i; i++;  // 115

        // --- MediaSize: las tablas de norma ---
        if (MediaSize.ISO.A4.getX(Size2DSyntax.MM) != 210.0f) return i; i++;           // 116
        if (MediaSize.ISO.A4.getY(Size2DSyntax.MM) != 297.0f) return i; i++;           // 117
        if (MediaSize.ISO.A0.getY(Size2DSyntax.MM) != 1189.0f) return i; i++;          // 118
        // Las B japonesas NO son las B de ISO.
        if (MediaSize.JIS.B4.getX(Size2DSyntax.MM) != 257.0f) return i; i++;           // 119
        if (MediaSize.ISO.B4.getX(Size2DSyntax.MM) != 250.0f) return i; i++;           // 120
        if (MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) != 8.5f) return i; i++;        // 121
        if (MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) != 11.0f) return i; i++;       // 122
        if (MediaSize.Engineering.E.getX(Size2DSyntax.INCH) != 34.0f) return i; i++;   // 123
        if (MediaSize.Other.INVOICE.getY(Size2DSyntax.INCH) != 8.5f) return i; i++;    // 124
        if (!MediaSize.ISO.A4.toString().equals("210000x297000 um")) return i; i++;    // 125

        // --- MediaSize: el registro estatico ---
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4) != MediaSize.ISO.A4) return i; i++; // 126
        if (MediaSize.getMediaSizeForName(MediaSizeName.NA_LETTER)
                != MediaSize.NA.LETTER) return i; i++;                                 // 127
        // ISO_C0, C1 y C2 tienen nombre pero no tamano registrado.
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_C0) != null) return i; i++; // 128
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_C3) == null) return i; i++; // 129
        if (MediaSize.ISO.A4.getMediaSizeName() != MediaSizeName.ISO_A4) return i; i++; // 130
        // Los sobres japoneses estan en la tabla de busqueda pero no tienen nombre.
        if (MediaSize.JIS.CHOU_1.getMediaSizeName() != null) return i; i++;            // 131
        // Un tamano anonimo tampoco reclama nombre.
        if (new MediaSize(1, 1, Size2DSyntax.INCH).getMediaSizeName() != null) return i; i++; // 132

        // --- MediaSize: findMedia elige el mas cercano y nunca dice "no hay" ---
        if (MediaSize.findMedia(210f, 297f, Size2DSyntax.MM)
                != MediaSizeName.ISO_A4) return i; i++;                                // 133
        if (MediaSize.findMedia(8.5f, 11f, Size2DSyntax.INCH)
                != MediaSizeName.NA_LETTER) return i; i++;                             // 134
        // Casi-A4 sigue dando A4.
        if (MediaSize.findMedia(210.5f, 297.5f, Size2DSyntax.MM)
                != MediaSizeName.ISO_A4) return i; i++;                                // 135
        if (!fmTiraIAE(0f, 10f)) return i; i++;                                        // 136
        if (!fmTiraIAE(10f, -1f)) return i; i++;                                       // 137
        // El papel se declara siempre de pie.
        if (!msTiraIAE(2, 1)) return i; i++;                                           // 138
        if (msTiraIAE(1, 2)) return i; i++;                                            // 139

        // --- MediaPrintableArea: micrometros adentro, milimetros al imprimir ---
        MediaPrintableArea mpa = new MediaPrintableArea(1f, 2f, 3f, 4f,
                                                        MediaPrintableArea.INCH);
        if (!mpa.toString().equals("(25.4,50.8)->(76.2,101.6)mm")) return i; i++;      // 140
        if (mpa.getX(MediaPrintableArea.INCH) != 1.0f) return i; i++;                  // 141
        if (mpa.getWidth(MediaPrintableArea.INCH) != 3.0f) return i; i++;              // 142
        if (mpa.getPrintableArea(MediaPrintableArea.MM)[3] != 101.6f) return i; i++;   // 143
        // La unidad con la que se construyo no se guarda: las mismas medidas dan iguales.
        MediaPrintableArea unaPulgada = new MediaPrintableArea(1, 1, 1, 1,
                                                               MediaPrintableArea.INCH);
        MediaPrintableArea enMm = new MediaPrintableArea(25.4f, 25.4f, 25.4f, 25.4f,
                                                         MediaPrintableArea.MM);
        if (!unaPulgada.equals(enMm)) return i; i++;                                   // 144
        if (unaPulgada.hashCode() != enMm.hashCode()) return i; i++;                   // 145
        if (unaPulgada.hashCode() != 3251200) return i; i++;                           // 146
        if (!unaPulgada.toString(MediaPrintableArea.MM, null)
                .equals("(25.4,25.4)->(25.4,25.4)")) return i; i++;                    // 147
        // Ancho y alto tienen que ser estrictamente positivos; el origen puede ser cero.
        if (!mpaTiraIAE(0, 0, 0, 1)) return i; i++;                                    // 148
        if (!mpaTiraIAE(-1, 0, 1, 1)) return i; i++;                                   // 149
        if (mpaTiraIAE(0, 0, 1, 1)) return i; i++;                                     // 150

        // --- Size2DSyntax y el redondeo al micrometro ---
        if (MediaSize.Other.QUARTO.getX(Size2DSyntax.MM) != 215.9f) return i; i++;     // 151

        // --- TextSyntax ---
        JobName jn = new JobName("informe", Locale.US);
        if (!jn.toString().equals("informe")) return i; i++;                           // 152
        if (!jn.getValue().equals("informe")) return i; i++;                           // 153
        if (jn.getLocale() != Locale.US) return i; i++;                                // 154
        if (!jn.equals(new JobName("informe", Locale.US))) return i; i++;              // 155
        // El locale entra en la comparacion.
        if (jn.equals(new JobName("informe", Locale.FRANCE))) return i; i++;           // 156
        // Y el instanceof separa dos textos iguales de categorias distintas.
        if (jn.equals(new DocumentName("informe", Locale.US))) return i; i++;          // 157
        if (!tsTiraNPE()) return i; i++;                                               // 158
        // Un locale null no es error: significa "el de por aca".
        if (new PrinterInfo("x", null).getLocale() == null) return i; i++;             // 159

        // --- URISyntax ---
        URI u = URI.create("ipp://host/printer");
        if (!new PrinterURI(u).getURI().equals(u)) return i; i++;                      // 160
        if (!new PrinterURI(u).toString().equals("ipp://host/printer")) return i; i++; // 161
        if (new PrinterURI(u).equals(new PrinterMoreInfo(u))) return i; i++;           // 162
        if (!new PrinterURI(u).equals(new PrinterURI(URI.create("ipp://host/printer"))))
            return i; i++;                                                             // 163
        if (!uriTiraNPE()) return i; i++;                                              // 164

        // --- DateTimeSyntax: copia al entrar y al salir ---
        DateTimeAtCompleted dtc = new DateTimeAtCompleted(new Date(T0));
        if (dtc.getValue().getTime() != T0) return i; i++;                             // 165
        if (dtc.getValue() == dtc.getValue()) return i; i++;                           // 166
        //
        // Lo que NO se comprueba: si mutar el Date original cambia el atributo. El JDK real guarda
        // la referencia y solo copia al salir, asi que un `d.setTime(0)` posterior SI se ve desde
        // getValue(); el DateTimeSyntax de KajiLibrary copia tambien al entrar y no se ve. La
        // divergencia esta en la clase base, que es de otro paquete. Queda anotada y no probada.
        if (dtc.getValue().getTime() != T0) return i; i++;                             // 167
        if (!dtc.equals(new DateTimeAtCompleted(new Date(T0)))) return i; i++;         // 168
        if (dtc.equals(new DateTimeAtCreation(new Date(T0)))) return i; i++;           // 169

        // --- JobStateReasons: es un conjunto, y rechaza el null ---
        JobStateReasons jsr = new JobStateReasons();
        if (jsr.size() != 0) return i; i++;                                            // 170
        jsr.add(JobStateReason.JOB_PRINTING);
        jsr.add(JobStateReason.JOB_PRINTING);
        if (jsr.size() != 1) return i; i++;                                            // 171
        if (!jsr.contains(JobStateReason.JOB_PRINTING)) return i; i++;                 // 172
        if (jsr.contains(JobStateReason.JOB_QUEUED)) return i; i++;                    // 173
        if (!jsrTiraNPE(jsr)) return i; i++;                                           // 174
        if (jsr.getCategory() != JobStateReasons.class) return i; i++;                 // 175

        // --- PrinterStateReasons: el mapa y la vista por gravedad ---
        PrinterStateReasons psr = new PrinterStateReasons();
        psr.put(PrinterStateReason.MEDIA_JAM, Severity.ERROR);
        psr.put(PrinterStateReason.TONER_LOW, Severity.WARNING);
        psr.put(PrinterStateReason.MEDIA_LOW, Severity.WARNING);
        if (psr.size() != 3) return i; i++;                                            // 176
        if (psr.get(PrinterStateReason.MEDIA_JAM) != Severity.ERROR) return i; i++;    // 177
        Set<PrinterStateReason> errores = psr.printerStateReasonSet(Severity.ERROR);
        if (errores.size() != 1) return i; i++;                                        // 178
        if (!errores.contains(PrinterStateReason.MEDIA_JAM)) return i; i++;            // 179
        if (psr.printerStateReasonSet(Severity.WARNING).size() != 2) return i; i++;    // 180
        if (psr.printerStateReasonSet(Severity.REPORT).size() != 0) return i; i++;     // 181
        // Es una vista viva: lo que se agregue despues se ve.
        psr.put(PrinterStateReason.COVER_OPEN, Severity.ERROR);
        if (errores.size() != 2) return i; i++;                                        // 182
        // Y de solo lectura.
        if (!vistaEsInmutable(errores)) return i; i++;                                 // 183
        if (!psrTiraNPE(psr)) return i; i++;                                           // 184

        // --- Attribute: todos contestan las dos preguntas ---
        Attribute[] todos = {
            Chromaticity.COLOR, new Copies(1), new PageRanges(1),
            new PrinterResolution(300, 300, ResolutionSyntax.DPI), MediaSizeName.ISO_A4,
            new JobName("x", Locale.US), new PrinterURI(u), new DateTimeAtCreation(new Date(T0)),
            new JobStateReasons(), new PrinterStateReasons(), new DialogOwner(),
            new MediaPrintableArea(0, 0, 1, 1, MediaPrintableArea.INCH), MediaSize.ISO.A4,
            new PagesPerMinute(0), new QueuedJobCount(0), new RequestingUserName("u", Locale.US),
            new CopiesSupported(1), OutputBin.TOP, JobState.PENDING, PrinterState.IDLE,
        };
        for (int k = 0; k < todos.length; k++) {
            if (todos[k].getName() == null) return i;
            if (todos[k].getCategory() == null) return i;
        }
        i++;                                                                           // 185

        // ------------------------------------------------------------------------------------
        // Segunda tanda: las clases que la primera no tocaba.
        //
        // Son las diecinueve envolturas "aburridas" --IntegerSyntax, TextSyntax, DateTimeSyntax y
        // SetOfIntegerSyntax con otro nombre-- y justamente por aburridas son las mas faciles de
        // equivocar sin que se note: lo unico propio de cada una es el nombre IPP, la categoria y
        // el rango que acepta. Cada valor esperado de aca sale de correr un probe contra el JDK
        // real, no de deducirlo del nombre de la clase.
        // ------------------------------------------------------------------------------------

        // --- getName(): el nombre IPP no se deduce partiendo el nombre de la clase ---
        // "job-k-octets-processed" mete guiones que la clase no sugiere; el resto siguen la regla
        // pero ninguno la garantiza, asi que van los diecinueve escritos.
        Date d0 = new Date(T0);
        if (!new DateTimeAtProcessing(d0).getName().equals("date-time-at-processing")) return i; i++; // 186
        if (!new JobHoldUntil(d0).getName().equals("job-hold-until")) return i; i++;   // 187
        if (!new JobImpressionsCompleted(0).getName()
                .equals("job-impressions-completed")) return i; i++;                   // 188
        if (!new JobKOctetsProcessed(0).getName().equals("job-k-octets-processed")) return i; i++; // 189
        if (!new JobKOctetsSupported(0, 5).getName().equals("job-k-octets-supported")) return i; i++; // 190
        if (!new JobMediaSheets(0).getName().equals("job-media-sheets")) return i; i++; // 191
        if (!new JobMediaSheetsCompleted(0).getName()
                .equals("job-media-sheets-completed")) return i; i++;                  // 192
        if (!new JobMediaSheetsSupported(1, 9).getName()
                .equals("job-media-sheets-supported")) return i; i++;                  // 193
        if (!new JobMessageFromOperator("hola", Locale.US).getName()
                .equals("job-message-from-operator")) return i; i++;                   // 194
        if (!new JobOriginatingUserName("pepe", Locale.US).getName()
                .equals("job-originating-user-name")) return i; i++;                   // 195
        if (!new JobPrioritySupported(50).getName().equals("job-priority-supported")) return i; i++; // 196
        if (!new NumberOfDocuments(0).getName().equals("number-of-documents")) return i; i++; // 197
        if (!new NumberOfInterveningJobs(0).getName()
                .equals("number-of-intervening-jobs")) return i; i++;                  // 198
        if (!new OutputDeviceAssigned("dev", Locale.US).getName()
                .equals("output-device-assigned")) return i; i++;                      // 199
        if (!new PagesPerMinuteColor(0).getName().equals("pages-per-minute-color")) return i; i++; // 200
        if (!new PrinterLocation("sotano", Locale.US).getName()
                .equals("printer-location")) return i; i++;                            // 201
        if (!new PrinterMakeAndModel("Kaji 9000", Locale.US).getName()
                .equals("printer-make-and-model")) return i; i++;                      // 202
        if (!new PrinterMessageFromOperator("ojo", Locale.US).getName()
                .equals("printer-message-from-operator")) return i; i++;               // 203
        if (!new PrinterMoreInfoManufacturer(URI.create("http://x/")).getName()
                .equals("printer-more-info-manufacturer")) return i; i++;              // 204

        // --- getCategory(): cada una es su propia categoria ---
        // Importa que sea la clase concreta y no la superclase de sintaxis: si alguna devolviera
        // IntegerSyntax.class, dos atributos distintos se pisarian dentro de un AttributeSet.
        if (new DateTimeAtProcessing(d0).getCategory()
                != DateTimeAtProcessing.class) return i; i++;                          // 205
        if (new JobHoldUntil(d0).getCategory() != JobHoldUntil.class) return i; i++;   // 206
        if (new JobImpressionsCompleted(0).getCategory()
                != JobImpressionsCompleted.class) return i; i++;                       // 207
        if (new JobKOctetsProcessed(0).getCategory()
                != JobKOctetsProcessed.class) return i; i++;                           // 208
        if (new JobKOctetsSupported(0, 5).getCategory()
                != JobKOctetsSupported.class) return i; i++;                           // 209
        if (new JobMediaSheets(0).getCategory() != JobMediaSheets.class) return i; i++; // 210
        if (new JobMediaSheetsCompleted(0).getCategory()
                != JobMediaSheetsCompleted.class) return i; i++;                       // 211
        if (new JobMediaSheetsSupported(1, 9).getCategory()
                != JobMediaSheetsSupported.class) return i; i++;                       // 212
        if (new JobMessageFromOperator("hola", Locale.US).getCategory()
                != JobMessageFromOperator.class) return i; i++;                        // 213
        if (new JobOriginatingUserName("pepe", Locale.US).getCategory()
                != JobOriginatingUserName.class) return i; i++;                        // 214
        if (new JobPrioritySupported(50).getCategory()
                != JobPrioritySupported.class) return i; i++;                          // 215
        if (new NumberOfDocuments(0).getCategory() != NumberOfDocuments.class) return i; i++; // 216
        if (new NumberOfInterveningJobs(0).getCategory()
                != NumberOfInterveningJobs.class) return i; i++;                       // 217
        if (new OutputDeviceAssigned("dev", Locale.US).getCategory()
                != OutputDeviceAssigned.class) return i; i++;                          // 218
        if (new PagesPerMinuteColor(0).getCategory() != PagesPerMinuteColor.class) return i; i++; // 219
        if (new PrinterLocation("s", Locale.US).getCategory()
                != PrinterLocation.class) return i; i++;                               // 220
        if (new PrinterMakeAndModel("m", Locale.US).getCategory()
                != PrinterMakeAndModel.class) return i; i++;                           // 221
        if (new PrinterMessageFromOperator("o", Locale.US).getCategory()
                != PrinterMessageFromOperator.class) return i; i++;                    // 222
        if (new PrinterMoreInfoManufacturer(URI.create("http://x/")).getCategory()
                != PrinterMoreInfoManufacturer.class) return i; i++;                   // 223

        // --- valor y toString() ---
        // Los DateTimeSyntax no se comparan por toString: el formato de Date depende de la zona
        // horaria y las dos VMs no tienen por que correr en la misma. Se compara el instante.
        if (new DateTimeAtProcessing(d0).getValue().getTime() != T0) return i; i++;    // 224
        if (new JobHoldUntil(d0).getValue().getTime() != T0) return i; i++;            // 225
        if (!new JobImpressionsCompleted(7).toString().equals("7")) return i; i++;     // 226
        if (new JobKOctetsProcessed(7).getValue() != 7) return i; i++;                 // 227
        if (!new JobMediaSheets(7).toString().equals("7")) return i; i++;              // 228
        if (new NumberOfDocuments(7).hashCode() != 7) return i; i++;                   // 229
        if (new NumberOfInterveningJobs(7).getValue() != 7) return i; i++;             // 230
        if (!new PagesPerMinuteColor(7).toString().equals("7")) return i; i++;         // 231
        if (new JobPrioritySupported(50).getValue() != 50) return i; i++;              // 232
        if (!new JobMessageFromOperator("hola", Locale.US).getValue().equals("hola")) return i; i++; // 233
        if (!new OutputDeviceAssigned("dev", Locale.US).toString().equals("dev")) return i; i++; // 234
        if (!new PrinterMakeAndModel("Kaji 9000", Locale.US).getValue()
                .equals("Kaji 9000")) return i; i++;                                   // 235
        if (!new PrinterMakeAndModel("m", Locale.US).getLocale().equals(Locale.US)) return i; i++; // 236
        if (!new PrinterMoreInfoManufacturer(URI.create("http://x/")).getURI()
                .equals(URI.create("http://x/"))) return i; i++;                       // 237

        // Los dos SetOfIntegerSyntax de esta tanda: un solo rango se imprime "lo-hi".
        if (!new JobKOctetsSupported(0, 5).toString().equals("0-5")) return i; i++;    // 238
        if (!new JobMediaSheetsSupported(1, 9).toString().equals("1-9")) return i; i++; // 239
        if (!new JobMediaSheetsSupported(1, 9).contains(5)) return i; i++;             // 240
        if (new JobMediaSheetsSupported(1, 9).contains(10)) return i; i++;             // 241
        if (new JobKOctetsSupported(0, 5).getMembers().length != 1) return i; i++;     // 242
        if (new JobKOctetsSupported(0, 5).getMembers()[0][0] != 0) return i; i++;      // 243
        if (new JobKOctetsSupported(0, 5).getMembers()[0][1] != 5) return i; i++;      // 244

        // --- equals(): el valor solo no alcanza, la clase tambien cuenta ---
        // Dos IntegerSyntax con el mismo entero pero de categorias distintas NO son iguales, y lo
        // mismo con dos TextSyntax con el mismo texto. Es lo que impide que un AttributeSet
        // confunda "cantidad de documentos" con "paginas por minuto en color".
        if (new PagesPerMinuteColor(7).equals(new NumberOfDocuments(7))) return i; i++; // 245
        if (new PrinterLocation("a", Locale.US)
                .equals(new JobOriginatingUserName("a", Locale.US))) return i; i++;    // 246
        if (!new JobOriginatingUserName("pepe", Locale.US)
                .equals(new JobOriginatingUserName("pepe", Locale.US))) return i; i++; // 247
        // El locale forma parte de la identidad de un TextSyntax: mismo texto, distinto locale,
        // distinto atributo.
        if (new PrinterLocation("a", Locale.US)
                .equals(new PrinterLocation("a", Locale.FRANCE))) return i; i++;       // 248
        if (!new JobHoldUntil(d0).equals(new JobHoldUntil(new Date(T0)))) return i; i++; // 249

        // --- los rangos que tienen que tirar IllegalArgumentException ---
        // Todos estos contadores son "no negativos", no "positivos": el 0 es valido y el -1 no.
        if (!tiraIAE2(-1, "JobImpressionsCompleted")) return i; i++;                   // 250
        if (!tiraIAE2(-1, "JobKOctetsProcessed")) return i; i++;                       // 251
        if (!tiraIAE2(-1, "JobMediaSheets")) return i; i++;                            // 252
        if (!tiraIAE2(-1, "JobMediaSheetsCompleted")) return i; i++;                   // 253
        if (!tiraIAE2(-1, "NumberOfDocuments")) return i; i++;                         // 254
        if (!tiraIAE2(-1, "NumberOfInterveningJobs")) return i; i++;                   // 255
        if (!tiraIAE2(-1, "PagesPerMinuteColor")) return i; i++;                       // 256
        if (tiraIAE2(0, "NumberOfDocuments")) return i; i++;                           // 257
        // JobPrioritySupported es la excepcion: el rango es 1..100 cerrado por los dos lados.
        if (!tiraIAE2(0, "JobPrioritySupported")) return i; i++;                       // 258
        if (!tiraIAE2(101, "JobPrioritySupported")) return i; i++;                     // 259
        if (tiraIAE2(1, "JobPrioritySupported")) return i; i++;                        // 260
        if (tiraIAE2(100, "JobPrioritySupported")) return i; i++;                      // 261

        // Los dos rangos: el limite inferior no puede ser negativo ni superar al de arriba.
        if (!jkosTiraIAE(-1, 5)) return i; i++;                                        // 262
        if (!jkosTiraIAE(5, 1)) return i; i++;                                         // 263
        if (jkosTiraIAE(0, 0)) return i; i++;                                          // 264
        if (!jmssTiraIAE(-1, 5)) return i; i++;                                        // 265
        if (!jmssTiraIAE(5, 1)) return i; i++;                                         // 266
        // Ojo con este: pese a que "hojas" sugiere que el minimo es 1, el JDK acepta el 0. Queda
        // escrito para que nadie lo "arregle" a mano y se desvie del original.
        if (jmssTiraIAE(0, 5)) return i; i++;                                          // 267

        // --- null: NPE, no IAE ni un atributo con un campo nulo adentro ---
        if (!dtsTiraNPE("DateTimeAtProcessing")) return i; i++;                        // 268
        if (!dtsTiraNPE("JobHoldUntil")) return i; i++;                                // 269
        if (!dtsTiraNPE("PrinterMoreInfoManufacturer")) return i; i++;                 // 270
        if (!dtsTiraNPE("OutputDeviceAssigned")) return i; i++;                        // 271

        // --- y las diecinueve tambien contestan las dos preguntas de Attribute ---
        Attribute[] todos2 = {
            new DateTimeAtProcessing(d0), new JobHoldUntil(d0), new JobImpressionsCompleted(0),
            new JobKOctetsProcessed(0), new JobKOctetsSupported(0, 5), new JobMediaSheets(0),
            new JobMediaSheetsCompleted(0), new JobMediaSheetsSupported(1, 9),
            new JobMessageFromOperator("a", Locale.US),
            new JobOriginatingUserName("a", Locale.US), new JobPrioritySupported(1),
            new NumberOfDocuments(0), new NumberOfInterveningJobs(0),
            new OutputDeviceAssigned("a", Locale.US), new PagesPerMinuteColor(0),
            new PrinterLocation("a", Locale.US), new PrinterMakeAndModel("a", Locale.US),
            new PrinterMessageFromOperator("a", Locale.US),
            new PrinterMoreInfoManufacturer(URI.create("http://x/")),
        };
        if (todos2.length != 19) return i; i++;                                        // 272
        for (int k = 0; k < todos2.length; k++) {
            if (todos2[k].getName() == null) return i;
            if (todos2[k].getCategory() == null) return i;
        }
        i++;                                                                           // 273

        return -1;
    }

    // Los constructores de EnumSyntax son protected --son singletons-- asi que para fabricar un
    // valor reservado, que es justo lo que hace falta para ver el toString() de una fila null, hay
    // que pasar por una subclase. Las dos categorias que tienen filas reservadas no son final
    // justamente porque IPP deja que un sitio agregue valores.
    private static class FinishingsProbe extends Finishings {
        FinishingsProbe(int value) {
            super(value);
        }
    }

    private static class JobStateProbe extends JobState {
        JobStateProbe(int value) {
            super(value);
        }
    }

    // --- ayudantes: aislan el try/catch para que el cuerpo de run() se lea de corrido ---

    private static boolean tiraIAE(int v, String clase) {
        try {
            if (clase.equals("Copies")) new Copies(v);
            else if (clase.equals("JobImpressions")) new JobImpressions(v);
            else if (clase.equals("QueuedJobCount")) new QueuedJobCount(v);
            else if (clase.equals("NumberUp")) new NumberUp(v);
            else if (clase.equals("JobPriority")) new JobPriority(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // Segunda tanda de ayudantes. Se separan de tiraIAE() en vez de agregarle ramas para no tocar
    // una funcion de la que dependen los indices ya acordados entre las dos VMs.
    private static boolean tiraIAE2(int v, String clase) {
        try {
            if (clase.equals("JobImpressionsCompleted")) new JobImpressionsCompleted(v);
            else if (clase.equals("JobKOctetsProcessed")) new JobKOctetsProcessed(v);
            else if (clase.equals("JobMediaSheets")) new JobMediaSheets(v);
            else if (clase.equals("JobMediaSheetsCompleted")) new JobMediaSheetsCompleted(v);
            else if (clase.equals("NumberOfDocuments")) new NumberOfDocuments(v);
            else if (clase.equals("NumberOfInterveningJobs")) new NumberOfInterveningJobs(v);
            else if (clase.equals("PagesPerMinuteColor")) new PagesPerMinuteColor(v);
            else if (clase.equals("JobPrioritySupported")) new JobPrioritySupported(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jkosTiraIAE(int lo, int hi) {
        try {
            new JobKOctetsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jmssTiraIAE(int lo, int hi) {
        try {
            new JobMediaSheetsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // Un null en el constructor tiene que llegar como NullPointerException. Si la implementacion
    // se olvidara de chequearlo, el atributo quedaria construido con un campo nulo y el error
    // saldria mucho despues, en un toString() o un equals() que nadie relaciona con el origen.
    private static boolean dtsTiraNPE(String clase) {
        try {
            if (clase.equals("DateTimeAtProcessing")) new DateTimeAtProcessing(null);
            else if (clase.equals("JobHoldUntil")) new JobHoldUntil(null);
            else if (clase.equals("PrinterMoreInfoManufacturer")) {
                new PrinterMoreInfoManufacturer(null);
            } else if (clase.equals("OutputDeviceAssigned")) {
                new OutputDeviceAssigned(null, Locale.US);
            }
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean prTiraIAE(String s) {
        try {
            new PageRanges(s);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean csTiraIAE(int lo, int hi) {
        try {
            new CopiesSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jisTiraIAE(int lo, int hi) {
        try {
            new JobImpressionsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean nusTiraIAE(int v) {
        try {
            new NumberUpSupported(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean fmTiraIAE(float x, float y) {
        try {
            MediaSize.findMedia(x, y, Size2DSyntax.MM);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean msTiraIAE(int x, int y) {
        try {
            new MediaSize(x, y, Size2DSyntax.MM);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean mpaTiraIAE(int x, int y, int w, int h) {
        try {
            new MediaPrintableArea(x, y, w, h, MediaPrintableArea.INCH);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean tsTiraNPE() {
        try {
            new JobName(null, Locale.US);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean uriTiraNPE() {
        try {
            new PrinterURI(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean jsrTiraNPE(JobStateReasons jsr) {
        try {
            jsr.add(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean psrTiraNPE(PrinterStateReasons psr) {
        try {
            psr.put(PrinterStateReason.PAUSED, null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean vistaEsInmutable(Set<PrinterStateReason> vista) {
        try {
            vista.add(PrinterStateReason.PAUSED);
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
