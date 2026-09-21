package java.awt;

/**
 * What is asked of a print job: how many copies, which pages, to which printer.
 *
 * <p>It is a configuration object and nothing more — it does not print, it does not talk to the
 * system, it validates against no real printer. What it does do, and it is its reason for being, is
 * **reject impossible configurations at the moment they are built**: zero copies, a first page
 * higher than the last, a page range that goes backwards. Without that, the mistake would show up
 * only at printing time and far from where it was made.
 *
 * <h2>The pages can be said in two ways</h2>
 *
 * <p>And it is worth understanding, because it is the only thing about this class that surprises.
 * There are `fromPage`/`toPage` on one side and `pageRanges` on the other, and **they are
 * independent fields**: setting one does not erase the other. What joins them are the accessors,
 * which answer with this preference:
 *
 * <ul>
 * <li>{@link #getFromPage} gives `fromPage` if it was set; if not, the start of the first range; if
 *     there are no ranges either, {@link #getMinPage}.</li>
 * <li>{@link #getToPage} gives `toPage` if it was set; if not, `fromPage`; if not, the end of the
 *     last range; if not that either, {@link #getMinPage}.</li>
 * <li>{@link #getPageRanges} gives the ranges if they were set; if not, it **synthesises** a single
 *     one out of what the two above answer — it never returns null.</li>
 * </ul>
 *
 * <p>The asymmetry between the first two is not an oversight: `getToPage` looks at `fromPage`
 * before the ranges and `getFromPage` does not look at `toPage`. Checked against JDK 25, which is
 * where these rules come from — a `setFromPage(3)` followed by `setPageRanges({{8,9}})` leaves
 * `from=3` and `to=3`, with the ranges untouched and taking no part.
 *
 * <h2>The nested types</h2>
 *
 * <p>They are five classes with constants instead of `enum` because this API is older than `enum`
 * and changing them would break compatibility. They inherit from {@link AttributeValue}, which
 * gives them the lower-case `toString()` with hyphens (`separate-documents-collated-copies`) and
 * the `hashCode()` by index.
 */
public final class JobAttributes implements Cloneable {

    /** What is printed by default when the user chooses nothing. */
    public static final class DefaultSelectionType extends AttributeValue {

        private static final int I_ALL = 0;
        private static final int I_RANGE = 1;
        private static final int I_SELECTION = 2;

        private static final String[] NAMES = { "all", "range", "selection" };

        /** The whole document. */
        public static final DefaultSelectionType ALL = new DefaultSelectionType(I_ALL);
        /** The page range that is configured. */
        public static final DefaultSelectionType RANGE = new DefaultSelectionType(I_RANGE);
        /** Whatever the user has selected. */
        public static final DefaultSelectionType SELECTION =
                new DefaultSelectionType(I_SELECTION);

        private DefaultSelectionType(int type) {
            super(type, NAMES);
        }
    }

    /** Where the output goes. */
    public static final class DestinationType extends AttributeValue {

        private static final int I_FILE = 0;
        private static final int I_PRINTER = 1;

        private static final String[] NAMES = { "file", "printer" };

        /** To a file, the one {@link JobAttributes#getFileName} says. */
        public static final DestinationType FILE = new DestinationType(I_FILE);
        /** To the printer. */
        public static final DestinationType PRINTER = new DestinationType(I_PRINTER);

        private DestinationType(int type) {
            super(type, NAMES);
        }
    }

    /** Which dialog is shown to the user before printing. */
    public static final class DialogType extends AttributeValue {

        private static final int I_COMMON = 0;
        private static final int I_NATIVE = 1;
        private static final int I_NONE = 2;

        private static final String[] NAMES = { "common", "native", "none" };

        /** The cross-platform dialog. */
        public static final DialogType COMMON = new DialogType(I_COMMON);
        /** The operating system's one. */
        public static final DialogType NATIVE = new DialogType(I_NATIVE);
        /** None: it prints straight away. */
        public static final DialogType NONE = new DialogType(I_NONE);

        private DialogType(int type) {
            super(type, NAMES);
        }
    }

    /**
     * How the copies of a document of several pages are grouped.
     *
     * <p>The difference shows with two copies of a document of three pages: **collated** they come
     * out 1,2,3,1,2,3 and **uncollated** they come out 1,1,2,2,3,3.
     */
    public static final class MultipleDocumentHandlingType extends AttributeValue {

        private static final int I_COLLATED = 0;
        private static final int I_UNCOLLATED = 1;

        private static final String[] NAMES = {
            "separate-documents-collated-copies", "separate-documents-uncollated-copies" };

