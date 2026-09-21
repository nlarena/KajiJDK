package javax.print.attribute.standard;

import java.util.HashMap;
import java.util.Vector;
import javax.print.attribute.Attribute;
import javax.print.attribute.Size2DSyntax;

/**
 * The measures of a paper size, and the registry connecting them to the {@link MediaSizeName}
 * names.
 *
 * <p>The class itself is a {@link Size2DSyntax} with a restriction --{@code x} cannot be greater
 * than {@code y}, that is the paper is always declared portrait-- and an optional name. What makes
 * it different from the rest of the package is the <b>static registry</b>: the nested classes below
 * declare the ninety-four standard sizes, and their constructors keep noting them down in two
 * tables.
 *
 * <ul>
 * <li>A name {@code ->} measures map, which is the one {@link #getMediaSizeForName} answers from.
 *     Only the ones declared with a {@link MediaSizeName} go in, and only the first to claim each
 *     name.</li>
 * <li>A list of all, even the anonymous ones, which is the one {@link #findMedia} searches.</li>
 * </ul>
 *
 * <p>That the registry is filled from the constructors has a consequence: the nested classes have
 * to be forced to load before answering either of the two queries, because if nobody touched {@code
 * MediaSize.ISO} yet, its constants do not exist and the tables are empty. The static block at the
 * end takes care of that. The recursion that sets up --{@code ISO} initializes {@code MediaSize},
 * which touches {@code ISO}-- is legal and the JVM cuts it by itself: the second access from the
 * same thread passes straight through.
 *
 * <p>These tables are <b>standards data</b> (ISO 216, JIS P 0138, ANSI), not locale data. They
 * depend neither on CLDR nor on any printer, so they go in complete and with the exact numbers.
 *
 * <p>That a size is here does not mean any printer has it loaded: {@code javax.print} answers that,
 * not this package.
 */
public class MediaSize extends Size2DSyntax implements Attribute {

    private static final long serialVersionUID = -1967958664615414771L;

    private MediaSizeName mediaName;

    // The registry's two tables. They are initialized up here, before the static block at the end,
    // because the nested classes' constructors write into them while that block runs.
    private static HashMap<MediaSizeName, MediaSize> mediaMap = new HashMap<MediaSizeName, MediaSize>(100, 10);

    private static Vector<MediaSize> sizeVector = new Vector<MediaSize>(100, 10);

    /** An anonymous size: it goes into {@link #findMedia}'s search but claims no name. */
    public MediaSize(float x, float y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        sizeVector.add(this);
    }

