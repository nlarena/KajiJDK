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
 * Behaviour test of javax.print.attribute.standard, written to run **the same** in this VM and in
 * the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed: a single int is enough to compare the two VMs without depending on the
 * console output matching character by character.
 *
 * <p>What is aimed at is what the JDK specifies and is easy to get wrong without it showing:
 *
 * <ul>
 * <li>the {@code toString()}s of the {@code EnumSyntax}es, which go to the character and are not
 *     the names of the constants;</li>
 * <li>the offsets of the three categories IPP numbers from 3, and the {@code null} rows the
 *     reserved values leave;</li>
 * <li>{@code getName()} and {@code getCategory()} of each family --above all that of {@code Media},
 *     which the three subclasses share, and that of {@code Destination}, whose IPP name does not
 *     look like that of the class;</li>
 * <li>the arithmetic of {@code PageRanges}: normalisation, merging of adjacent ranges,
 *     {@code next()};</li>
 * <li>the ranges that have to throw {@code IllegalArgumentException}, including the case where the
 *     error is not the value but that the set was left empty;</li>
 * <li>the tables of {@code MediaSize} and its static registry.</li>
 * </ul>
 *
 * <p>Nothing that depends on a real printer or on a locale is tested: this package is data.
 */
public class PrintStdTest {

    // A fixed instant. It is never printed --the format of Date does depend on the time zone--, it
    // is only compared.
    private static final long T0 = 1000000000L;