        /** Collated: each copy complete before the next one. */
        public static final MultipleDocumentHandlingType SEPARATE_DOCUMENTS_COLLATED_COPIES =
                new MultipleDocumentHandlingType(I_COLLATED);
        /** Uncollated: all the copies of each page together. */
        public static final MultipleDocumentHandlingType SEPARATE_DOCUMENTS_UNCOLLATED_COPIES =
                new MultipleDocumentHandlingType(I_UNCOLLATED);

        private MultipleDocumentHandlingType(int type) {
            super(type, NAMES);
        }
    }

    /** Whether it prints on one side or on both, and which edge the sheet is turned on. */
    public static final class SidesType extends AttributeValue {

        private static final int I_ONE_SIDED = 0;
        private static final int I_TWO_SIDED_LONG_EDGE = 1;
        private static final int I_TWO_SIDED_SHORT_EDGE = 2;

        private static final String[] NAMES = {
            "one-sided", "two-sided-long-edge", "two-sided-short-edge" };

        /** On one side only. */
        public static final SidesType ONE_SIDED = new SidesType(I_ONE_SIDED);
        /** On both, turning on the long edge — like a book. */
        public static final SidesType TWO_SIDED_LONG_EDGE =
                new SidesType(I_TWO_SIDED_LONG_EDGE);
        /** On both, turning on the short edge — like a notepad. */
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

    /** The default values: one copy, the whole document, to the printer, native dialog. */
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
     * A copy of `obj`.
     *
     * @throws NullPointerException if `obj` is null
     */
    public JobAttributes(JobAttributes obj) {
        this.set(obj);
    }

    /**
     * With every value given.
     *
     * @throws IllegalArgumentException if any of them is not valid — see the setters, which apply
     *     the same rules
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
     * A copy.
     *
     * <p>Shallow in everything but the ranges, which are really copied: they are the only mutable
     * field —an `int[][]`— and sharing it would let changing the copy change the original.
     */
    public Object clone() {
        JobAttributes copy = new JobAttributes(this);
        return copy;
    }

    /**
     * Takes every value from `obj`.
     *
     * @throws NullPointerException if `obj` is null
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

    /** How many copies. */
    public int getCopies() {
        return this.copies;
    }

    /**
     * @throws IllegalArgumentException if `copies` is less than 1
     */
    public void setCopies(int copies) {
        if (copies <= 0) {
            throw new IllegalArgumentException("Invalid value for attribute copies");
        }
        this.copies = copies;
    }

    /** Goes back to one copy. */
    public void setCopiesToDefault() {
        this.setCopies(1);
    }

