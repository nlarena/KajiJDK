package java.awt;

/**
 * Lo que se le pide a un trabajo de impresión: cuántas copias, qué páginas, a qué impresora.
 *
 * <p>Es un objeto de configuración y nada más — no imprime, no habla con el sistema, no valida
 * contra ninguna impresora real. Lo que sí hace, y es su razón de ser, es **rechazar
 * configuraciones imposibles en el momento de armarlas**: cero copias, una página inicial mayor que
 * la final, un rango de páginas que retrocede. Sin eso, el error aparecería recién al imprimir y
 * lejos de donde se cometió.
 *
 * <h2>Las páginas se pueden decir de dos formas</h2>
 *
 * <p>Y conviene entenderlo porque es lo único de esta clase que sorprende. Están
 * `fromPage`/`toPage` por un lado y `pageRanges` por el otro, y **son campos independientes**: fijar
 * uno no borra el otro. Lo que los une son los accesores, que responden con esta preferencia:
 *
 * <ul>
 * <li>{@link #getFromPage} da `fromPage` si se fijó; si no, el comienzo del primer rango; si
 *     tampoco hay rangos, {@link #getMinPage}.</li>
 * <li>{@link #getToPage} da `toPage` si se fijó; si no, `fromPage`; si no, el final del último
 *     rango; si tampoco, {@link #getMinPage}.</li>
 * <li>{@link #getPageRanges} da los rangos si se fijaron; si no, **sintetiza** uno solo con lo que
 *     contesten los dos anteriores — nunca devuelve nulo.</li>
 * </ul>
 *
 * <p>La asimetría entre los dos primeros no es un descuido: `getToPage` mira `fromPage` antes que
 * los rangos y `getFromPage` no mira `toPage`. Comprobado contra el JDK 25, que es de donde salen
 * estas reglas — un `setFromPage(3)` seguido de `setPageRanges({{8,9}})` deja `from=3` y `to=3`,
 * con los rangos intactos y sin participar.
 *
 * <h2>Los tipos anidados</h2>
 *
 * <p>Son cinco clases con constantes en vez de `enum` porque esta API es anterior a `enum` y
 * cambiarlas rompería la compatibilidad. Heredan de {@link AttributeValue}, que les da el
 * `toString()` en minúsculas con guiones (`separate-documents-collated-copies`) y el `hashCode()`
 * por índice.
 */
public final class JobAttributes implements Cloneable {

    /** Qué se imprime por omisión cuando el usuario no elige. */
    public static final class DefaultSelectionType extends AttributeValue {

        private static final int I_ALL = 0;
        private static final int I_RANGE = 1;
        private static final int I_SELECTION = 2;

        private static final String[] NAMES = { "all", "range", "selection" };

        /** Todo el documento. */
        public static final DefaultSelectionType ALL = new DefaultSelectionType(I_ALL);
        /** El rango de páginas configurado. */
        public static final DefaultSelectionType RANGE = new DefaultSelectionType(I_RANGE);
        /** Lo que el usuario haya seleccionado. */
        public static final DefaultSelectionType SELECTION =
                new DefaultSelectionType(I_SELECTION);

        private DefaultSelectionType(int type) {
            super(type, NAMES);
        }
    }

    /** A dónde va la salida. */
    public static final class DestinationType extends AttributeValue {

        private static final int I_FILE = 0;
        private static final int I_PRINTER = 1;

        private static final String[] NAMES = { "file", "printer" };

        /** A un archivo, el que diga {@link JobAttributes#getFileName}. */
        public static final DestinationType FILE = new DestinationType(I_FILE);
        /** A la impresora. */
        public static final DestinationType PRINTER = new DestinationType(I_PRINTER);

        private DestinationType(int type) {
            super(type, NAMES);
        }
    }

    /** Qué diálogo se le muestra al usuario antes de imprimir. */
    public static final class DialogType extends AttributeValue {

        private static final int I_COMMON = 0;
        private static final int I_NATIVE = 1;
        private static final int I_NONE = 2;

