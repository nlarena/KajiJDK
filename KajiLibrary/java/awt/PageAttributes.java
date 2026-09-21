package java.awt;

import java.util.Locale;

/**
 * What the printed page is like: colour or black and white, which paper, in which orientation and
 * at which resolution.
 *
 * <p>The counterpart of {@link JobAttributes}: that one describes the **job** --how many copies,
 * which pages-- and this one each **page**. Like that one, it is only configuration: it draws
 * nothing and asks no printer anything. What it does do is reject impossible values when they are
 * set.
 *
 * <h2>The default paper depends on the country</h2>
 *
 * <p>{@link MediaType#NA_LETTER} in the United States and Canada, {@link MediaType#ISO_A4}
 * everywhere else. Checked against JDK 25 country by country: only those two give letter. It is not
 * a decision of this library --it is what the JDK does-- and it is taken from `Locale.getDefault()`
 * at the moment the object is built, so changing the locale afterwards does not change it.
 *
 * <h2>The resolution is three numbers</h2>
 *
 * <p>`{x, y, unit}`, and the unit is **3 for dots per inch and 4 for dots per centimetre** — the
 * codes IPP uses. It is the part that surprises: an array of two numbers is no good, and one with
 * any other unit is not either. {@link #setPrinterResolution(int)} exists for the normal case,
 * which is the same value on both axes and in dots per inch.
 */
public final class PageAttributes implements Cloneable {

    /** Whether it is printed in colour or in black and white. */
    public static final class ColorType extends AttributeValue {

        private static final int I_COLOR = 0;
        private static final int I_MONOCHROME = 1;

        private static final String[] NAMES = { "color", "monochrome" };

        /** In colour. */
        public static final ColorType COLOR = new ColorType(I_COLOR);
        /** In black and white. */
        public static final ColorType MONOCHROME = new ColorType(I_MONOCHROME);

        private ColorType(int type) {
            super(type, NAMES);
        }
    }

    /** Whether the page goes landscape or portrait. */
    public static final class OrientationRequestedType extends AttributeValue {

        private static final int I_PORTRAIT = 0;
        private static final int I_LANDSCAPE = 1;

        private static final String[] NAMES = { "portrait", "landscape" };

        /** Vertical. */
        public static final OrientationRequestedType PORTRAIT =
                new OrientationRequestedType(I_PORTRAIT);
        /** Apaisada. */
        public static final OrientationRequestedType LANDSCAPE =
                new OrientationRequestedType(I_LANDSCAPE);

