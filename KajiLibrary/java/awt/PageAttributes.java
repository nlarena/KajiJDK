package java.awt;

import java.util.Locale;

/**
 * Cómo es la página que se imprime: color o blanco y negro, qué papel, en qué orientación y con
 * qué resolución.
 *
 * <p>La contraparte de {@link JobAttributes}: aquélla describe el **trabajo** --cuántas copias, qué
 * páginas-- y ésta cada **página**. Como aquélla, es sólo configuración: no dibuja nada y no
 * consulta a ninguna impresora. Lo que hace es rechazar valores imposibles al fijarlos.
 *
 * <h2>El papel por omisión depende del país</h2>
 *
 * <p>{@link MediaType#NA_LETTER} en Estados Unidos y Canadá, {@link MediaType#ISO_A4} en el resto.
 * Comprobado contra el JDK 25 país por país: sólo esos dos dan carta. No es una decisión de esta
 * biblioteca --es lo que hace el JDK-- y se toma de `Locale.getDefault()` en el momento de
 * construir el objeto, así que cambiar el locale después no lo cambia.
 *
 * <h2>La resolución son tres números</h2>
 *
 * <p>`{x, y, unidad}`, y la unidad es **3 para puntos por pulgada y 4 para puntos por centímetro**
 * — los códigos que usa IPP. Es la parte que sorprende: un arreglo de dos números no vale, y uno
 * con cualquier otra unidad tampoco. {@link #setPrinterResolution(int)} existe para el caso normal,
 * que es el mismo valor en los dos ejes y en puntos por pulgada.
 */
public final class PageAttributes implements Cloneable {

    /** Si se imprime a color o en blanco y negro. */
    public static final class ColorType extends AttributeValue {

        private static final int I_COLOR = 0;
        private static final int I_MONOCHROME = 1;

        private static final String[] NAMES = { "color", "monochrome" };

        /** A color. */
        public static final ColorType COLOR = new ColorType(I_COLOR);
        /** En blanco y negro. */
        public static final ColorType MONOCHROME = new ColorType(I_MONOCHROME);

        private ColorType(int type) {
            super(type, NAMES);
        }
    }