        private static final String[] NAMES = { "common", "native", "none" };

        /** El diálogo multiplataforma. */
        public static final DialogType COMMON = new DialogType(I_COMMON);
        /** El del sistema operativo. */
        public static final DialogType NATIVE = new DialogType(I_NATIVE);
        /** Ninguno: se imprime directo. */
        public static final DialogType NONE = new DialogType(I_NONE);

        private DialogType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * Cómo se agrupan las copias de un documento de varias páginas.
     *
     * <p>La diferencia se ve con dos copias de un documento de tres páginas: **intercaladas** salen
     * 1,2,3,1,2,3 y **sin intercalar** salen 1,1,2,2,3,3.
     */
    public static final class MultipleDocumentHandlingType extends AttributeValue {

        private static final int I_COLLATED = 0;
        private static final int I_UNCOLLATED = 1;

        private static final String[] NAMES = {
            "separate-documents-collated-copies", "separate-documents-uncollated-copies" };

        /** Intercaladas: cada copia completa antes de la siguiente. */
        public static final MultipleDocumentHandlingType SEPARATE_DOCUMENTS_COLLATED_COPIES =
                new MultipleDocumentHandlingType(I_COLLATED);
        /** Sin intercalar: todas las copias de cada página juntas. */
        public static final MultipleDocumentHandlingType SEPARATE_DOCUMENTS_UNCOLLATED_COPIES =
                new MultipleDocumentHandlingType(I_UNCOLLATED);

        private MultipleDocumentHandlingType(int type) {
            super(type, NAMES);
        }
    }

    /** Si se imprime de un lado o de los dos, y por qué borde se da vuelta la hoja. */
    public static final class SidesType extends AttributeValue {

        private static final int I_ONE_SIDED = 0;
        private static final int I_TWO_SIDED_LONG_EDGE = 1;
        private static final int I_TWO_SIDED_SHORT_EDGE = 2;

        private static final String[] NAMES = {
            "one-sided", "two-sided-long-edge", "two-sided-short-edge" };

        /** De un solo lado. */
        public static final SidesType ONE_SIDED = new SidesType(I_ONE_SIDED);
        /** De los dos, dando vuelta por el borde largo — como un libro. */
        public static final SidesType TWO_SIDED_LONG_EDGE =
                new SidesType(I_TWO_SIDED_LONG_EDGE);
        /** De los dos, dando vuelta por el borde corto — como un anotador. */
        public static final SidesType TWO_SIDED_SHORT_EDGE =
                new SidesType(I_TWO_SIDED_SHORT_EDGE);

        private SidesType(int type) {
            super(type, NAMES);
        }
    }

    private int copies;
    private DefaultSelectionType defaultSelection;
    private DestinationType destination;
    private DialogType dialog;
    private String fileName;
    private int fromPage;
    private int maxPage;
    private int minPage;
    private MultipleDocumentHandlingType multipleDocumentHandling;
    private int[][] pageRanges;
    private String printer;
    private SidesType sides;
    private int toPage;

    /** Los valores por omisión: una copia, todo el documento, a la impresora, diálogo nativo. */
    public JobAttributes() {
        this.setCopiesToDefault();
        this.setDefaultSelection(DefaultSelectionType.ALL);
        this.setDestination(DestinationType.PRINTER);
        this.setDialog(DialogType.NATIVE);
        this.setMaxPage(Integer.MAX_VALUE);
        this.setMinPage(1);
        this.setMultipleDocumentHandlingToDefault();
        this.setSidesToDefault();
    }

    /**
     * Una copia de `obj`.
     *
     * @throws NullPointerException si `obj` es nulo
     */
    public JobAttributes(JobAttributes obj) {
        this.set(obj);
    }

