package java.awt.print;

import java.awt.HeadlessException;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * The job {@code PrinterJob.getPrinterJob()} returns when there is no printing system.
 *
 * <p>Package-private: it is not API, it is the detail that lets {@code getPrinterJob} keep its
 * contract of never returning null. See the note in {@link PrinterJob} on the scope.
 *
 * <p>What depends on the machine fails in a declared way; the rest --the name, the copies,
 * cancellation, adjusting formats-- behaves as the JDK's does, because it needs no printer.
 */
final class ServicelessPrinterJob extends PrinterJob {

    /** What was asked to be printed; kept even though it cannot be printed. */
    private Pageable document;

    /** How many copies. */
    private int copies = 1;

    /** The name in the queue. */
    private String jobName = "Java Printing";

    /**
     * Whether a print is in progress. There never is, because {@link #print} fails at once.
     *
     * <p>It exists because {@link #cancel} only has an effect during a print, and without this
     * field the reason it does nothing would not be readable in the code.
     */
    private volatile boolean printing = false;

    ServicelessPrinterJob() {
    }

    @Override
    public void setPrintable(Printable painter) {
        setPrintable(painter, new PageFormat());
    }

    @Override
    public void setPrintable(Printable painter, PageFormat format) {
        Book book = new Book();
        book.append(painter, format);
        this.document = book;
    }

    @Override
    public void setPageable(Pageable document) throws NullPointerException {
        if (document == null) {
            throw new NullPointerException();
        }
        this.document = document;
    }

    /** There is no screen. */
    @Override
    public boolean printDialog() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * There is no screen, and that wins over the base class's null check.
     *
     * <p>It is what the JDK does with no screen: its concrete subclass checks first, and that is
     * why {@code printDialog(null)} does not give a {@code NullPointerException} there either.
     */
    @Override
    public boolean printDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        throw new HeadlessException();
    }

    /** There is no screen. */
    @Override
    public PageFormat pageDialog(PageFormat page) throws HeadlessException {
        throw new HeadlessException();
    }

    /** The same as {@link #printDialog(PrintRequestAttributeSet)}. */
    @Override
    public PageFormat pageDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        throw new HeadlessException();
    }

    /** With no printer to adjust against, a copy as is. */
    @Override
    public PageFormat defaultPage(PageFormat page) {
        return (PageFormat) page.clone();
    }

    /**
     * Narrows the imageable area to the sheet.
     *
     * <p>It is the only thing that can be validated without knowing the mechanical margins of a
     * concrete printer, and it is already enough to fix the typical mistake: a negative area or one
     * larger than the paper.
     */
    @Override
    public PageFormat validatePage(PageFormat page) {
        PageFormat copy = (PageFormat) page.clone();
        Paper paper = copy.getPaper();
        double x = Math.max(0.0, paper.getImageableX());
        double y = Math.max(0.0, paper.getImageableY());
        double w = Math.min(paper.getImageableWidth(), paper.getWidth() - x);
        double h = Math.min(paper.getImageableHeight(), paper.getHeight() - y);
        paper.setImageableArea(x, y, Math.max(0.0, w), Math.max(0.0, h));
        copy.setPaper(paper);
        return copy;
    }

    /** There is nowhere to send it. */
    @Override
    public void print() throws PrinterException {
        throw new PrinterException("No print service found.");
    }

    @Override
    public void setCopies(int copies) {
        this.copies = copies;
    }

    @Override
    public int getCopies() {
        return this.copies;
    }

    /** The system user, if it can be read. */
    @Override
    public String getUserName() {
        try {
            return System.getProperty("user.name");
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    @Override
    public String getJobName() {
        return this.jobName;
    }

    /**
     * Cancels the print in progress, if there were one.
     *
     * <p>There never is, so it does nothing. It is the same as the JDK does when it is called
     * outside a print --checked against JDK 25--, and what the method's documentation says: it
     * cancels a job <b>in progress</b>.
     */
    @Override
    public void cancel() {
        if (this.printing) {
            this.printing = false;
        }
    }

    /** Always false; see {@link #cancel}. */
    @Override
    public boolean isCancelled() {
        return false;
    }
}