    /** Si la página va apaisada o vertical. */
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
     * Desde dónde se miden las coordenadas de la página.
     *
     * <p>La diferencia importa al posicionar: {@link #PHYSICAL} mide desde la esquina del papel y
     * {@link #PRINTABLE} desde donde la impresora **puede** imprimir, que está adentro por el margen
     * que el mecanismo no alcanza. Dibujar en (0,0) da resultados distintos según cuál se use.
     */
    public static final class OriginType extends AttributeValue {

        private static final int I_PHYSICAL = 0;
        private static final int I_PRINTABLE = 1;

        private static final String[] NAMES = { "physical", "printable" };

        /** Desde la esquina del papel. */
        public static final OriginType PHYSICAL = new OriginType(I_PHYSICAL);
        /** Desde el área imprimible. */
        public static final OriginType PRINTABLE = new OriginType(I_PRINTABLE);

        private OriginType(int type) {
            super(type, NAMES);
        }
    }

    /** Qué tan buena tiene que salir. */
    public static final class PrintQualityType extends AttributeValue {

        private static final int I_HIGH = 0;
        private static final int I_NORMAL = 1;
        private static final int I_DRAFT = 2;

        private static final String[] NAMES = { "high", "normal", "draft" };

        /** La mejor que la impresora dé. */
        public static final PrintQualityType HIGH = new PrintQualityType(I_HIGH);
        /** La normal. */
        public static final PrintQualityType NORMAL = new PrintQualityType(I_NORMAL);
        /** Borrador: rápido y con menos tinta. */
        public static final PrintQualityType DRAFT = new PrintQualityType(I_DRAFT);

        private PrintQualityType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * Los tamaños de papel que la API nombra: 75 distintos y 72 alias.
     *
     * <p>Los alias son constantes **idénticas** a otra, no equivalentes: `A4 == ISO_A4` da `true`.
     * Existen porque la misma hoja tiene nombres distintos según de dónde venga quien la nombra
     * --`ISO_A4` para el estándar, `A4` para el uso corriente, `ENV_10` y
     * `NA_NUMBER_10_ENVELOPE` para el mismo sobre--, y tener las dos formas evita que quien lee un
     * archivo de configuración tenga que traducir.
     */
    public static final class MediaType extends AttributeValue {


        // Los nombres que devuelve `toString()`, en el orden de los indices.
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


        // ---- Los tamanos ISO 216, la serie A: cada uno es la mitad del anterior.
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

        // ---- La serie B de ISO 216, entre dos tamanos A consecutivos.
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

        // ---- La serie B japonesa, que **no** coincide con la B de ISO pese al nombre.
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

        // ---- La serie C de ISO 269: sobres para la serie A del mismo numero.
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

        // ---- Tamanos norteamericanos que no siguen ninguna serie.
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

        // ---- Sobres norteamericanos, por sus medidas en pulgadas.
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

        // ---- Sobres norteamericanos por numero comercial.
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

        // ---- Sobres con nombre propio.
        /** `invite-envelope`. */
        public static final MediaType INVITE_ENVELOPE = new MediaType(71);
        /** `italy-envelope`. */
        public static final MediaType ITALY_ENVELOPE = new MediaType(72);
        /** `monarch-envelope`. */
        public static final MediaType MONARCH_ENVELOPE = new MediaType(73);
        /** `personal-envelope`. */
        public static final MediaType PERSONAL_ENVELOPE = new MediaType(74);

        // ---- Los alias. Son la MISMA instancia, no una equivalente:
        // `A4 == ISO_A4` da `true`. Ver la nota de la clase.
        /** Igual que {@link #ISO_A0}. */
        public static final MediaType A0 = ISO_A0;
        /** Igual que {@link #ISO_A1}. */
        public static final MediaType A1 = ISO_A1;
        /** Igual que {@link #ISO_A2}. */
        public static final MediaType A2 = ISO_A2;
        /** Igual que {@link #ISO_A3}. */
        public static final MediaType A3 = ISO_A3;
        /** Igual que {@link #ISO_A4}. */
        public static final MediaType A4 = ISO_A4;
        /** Igual que {@link #ISO_A5}. */
        public static final MediaType A5 = ISO_A5;
        /** Igual que {@link #ISO_A6}. */
        public static final MediaType A6 = ISO_A6;
        /** Igual que {@link #ISO_A7}. */
        public static final MediaType A7 = ISO_A7;
        /** Igual que {@link #ISO_A8}. */
        public static final MediaType A8 = ISO_A8;
        /** Igual que {@link #ISO_A9}. */
        public static final MediaType A9 = ISO_A9;
        /** Igual que {@link #ISO_A10}. */
        public static final MediaType A10 = ISO_A10;
        /** Igual que {@link #ISO_B0}. */
        public static final MediaType B0 = ISO_B0;
        /** Igual que {@link #ISO_B1}. */
        public static final MediaType B1 = ISO_B1;
        /** Igual que {@link #ISO_B2}. */
        public static final MediaType B2 = ISO_B2;
        /** Igual que {@link #ISO_B3}. */
        public static final MediaType B3 = ISO_B3;
        /** Igual que {@link #ISO_B4}. */
        public static final MediaType B4 = ISO_B4;
        /** Igual que {@link #ISO_B4}. */
        public static final MediaType ISO_B4_ENVELOPE = ISO_B4;
        /** Igual que {@link #ISO_B5}. */
        public static final MediaType B5 = ISO_B5;
        /** Igual que {@link #ISO_B5}. */
        public static final MediaType ISO_B5_ENVELOPE = ISO_B5;
        /** Igual que {@link #ISO_B6}. */
        public static final MediaType B6 = ISO_B6;
        /** Igual que {@link #ISO_B7}. */
        public static final MediaType B7 = ISO_B7;
        /** Igual que {@link #ISO_B8}. */
        public static final MediaType B8 = ISO_B8;
        /** Igual que {@link #ISO_B9}. */
        public static final MediaType B9 = ISO_B9;
        /** Igual que {@link #ISO_B10}. */
        public static final MediaType B10 = ISO_B10;
        /** Igual que {@link #ISO_C0}. */
        public static final MediaType C0 = ISO_C0;
        /** Igual que {@link #ISO_C0}. */
        public static final MediaType ISO_C0_ENVELOPE = ISO_C0;
        /** Igual que {@link #ISO_C1}. */
        public static final MediaType C1 = ISO_C1;
        /** Igual que {@link #ISO_C1}. */
        public static final MediaType ISO_C1_ENVELOPE = ISO_C1;
        /** Igual que {@link #ISO_C2}. */
        public static final MediaType C2 = ISO_C2;
        /** Igual que {@link #ISO_C2}. */
        public static final MediaType ISO_C2_ENVELOPE = ISO_C2;
        /** Igual que {@link #ISO_C3}. */
        public static final MediaType C3 = ISO_C3;
        /** Igual que {@link #ISO_C3}. */
        public static final MediaType ISO_C3_ENVELOPE = ISO_C3;
        /** Igual que {@link #ISO_C4}. */
        public static final MediaType C4 = ISO_C4;
        /** Igual que {@link #ISO_C4}. */
        public static final MediaType ISO_C4_ENVELOPE = ISO_C4;
        /** Igual que {@link #ISO_C5}. */
        public static final MediaType C5 = ISO_C5;
        /** Igual que {@link #ISO_C5}. */
        public static final MediaType ISO_C5_ENVELOPE = ISO_C5;
        /** Igual que {@link #ISO_C6}. */
        public static final MediaType C6 = ISO_C6;
        /** Igual que {@link #ISO_C6}. */
        public static final MediaType ISO_C6_ENVELOPE = ISO_C6;
        /** Igual que {@link #ISO_C7}. */
        public static final MediaType C7 = ISO_C7;
        /** Igual que {@link #ISO_C7}. */
        public static final MediaType ISO_C7_ENVELOPE = ISO_C7;
        /** Igual que {@link #ISO_C8}. */
        public static final MediaType C8 = ISO_C8;
        /** Igual que {@link #ISO_C8}. */
        public static final MediaType ISO_C8_ENVELOPE = ISO_C8;
        /** Igual que {@link #ISO_C9}. */
        public static final MediaType C9 = ISO_C9;
        /** Igual que {@link #ISO_C9}. */
        public static final MediaType ISO_C9_ENVELOPE = ISO_C9;
        /** Igual que {@link #ISO_C10}. */
        public static final MediaType C10 = ISO_C10;
        /** Igual que {@link #ISO_C10}. */
        public static final MediaType ISO_C10_ENVELOPE = ISO_C10;
        /** Igual que {@link #ISO_DESIGNATED_LONG}. */
        public static final MediaType ISO_DESIGNATED_LONG_ENVELOPE = ISO_DESIGNATED_LONG;
        /** Igual que {@link #INVOICE}. */
        public static final MediaType STATEMENT = INVOICE;
        /** Igual que {@link #LEDGER}. */
        public static final MediaType TABLOID = LEDGER;
        /** Igual que {@link #NA_LETTER}. */
        public static final MediaType LETTER = NA_LETTER;
        /** Igual que {@link #NA_LETTER}. */
        public static final MediaType NOTE = NA_LETTER;
        /** Igual que {@link #NA_LEGAL}. */
        public static final MediaType LEGAL = NA_LEGAL;
        /** Igual que {@link #NA_10X15_ENVELOPE}. */
        public static final MediaType ENV_10X15 = NA_10X15_ENVELOPE;
        /** Igual que {@link #NA_10X14_ENVELOPE}. */
        public static final MediaType ENV_10X14 = NA_10X14_ENVELOPE;
        /** Igual que {@link #NA_10X13_ENVELOPE}. */
        public static final MediaType ENV_10X13 = NA_10X13_ENVELOPE;
        /** Igual que {@link #NA_9X12_ENVELOPE}. */
        public static final MediaType ENV_9X12 = NA_9X12_ENVELOPE;
        /** Igual que {@link #NA_9X11_ENVELOPE}. */
        public static final MediaType ENV_9X11 = NA_9X11_ENVELOPE;
        /** Igual que {@link #NA_7X9_ENVELOPE}. */
        public static final MediaType ENV_7X9 = NA_7X9_ENVELOPE;
        /** Igual que {@link #NA_6X9_ENVELOPE}. */
        public static final MediaType ENV_6X9 = NA_6X9_ENVELOPE;
        /** Igual que {@link #NA_NUMBER_9_ENVELOPE}. */
        public static final MediaType ENV_9 = NA_NUMBER_9_ENVELOPE;
        /** Igual que {@link #NA_NUMBER_10_ENVELOPE}. */
        public static final MediaType ENV_10 = NA_NUMBER_10_ENVELOPE;
        /** Igual que {@link #NA_NUMBER_11_ENVELOPE}. */
        public static final MediaType ENV_11 = NA_NUMBER_11_ENVELOPE;
        /** Igual que {@link #NA_NUMBER_12_ENVELOPE}. */
        public static final MediaType ENV_12 = NA_NUMBER_12_ENVELOPE;
        /** Igual que {@link #NA_NUMBER_14_ENVELOPE}. */
        public static final MediaType ENV_14 = NA_NUMBER_14_ENVELOPE;
        /** Igual que {@link #INVITE_ENVELOPE}. */
        public static final MediaType ENV_INVITE = INVITE_ENVELOPE;
        /** Igual que {@link #ITALY_ENVELOPE}. */
        public static final MediaType ENV_ITALY = ITALY_ENVELOPE;
        /** Igual que {@link #MONARCH_ENVELOPE}. */
        public static final MediaType ENV_MONARCH = MONARCH_ENVELOPE;
        /** Igual que {@link #PERSONAL_ENVELOPE}. */
        public static final MediaType ENV_PERSONAL = PERSONAL_ENVELOPE;
        /** Igual que {@link #INVITE_ENVELOPE}. */
        public static final MediaType INVITE = INVITE_ENVELOPE;
        /** Igual que {@link #ITALY_ENVELOPE}. */
        public static final MediaType ITALY = ITALY_ENVELOPE;
        /** Igual que {@link #MONARCH_ENVELOPE}. */
        public static final MediaType MONARCH = MONARCH_ENVELOPE;
        /** Igual que {@link #PERSONAL_ENVELOPE}. */
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

    /** Los valores por omisión. El papel depende del país; ver la nota de la clase. */
    public PageAttributes() {
        this.setColor(ColorType.MONOCHROME);
        this.setMediaToDefault();
        this.setOrientationRequestedToDefault();
        this.setOrigin(OriginType.PHYSICAL);
        this.setPrintQualityToDefault();
        this.setPrinterResolutionToDefault();
    }

    /**
     * Una copia de `obj`.
     *
     * @throws NullPointerException si `obj` es nulo
     */
    public PageAttributes(PageAttributes obj) {
        this.set(obj);
    }

    /**
     * Con todos los valores dados.
     *
     * @throws IllegalArgumentException si alguno no es válido
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

    /** Una copia. La resolución se copia de verdad: es el único campo mutable. */
    public Object clone() {
        PageAttributes copy = new PageAttributes(this);
        return copy;
    }

    /**
     * Toma todos los valores de `obj`.
     *
     * @throws NullPointerException si `obj` es nulo
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

    /** Color o blanco y negro. */
    public ColorType getColor() {
        return this.color;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setColor(ColorType color) {
        if (color == null) {
            throw new IllegalArgumentException("Invalid value for attribute color");
        }
        this.color = color;
    }

    /** El papel. */
    public MediaType getMedia() {
        return this.media;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setMedia(MediaType media) {
        if (media == null) {
            throw new IllegalArgumentException("Invalid value for attribute media");
        }
        this.media = media;
    }

    /**
     * Vuelve al papel por omisión del país.
     *
     * <p>`NA_LETTER` en `US` y `CA`, `ISO_A4` en todo lo demás. Es el reparto exacto del JDK,
     * comprobado país por país -- México, Filipinas y Puerto Rico usan carta en la práctica y aun
     * así el JDK les da A4, así que la regla es la de los dos países y no la del continente.
     */
    public void setMediaToDefault() {
        String pais = Locale.getDefault().getCountry();
        if ("US".equals(pais) || "CA".equals(pais)) {
            this.setMedia(MediaType.NA_LETTER);
        } else {
            this.setMedia(MediaType.ISO_A4);
        }
    }

    /** Vertical o apaisada. */
    public OrientationRequestedType getOrientationRequested() {
        return this.orientationRequested;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setOrientationRequested(OrientationRequestedType orientationRequested) {
        if (orientationRequested == null) {
            throw new IllegalArgumentException(
                    "Invalid value for attribute orientationRequested");
        }
        this.orientationRequested = orientationRequested;
    }

    /**
     * Por su código IPP: **3 es vertical y 4 apaisada**.
     *
     * <p>Los números no empiezan en cero ni en uno porque son los de la especificación IPP, no un
     * índice de esta API.
     *
     * @throws IllegalArgumentException si no es 3 ni 4
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

    /** Vuelve a vertical. */
    public void setOrientationRequestedToDefault() {
        this.setOrientationRequested(OrientationRequestedType.PORTRAIT);
    }

    /** Desde dónde se miden las coordenadas. */
    public OriginType getOrigin() {
        return this.origin;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setOrigin(OriginType origin) {
        if (origin == null) {
            throw new IllegalArgumentException("Invalid value for attribute origin");
        }
        this.origin = origin;
    }

    /** La calidad. */
    public PrintQualityType getPrintQuality() {
        return this.printQuality;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setPrintQuality(PrintQualityType printQuality) {
        if (printQuality == null) {
            throw new IllegalArgumentException("Invalid value for attribute printQuality");
        }
        this.printQuality = printQuality;
    }

    /**
     * Por su código IPP: **3 borrador, 4 normal, 5 alta**.
     *
     * <p>Ojo con el orden, que va al revés de lo que uno diría: el número más chico es la calidad
     * más baja.
     *
     * @throws IllegalArgumentException si no es 3, 4 ni 5
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

    /** Vuelve a calidad normal. */
    public void setPrintQualityToDefault() {
        this.setPrintQuality(PrintQualityType.NORMAL);
    }

    /** Una copia de los tres números. Ver la nota de la clase sobre qué significan. */
    public int[] getPrinterResolution() {
        return copyRes(this.printerResolution);
    }

    /**
     * Fija la resolución: `{x, y, unidad}` con la unidad en 3 (por pulgada) o 4 (por centímetro).
     *
     * @throws IllegalArgumentException si el arreglo es nulo, no tiene exactamente tres números,
     *     alguno de los dos primeros no es positivo, o la unidad no es 3 ni 4
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
     * La misma resolución en los dos ejes, en puntos por pulgada.
     *
     * @throws IllegalArgumentException si no es positiva
     */
    public void setPrinterResolution(int printerResolution) {
        this.setPrinterResolution(
                new int[] { printerResolution, printerResolution, 3 });
    }

    /** Vuelve a 72 puntos por pulgada, que es la unidad tipográfica clásica. */
    public void setPrinterResolutionToDefault() {
        this.setPrinterResolution(72);
    }

    /** Igualdad por todos los campos, con la resolución comparada por contenido. */
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

    /** El mismo formato que el JDK. */
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