    /**
     * Con todos los valores dados.
     *
     * @throws IllegalArgumentException si alguno no es válido — ver los setters, que aplican las
     *     mismas reglas
     */
    public JobAttributes(int copies, DefaultSelectionType defaultSelection,
            DestinationType destination, DialogType dialog, String fileName, int maxPage,
            int minPage, MultipleDocumentHandlingType multipleDocumentHandling,
            int[][] pageRanges, String printer, SidesType sides) {
        this.setCopies(copies);
        this.setDefaultSelection(defaultSelection);
        this.setDestination(destination);
        this.setDialog(dialog);
        this.setFileName(fileName);
        this.setMaxPage(maxPage);
        this.setMinPage(minPage);
        this.setMultipleDocumentHandling(multipleDocumentHandling);
        this.setPageRanges(pageRanges);
        this.setPrinter(printer);
        this.setSides(sides);
    }

    /**
     * Una copia.
     *
     * <p>Superficial en todo salvo los rangos, que se copian de verdad: son el único campo mutable
     * —un `int[][]`— y compartirlo dejaría que cambiar la copia cambiara el original.
     */
    public Object clone() {
        JobAttributes copy = new JobAttributes(this);
        return copy;
    }

    /**
     * Toma todos los valores de `obj`.
     *
     * @throws NullPointerException si `obj` es nulo
     */
    public void set(JobAttributes obj) {
        this.copies = obj.copies;
        this.defaultSelection = obj.defaultSelection;
        this.destination = obj.destination;
        this.dialog = obj.dialog;
        this.fileName = obj.fileName;
        this.fromPage = obj.fromPage;
        this.maxPage = obj.maxPage;
        this.minPage = obj.minPage;
        this.multipleDocumentHandling = obj.multipleDocumentHandling;
        this.pageRanges = copyRanges(obj.pageRanges);
        this.printer = obj.printer;
        this.sides = obj.sides;
        this.toPage = obj.toPage;
    }

    private static int[][] copyRanges(int[][] src) {
        if (src == null) {
            return null;
        }
        int[][] out = new int[src.length][2];
        for (int i = 0; i < src.length; i++) {
            out[i][0] = src[i][0];
            out[i][1] = src[i][1];
        }
        return out;
    }

    /** Cuántas copias. */
    public int getCopies() {
        return this.copies;
    }

    /**
     * @throws IllegalArgumentException si `copies` es menor que 1
     */
    public void setCopies(int copies) {
        if (copies <= 0) {
            throw new IllegalArgumentException("Invalid value for attribute copies");
        }
        this.copies = copies;
    }

    /** Vuelve a una copia. */
    public void setCopiesToDefault() {
        this.setCopies(1);
    }