    public static int run() {
        int i = 0;

        // --- toString() of the EnumSyntaxes: the JDK specifies them to the character ---
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

        // --- the aliases of Sides are the same object, not new values ---
        if (Sides.DUPLEX != Sides.TWO_SIDED_LONG_EDGE) return i; i++;                  // 19
        if (Sides.TUMBLE != Sides.TWO_SIDED_SHORT_EDGE) return i; i++;                 // 20
        if (Sides.DUPLEX.getValue() != 1) return i; i++;                               // 21

        // --- the three categories IPP numbers from 3 ---
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

        // --- a value outside the table prints the bare integer ---
        // Below the offset and above the last row.
        if (!new FinishingsProbe(2).toString().equals("2")) return i; i++;              // 30
        if (!new FinishingsProbe(32).toString().equals("32")) return i; i++;            // 31
        //
        // What is NOT checked here, and it is worth knowing why: a value that falls INSIDE the
        // table but in a reserved row --Finishings 5 and 10..19, JobState 1 and 2-- gives `null` in
        // the real JDK, because its EnumSyntax.toString() does not look at whether the entry is
        // null before returning it. The EnumSyntax of KajiLibrary does look at it and falls back to
        // the integer. The divergence is in the base class, which is of another package; putting it
        // here would make the two VMs not match without the problem being of this package. It is
        // noted and not tested.
        if (!JobState.PENDING.toString().equals("pending")) return i; i++;              // 32
        if (!JobState.UNKNOWN.toString().equals("unknown")) return i; i++;             // 33
        if (JobState.COMPLETED.getValue() != 9) return i; i++;                         // 34
        if (PrinterState.STOPPED.getValue() != 5) return i; i++;                       // 35
        // A value outside the whole table also falls back to the integer.
        if (!new FinishingsProbe(99).toString().equals("99")) return i; i++;                // 36

        // --- getName(): the IPP name, which does not always look like that of the class ---
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

        // --- getCategory(): the three ways of saying "which paper" share that of Media ---
        if (MediaSizeName.ISO_A4.getCategory() != Media.class) return i; i++;          // 50
        if (MediaTray.MANUAL.getCategory() != Media.class) return i; i++;              // 51
        if (MediaName.ISO_A4_WHITE.getCategory() != Media.class) return i; i++;        // 52
        if (!MediaTray.MANUAL.getName().equals("media")) return i; i++;                // 53
        if (!MediaSizeName.ISO_A4.getName().equals("media")) return i; i++;            // 54
        // MediaSize on the other hand is its own category: they are measurements, not a choice of
        // paper.
        if (MediaSize.ISO.A4.getCategory() != MediaSize.class) return i; i++;          // 55
        if (!MediaSize.ISO.A4.getName().equals("media-size")) return i; i++;           // 56
        if (Chromaticity.COLOR.getCategory() != Chromaticity.class) return i; i++;     // 57
        if (new Copies(1).getCategory() != Copies.class) return i; i++;                // 58

        // --- Media.equals looks at the concrete class, not only at the integer ---
        // MediaTray.TOP and MediaName.NA_LETTER_WHITE are both worth zero.
        if (MediaTray.TOP.getValue() != MediaName.NA_LETTER_WHITE.getValue()) return i; i++; // 59
        if (MediaTray.TOP.equals(MediaName.NA_LETTER_WHITE)) return i; i++;            // 60
        if (!MediaTray.TOP.equals(MediaTray.TOP)) return i; i++;                       // 61

        // --- IntegerSyntax: the instanceof keeps two different attributes from coming out equal
        // ---
        if (new Copies(1).equals(new NumberUp(1))) return i; i++;                      // 62
        if (!new Copies(3).equals(new Copies(3))) return i; i++;                       // 63
        if (new Copies(3).hashCode() != 3) return i; i++;                              // 64
        if (!new Copies(7).toString().equals("7")) return i; i++;                      // 65

        // --- the legal ranges ---
        if (!throwsIAE(0, "Copies")) return i; i++;                                      // 66
        if (!throwsIAE(-1, "Copies")) return i; i++;                                     // 67
        if (throwsIAE(1, "Copies")) return i; i++;                                       // 68
        if (throwsIAE(0, "JobImpressions")) return i; i++;                               // 69
        if (!throwsIAE(-1, "JobImpressions")) return i; i++;                             // 70
        if (throwsIAE(0, "QueuedJobCount")) return i; i++;                               // 71
        if (!throwsIAE(0, "NumberUp")) return i; i++;                                    // 72
        if (throwsIAE(100, "JobPriority")) return i; i++;                                // 73
        if (!throwsIAE(101, "JobPriority")) return i; i++;                               // 74
        if (!throwsIAE(0, "JobPriority")) return i; i++;                                 // 75
        if (throwsIAE(Integer.MAX_VALUE, "Copies")) return i; i++;                       // 76
        if (!throwsIAE(Integer.MAX_VALUE, "JobPriority")) return i; i++;                 // 77

        // --- PageRanges: canonicalisation ---
        // Unordered and overlapping comes out ordered and merged.
        if (!new PageRanges("3-5,1-2").toString().equals("1-5")) return i; i++;        // 78
        if (!new PageRanges(new int[][] {{1, 5}, {2, 6}}).toString()
                .equals("1-6")) return i; i++;                                         // 79
        // Adjacent ones are merged too: 1-3 and 4-6 leave no page out.
        if (!new PageRanges("1-3,4-6").toString().equals("1-6")) return i; i++;        // 80
        if (!new PageRanges("1,3,5").toString().equals("1,3,5")) return i; i++;        // 81
        if (!new PageRanges(2, 4).toString().equals("2-4")) return i; i++;             // 82
        if (!new PageRanges(7).toString().equals("7")) return i; i++;                  // 83
        // And the canonical form is what makes equals cheap and right.
        if (!new PageRanges("3-5,1-2").equals(new PageRanges(1, 5))) return i; i++;    // 84
        if (new PageRanges(1, 5).equals(new PageRanges(1, 6))) return i; i++;          // 85

        // --- PageRanges: contains and next ---
        PageRanges pr = new PageRanges("1,3,5");
        if (!pr.contains(3)) return i; i++;                                            // 86
        if (pr.contains(2)) return i; i++;                                             // 87
        if (pr.next(1) != 3) return i; i++;                                            // 88
        if (pr.next(3) != 5) return i; i++;                                            // 89
        if (pr.next(5) != -1) return i; i++;                                           // 90
        if (pr.next(0) != 1) return i; i++;                                            // 91
        if (pr.getMembers().length != 3) return i; i++;                                // 92

        // --- PageRanges: what it rejects ---
        if (!prThrowsIAE("0")) return i; i++;                                            // 93
        // "5-1" is an empty range: the base discards it and fails for an empty set, not for the
        // value.
        if (!prThrowsIAE("5-1")) return i; i++;                                          // 94
        if (prThrowsIAE("1-3,7,10-12")) return i; i++;                                   // 95
        if (!prThrowsIAE("1,")) return i; i++;                                           // 96
        if (!prThrowsIAE("abc")) return i; i++;                                          // 97

        // --- the sets of supported values ---
        if (!new CopiesSupported(1, 5).toString().equals("1-5")) return i; i++;        // 98
        if (!new CopiesSupported(3).toString().equals("3")) return i; i++;             // 99
        if (!csThrowsIAE(0, 5)) return i; i++;                                           // 100
        if (csThrowsIAE(1, 99)) return i; i++;                                           // 101
        if (!new JobImpressionsSupported(0, 10).toString().equals("0-10")) return i; i++; // 102
        if (!jisThrowsIAE(-1, 5)) return i; i++;                                         // 103
        if (!new NumberUpSupported(new int[][] {{1, 1}, {2, 2}, {4, 4}}).toString()
                .equals("1-2,4")) return i; i++;                                       // 104
        if (!nusThrowsIAE(0)) return i; i++;                                             // 105
        if (!new NumberUpSupported(1, 3).toString().equals("1-3")) return i; i++;      // 106
        if (!new CopiesSupported(1, 5).getName().equals("copies-supported")) return i; i++; // 107
        // The instanceof separates two sets with the same numbers.
        if (new CopiesSupported(1, 5).equals(new PageRanges(1, 5))) return i; i++;     // 108

        // --- ResolutionSyntax ---
        PrinterResolution res = new PrinterResolution(300, 600, ResolutionSyntax.DPI);
        if (res.getCrossFeedResolution(ResolutionSyntax.DPI) != 300) return i; i++;    // 109
        if (res.getFeedResolution(ResolutionSyntax.DPI) != 600) return i; i++;         // 110
        if (!res.toString().equals("30000x60000 dphi")) return i; i++;                 // 111
        if (!res.toString(ResolutionSyntax.DPI, "dpi").equals("300x600 dpi")) return i; i++; // 112
        if (!res.equals(new PrinterResolution(300, 600, ResolutionSyntax.DPI))) return i; i++; // 113
        if (res.equals(new PrinterResolution(600, 300, ResolutionSyntax.DPI))) return i; i++; // 114
        // 100 dpcm are 254 dphi per unit: the internal unit makes both scales exact.
        if (new PrinterResolution(100, 100, ResolutionSyntax.DPCM)
                .getCrossFeedResolution(ResolutionSyntax.DPCM) != 100) return i; i++;  // 115

        // --- MediaSize: the standard tables ---
        if (MediaSize.ISO.A4.getX(Size2DSyntax.MM) != 210.0f) return i; i++;           // 116
        if (MediaSize.ISO.A4.getY(Size2DSyntax.MM) != 297.0f) return i; i++;           // 117
        if (MediaSize.ISO.A0.getY(Size2DSyntax.MM) != 1189.0f) return i; i++;          // 118
        // The Japanese Bs are NOT the ISO Bs.
        if (MediaSize.JIS.B4.getX(Size2DSyntax.MM) != 257.0f) return i; i++;           // 119
        if (MediaSize.ISO.B4.getX(Size2DSyntax.MM) != 250.0f) return i; i++;           // 120
        if (MediaSize.NA.LETTER.getX(Size2DSyntax.INCH) != 8.5f) return i; i++;        // 121
        if (MediaSize.NA.LETTER.getY(Size2DSyntax.INCH) != 11.0f) return i; i++;       // 122
        if (MediaSize.Engineering.E.getX(Size2DSyntax.INCH) != 34.0f) return i; i++;   // 123
        if (MediaSize.Other.INVOICE.getY(Size2DSyntax.INCH) != 8.5f) return i; i++;    // 124
        if (!MediaSize.ISO.A4.toString().equals("210000x297000 um")) return i; i++;    // 125

        // --- MediaSize: the static registry ---
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_A4) != MediaSize.ISO.A4) return i; i++; // 126
        if (MediaSize.getMediaSizeForName(MediaSizeName.NA_LETTER)
                != MediaSize.NA.LETTER) return i; i++;                                 // 127
        // ISO_C0, C1 and C2 have a name but no registered size.
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_C0) != null) return i; i++; // 128
        if (MediaSize.getMediaSizeForName(MediaSizeName.ISO_C3) == null) return i; i++; // 129
        if (MediaSize.ISO.A4.getMediaSizeName() != MediaSizeName.ISO_A4) return i; i++; // 130
        // The Japanese envelopes are in the lookup table but have no name.
        if (MediaSize.JIS.CHOU_1.getMediaSizeName() != null) return i; i++;            // 131
        // An anonymous size does not claim a name either.
        if (new MediaSize(1, 1, Size2DSyntax.INCH).getMediaSizeName() != null) return i; i++; // 132

        // --- MediaSize: findMedia chooses the nearest and never says "there is none" ---
        if (MediaSize.findMedia(210f, 297f, Size2DSyntax.MM)
                != MediaSizeName.ISO_A4) return i; i++;                                // 133
        if (MediaSize.findMedia(8.5f, 11f, Size2DSyntax.INCH)
                != MediaSizeName.NA_LETTER) return i; i++;                             // 134
        // Almost-A4 still gives A4.
        if (MediaSize.findMedia(210.5f, 297.5f, Size2DSyntax.MM)
                != MediaSizeName.ISO_A4) return i; i++;                                // 135
        if (!fmThrowsIAE(0f, 10f)) return i; i++;                                        // 136
        if (!fmThrowsIAE(10f, -1f)) return i; i++;                                       // 137
        // The paper is always declared portrait.
        if (!msThrowsIAE(2, 1)) return i; i++;                                           // 138
        if (msThrowsIAE(1, 2)) return i; i++;                                            // 139

        // --- MediaPrintableArea: micrometres inside, millimetres when printing ---
        MediaPrintableArea mpa = new MediaPrintableArea(1f, 2f, 3f, 4f,
                                                        MediaPrintableArea.INCH);
        if (!mpa.toString().equals("(25.4,50.8)->(76.2,101.6)mm")) return i; i++;      // 140
        if (mpa.getX(MediaPrintableArea.INCH) != 1.0f) return i; i++;                  // 141
        if (mpa.getWidth(MediaPrintableArea.INCH) != 3.0f) return i; i++;              // 142
        if (mpa.getPrintableArea(MediaPrintableArea.MM)[3] != 101.6f) return i; i++;   // 143
        // The unit it was built with is not kept: the same measurements come out equal.
        MediaPrintableArea oneInch = new MediaPrintableArea(1, 1, 1, 1,
                                                               MediaPrintableArea.INCH);
        MediaPrintableArea inMm = new MediaPrintableArea(25.4f, 25.4f, 25.4f, 25.4f,
                                                         MediaPrintableArea.MM);
        if (!oneInch.equals(inMm)) return i; i++;                                   // 144
        if (oneInch.hashCode() != inMm.hashCode()) return i; i++;                   // 145
        if (oneInch.hashCode() != 3251200) return i; i++;                           // 146
        if (!oneInch.toString(MediaPrintableArea.MM, null)
                .equals("(25.4,25.4)->(25.4,25.4)")) return i; i++;                    // 147
        // Width and height have to be strictly positive; the origin may be zero.
        if (!mpaThrowsIAE(0, 0, 0, 1)) return i; i++;                                    // 148
        if (!mpaThrowsIAE(-1, 0, 1, 1)) return i; i++;                                   // 149
        if (mpaThrowsIAE(0, 0, 1, 1)) return i; i++;                                     // 150

        // --- Size2DSyntax and the rounding to the micrometre ---
        if (MediaSize.Other.QUARTO.getX(Size2DSyntax.MM) != 215.9f) return i; i++;     // 151

        // --- TextSyntax ---
        JobName jn = new JobName("informe", Locale.US);
        if (!jn.toString().equals("informe")) return i; i++;                           // 152
        if (!jn.getValue().equals("informe")) return i; i++;                           // 153
        if (jn.getLocale() != Locale.US) return i; i++;                                // 154
        if (!jn.equals(new JobName("informe", Locale.US))) return i; i++;              // 155
        // The locale enters into the comparison.
        if (jn.equals(new JobName("informe", Locale.FRANCE))) return i; i++;           // 156
        // And the instanceof separates two equal texts of different categories.
        if (jn.equals(new DocumentName("informe", Locale.US))) return i; i++;          // 157
        if (!tsThrowsNPE()) return i; i++;                                               // 158
        // A null locale is not an error: it means "the one from around here".
        if (new PrinterInfo("x", null).getLocale() == null) return i; i++;             // 159

        // --- URISyntax ---
        URI u = URI.create("ipp://host/printer");
        if (!new PrinterURI(u).getURI().equals(u)) return i; i++;                      // 160
        if (!new PrinterURI(u).toString().equals("ipp://host/printer")) return i; i++; // 161
        if (new PrinterURI(u).equals(new PrinterMoreInfo(u))) return i; i++;           // 162
        if (!new PrinterURI(u).equals(new PrinterURI(URI.create("ipp://host/printer"))))
            return i; i++;                                                             // 163
        if (!uriThrowsNPE()) return i; i++;                                              // 164

        // --- DateTimeSyntax: a copy on the way in and on the way out ---
        DateTimeAtCompleted dtc = new DateTimeAtCompleted(new Date(T0));
        if (dtc.getValue().getTime() != T0) return i; i++;                             // 165
        if (dtc.getValue() == dtc.getValue()) return i; i++;                           // 166
        //
        // What is NOT checked: whether mutating the original Date changes the attribute. The real
        // JDK keeps the reference and only copies on the way out, so a later `d.setTime(0)` IS seen
        // from getValue(); the DateTimeSyntax of KajiLibrary copies on the way in as well and it is
        // not seen. The divergence is in the base class, which is of another package. It is noted
        // and not tested.
        if (dtc.getValue().getTime() != T0) return i; i++;                             // 167
        if (!dtc.equals(new DateTimeAtCompleted(new Date(T0)))) return i; i++;         // 168
        if (dtc.equals(new DateTimeAtCreation(new Date(T0)))) return i; i++;           // 169

        // --- JobStateReasons: it is a set, and it rejects null ---
        JobStateReasons jsr = new JobStateReasons();
        if (jsr.size() != 0) return i; i++;                                            // 170
        jsr.add(JobStateReason.JOB_PRINTING);
        jsr.add(JobStateReason.JOB_PRINTING);
        if (jsr.size() != 1) return i; i++;                                            // 171
        if (!jsr.contains(JobStateReason.JOB_PRINTING)) return i; i++;                 // 172
        if (jsr.contains(JobStateReason.JOB_QUEUED)) return i; i++;                    // 173
        if (!jsrThrowsNPE(jsr)) return i; i++;                                           // 174
        if (jsr.getCategory() != JobStateReasons.class) return i; i++;                 // 175

        // --- PrinterStateReasons: the map and the view by severity ---
        PrinterStateReasons psr = new PrinterStateReasons();
        psr.put(PrinterStateReason.MEDIA_JAM, Severity.ERROR);
        psr.put(PrinterStateReason.TONER_LOW, Severity.WARNING);
        psr.put(PrinterStateReason.MEDIA_LOW, Severity.WARNING);
        if (psr.size() != 3) return i; i++;                                            // 176
        if (psr.get(PrinterStateReason.MEDIA_JAM) != Severity.ERROR) return i; i++;    // 177
        Set<PrinterStateReason> errors = psr.printerStateReasonSet(Severity.ERROR);
        if (errors.size() != 1) return i; i++;                                        // 178
        if (!errors.contains(PrinterStateReason.MEDIA_JAM)) return i; i++;            // 179
        if (psr.printerStateReasonSet(Severity.WARNING).size() != 2) return i; i++;    // 180
        if (psr.printerStateReasonSet(Severity.REPORT).size() != 0) return i; i++;     // 181
        // It is a live view: whatever is added afterwards is seen.
        psr.put(PrinterStateReason.COVER_OPEN, Severity.ERROR);
        if (errors.size() != 2) return i; i++;                                        // 182
        // And read-only.
        if (!viewIsImmutable(errors)) return i; i++;                                 // 183
        if (!psrThrowsNPE(psr)) return i; i++;                                           // 184

        // --- Attribute: they all answer both questions ---
        Attribute[] all = {
            Chromaticity.COLOR, new Copies(1), new PageRanges(1),
            new PrinterResolution(300, 300, ResolutionSyntax.DPI), MediaSizeName.ISO_A4,
            new JobName("x", Locale.US), new PrinterURI(u), new DateTimeAtCreation(new Date(T0)),
            new JobStateReasons(), new PrinterStateReasons(), new DialogOwner(),
            new MediaPrintableArea(0, 0, 1, 1, MediaPrintableArea.INCH), MediaSize.ISO.A4,
            new PagesPerMinute(0), new QueuedJobCount(0), new RequestingUserName("u", Locale.US),
            new CopiesSupported(1), OutputBin.TOP, JobState.PENDING, PrinterState.IDLE,
        };
        for (int k = 0; k < all.length; k++) {
            if (all[k].getName() == null) return i;
            if (all[k].getCategory() == null) return i;
        }
        i++;                                                                           // 185

        // ------------------------------------------------------------------------------------
        // Second batch: the classes the first one did not touch.
        //
        // They are the nineteen "boring" wrappers --IntegerSyntax, TextSyntax, DateTimeSyntax and
        // SetOfIntegerSyntax with another name-- and precisely because they are boring they are the
        // easiest to get wrong without it showing: the only thing of each one's own is the IPP
        // name, the category and the range it accepts. Each expected value here comes from running
        // a probe against the real JDK, not from deducing it from the name of the class.
        // ------------------------------------------------------------------------------------

        // --- getName(): the IPP name is not deduced by splitting the name of the class ---
        // "job-k-octets-processed" puts in hyphens the class does not suggest; the rest follow the
        // rule but none guarantees it, so the nineteen go written out.
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

        // --- getCategory(): each one is its own category --- It matters that it be the concrete
        // class and not the superclass of syntax: if any returned IntegerSyntax.class, two
        // different attributes would step on each other inside an AttributeSet.
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

        // --- value and toString() --- The DateTimeSyntaxes are not compared by toString: the
        // format of Date depends on the time zone and the two VMs have no reason to run in the same
        // one. The instant is compared.
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

        // The two SetOfIntegerSyntaxes of this batch: a single range prints "lo-hi".
        if (!new JobKOctetsSupported(0, 5).toString().equals("0-5")) return i; i++;    // 238
        if (!new JobMediaSheetsSupported(1, 9).toString().equals("1-9")) return i; i++; // 239
        if (!new JobMediaSheetsSupported(1, 9).contains(5)) return i; i++;             // 240
        if (new JobMediaSheetsSupported(1, 9).contains(10)) return i; i++;             // 241
        if (new JobKOctetsSupported(0, 5).getMembers().length != 1) return i; i++;     // 242
        if (new JobKOctetsSupported(0, 5).getMembers()[0][0] != 0) return i; i++;      // 243
        if (new JobKOctetsSupported(0, 5).getMembers()[0][1] != 5) return i; i++;      // 244

        // --- equals(): the value alone is not enough, the class counts too --- Two IntegerSyntaxes
        // with the same integer but of different categories are NOT equal, and the same with two
        // TextSyntaxes with the same text. It is what keeps an AttributeSet from confusing "number
        // of documents" with "pages per minute in colour".
        if (new PagesPerMinuteColor(7).equals(new NumberOfDocuments(7))) return i; i++; // 245
        if (new PrinterLocation("a", Locale.US)
                .equals(new JobOriginatingUserName("a", Locale.US))) return i; i++;    // 246
        if (!new JobOriginatingUserName("pepe", Locale.US)
                .equals(new JobOriginatingUserName("pepe", Locale.US))) return i; i++; // 247
        // The locale is part of the identity of a TextSyntax: the same text, a different locale, a
        // different attribute.
        if (new PrinterLocation("a", Locale.US)
                .equals(new PrinterLocation("a", Locale.FRANCE))) return i; i++;       // 248
        if (!new JobHoldUntil(d0).equals(new JobHoldUntil(new Date(T0)))) return i; i++; // 249

        // --- the ranges that have to throw IllegalArgumentException ---
        // All these counters are "non-negative", not "positive": 0 is valid and -1 is not.
        if (!throwsIAE2(-1, "JobImpressionsCompleted")) return i; i++;                   // 250
        if (!throwsIAE2(-1, "JobKOctetsProcessed")) return i; i++;                       // 251
        if (!throwsIAE2(-1, "JobMediaSheets")) return i; i++;                            // 252
        if (!throwsIAE2(-1, "JobMediaSheetsCompleted")) return i; i++;                   // 253
        if (!throwsIAE2(-1, "NumberOfDocuments")) return i; i++;                         // 254
        if (!throwsIAE2(-1, "NumberOfInterveningJobs")) return i; i++;                   // 255
        if (!throwsIAE2(-1, "PagesPerMinuteColor")) return i; i++;                       // 256
        if (throwsIAE2(0, "NumberOfDocuments")) return i; i++;                           // 257
        // JobPrioritySupported is the exception: the range is 1..100 closed at both ends.
        if (!throwsIAE2(0, "JobPrioritySupported")) return i; i++;                       // 258
        if (!throwsIAE2(101, "JobPrioritySupported")) return i; i++;                     // 259
        if (throwsIAE2(1, "JobPrioritySupported")) return i; i++;                        // 260
        if (throwsIAE2(100, "JobPrioritySupported")) return i; i++;                      // 261

        // The two ranges: the lower limit cannot be negative or exceed the upper one.
        if (!jkosThrowsIAE(-1, 5)) return i; i++;                                        // 262
        if (!jkosThrowsIAE(5, 1)) return i; i++;                                         // 263
        if (jkosThrowsIAE(0, 0)) return i; i++;                                          // 264
        if (!jmssThrowsIAE(-1, 5)) return i; i++;                                        // 265
        if (!jmssThrowsIAE(5, 1)) return i; i++;                                         // 266
        // Careful with this one: although "sheets" suggests that the minimum is 1, the JDK accepts
        // 0. It is written down so that nobody "fixes" it by hand and departs from the original.
        if (jmssThrowsIAE(0, 5)) return i; i++;                                          // 267

        // --- null: NPE, not IAE and not an attribute with a null field inside ---
        if (!dtsThrowsNPE("DateTimeAtProcessing")) return i; i++;                        // 268
        if (!dtsThrowsNPE("JobHoldUntil")) return i; i++;                                // 269
        if (!dtsThrowsNPE("PrinterMoreInfoManufacturer")) return i; i++;                 // 270
        if (!dtsThrowsNPE("OutputDeviceAssigned")) return i; i++;                        // 271

        // --- and the nineteen also answer both questions of Attribute ---
        Attribute[] all2 = {
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
        if (all2.length != 19) return i; i++;                                        // 272
        for (int k = 0; k < all2.length; k++) {
            if (all2[k].getName() == null) return i;
            if (all2[k].getCategory() == null) return i;
        }
        i++;                                                                           // 273

        return -1;
    }

    // The constructors of EnumSyntax are protected --they are singletons-- so in order to make a
    // reserved value, which is just what is needed in order to see the toString() of a null row,
    // one has to go through a subclass. The two categories that have reserved rows are not final
    // precisely because IPP lets a site add values.
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

    // --- helpers: they isolate the try/catch so that the body of run() reads straight through ---

    private static boolean throwsIAE(int v, String cls) {
        try {
            if (cls.equals("Copies")) new Copies(v);
            else if (cls.equals("JobImpressions")) new JobImpressions(v);
            else if (cls.equals("QueuedJobCount")) new QueuedJobCount(v);
            else if (cls.equals("NumberUp")) new NumberUp(v);
            else if (cls.equals("JobPriority")) new JobPriority(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // A second batch of helpers. They are kept apart from throwsIAE() instead of adding branches to
    // it so as not to touch a function the indices already agreed between the two VMs depend on.
    private static boolean throwsIAE2(int v, String cls) {
        try {
            if (cls.equals("JobImpressionsCompleted")) new JobImpressionsCompleted(v);
            else if (cls.equals("JobKOctetsProcessed")) new JobKOctetsProcessed(v);
            else if (cls.equals("JobMediaSheets")) new JobMediaSheets(v);
            else if (cls.equals("JobMediaSheetsCompleted")) new JobMediaSheetsCompleted(v);
            else if (cls.equals("NumberOfDocuments")) new NumberOfDocuments(v);
            else if (cls.equals("NumberOfInterveningJobs")) new NumberOfInterveningJobs(v);
            else if (cls.equals("PagesPerMinuteColor")) new PagesPerMinuteColor(v);
            else if (cls.equals("JobPrioritySupported")) new JobPrioritySupported(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jkosThrowsIAE(int lo, int hi) {
        try {
            new JobKOctetsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jmssThrowsIAE(int lo, int hi) {
        try {
            new JobMediaSheetsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    // A null in the constructor has to arrive as a NullPointerException. If the implementation
    // forgot to check it, the attribute would be left built with a null field and the error would
    // come out much later, in a toString() or an equals() nobody relates to the origin.
    private static boolean dtsThrowsNPE(String cls) {
        try {
            if (cls.equals("DateTimeAtProcessing")) new DateTimeAtProcessing(null);
            else if (cls.equals("JobHoldUntil")) new JobHoldUntil(null);
            else if (cls.equals("PrinterMoreInfoManufacturer")) {
                new PrinterMoreInfoManufacturer(null);
            } else if (cls.equals("OutputDeviceAssigned")) {
                new OutputDeviceAssigned(null, Locale.US);
            }
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean prThrowsIAE(String s) {
        try {
            new PageRanges(s);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean csThrowsIAE(int lo, int hi) {
        try {
            new CopiesSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean jisThrowsIAE(int lo, int hi) {
        try {
            new JobImpressionsSupported(lo, hi);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean nusThrowsIAE(int v) {
        try {
            new NumberUpSupported(v);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean fmThrowsIAE(float x, float y) {
        try {
            MediaSize.findMedia(x, y, Size2DSyntax.MM);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean msThrowsIAE(int x, int y) {
        try {
            new MediaSize(x, y, Size2DSyntax.MM);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean mpaThrowsIAE(int x, int y, int w, int h) {
        try {
            new MediaPrintableArea(x, y, w, h, MediaPrintableArea.INCH);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean tsThrowsNPE() {
        try {
            new JobName(null, Locale.US);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean uriThrowsNPE() {
        try {
            new PrinterURI(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean jsrThrowsNPE(JobStateReasons jsr) {
        try {
            jsr.add(null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean psrThrowsNPE(PrinterStateReasons psr) {
        try {
            psr.put(PrinterStateReason.PAUSED, null);
            return false;
        } catch (NullPointerException e) {
            return true;
        }
    }

    private static boolean viewIsImmutable(Set<PrinterStateReason> view) {
        try {
            view.add(PrinterStateReason.PAUSED);
            return false;
        } catch (UnsupportedOperationException e) {
            return true;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