    /** What is printed by default. */
    public DefaultSelectionType getDefaultSelection() {
        return this.defaultSelection;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setDefaultSelection(DefaultSelectionType defaultSelection) {
        if (defaultSelection == null) {
            throw new IllegalArgumentException("Invalid value for attribute defaultSelection");
        }
        this.defaultSelection = defaultSelection;
    }

    /** Where the output goes. */
    public DestinationType getDestination() {
        return this.destination;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setDestination(DestinationType destination) {
        if (destination == null) {
            throw new IllegalArgumentException("Invalid value for attribute destination");
        }
        this.destination = destination;
    }

    /** Which dialog is shown. */
    public DialogType getDialog() {
        return this.dialog;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setDialog(DialogType dialog) {
        if (dialog == null) {
            throw new IllegalArgumentException("Invalid value for attribute dialog");
        }
        this.dialog = dialog;
    }

    /** The output file, or null if none was set. */
    public String getFileName() {
        return this.fileName;
    }

    /** Sets the output file. Null is valid and means "no file". */
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /** The first page. See the note of the class about how it is resolved. */
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
     * @throws IllegalArgumentException if it is less than 1, less than {@link #getMinPage}, greater
     *     than {@link #getMaxPage}, or greater than `toPage` if that one was already set
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

    /** The highest page that can be asked for. */
    public int getMaxPage() {
        return this.maxPage;
    }

    /**
     * @throws IllegalArgumentException if it is less than 1 or less than {@link #getMinPage}
     */
    public void setMaxPage(int maxPage) {
        if (maxPage <= 0 || maxPage < this.minPage) {
            throw new IllegalArgumentException("Invalid value for attribute maxPage");
        }
        this.maxPage = maxPage;
    }

    /** The lowest page that can be asked for. */
    public int getMinPage() {
        return this.minPage;
    }

    /**
     * @throws IllegalArgumentException if it is less than 1 or greater than {@link #getMaxPage}
     */
    public void setMinPage(int minPage) {
        if (minPage <= 0 || minPage > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute minPage");
        }
        this.minPage = minPage;
    }

    /** How the copies are grouped. */
    public MultipleDocumentHandlingType getMultipleDocumentHandling() {
        return this.multipleDocumentHandling;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setMultipleDocumentHandling(
            MultipleDocumentHandlingType multipleDocumentHandling) {
        if (multipleDocumentHandling == null) {
            throw new IllegalArgumentException(
                    "Invalid value for attribute multipleDocumentHandling");
        }
        this.multipleDocumentHandling = multipleDocumentHandling;
    }

    /** Goes back to uncollated copies, which is the default. */
    public void setMultipleDocumentHandlingToDefault() {
        this.setMultipleDocumentHandling(
                MultipleDocumentHandlingType.SEPARATE_DOCUMENTS_UNCOLLATED_COPIES);
    }

    /**
     * The page ranges.
     *
     * <p>**It never returns null**: with no explicit ranges it synthesises a single one out of
     * {@link #getFromPage} and {@link #getToPage}. The array is a copy.
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
     * Sets the ranges.
     *
     * <p>Each element is `{from, to}` or `{page}`. The ranges have to go **in order and without
     * overlapping**, which is what makes the printing reproducible: two ranges that step on each
     * other would print the same page twice without whoever wrote them asking for it.
     *
     * @throws NullPointerException if the array is null
     * @throws IllegalArgumentException if it is empty, if some element does not have one or two
     *     numbers, if a range goes backwards, if two overlap or go out of order, or if any falls
     *     outside {@link #getMinPage}..{@link #getMaxPage}
     */
    public void setPageRanges(int[][] pageRanges) {
        if (pageRanges == null) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        if (pageRanges.length == 0) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        int previous = 0;
        int[][] copy = new int[pageRanges.length][2];
        for (int i = 0; i < pageRanges.length; i++) {
            int[] r = pageRanges[i];
            if (r == null || r.length < 1 || r.length > 2) {
                throw new IllegalArgumentException("Invalid value for attribute pageRanges");
            }
            int from = r[0];
            int to = r.length == 2 ? r[1] : r[0];
            if (from <= 0 || to < from || from <= previous) {
                throw new IllegalArgumentException("Invalid value for attribute pageRanges");
            }
            copy[i][0] = from;
            copy[i][1] = to;
            previous = to;
        }
        if (copy[0][0] < this.minPage || copy[copy.length - 1][1] > this.maxPage) {
            throw new IllegalArgumentException("Invalid value for attribute pageRanges");
        }
        this.pageRanges = copy;
    }

    /** The printer, or null if none was set. */
    public String getPrinter() {
        return this.printer;
    }

    /** Sets the printer. Null is valid and means "whichever". */
    public void setPrinter(String printer) {
        this.printer = printer;
    }

    /** On one side or on both. */
    public SidesType getSides() {
        return this.sides;
    }

    /**
     * @throws IllegalArgumentException if it is null
     */
    public void setSides(SidesType sides) {
        if (sides == null) {
            throw new IllegalArgumentException("Invalid value for attribute sides");
        }
        this.sides = sides;
    }

    /** Goes back to one side only. */
    public void setSidesToDefault() {
        this.setSides(SidesType.ONE_SIDED);
    }

    /** The last page. See the note of the class about how it is resolved. */
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
     * @throws IllegalArgumentException if it is less than 1, less than `fromPage` if that one was
     *     already set, or outside {@link #getMinPage}..{@link #getMaxPage}
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

    /** Equality by every field, with the ranges compared by content. */
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

    /** The same format as the JDK: the attributes separated by commas, in alphabetical order. */
    public String toString() {
        StringBuilder ranges = new StringBuilder("[");
        int[][] rs = this.getPageRanges();
        for (int i = 0; i < rs.length; i++) {
            if (i > 0) {
                ranges.append(",");
            }
            ranges.append(rs[i][0]).append(":").append(rs[i][1]);
        }
        ranges.append("]");
        return "copies=" + this.getCopies()
                + ",defaultSelection=" + this.getDefaultSelection()
                + ",destination=" + this.getDestination()
                + ",dialog=" + this.getDialog()
                + ",fileName=" + this.getFileName()
                + ",fromPage=" + this.getFromPage()
                + ",maxPage=" + this.getMaxPage()
                + ",minPage=" + this.getMinPage()
                + ",multiple-document-handling=" + this.getMultipleDocumentHandling()
                + ",page-ranges=" + ranges.toString()
                + ",printer=" + this.getPrinter()
                + ",sides=" + this.getSides()
                + ",toPage=" + this.getToPage();
    }
}