    /** Qué se imprime por omisión. */
    public DefaultSelectionType getDefaultSelection() {
        return this.defaultSelection;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setDefaultSelection(DefaultSelectionType defaultSelection) {
        if (defaultSelection == null) {
            throw new IllegalArgumentException("Invalid value for attribute defaultSelection");
        }
        this.defaultSelection = defaultSelection;
    }

    /** A dónde va la salida. */
    public DestinationType getDestination() {
        return this.destination;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setDestination(DestinationType destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Invalid value for attribute destination");
        }
        this.destination = destination;
    }

    /** Qué diálogo se muestra. */
    public DialogType getDialog() {
        return this.dialog;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setDialog(DialogType dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Invalid value for attribute dialog");
        }
        this.dialog = dialog;
    }

    /** El archivo de salida, o nulo si no se fijó. */
    public String getFileName() {
        return this.fileName;
    }

    /** Fija el archivo de salida. El nulo es válido y significa "sin archivo". */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /** La primera página. Ver la nota de la clase sobre cómo se resuelve. */
    public int getFromPage() {
        if (this.fromPage != 0) {
            return this.fromPage;
        }
        if (this.pageRanges != null) {
            return this.pageRanges[0][0];
        }
        return this.getMinPage();
    }

    /**
     * @throws IllegalArgumentException si es menor que 1, menor que {@link #getMinPage}, mayor que
     *     {@link #getMaxPage}, o mayor que `toPage` si ése ya se fijó
     */
    public void setFromPage(int fromPage) {
        if (fromPage <= 0
                || (this.toPage != 0 && fromPage > this.toPage)
                || fromPage < this.minPage
                || fromPage > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute fromPage");
        }
        this.fromPage = fromPage;
    }

    /** La página más alta que se puede pedir. */
    public int getMaxPage() {
        return this.maxPage;
    }

    /**
     * @throws IllegalArgumentException si es menor que 1 o menor que {@link #getMinPage}
     */
    public void setMaxPage(int maxPage) {
        if (maxPage <= 0 || maxPage < this.minPage) {
            throw new IllegalArgumentException("Invalid value for attribute maxPage");
        }
        this.maxPage = maxPage;
    }

    /** La página más baja que se puede pedir. */
    public int getMinPage() {
        return this.minPage;
    }

    /**
     * @throws IllegalArgumentException si es menor que 1 o mayor que {@link #getMaxPage}
     */
    public void setMinPage(int minPage) {
        if (minPage <= 0 || minPage > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute minPage");
        }
        this.minPage = minPage;
    }

    /** Cómo se agrupan las copias. */
    public MultipleDocumentHandlingType getMultipleDocumentHandling() {
        return this.multipleDocumentHandling;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setMultipleDocumentHandling(
            MultipleDocumentHandlingType multipleDocumentHandling) {
        if (multipleDocumentHandling == null) {
            throw new IllegalArgumentException(
                    "Invalid value for attribute multipleDocumentHandling");
        }
        this.multipleDocumentHandling = multipleDocumentHandling;
    }

    /** Vuelve a copias sin intercalar, que es la omisión. */
    public void setMultipleDocumentHandlingToDefault() {
        this.setMultipleDocumentHandling(
                MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);
    }

    /**
     * Los rangos de páginas.
     *
     * <p>**Nunca devuelve nulo**: sin rangos explícitos sintetiza uno solo con
     * {@link #getFromPage} y {@link #getToPage}. El arreglo es una copia.
     */
    public int[][] getPageRanges() {
        if (this.pageRanges != null) {
            return copyRanges(this.pageRanges);
        }
        int[][] out = new int[1][2];
        out[0][0] = this.getFromPage();
        out[0][1] = this.getToPage();
        return out;
    }

    /**
     * Fija los rangos.
     *
     * <p>Cada elemento es `{desde, hasta}` o `{página}`. Los rangos tienen que ir **en orden y sin
     * solaparse**, que es lo que hace que la impresión sea reproducible: dos rangos que se pisan
     * imprimirían la misma página dos veces sin que quien los escribió lo pidiera.
     *
     * @throws NullPointerException si el arreglo es nulo
     * @throws IllegalArgumentException si está vacío, si algún elemento no tiene uno o dos
     *     números, si un rango retrocede, si dos se solapan o van desordenados, o si alguno cae
     *     fuera de {@link #getMinPage}..{@link #getMaxPage}
     */
    public void setPageRanges(int[][] pageRanges) {
        if (pageRanges == null) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        if (pageRanges.length == 0) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        int previo = 0;
        int[][] copia = new int[pageRanges.length][2];
        for (int i = 0; i < pageRanges.length; i++) {
            int[] r = pageRanges[i];
            if (r == null || r.length < 1 || r.length > 2) {
                throw new IllegalArgumentException("Invalid value for attribute pageRanges");
            }
            int desde = r[0];
            int hasta = r.length == 2 ? r[1] : r[0];
            if (desde <= 0 || hasta < desde || desde <= previo) {
                throw new IllegalArgumentException("Invalid value for attribute pageRanges");
            }
            copia[i][0] = desde;
            copia[i][1] = hasta;
            previo = hasta;
        }
        if (copia[0][0] < this.minPage || copia[copia.length - 1][1] > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        this.pageRanges = copia;
    }

    /** La impresora, o nulo si no se fijó. */
    public String getPrinter() {
        return this.printer;
    }

    /** Fija la impresora. El nulo es válido y significa "la que sea". */
    public void setPrinter(String printer) {
        this.printer = printer;
    }

    /** De uno o de los dos lados. */
    public SidesType getSides() {
        return this.sides;
    }

    /**
     * @throws IllegalArgumentException si es nulo
     */
    public void setSides(SidesType sides) {
        if (sides == null) {
            throw new IllegalArgumentException("Invalid value for attribute sides");
        }
        this.sides = sides;
    }

    /** Vuelve a un solo lado. */
    public void setSidesToDefault() {
        this.setSides(SidesType.ONE_SIDED);
    }

    /** La última página. Ver la nota de la clase sobre cómo se resuelve. */
    public int getToPage() {
        if (this.toPage != 0) {
            return this.toPage;
        }
        if (this.fromPage != 0) {
            return this.fromPage;
        }
        if (this.pageRanges != null) {
            return this.pageRanges[this.pageRanges.length - 1][1];
        }
        return this.getMinPage();
    }

    /**
     * @throws IllegalArgumentException si es menor que 1, menor que `fromPage` si ése ya se fijó,
     *     o fuera de {@link #getMinPage}..{@link #getMaxPage}
     */
    public void setToPage(int toPage) {
        if (toPage <= 0
                || (this.fromPage != 0 && toPage < this.fromPage)
                || toPage < this.minPage
                || toPage > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute toPage");
        }
        this.toPage = toPage;
    }

    /** Igualdad por todos los campos, con los rangos comparados por contenido. */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof JobAttributes)) {
            return false;
        }
        JobAttributes other = (JobAttributes) obj;
        return this.copies == other.copies
                && this.defaultSelection == other.defaultSelection
                && this.destination == other.destination
                && this.dialog == other.dialog
                && sameText(this.fileName, other.fileName)
                && this.fromPage == other.fromPage
                && this.maxPage == other.maxPage
                && this.minPage == other.minPage
                && this.multipleDocumentHandling == other.multipleDocumentHandling
                && sameRanges(this.pageRanges, other.pageRanges)
                && sameText(this.printer, other.printer)
                && this.sides == other.sides
                && this.toPage == other.toPage;
    }

