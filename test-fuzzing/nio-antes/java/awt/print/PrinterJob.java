package java.awt.print;

import java.awt.HeadlessException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.StreamPrintServiceFactory;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Media;
import javax.print.attribute.standard.MediaPrintableArea;
import javax.print.attribute.standard.MediaSize;
import javax.print.attribute.standard.MediaSizeName;
import javax.print.attribute.standard.OrientationRequested;

/**
 * KajiLibrary's java.awt.print.PrinterJob -- a print job of the old system.
 *
 * <p>The entry point of {@code java.awt.print}: one is requested with {@link #getPrinterJob}, told
 * what to print with {@link #setPrintable} or {@link #setPageable}, and {@link #print} is called.
 *
 * <h2>The two printing systems</h2>
 *
 * <p>This package and {@code javax.print} coexist and are not the same. This one is oriented to
 * <b>drawing</b> --it is given an object that paints pages--; the other is oriented to
 * <b>documents</b> --it is given a ready-made PDF or PostScript--.
 *
 * <p>They meet at one point: {@link #setPrintService} and {@link #lookupPrintServices} use the new
 * system's {@link PrintService}. So in the JDK a printer can be chosen with the new API and drawn
 * to with the old one, which is what most code that prints graphics does.
 *
 * <h2>{@link #print} blocks</h2>
 *
 * <p>It returns when the job has been delivered, and meanwhile it calls {@code Printable.print}
 * many times --several per page; see {@link Printable}--. It has to be called off the UI thread.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>This library does not talk to the operating system's printing system, which requires native
 * code. {@link #getPrinterJob} returns a real job --it keeps its name and its copies, and accepts
 * {@code cancel}, which outside a print does nothing, as in the JDK-- that throws
 * {@link PrinterException} with "No print service found." when printing, which is what the JDK does
 * on a machine with no printers. The dialogs throw {@link HeadlessException}, which is what they
 * already declare and what fits with no screen.
 *
 * <p>Everything that does not depend on the system is really implemented, including
 * {@link #getPageFormat(PrintRequestAttributeSet)}, which translates attributes into a
 * {@link PageFormat}. This note left out that the job has no print service here: it inherits
 * {@link #setPrintService}, which always throws, so no printer can be chosen with the new API, and
 * {@code getPageFormat} therefore always returns the default page.
 */
public abstract class PrinterJob {

    /** For subclasses. */
    public PrinterJob() {
    }

    /**
     * A new job, associated with the default printer.
     *
     * <p>It never returns null. See the class note on what the one returned here can do.
     */
    public static PrinterJob getPrinterJob() {
        return new ServicelessPrinterJob();
    }

    /**
     * The printers that accept drawings from this system.
     *
     * <p>It is a shortcut for {@code PrintServiceLookup.lookupPrintServices} filtering by the
     * {@code SERVICE_FORMATTED.PAGEABLE} flavor, which is the one that corresponds to drawing.
     */
    public static PrintService[] lookupPrintServices() {
        return PrintServiceLookup.lookupPrintServices(
            javax.print.DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
    }

    /**
     * The factories that convert drawings to that MIME type.
     *
     * @param mimeType what to write, or null for all
     */
    public static StreamPrintServiceFactory[] lookupStreamPrintServices(String mimeType) {
        return StreamPrintServiceFactory.lookupStreamPrintServiceFactories(
            javax.print.DocFlavor.SERVICE_FORMATTED.PAGEABLE, mimeType);
    }

    /** Which printer it goes to, or null if none was chosen. */
    public PrintService getPrintService() {
        return null;
    }

    /**
     * Chooses the printer.
     *
     * @throws PrinterException if that printer is not suitable for this job; this base
     *     implementation always throws, as the JDK's does
     */
    public void setPrintService(PrintService service) throws PrinterException {
        throw new PrinterException("Setting a service is not supported on this class");
    }

    /** What to draw, with the default format. */
    public abstract void setPrintable(Printable painter);

    /** What to draw, with that format for every page. */
    public abstract void setPrintable(Printable painter, PageFormat format);

    /**
     * What to draw, with a format per page.
     *
     * @throws NullPointerException if it is null
     */
    public abstract void setPageable(Pageable document) throws NullPointerException;

    /**
     * Shows the print dialog.
     *
     * @return whether the user accepted
     * @throws HeadlessException if there is no screen
     */
    public abstract boolean printDialog() throws HeadlessException;

    /**
     * The same, filled in from those attributes and returning what was chosen in them.
     *
     * <p>As in {@code javax.print.ServiceUI}, the set is for input <b>and</b> output.
     *
     * @throws HeadlessException if there is no screen
     */
    public boolean printDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        if (attributes == null) {
            throw new NullPointerException("attributes");
        }
        return printDialog();
    }

    /**
     * Shows the page setup dialog.
     *
     * @return the chosen format, or the same one that was passed if it was cancelled
     * @throws HeadlessException if there is no screen
     */
    public abstract PageFormat pageDialog(PageFormat page) throws HeadlessException;