        private OrientationRequestedType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * Where the coordinates of the page are measured from.
     *
     * <p>The difference matters when positioning: {@link #PHYSICAL} measures from the corner of the
     * paper and {@link #PRINTABLE} from where the printer **can** print, which is further in by the
     * margin the mechanism cannot reach. Drawing at (0,0) gives different results depending on
     * which one is used.
     */
    public static final class OriginType extends AttributeValue {

        private static final int I_PHYSICAL = 0;
        private static final int I_PRINTABLE = 1;

        private static final String[] NAMES = { "physical", "printable" };

        /** From the corner of the paper. */
        public static final OriginType PHYSICAL = new OriginType(I_PHYSICAL);
        /** From the printable area. */
        public static final OriginType PRINTABLE = new OriginType(I_PRINTABLE);

        private OriginType(int type) {
            super(type, NAMES);
        }
    }

    /** How good it has to come out. */
    public static final class PrintQualityType extends AttributeValue {

        private static final int I_HIGH = 0;
        private static final int I_NORMAL = 1;
        private static final int I_DRAFT = 2;

        private static final String[] NAMES = { "high", "normal", "draft" };

        /** The best the printer gives. */
        public static final PrintQualityType HIGH = new PrintQualityType(I_HIGH);
        /** The normal one. */
        public static final PrintQualityType NORMAL = new PrintQualityType(I_NORMAL);
        /** Draft: fast and with less ink. */
        public static final PrintQualityType DRAFT = new PrintQualityType(I_DRAFT);

        private PrintQualityType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * The paper sizes the API names: 75 different ones and 72 aliases.
     *
     * <p>The aliases are constants **identical** to another one, not equivalent: `A4 == ISO_A4`
     * gives `true`. They exist because the same sheet has different names depending on where
     * whoever names it comes from --`ISO_A4` for the standard, `A4` for everyday use, `ENV_10` and
     * `NA_NUMBER_10_ENVELOPE` for the same envelope--, and having both forms saves whoever reads a
     * configuration file from having to translate.
     */
    public static final class MediaType extends AttributeValue {


        // The names `toString()` returns, in index order.
        private static final String[] NAMES = {
            "iso-4a0", "iso-2a0", "iso-a0", "iso-a1", "iso-a2", "iso-a3", "iso-a4", "iso-a5",
            "iso-a6", "iso-a7", "iso-a8", "iso-a9", "iso-a10", "iso-b0", "iso-b1", "iso-b2",
            "iso-b3", "iso-b4", "iso-b5", "iso-b6", "iso-b7", "iso-b8", "iso-b9", "iso-b10",
            "jis-b0", "jis-b1", "jis-b2", "jis-b3", "jis-b4", "jis-b5", "jis-b6", "jis-b7",
            "jis-b8", "jis-b9", "jis-b10", "iso-c0", "iso-c1", "iso-c2", "iso-c3", "iso-c4",
            "iso-c5", "iso-c6", "iso-c7", "iso-c8", "iso-c9", "iso-c10", "iso-designated-long",
            "executive", "folio", "invoice", "ledger", "na-letter", "na-legal", "quarto", "a",
            "b", "c", "d", "e", "na-10x15-envelope", "na-10x14-envelope", "na-10x13-envelope",
            "na-9x12-envelope", "na-9x11-envelope", "na-7x9-envelope", "na-6x9-envelope",
            "na-number-9-envelope", "na-number-10-envelope", "na-number-11-envelope",
            "na-number-12-envelope", "na-number-14-envelope", "invite-envelope",
            "italy-envelope", "monarch-envelope", "personal-envelope"
        };


        // ---- The ISO 216 sizes, the A series: each one is half the previous one.
        /** `iso-4a0`. */
        public static final MediaType ISO_4A0 = new MediaType(0);
        /** `iso-2a0`. */
        public static final MediaType ISO_2A0 = new MediaType(1);
        /** `iso-a0`. */
        public static final MediaType ISO_A0 = new MediaType(2);
        /** `iso-a1`. */
        public static final MediaType ISO_A1 = new MediaType(3);
        /** `iso-a2`. */
        public static final MediaType ISO_A2 = new MediaType(4);
        /** `iso-a3`. */
        public static final MediaType ISO_A3 = new MediaType(5);
        /** `iso-a4`. */
        public static final MediaType ISO_A4 = new MediaType(6);
        /** `iso-a5`. */
        public static final MediaType ISO_A5 = new MediaType(7);
        /** `iso-a6`. */
        public static final MediaType ISO_A6 = new MediaType(8);
        /** `iso-a7`. */
        public static final MediaType ISO_A7 = new MediaType(9);
        /** `iso-a8`. */
        public static final MediaType ISO_A8 = new MediaType(10);
        /** `iso-a9`. */
        public static final MediaType ISO_A9 = new MediaType(11);
        /** `iso-a10`. */
        public static final MediaType ISO_A10 = new MediaType(12);

        // ---- The B series of ISO 216, between two consecutive A sizes.
        /** `iso-b0`. */
        public static final MediaType ISO_B0 = new MediaType(13);
        /** `iso-b1`. */
        public static final MediaType ISO_B1 = new MediaType(14);
        /** `iso-b2`. */
        public static final MediaType ISO_B2 = new MediaType(15);
        /** `iso-b3`. */
        public static final MediaType ISO_B3 = new MediaType(16);
        /** `iso-b4`. */
        public static final MediaType ISO_B4 = new MediaType(17);
        /** `iso-b5`. */
        public static final MediaType ISO_B5 = new MediaType(18);
        /** `iso-b6`. */
        public static final MediaType ISO_B6 = new MediaType(19);
        /** `iso-b7`. */
        public static final MediaType ISO_B7 = new MediaType(20);
        /** `iso-b8`. */
        public static final MediaType ISO_B8 = new MediaType(21);
        /** `iso-b9`. */
        public static final MediaType ISO_B9 = new MediaType(22);
        /** `iso-b10`. */
        public static final MediaType ISO_B10 = new MediaType(23);

        // ---- The Japanese B series, which does **not** match the ISO B one despite the name.
        /** `jis-b0`. */
        public static final MediaType JIS_B0 = new MediaType(24);
        /** `jis-b1`. */
        public static final MediaType JIS_B1 = new MediaType(25);
        /** `jis-b2`. */
        public static final MediaType JIS_B2 = new MediaType(26);
        /** `jis-b3`. */
        public static final MediaType JIS_B3 = new MediaType(27);
        /** `jis-b4`. */
        public static final MediaType JIS_B4 = new MediaType(28);
        /** `jis-b5`. */
        public static final MediaType JIS_B5 = new MediaType(29);
        /** `jis-b6`. */
        public static final MediaType JIS_B6 = new MediaType(30);
        /** `jis-b7`. */
        public static final MediaType JIS_B7 = new MediaType(31);
        /** `jis-b8`. */
        public static final MediaType JIS_B8 = new MediaType(32);
        /** `jis-b9`. */
        public static final MediaType JIS_B9 = new MediaType(33);
        /** `jis-b10`. */
        public static final MediaType JIS_B10 = new MediaType(34);

        // ---- The C series of ISO 269: envelopes for the A series of the same number.
        /** `iso-c0`. */
        public static final MediaType ISO_C0 = new MediaType(35);
        /** `iso-c1`. */
        public static final MediaType ISO_C1 = new MediaType(36);
        /** `iso-c2`. */
        public static final MediaType ISO_C2 = new MediaType(37);
        /** `iso-c3`. */
        public static final MediaType ISO_C3 = new MediaType(38);
        /** `iso-c4`. */
        public static final MediaType ISO_C4 = new MediaType(39);
        /** `iso-c5`. */
        public static final MediaType ISO_C5 = new MediaType(40);
        /** `iso-c6`. */
        public static final MediaType ISO_C6 = new MediaType(41);
        /** `iso-c7`. */
        public static final MediaType ISO_C7 = new MediaType(42);
        /** `iso-c8`. */
        public static final MediaType ISO_C8 = new MediaType(43);
        /** `iso-c9`. */
        public static final MediaType ISO_C9 = new MediaType(44);
        /** `iso-c10`. */
        public static final MediaType ISO_C10 = new MediaType(45);
        /** `iso-designated-long`. */
        public static final MediaType ISO_DESIGNATED_LONG = new MediaType(46);

        // ---- North American sizes that follow no series.
        /** `executive`. */
        public static final MediaType EXECUTIVE = new MediaType(47);
        /** `folio`. */
        public static final MediaType FOLIO = new MediaType(48);
        /** `invoice`. */
        public static final MediaType INVOICE = new MediaType(49);
        /** `ledger`. */
        public static final MediaType LEDGER = new MediaType(50);
        /** `na-letter`. */
        public static final MediaType NA_LETTER = new MediaType(51);
        /** `na-legal`. */
        public static final MediaType NA_LEGAL = new MediaType(52);
        /** `quarto`. */
        public static final MediaType QUARTO = new MediaType(53);
        /** `a`. */
        public static final MediaType A = new MediaType(54);
        /** `b`. */
        public static final MediaType B = new MediaType(55);
        /** `c`. */
        public static final MediaType C = new MediaType(56);
        /** `d`. */
        public static final MediaType D = new MediaType(57);
        /** `e`. */
        public static final MediaType E = new MediaType(58);

        // ---- North American envelopes, by their measures in inches.
        /** `na-10x15-envelope`. */
        public static final MediaType NA_10X15_ENVELOPE = new MediaType(59);
        /** `na-10x14-envelope`. */
        public static final MediaType NA_10X14_ENVELOPE = new MediaType(60);
        /** `na-10x13-envelope`. */
        public static final MediaType NA_10X13_ENVELOPE = new MediaType(61);
        /** `na-9x12-envelope`. */
        public static final MediaType NA_9X12_ENVELOPE = new MediaType(62);
        /** `na-9x11-envelope`. */
        public static final MediaType NA_9X11_ENVELOPE = new MediaType(63);
        /** `na-7x9-envelope`. */
        public static final MediaType NA_7X9_ENVELOPE = new MediaType(64);
        /** `na-6x9-envelope`. */
        public static final MediaType NA_6X9_ENVELOPE = new MediaType(65);

        // ---- North American envelopes by commercial number.
        /** `na-number-9-envelope`. */
        public static final MediaType NA_NUMBER_9_ENVELOPE = new MediaType(66);
        /** `na-number-10-envelope`. */
        public static final MediaType NA_NUMBER_10_ENVELOPE = new MediaType(67);
        /** `na-number-11-envelope`. */
        public static final MediaType NA_NUMBER_11_ENVELOPE = new MediaType(68);
        /** `na-number-12-envelope`. */
        public static final MediaType NA_NUMBER_12_ENVELOPE = new MediaType(69);
        /** `na-number-14-envelope`. */
        public static final MediaType NA_NUMBER_14_ENVELOPE = new MediaType(70);

        // ---- Envelopes with names of their own.
        /** `invite-envelope`. */
        public static final MediaType INVITE_ENVELOPE = new MediaType(71);
        /** `italy-envelope`. */
        public static final MediaType ITALY_ENVELOPE = new MediaType(72);
        /** `monarch-envelope`. */
        public static final MediaType MONARCH_ENVELOPE = new MediaType(73);
        /** `personal-envelope`. */
        public static final MediaType PERSONAL_ENVELOPE = new MediaType(74);

        // ---- The aliases. They are the SAME instance, not an equivalent one:
        // `A4 == ISO_A4` gives `true`. See the note of the class.
        /** The same as {@link #ISO_A0}. */
        public static final MediaType A0 = ISO_A0;
        /** The same as {@link #ISO_A1}. */
        public static final MediaType A1 = ISO_A1;
        /** The same as {@link #ISO_A2}. */
        public static final MediaType A2 = ISO_A2;
        /** The same as {@link #ISO_A3}. */
        public static final MediaType A3 = ISO_A3;
        /** The same as {@link #ISO_A4}. */
        public static final MediaType A4 = ISO_A4;
        /** The same as {@link #ISO_A5}. */
        public static final MediaType A5 = ISO_A5;
        /** The same as {@link #ISO_A6}. */
        public static final MediaType A6 = ISO_A6;
        /** The same as {@link #ISO_A7}. */
        public static final MediaType A7 = ISO_A7;
        /** The same as {@link #ISO_A8}. */
        public static final MediaType A8 = ISO_A8;
        /** The same as {@link #ISO_A9}. */
        public static final MediaType A9 = ISO_A9;
        /** The same as {@link #ISO_A10}. */
        public static final MediaType A10 = ISO_A10;
        /** The same as {@link #ISO_B0}. */
        public static final MediaType B0 = ISO_B0;
        /** The same as {@link #ISO_B1}. */
        public static final MediaType B1 = ISO_B1;
        /** The same as {@link #ISO_B2}. */
        public static final MediaType B2 = ISO_B2;
        /** The same as {@link #ISO_B3}. */
        public static final MediaType B3 = ISO_B3;
        /** The same as {@link #ISO_B4}. */
        public static final MediaType B4 = ISO_B4;
        /** The same as {@link #ISO_B4}. */
        public static final MediaType ISO_B4_ENVELOPE = ISO_B4;
        /** The same as {@link #ISO_B5}. */
        public static final MediaType B5 = ISO_B5;
        /** The same as {@link #ISO_B5}. */
        public static final MediaType ISO_B5_ENVELOPE = ISO_B5;
        /** The same as {@link #ISO_B6}. */
        public static final MediaType B6 = ISO_B6;
        /** The same as {@link #ISO_B7}. */
        public static final MediaType B7 = ISO_B7;
        /** The same as {@link #ISO_B8}. */
        public static final MediaType B8 = ISO_B8;
        /** The same as {@link #ISO_B9}. */
        public static final MediaType B9 = ISO_B9;
        /** The same as {@link #ISO_B10}. */
        public static final MediaType B10 = ISO_B10;
        /** The same as {@link #ISO_C0}. */
        public static final MediaType C0 = ISO_C0;
        /** The same as {@link #ISO_C0}. */
        public static final MediaType ISO_C0_ENVELOPE = ISO_C0;
        /** The same as {@link #ISO_C1}. */
        public static final MediaType C1 = ISO_C1;
        /** The same as {@link #ISO_C1}. */
        public static final MediaType ISO_C1_ENVELOPE = ISO_C1;
        /** The same as {@link #ISO_C2}. */
        public static final MediaType C2 = ISO_C2;
        /** The same as {@link #ISO_C2}. */
        public static final MediaType ISO_C2_ENVELOPE = ISO_C2;
        /** The same as {@link #ISO_C3}. */
        public static final MediaType C3 = ISO_C3;
        /** The same as {@link #ISO_C3}. */
        public static final MediaType ISO_C3_ENVELOPE = ISO_C3;
        /** The same as {@link #ISO_C4}. */
        public static final MediaType C4 = ISO_C4;
        /** The same as {@link #ISO_C4}. */
        public static final MediaType ISO_C4_ENVELOPE = ISO_C4;
        /** The same as {@link #ISO_C5}. */
        public static final MediaType C5 = ISO_C5;
        /** The same as {@link #ISO_C5}. */
        public static final MediaType ISO_C5_ENVELOPE = ISO_C5;
        /** The same as {@link #ISO_C6}. */
        public static final MediaType C6 = ISO_C6;
        /** The same as {@link #ISO_C6}. */
        public static final MediaType ISO_C6_ENVELOPE = ISO_C6;
        /** The same as {@link #ISO_C7}. */
        public static final MediaType C7 = ISO_C7;
        /** The same as {@link #ISO_C7}. */
        public static final MediaType ISO_C7_ENVELOPE = ISO_C7;
        /** The same as {@link #ISO_C8}. */
        public static final MediaType C8 = ISO_C8;
        /** The same as {@link #ISO_C8}. */
        public static final MediaType ISO_C8_ENVELOPE = ISO_C8;
        /** The same as {@link #ISO_C9}. */
        public static final MediaType C9 = ISO_C9;
        /** The same as {@link #ISO_C9}. */
        public static final MediaType ISO_C9_ENVELOPE = ISO_C9;
        /** The same as {@link #ISO_C10}. */
        public static final MediaType C10 = ISO_C10;
        /** The same as {@link #ISO_C10}. */
        public static final MediaType ISO_C10_ENVELOPE = ISO_C10;
        /** The same as {@link #ISO_DESIGNATED_LONG}. */
        public static final MediaType ISO_DESIGNATED_LONG_ENVELOPE = ISO_DESIGNATED_LONG;
        /** The same as {@link #INVOICE}. */
        public static final MediaType STATEMENT = INVOICE;
        /** The same as {@link #LEDGER}. */
        public static final MediaType TABLOID = LEDGER;
        /** The same as {@link #NA_LETTER}. */
        public static final MediaType LETTER = NA_LETTER;
        /** The same as {@link #NA_LETTER}. */
        public static final MediaType NOTE = NA_LETTER;
        /** The same as {@link #NA_LEGAL}. */
        public static final MediaType LEGAL = NA_LEGAL;
        /** The same as {@link #NA_10X15_ENVELOPE}. */
        public static final MediaType ENV_10X15 = NA_10X15_ENVELOPE;
        /** The same as {@link #NA_10X14_ENVELOPE}. */
        public static final MediaType ENV_10X14 = NA_10X14_ENVELOPE;
        /** The same as {@link #NA_10X13_ENVELOPE}. */
        public static final MediaType ENV_10X13 = NA_10X13_ENVELOPE;
        /** The same as {@link #NA_9X12_ENVELOPE}. */
        public static final MediaType ENV_9X12 = NA_9X12_ENVELOPE;
        /** The same as {@link #NA_9X11_ENVELOPE}. */
        public static final MediaType ENV_9X11 = NA_9X11_ENVELOPE;
        /** The same as {@link #NA_7X9_ENVELOPE}. */
        public static final MediaType ENV_7X9 = NA_7X9_ENVELOPE;
        /** The same as {@link #NA_6X9_ENVELOPE}. */
        public static final MediaType ENV_6X9 = NA_6X9_ENVELOPE;
        /** The same as {@link #NA_NUMBER_9_ENVELOPE}. */
        public static final MediaType ENV_9 = NA_NUMBER_9_ENVELOPE;
        /** The same as {@link #NA_NUMBER_10_ENVELOPE}. */
        public static final MediaType ENV_10 = NA_NUMBER_10_ENVELOPE;
        /** The same as {@link #NA_NUMBER_11_ENVELOPE}. */
        public static final MediaType ENV_11 = NA_NUMBER_11_ENVELOPE;
        /** The same as {@link #NA_NUMBER_12_ENVELOPE}. */
        public static final MediaType ENV_12 = NA_NUMBER_12_ENVELOPE;
        /** The same as {@link #NA_NUMBER_14_ENVELOPE}. */
        public static final MediaType ENV_14 = NA_NUMBER_14_ENVELOPE;
        /** The same as {@link #INVITE_ENVELOPE}. */
        public static final MediaType ENV_INVITE = INVITE_ENVELOPE;
        /** The same as {@link #ITALY_ENVELOPE}. */
        public static final MediaType ENV_ITALY = ITALY_ENVELOPE;
        /** The same as {@link #MONARCH_ENVELOPE}. */
        public static final MediaType ENV_MONARCH = MONARCH_ENVELOPE;
        /** The same as {@link #PERSONAL_ENVELOPE}. */
        public static final MediaType ENV_PERSONAL = PERSONAL_ENVELOPE;
        /** The same as {@link #INVITE_ENVELOPE}. */
        public static final MediaType INVITE = INVITE_ENVELOPE;
        /** The same as {@link #ITALY_ENVELOPE}. */
        public static final MediaType ITALY = ITALY_ENVELOPE;
        /** The same as {@link #MONARCH_ENVELOPE}. */
        public static final MediaType MONARCH = MONARCH_ENVELOPE;
        /** The same as {@link #PERSONAL_ENVELOPE}. */
        public static final MediaType PERSONAL = PERSONAL_ENVELOPE;

        private MediaType(int type) {
            super(type, NAMES);
        }
    }

    private ColorType color;
    private MediaType media;
    private OrientationRequestedType orientationRequested;
    private OriginType origin;
    private PrintQualityType printQuality;
    private int[] printerResolution;

    /** The default values. The paper depends on the country; see the note of the class. */
    public PageAttributes() {
        this.setColor(ColorType.MONOCHROME);
        this.setMediaToDefault();
        this.setOrientationRequestedToDefault();
        this.setOrigin(OriginType.PHYSICAL);
        this.setPrintQualityToDefault();
        this.setPrinterResolutionToDefault();
    }

    /**
     * A copy of `obj`.
     *
     * @throws NullPointerException if `obj` is null
     */
    public PageAttributes(PageAttributes obj) {
        this.set(obj);
    }

    /**
     * With every value given.
     *
     * @throws IllegalArgumentException if any of them is not valid
     */
    public PageAttributes(ColorType color, MediaType media,
            OrientationRequestedType orientationRequested, OriginType origin,
            PrintQualityType printQuality, int[] printerResolution) {
        this.setColor(color);
        this.setMedia(media);
        this.setOrientationRequested(orientationRequested);
        this.setOrigin(origin);
        this.setPrintQuality(printQuality);
        this.setPrinterResolution(printerResolution);
    }

    /** A copy. The resolution is really copied: it is the only mutable field. */
    public Object clone() {
        PageAttributes copy = new PageAttributes(this);
        return copy;
    }

    /**
     * Takes every value from `obj`.
     *
     * @throws NullPointerException if `obj` is null
     */
    public void set(PageAttributes obj) {
        this.color = obj.color;
        this.media = obj.media;
        this.orientationRequested = obj.orientationRequested;
        this.origin = obj.origin;
        this.printQuality = obj.printQuality;
        this.printerResolution = copyRes(obj.printerResolution);
    }

    private static int[] copyRes(int[] src) {
        if (src == null) {
            return null;
        }
        int[] out = new int[src.length];
        System.arraycopy(src, 0, out, 0, src.length);
        return out;
    }

    /** Colour or black and white. */
    public ColorType getColor() {
        return this.color;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setColor(ColorType color) {
        if (color == null) {
            throw new IllegalArgumentException("Invalid value for attribute color");
        }
        this.color = color;
    }

    /** The paper. */
    public MediaType getMedia() {
        return this.media;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setMedia(MediaType media) {
        if (media == null) {
            throw new IllegalArgumentException("Invalid value for attribute media");
        }
        this.media = media;
    }

    /**
     * Goes back to the default paper of the country.
     *
     * <p>`NA_LETTER` in `US` and `CA`, `ISO_A4` in everything else. It is the JDK's exact split,
     * checked country by country -- Mexico, the Philippines and Puerto Rico use letter in practice
     * and even so the JDK gives them A4, so the rule is the one of the two countries and not the
     * one of the continent.
     */
    public void setMediaToDefault() {
        String country = Locale.getDefault().getCountry();
        if ("US".equals(country) || "CA".equals(country)) {
            this.setMedia(MediaType.NA_LETTER);
        } else {
            this.setMedia(MediaType.ISO_A4);
        }
    }

    /** Portrait or landscape. */
    public OrientationRequestedType getOrientationRequested() {
        return this.orientationRequested;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setOrientationRequested(OrientationRequestedType orientationRequested) {
        if (orientationRequested == null) {
            throw new IllegalArgumentException(
                    "Invalid value for attribute orientationRequested");
        }
        this.orientationRequested = orientationRequested;
    }

    /**
     * By its IPP code: **3 is portrait and 4 landscape**.
     *
     * <p>The numbers do not start at zero or at one because they are the ones of the IPP
     * specification, not an index of this API.
     *
     * @throws IllegalArgumentException if it is neither 3 nor 4
     */
    public void setOrientationRequested(int orientationRequested) {
        if (orientationRequested == 3) {
            this.setOrientationRequested(OrientationRequestedType.PORTRAIT);
        } else if (orientationRequested == 4) {
            this.setOrientationRequested(OrientationRequestedType.LANDSCAPE);
        } else {
            throw new IllegalArgumentException(
                    "Invalid value for attribute orientationRequested");
        }
    }

    /** Goes back to portrait. */
    public void setOrientationRequestedToDefault() {
        this.setOrientationRequested(OrientationRequestedType.PORTRAIT);
    }

    /** Where the coordinates are measured from. */
    public OriginType getOrigin() {
        return this.origin;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setOrigin(OriginType origin) {
        if (origin == null) {
            throw new IllegalArgumentException("Invalid value for attribute origin");
        }
        this.origin = origin;
    }

    /** The quality. */
    public PrintQualityType getPrintQuality() {
        return this.printQuality;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setPrintQuality(PrintQualityType printQuality) {
        if (printQuality == null) {
            throw new IllegalArgumentException("Invalid value for attribute printQuality");
        }
        this.printQuality = printQuality;
    }

    /**
     * By its IPP code: **3 draft, 4 normal, 5 high**.
     *
     * <p>Mind the order, which goes the other way round from what one would say: the smaller number
     * is the lower quality.
     *
     * @throws IllegalArgumentException if it is neither 3, 4 nor 5
     */
    public void setPrintQuality(int printQuality) {
        if (printQuality == 3) {
            this.setPrintQuality(PrintQualityType.DRAFT);
        } else if (printQuality == 4) {
            this.setPrintQuality(PrintQualityType.NORMAL);
        } else if (printQuality == 5) {
            this.setPrintQuality(PrintQualityType.HIGH);
        } else {
            throw new IllegalArgumentException("Invalid value for attribute printQuality");
        }
    }

    /** Goes back to normal quality. */
    public void setPrintQualityToDefault() {
        this.setPrintQuality(PrintQualityType.NORMAL);
    }

    /** A copy of the three numbers. See the note of the class about what they mean. */
    public int[] getPrinterResolution() {
        return copyRes(this.printerResolution);
    }

    /**
     * Sets the resolution: `{x, y, unit}` with the unit at 3 (per inch) or 4 (per centimetre).
     *
     * @throws IllegalArgumentException if the array is null, does not have exactly three numbers,
     *     either of the first two is not positive, or the unit is neither 3 nor 4
     */
    public void setPrinterResolution(int[] printerResolution) {
        if (printerResolution == null
                || printerResolution.length != 3
                || printerResolution[0] <= 0
                || printerResolution[1] <= 0
                || (printerResolution[2] != 3 && printerResolution[2] != 4)) {
            throw new IllegalArgumentException("Invalid value for attribute printerResolution");
        }
        this.printerResolution = copyRes(printerResolution);
    }

    /**
     * The same resolution on both axes, in dots per inch.
     *
     * @throws IllegalArgumentException if it is not positive
     */
    public void setPrinterResolution(int printerResolution) {
        this.setPrinterResolution(
                new int[] { printerResolution, printerResolution, 3 });
    }

    /** Goes back to 72 dots per inch, which is the classic typographic unit. */
    public void setPrinterResolutionToDefault() {
        this.setPrinterResolution(72);
    }

    /** Equality by every field, with the resolution compared by content. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PageAttributes)) {
            return false;
        }
        PageAttributes other = (PageAttributes) obj;
        return this.color == other.color
                && this.media == other.media
                && this.orientationRequested == other.orientationRequested
                && this.origin == other.origin
                && this.printQuality == other.printQuality
                && sameRes(this.printerResolution, other.printerResolution);
    }

    private static boolean sameRes(int[] a, int[] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.color.hashCode() * 31
                + this.media.hashCode() * 37
                + this.orientationRequested.hashCode() * 41
                + this.origin.hashCode() * 43
                + this.printQuality.hashCode() * 47;
        if (this.printerResolution != null) {
            for (int i = 0; i < this.printerResolution.length; i++) {
                h = h + this.printerResolution[i] * (i + 1);
            }
        }
        return h;
    }

    /** The same format as the JDK. */
    public String toString() {
        StringBuilder res = new StringBuilder("[");
        int[] r = this.getPrinterResolution();
        for (int i = 0; i < r.length; i++) {
            if (i > 0) {
                res.append(",");
            }
            res.append(r[i]);
        }
        res.append("]");
        return "color=" + this.getColor()
                + ",media=" + this.getMedia()
                + ",orientation-requested=" + this.getOrientationRequested()
                + ",origin=" + this.getOrigin()
                + ",print-quality=" + this.getPrintQuality()
                + ",printer-resolution=" + res.toString();
    }
}