    private static boolean sameText(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    private static boolean sameRanges(int[][] a, int[][] b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i][0] != b[i][0] || a[i][1] != b[i][1]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = this.copies + this.fromPage + this.toPage + this.minPage + this.maxPage;
        h = h + this.defaultSelection.hashCode() * 31;
        h = h + this.destination.hashCode() * 37;
        h = h + this.dialog.hashCode() * 41;
        h = h + this.multipleDocumentHandling.hashCode() * 43;
        h = h + this.sides.hashCode() * 47;
        if (this.fileName != null) {
            h = h + this.fileName.hashCode();
        }
        if (this.printer != null) {
            h = h + this.printer.hashCode();
        }
        if (this.pageRanges != null) {
            for (int i = 0; i < this.pageRanges.length; i++) {
                h = h + this.pageRanges[i][0] * 3 + this.pageRanges[i][1] * 5;
            }
        }
        return h;
    }

    /** El mismo formato que el JDK: los atributos separados por comas, en orden alfabético. */
    public String toString() {
        StringBuilder rangos = new StringBuilder("[");
        int[][] rs = this.getPageRanges();
        for (int i = 0; i < rs.length; i++) {
            if (i > 0) {
                rangos.append(",");
            }
            rangos.append(rs[i][0]).append(":").append(rs[i][1]);
        }
        rangos.append("]");
        return "copies=" + this.getCopies()
                + ",defaultSelection=" + this.getDefaultSelection()
                + ",destination=" + this.getDestination()
                + ",dialog=" + this.getDialog()
                + ",fileName=" + this.getFileName()
                + ",fromPage=" + this.getFromPage()
                + ",maxPage=" + this.getMaxPage()
                + ",minPage=" + this.getMinPage()
                + ",multiple-document-handling=" + this.getMultipleDocumentHandling()
                + ",page-ranges=" + rangos.toString()
                + ",printer=" + this.getPrinter()
                + ",sides=" + this.getSides()
                + ",toPage=" + this.getToPage();
    }
}