    public MediaSize(int x, int y, int units) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        sizeVector.add(this);
    }

    /**
     * A named size. If that name was already taken the object is built all the same but is not
     * registered anywhere --not even in the search list-- and its {@link #getMediaSizeName} is left
     * null: the first to claim a name keeps it.
     */
    public MediaSize(float x, float y, int units, MediaSizeName media) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        if (media != null && mediaMap.get(media) == null) {
            this.mediaName = media;
            mediaMap.put(media, this);
            sizeVector.add(this);
        }
    }

    public MediaSize(int x, int y, int units, MediaSizeName media) {
        super(x, y, units);
        if (x > y) {
            throw new IllegalArgumentException("X dimension > Y dimension");
        }
        if (media != null && mediaMap.get(media) == null) {
            this.mediaName = media;
            mediaMap.put(media, this);
            sizeVector.add(this);
        }
    }

    /** The name it was registered with, or null if it is anonymous. */
    public MediaSizeName getMediaSizeName() {
        return this.mediaName;
    }

    /** A name's measures, or null if that name has none registered. */
    public static MediaSize getMediaSizeForName(MediaSizeName media) {
        return mediaMap.get(media);
    }

    /**
     * The name of the registered size closest to the given measures.
     *
     * <p>It never returns "not found": it chooses the one at the <b>smallest Euclidean distance</b>
     * between the two pairs of measures and stops as soon as it finds an exact match. Two
     * consequences worth knowing before trusting the result: absurd measures still return
     * something, and the result may be {@code null} without anything having failed --if the closest
     * turns out to be one of the Japanese envelopes, which have no name.
     *
     * <p>The initial candidate is A4, so with the list empty that is what comes out.
     */
    public static MediaSizeName findMedia(float x, float y, int units) {
        MediaSize match = MediaSize.ISO.A4;
        if (x <= 0.0f || y <= 0.0f || units < 1) {
            throw new IllegalArgumentException("args must be +ve values");
        }
        double ls = x * x + y * y;
        for (int i = 0; i < sizeVector.size(); i++) {
            MediaSize mediaSize = sizeVector.elementAt(i);
            float[] dim = mediaSize.getSize(units);
            if (x == dim[0] && y == dim[1]) {
                match = mediaSize;
                break;
            }
            double diffx = x - dim[0];
            double diffy = y - dim[1];
            double tmpLs = diffx * diffx + diffy * diffy;
            if (tmpLs < ls) {
                ls = tmpLs;
                match = mediaSize;
            }
        }
        return match.getMediaSizeName();
    }

    /** Same measures and also being a {@code MediaSize}: the name does not enter the comparison. */
    public boolean equals(Object object) {
        return super.equals(object) && object instanceof MediaSize;
    }

    public final Class<? extends Attribute> getCategory() {
        return MediaSize.class;
    }

    public final String getName() {
        return "media-size";
    }

    /**
     * The sizes of the ISO 216 standard: the A, B and C series plus the DL envelope.
     *
     * <p>The whole A series comes from halving A0 --one square metre-- along the long side, again
     * and again, with the square-root-of-two proportion that makes half a sheet similar to the
     * sheet. The B series are the geometric means between two consecutive As and the C, the
     * envelopes for putting an A in without folding it. The numbers go in millimetres because that
     * is how the standard defines them.
     *
     * <p>{@code C0}, {@code C1} and {@code C2} exist as {@link MediaSizeName}s but have no constant
     * here: nobody prints on a one-metre envelope.
     */
    public static final class ISO {

        private ISO() {
        }

        public static final MediaSize A0 =
            new MediaSize(841, 1189, Size2DSyntax.MM, MediaSizeName.ISO_A0);

        public static final MediaSize A1 =
            new MediaSize(594, 841, Size2DSyntax.MM, MediaSizeName.ISO_A1);

        public static final MediaSize A2 =
            new MediaSize(420, 594, Size2DSyntax.MM, MediaSizeName.ISO_A2);

        public static final MediaSize A3 =
            new MediaSize(297, 420, Size2DSyntax.MM, MediaSizeName.ISO_A3);

        public static final MediaSize A4 =
            new MediaSize(210, 297, Size2DSyntax.MM, MediaSizeName.ISO_A4);

        public static final MediaSize A5 =
            new MediaSize(148, 210, Size2DSyntax.MM, MediaSizeName.ISO_A5);

        public static final MediaSize A6 =
            new MediaSize(105, 148, Size2DSyntax.MM, MediaSizeName.ISO_A6);

        public static final MediaSize A7 =
            new MediaSize(74, 105, Size2DSyntax.MM, MediaSizeName.ISO_A7);

        public static final MediaSize A8 =
            new MediaSize(52, 74, Size2DSyntax.MM, MediaSizeName.ISO_A8);

        public static final MediaSize A9 =
            new MediaSize(37, 52, Size2DSyntax.MM, MediaSizeName.ISO_A9);

        public static final MediaSize A10 =
            new MediaSize(26, 37, Size2DSyntax.MM, MediaSizeName.ISO_A10);

        public static final MediaSize B0 =
            new MediaSize(1000, 1414, Size2DSyntax.MM, MediaSizeName.ISO_B0);

        public static final MediaSize B1 =
            new MediaSize(707, 1000, Size2DSyntax.MM, MediaSizeName.ISO_B1);

        public static final MediaSize B2 =
            new MediaSize(500, 707, Size2DSyntax.MM, MediaSizeName.ISO_B2);

        public static final MediaSize B3 =
            new MediaSize(353, 500, Size2DSyntax.MM, MediaSizeName.ISO_B3);

        public static final MediaSize B4 =
            new MediaSize(250, 353, Size2DSyntax.MM, MediaSizeName.ISO_B4);

        public static final MediaSize B5 =
            new MediaSize(176, 250, Size2DSyntax.MM, MediaSizeName.ISO_B5);

        public static final MediaSize B6 =
            new MediaSize(125, 176, Size2DSyntax.MM, MediaSizeName.ISO_B6);

        public static final MediaSize B7 =
            new MediaSize(88, 125, Size2DSyntax.MM, MediaSizeName.ISO_B7);

        public static final MediaSize B8 =
            new MediaSize(62, 88, Size2DSyntax.MM, MediaSizeName.ISO_B8);

        public static final MediaSize B9 =
            new MediaSize(44, 62, Size2DSyntax.MM, MediaSizeName.ISO_B9);

        public static final MediaSize B10 =
            new MediaSize(31, 44, Size2DSyntax.MM, MediaSizeName.ISO_B10);

        public static final MediaSize C3 =
            new MediaSize(324, 458, Size2DSyntax.MM, MediaSizeName.ISO_C3);

        public static final MediaSize C4 =
            new MediaSize(229, 324, Size2DSyntax.MM, MediaSizeName.ISO_C4);

        public static final MediaSize C5 =
            new MediaSize(162, 229, Size2DSyntax.MM, MediaSizeName.ISO_C5);

        public static final MediaSize C6 =
            new MediaSize(114, 162, Size2DSyntax.MM, MediaSizeName.ISO_C6);

        public static final MediaSize DESIGNATED_LONG =
            new MediaSize(110, 220, Size2DSyntax.MM, MediaSizeName.ISO_DESIGNATED_LONG);
    }

    /**
     * The Japanese sizes of the JIS P 0138 standard.
     *
     * <p>The Japanese Bs are <b>not</b> ISO's Bs: JIS defines them as the arithmetic mean between
     * two As and not the geometric one, so JIS B4 measures 257x364 and not 250x353. It is the kind
     * of detail that makes a job fail silently if one confuses the tables.
     *
     * <p>The CHOU, KAKU and YOU series are envelopes, and they are the package's only entries
     * without a {@link MediaSizeName}: {@code getMediaSizeName()} returns {@code null} for them,
     * although {@link MediaSize#findMedia} still considers them.
     */
    public static final class JIS {

        private JIS() {
        }

        public static final MediaSize B0 =
            new MediaSize(1030, 1456, Size2DSyntax.MM, MediaSizeName.JIS_B0);

        public static final MediaSize B1 =
            new MediaSize(728, 1030, Size2DSyntax.MM, MediaSizeName.JIS_B1);

        public static final MediaSize B2 =
            new MediaSize(515, 728, Size2DSyntax.MM, MediaSizeName.JIS_B2);

        public static final MediaSize B3 =
            new MediaSize(364, 515, Size2DSyntax.MM, MediaSizeName.JIS_B3);

        public static final MediaSize B4 =
            new MediaSize(257, 364, Size2DSyntax.MM, MediaSizeName.JIS_B4);

        public static final MediaSize B5 =
            new MediaSize(182, 257, Size2DSyntax.MM, MediaSizeName.JIS_B5);

        public static final MediaSize B6 =
            new MediaSize(128, 182, Size2DSyntax.MM, MediaSizeName.JIS_B6);

        public static final MediaSize B7 =
            new MediaSize(91, 128, Size2DSyntax.MM, MediaSizeName.JIS_B7);

        public static final MediaSize B8 =
            new MediaSize(64, 91, Size2DSyntax.MM, MediaSizeName.JIS_B8);

        public static final MediaSize B9 =
            new MediaSize(45, 64, Size2DSyntax.MM, MediaSizeName.JIS_B9);

        public static final MediaSize B10 =
            new MediaSize(32, 45, Size2DSyntax.MM, MediaSizeName.JIS_B10);

        public static final MediaSize CHOU_1 =
            new MediaSize(142, 332, Size2DSyntax.MM);

        public static final MediaSize CHOU_2 =
            new MediaSize(119, 277, Size2DSyntax.MM);

        public static final MediaSize CHOU_3 =
            new MediaSize(120, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_4 =
            new MediaSize(90, 205, Size2DSyntax.MM);

        public static final MediaSize CHOU_30 =
            new MediaSize(92, 235, Size2DSyntax.MM);

        public static final MediaSize CHOU_40 =
            new MediaSize(90, 225, Size2DSyntax.MM);

        public static final MediaSize KAKU_0 =
            new MediaSize(287, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_1 =
            new MediaSize(270, 382, Size2DSyntax.MM);

        public static final MediaSize KAKU_2 =
            new MediaSize(240, 332, Size2DSyntax.MM);

        public static final MediaSize KAKU_3 =
            new MediaSize(216, 277, Size2DSyntax.MM);

        public static final MediaSize KAKU_4 =
            new MediaSize(197, 267, Size2DSyntax.MM);

        public static final MediaSize KAKU_5 =
            new MediaSize(190, 240, Size2DSyntax.MM);

        public static final MediaSize KAKU_6 =
            new MediaSize(162, 229, Size2DSyntax.MM);

        public static final MediaSize KAKU_7 =
            new MediaSize(142, 205, Size2DSyntax.MM);

        public static final MediaSize KAKU_8 =
            new MediaSize(119, 197, Size2DSyntax.MM);

        public static final MediaSize KAKU_20 =
            new MediaSize(229, 324, Size2DSyntax.MM);

        public static final MediaSize KAKU_A4 =
            new MediaSize(228, 312, Size2DSyntax.MM);

        public static final MediaSize YOU_1 =
            new MediaSize(120, 176, Size2DSyntax.MM);

        public static final MediaSize YOU_2 =
            new MediaSize(114, 162, Size2DSyntax.MM);

        public static final MediaSize YOU_3 =
            new MediaSize(98, 148, Size2DSyntax.MM);

        public static final MediaSize YOU_4 =
            new MediaSize(105, 235, Size2DSyntax.MM);

        public static final MediaSize YOU_5 =
            new MediaSize(95, 217, Size2DSyntax.MM);

        public static final MediaSize YOU_6 =
            new MediaSize(98, 190, Size2DSyntax.MM);

        public static final MediaSize YOU_7 =
            new MediaSize(92, 165, Size2DSyntax.MM);
    }

    /**
     * The North American sizes, including the numbered envelopes.
     *
     * <p>They are defined in inches and that is why they are declared in inches: {@code 8.5x11}
     * gives exactly 215900x279400 micrometres, whereas writing it in millimetres would lose tenths.
     *
     * <p>The constants' names have an inconsistent capital X --{@code NA_9x11_ENVELOPE} against
     * {@code NA_10X15_ENVELOPE}-- because that is how they are in the JDK and changing it would
     * break the code that uses them.
     */
    public static final class NA {

        private NA() {
        }

        public static final MediaSize LETTER =
            new MediaSize(8.5f, 11.0f, Size2DSyntax.INCH, MediaSizeName.NA_LETTER);

        public static final MediaSize LEGAL =
            new MediaSize(8.5f, 14.0f, Size2DSyntax.INCH, MediaSizeName.NA_LEGAL);

        public static final MediaSize NA_5X7 =
            new MediaSize(5f, 7f, Size2DSyntax.INCH, MediaSizeName.NA_5X7);

        public static final MediaSize NA_8X10 =
            new MediaSize(8f, 10f, Size2DSyntax.INCH, MediaSizeName.NA_8X10);

        public static final MediaSize NA_NUMBER_9_ENVELOPE =
            new MediaSize(3.875f, 8.875f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_9_ENVELOPE);

        public static final MediaSize NA_NUMBER_10_ENVELOPE =
            new MediaSize(4.125f, 9.5f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_10_ENVELOPE);

        public static final MediaSize NA_NUMBER_11_ENVELOPE =
            new MediaSize(4.5f, 10.375f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_11_ENVELOPE);

        public static final MediaSize NA_NUMBER_12_ENVELOPE =
            new MediaSize(4.75f, 11.0f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_12_ENVELOPE);

        public static final MediaSize NA_NUMBER_14_ENVELOPE =
            new MediaSize(5f, 11.5f, Size2DSyntax.INCH, MediaSizeName.NA_NUMBER_14_ENVELOPE);

        public static final MediaSize NA_6X9_ENVELOPE =
            new MediaSize(6f, 9f, Size2DSyntax.INCH, MediaSizeName.NA_6X9_ENVELOPE);

        public static final MediaSize NA_7X9_ENVELOPE =
            new MediaSize(7f, 9f, Size2DSyntax.INCH, MediaSizeName.NA_7X9_ENVELOPE);

        public static final MediaSize NA_9x11_ENVELOPE =
            new MediaSize(9f, 11f, Size2DSyntax.INCH, MediaSizeName.NA_9X11_ENVELOPE);

        public static final MediaSize NA_9x12_ENVELOPE =
            new MediaSize(9f, 12f, Size2DSyntax.INCH, MediaSizeName.NA_9X12_ENVELOPE);

        public static final MediaSize NA_10x13_ENVELOPE =
            new MediaSize(10f, 13f, Size2DSyntax.INCH, MediaSizeName.NA_10X13_ENVELOPE);

        public static final MediaSize NA_10x14_ENVELOPE =
            new MediaSize(10f, 14f, Size2DSyntax.INCH, MediaSizeName.NA_10X14_ENVELOPE);

        public static final MediaSize NA_10X15_ENVELOPE =
            new MediaSize(10f, 15f, Size2DSyntax.INCH, MediaSizeName.NA_10X15_ENVELOPE);
    }

    /**
     * ANSI's five engineering drawing sizes, from A to E.
     *
     * <p>Each is double the previous one turned ninety degrees, starting from letter: A is 8.5x11,
     * B is 11x17, and so on. Mind the name clash: this {@code A} is North American letter and has
     * nothing to do with {@code ISO.A4}.
     */
    public static final class Engineering {

        private Engineering() {
        }

        public static final MediaSize A =
            new MediaSize(8.5f, 11f, Size2DSyntax.INCH, MediaSizeName.A);

        public static final MediaSize B =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.B);

        public static final MediaSize C =
            new MediaSize(17f, 22f, Size2DSyntax.INCH, MediaSizeName.C);

        public static final MediaSize D =
            new MediaSize(22f, 34f, Size2DSyntax.INCH, MediaSizeName.D);

        public static final MediaSize E =
            new MediaSize(34f, 44f, Size2DSyntax.INCH, MediaSizeName.E);
    }

    /**
     * The ones that fit in no standard: the inherited office formats and some loose envelopes.
     *
     * <p>{@code LEDGER} and {@code TABLOID} measure exactly the same (11x17 inches) and are two
     * different constants because the name says the orientation each is used in --that is why
     * {@link MediaSize#findMedia} with those measures returns one of the two and not the other.
     */
    public static final class Other {

        private Other() {
        }

        public static final MediaSize EXECUTIVE =
            new MediaSize(7.25f, 10.5f, Size2DSyntax.INCH, MediaSizeName.EXECUTIVE);

        public static final MediaSize LEDGER =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.LEDGER);

        public static final MediaSize TABLOID =
            new MediaSize(11f, 17f, Size2DSyntax.INCH, MediaSizeName.TABLOID);

        public static final MediaSize INVOICE =
            new MediaSize(5.5f, 8.5f, Size2DSyntax.INCH, MediaSizeName.INVOICE);

        public static final MediaSize FOLIO =
            new MediaSize(8.5f, 13f, Size2DSyntax.INCH, MediaSizeName.FOLIO);

        public static final MediaSize QUARTO =
            new MediaSize(8.5f, 10.83f, Size2DSyntax.INCH, MediaSizeName.QUARTO);

        public static final MediaSize ITALY_ENVELOPE =
            new MediaSize(110f, 230f, Size2DSyntax.MM, MediaSizeName.ITALY_ENVELOPE);

        public static final MediaSize MONARCH_ENVELOPE =
            new MediaSize(3.87f, 7.5f, Size2DSyntax.INCH, MediaSizeName.MONARCH_ENVELOPE);

        public static final MediaSize PERSONAL_ENVELOPE =
            new MediaSize(3.625f, 6.5f, Size2DSyntax.INCH, MediaSizeName.PERSONAL_ENVELOPE);

        public static final MediaSize JAPANESE_POSTCARD =
            new MediaSize(100f, 148f, Size2DSyntax.MM, MediaSizeName.JAPANESE_POSTCARD);

        public static final MediaSize JAPANESE_DOUBLE_POSTCARD =
            new MediaSize(148f, 200f, Size2DSyntax.MM, MediaSizeName.JAPANESE_DOUBLE_POSTCARD);
    }

    // Force the five nested classes to load. Without this, asking about a size before having named
    // any of them would answer against empty tables. Referencing one is enough: each drags in
    // MediaSize, which drags in the next.
    static {
        MediaSize[] force = {ISO.A4, JIS.B4, NA.LETTER, Engineering.A, Other.EXECUTIVE};
    }
}