    /**
     * The same, starting from attributes.
     *
     * @throws HeadlessException if there is no screen
     */
    public PageFormat pageDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        if (attributes == null) {
            throw new NullPointerException("attributes");
        }
        return pageDialog(defaultPage());
    }

    /** A copy of that format adjusted to what the printer can do. */
    public abstract PageFormat defaultPage(PageFormat page);

    /** The printer's default format. */
    public PageFormat defaultPage() {
        return defaultPage(new PageFormat());
    }

    /**
     * The {@link PageFormat} those attributes describe.
     *
     * <p>It looks at three: {@link Media} for the sheet size, {@link MediaPrintableArea} for the
     * imageable area and {@link OrientationRequested} for the orientation. The ones not present
     * stay as in {@link #defaultPage}.
     *
     * <p>It only applies the ones the chosen printer supports, so with no printer it returns the
     * default format as is.
     */
    public PageFormat getPageFormat(PrintRequestAttributeSet attributes) {
        PrintService service = getPrintService();
        PageFormat pf = defaultPage();
        if (service == null || attributes == null) {
            return pf;
        }
        Media media = (Media) attributes.get(Media.class);
        MediaPrintableArea mpa = (MediaPrintableArea) attributes.get(MediaPrintableArea.class);
        OrientationRequested orientReq =
            (OrientationRequested) attributes.get(OrientationRequested.class);
        if (media == null && mpa == null && orientReq == null) {
            return pf;
        }
        Paper paper = pf.getPaper();
        if (mpa == null && media != null
            && service.isAttributeCategorySupported(MediaPrintableArea.class)) {
            Object mpaVals =
                service.getSupportedAttributeValues(MediaPrintableArea.class, null, attributes);
            if (mpaVals instanceof MediaPrintableArea[]
                && ((MediaPrintableArea[]) mpaVals).length > 0) {
                mpa = ((MediaPrintableArea[]) mpaVals)[0];
            }
        }
        if (media != null && service.isAttributeValueSupported(media, null, attributes)
            && media instanceof MediaSizeName) {
            MediaSize msz = MediaSize.getMediaSizeForName((MediaSizeName) media);
            if (msz != null) {
                double inch = 72.0;
                double paperWid = msz.getX(MediaSize.INCH) * inch;
                double paperHgt = msz.getY(MediaSize.INCH) * inch;
                paper.setSize(paperWid, paperHgt);
                if (mpa == null) {
                    paper.setImageableArea(inch, inch, paperWid - 2 * inch, paperHgt - 2 * inch);
                }
            }
        }
        if (mpa != null && service.isAttributeValueSupported(mpa, null, attributes)) {
            float[] printableArea = mpa.getPrintableArea(MediaPrintableArea.INCH);
            int i = 0;
            while (i < printableArea.length) {
                printableArea[i] = printableArea[i] * 72.0f;
                i = i + 1;
            }
            paper.setImageableArea(printableArea[0], printableArea[1],
                                   printableArea[2], printableArea[3]);
        }
        if (orientReq != null && service.isAttributeValueSupported(orientReq, null, attributes)) {
            int orient;
            if (orientReq.equals(OrientationRequested.REVERSE_LANDSCAPE)) {
                orient = PageFormat.REVERSE_LANDSCAPE;
            } else if (orientReq.equals(OrientationRequested.LANDSCAPE)) {
                orient = PageFormat.LANDSCAPE;
            } else {
                orient = PageFormat.PORTRAIT;
            }
            pf.setOrientation(orient);
        }
        pf.setPaper(paper);
        return validatePage(pf);
    }

    /**
     * A copy of that format with the imageable area narrowed to what the printer can do.
     *
     * <p>It is what corrects a {@link Paper} put together by hand, which validates nothing.
     */
    public abstract PageFormat validatePage(PageFormat page);

    /**
     * Prints. It blocks; see the class note.
     *
     * @throws PrinterException if it failed
     */
    public abstract void print() throws PrinterException;

    /**
     * The same, with those attributes.
     *
     * @throws PrinterException if it failed
     */
    public void print(PrintRequestAttributeSet attributes) throws PrinterException {
        print();
    }

    /** How many copies. */
    public abstract void setCopies(int copies);

    /** How many copies. */
    public abstract int getCopies();

    /** In whose name the job goes. */
    public abstract String getUserName();

    /** The name seen in the queue. */
    public abstract void setJobName(String jobName);

    /** The name seen in the queue. */
    public abstract String getJobName();

    /**
     * Requests cancellation.
     *
     * <p>It is asynchronous and called from another thread: the one that called {@link #print} is
     * blocked. What happens is that the next call to {@code Printable.print} does not occur and
     * {@code print} exits with {@link PrinterAbortException}.
     */
    public abstract void cancel();

    /** Whether a print in progress is going to be cancelled. */
    public abstract boolean isCancelled();
}
