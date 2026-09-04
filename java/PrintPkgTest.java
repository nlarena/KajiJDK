import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Pageable;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterIOException;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.instrument.UnmodifiableModuleException;
import javax.print.DocFlavor;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.ServiceUI;
import javax.print.ServiceUIFactory;
import javax.print.SimpleDoc;
import javax.print.StreamPrintServiceFactory;
import javax.print.URIException;
import javax.print.event.PrintEvent;
import javax.print.event.PrintJobEvent;

/**
 * Comportamiento de java.lang.instrument, javax.print, javax.print.event y java.awt.print.
 *
 * <p>Cada caso vale contra el JDK 25 real y contra KajiJDK. {@link #run} devuelve -1 si pasan todos, o
 * el indice del primero que falla.
 */
public class PrintPkgTest {

    /** Un transformador que no redefine nada: los dos default tienen que dar null. */
    static class NoopTransformer implements ClassFileTransformer {
    }

    /** Un transformador que redefine solo el de cinco; el de seis tiene que delegar en el. */
    static class FiveArgTransformer implements ClassFileTransformer {
        public byte[] transform(ClassLoader loader, String className, Class<?> being,
                                java.security.ProtectionDomain pd, byte[] buf)
            throws IllegalClassFormatException {
            return new byte[] { 42 };
        }
    }

    /** No dibuja nada; solo hace falta que exista para armar un Book. */
    static class Nothing implements Printable {
        public int print(java.awt.Graphics g, PageFormat pf, int i) {
            return Printable.NO_SUCH_PAGE;
        }
    }

    public static int run() {
        int i = 0;

        // ---- java.lang.instrument ----------------------------------------
        byte[] bytes = new byte[] { 1, 2, 3 };
        ClassDefinition cd = new ClassDefinition(String.class, bytes);
        if (cd.getDefinitionClass() != String.class) {
            return i;
        }
        i++;
        // Los bytes no se copian: el arreglo que sale es el mismo que entro.
        if (cd.getDefinitionClassFile() != bytes) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new ClassDefinition(null, new byte[0]);
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new ClassDefinition(String.class, null);
            }
        })) {
            return i;
        }
        i++;
        try {
            ClassFileTransformer t = new NoopTransformer();
            if (t.transform(null, "a/B", null, null, bytes) != null) {
                return i;
            }
            i++;
            if (t.transform(String.class.getModule(), null, "a/B", null, null, bytes) != null) {
                return i;
            }
            i++;
            // El de seis delega en el de cinco cuando solo ese esta redefinido.
            ClassFileTransformer five = new FiveArgTransformer();
            byte[] out = five.transform(String.class.getModule(), null, "a/B", null, null, bytes);
            if (out == null || out.length != 1 || out[0] != 42) {
                return i;
            }
            i++;
        } catch (IllegalClassFormatException e) {
            return i;
        }
        if (!(new UnmodifiableModuleException("x") instanceof RuntimeException)) {
            return i;
        }
        i++;
        // La de clases si es comprobada; la de modulos no.
        if (RuntimeException.class.isInstance(new UnmodifiableClassException("x"))) {
            return i;
        }
        i++;
        if (RuntimeException.class.isInstance(new IllegalClassFormatException("x"))) {
            return i;
        }
        i++;

        // ---- javax.print: DocFlavor --------------------------------------
        if (!DocFlavor.BYTE_ARRAY.PDF.getMimeType().equals("application/pdf")) {
            return i;
        }
        i++;
        if (!DocFlavor.BYTE_ARRAY.PDF.getRepresentationClassName().equals("[B")) {
            return i;
        }
        i++;
        if (!DocFlavor.INPUT_STREAM.PDF.getRepresentationClassName().equals("java.io.InputStream")) {
            return i;
        }
        i++;
        if (!DocFlavor.BYTE_ARRAY.AUTOSENSE.getMimeType().equals("application/octet-stream")) {
            return i;
        }
        i++;
        if (!DocFlavor.BYTE_ARRAY.PCL.getMimeType().equals("application/vnd.hp-pcl")) {
            return i;
        }
        i++;
        if (!DocFlavor.STRING.TEXT_PLAIN.getMimeType().equals("text/plain; charset=\"utf-16\"")) {
            return i;
        }
        i++;
        if (!DocFlavor.CHAR_ARRAY.TEXT_HTML.getRepresentationClassName().equals("[C")) {
            return i;
        }
        i++;
        if (!DocFlavor.SERVICE_FORMATTED.PRINTABLE.getMimeType()
                .equals("application/x-java-jvm-local-objectref")) {
            return i;
        }
        i++;
        if (!DocFlavor.SERVICE_FORMATTED.PAGEABLE.getRepresentationClassName()
                .equals("java.awt.print.Pageable")) {
            return i;
        }
        i++;
        if (!DocFlavor.STRING.TEXT_PLAIN.toString()
                .equals("text/plain; charset=\"utf-16\"; class=\"java.lang.String\"")) {
            return i;
        }
        i++;
        // La normalizacion: mayusculas, orden de parametros, y el charset a minusculas.
        DocFlavor mixed = new DocFlavor("Text/Plain; CharSet=\"Utf-8\"; b=2; a=1",
                                        "java.lang.String");
        if (!mixed.getMimeType().equals("text/plain; a=\"1\"; b=\"2\"; charset=\"utf-8\"")) {
            return i;
        }
        i++;
        if (!mixed.getMediaType().equals("text") || !mixed.getMediaSubtype().equals("plain")) {
            return i;
        }
        i++;
        // El nombre del parametro se busca sin distinguir mayusculas.
        if (!"utf-8".equals(mixed.getParameter("CHARSET"))) {
            return i;
        }
        i++;
        if (mixed.getParameter("noesta") != null) {
            return i;
        }
        i++;
        // Pero el valor de un parametro cualquiera se respeta tal cual.
        DocFlavor cased = new DocFlavor("text/plain; NAME=MixedCase", "java.lang.String");
        if (!"MixedCase".equals(cased.getParameter("name"))) {
            return i;
        }
        i++;
        if (!cased.getMimeType().equals("text/plain; name=\"MixedCase\"")) {
            return i;
        }
        i++;
        // Dos escrituras equivalentes son el mismo formato.
        DocFlavor a = new DocFlavor("TEXT/PLAIN", "java.lang.String");
        DocFlavor b = new DocFlavor("text/plain", "java.lang.String");
        if (!a.equals(b) || a.hashCode() != b.hashCode()) {
            return i;
        }
        i++;
        // Pero distinta representacion es distinto formato.
        if (b.equals(new DocFlavor("text/plain", "java.io.Reader"))) {
            return i;
        }
        i++;
        if (b.equals(null) || b.equals("text/plain")) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new DocFlavor(null, "java.lang.String");
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                new DocFlavor("text/plain", null);
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new DocFlavor("basura", "java.lang.String");
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new DocFlavor("text/plain;=x", "java.lang.String");
            }
        })) {
            return i;
        }
        i++;

        // ---- javax.print: SimpleDoc --------------------------------------
        SimpleDoc sd = new SimpleDoc("hola", DocFlavor.STRING.TEXT_PLAIN, null);
        if (!"hola".equals(get(sd)) || sd.getAttributes() != null) {
            return i;
        }
        i++;
        try {
            Reader r1 = sd.getReaderForText();
            if (r1 == null || r1 != sd.getReaderForText()) {
                return i;
            }
        } catch (IOException e) {
            return i;
        }
        i++;
        // Un String no es bytes: aca va null, y eso no es un error.
        try {
            if (sd.getStreamForBytes() != null) {
                return i;
            }
        } catch (IOException e) {
            return i;
        }
        i++;
        SimpleDoc bd = new SimpleDoc(new byte[] { 104, 105 },
                                     DocFlavor.BYTE_ARRAY.TEXT_PLAIN_UTF_8, null);
        try {
            InputStream in = bd.getStreamForBytes();
            if (in == null || in != bd.getStreamForBytes()) {
                return i;
            }
        } catch (IOException e) {
            return i;
        }
        i++;
        try {
            if (bd.getReaderForText() != null) {
                return i;
            }
        } catch (IOException e) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new SimpleDoc("x", null, null);
            }
        })) {
            return i;
        }
        i++;
        // El dato tiene que ser de la clase que declara el formato.
        if (!iae(new Runnable() {
            public void run() {
                new SimpleDoc(new Object(), DocFlavor.STRING.TEXT_PLAIN, null);
            }
        })) {
            return i;
        }
        i++;

        // ---- javax.print: constantes y busquedas -------------------------
        if (URIException.URIInaccessible != 1 || URIException.URISchemeNotSupported != 2
            || URIException.URIOtherProblem != -1) {
            return i;
        }
        i++;
        if (ServiceUIFactory.ABOUT_UIROLE != 1 || ServiceUIFactory.ADMIN_UIROLE != 2
            || ServiceUIFactory.MAIN_UIROLE != 3 || ServiceUIFactory.RESERVED_UIROLE != 99) {
            return i;
        }
        i++;
        if (!ServiceUIFactory.JCOMPONENT_UI.equals("javax.swing.JComponent")
            || !ServiceUIFactory.DIALOG_UI.equals("java.awt.Dialog")) {
            return i;
        }
        i++;
        if (PrintServiceLookup.registerService(null)) {
            return i;
        }
        i++;
        if (PrintServiceLookup.lookupPrintServices(null, null) == null) {
            return i;
        }
        i++;
        if (StreamPrintServiceFactory.lookupStreamPrintServiceFactories(null, null) == null) {
            return i;
        }
        i++;
        // Sin pantalla, el HeadlessException gana sobre la validacion de argumentos: el JDK 25 con
        // -Djava.awt.headless=true hace lo mismo.
        if (!headless(new Runnable() {
            public void run() {
                ServiceUI.printDialog(null, 0, 0, null, null, null, null);
            }
        })) {
            return i;
        }
        i++;
        if (!headless(new Runnable() {
            public void run() {
                ServiceUI.printDialog(null, 0, 0, new PrintService[0], null, null, null);
            }
        })) {
            return i;
        }
        i++;
        if (!(new PrintException("x") instanceof Exception)) {
            return i;
        }
        i++;

        // ---- javax.print.event -------------------------------------------
        PrintEvent pe = new PrintEvent("src");
        if (!pe.toString().equals("PrintEvent on src") || !"src".equals(pe.getSource())) {
            return i;
        }
        i++;
        // Fuente null da IllegalArgumentException, no NullPointerException.
        if (!iae(new Runnable() {
            public void run() {
                new PrintEvent(null);
            }
        })) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new PrintJobEvent(null, PrintJobEvent.JOB_COMPLETE);
            }
        })) {
            return i;
        }
        i++;
        if (PrintJobEvent.JOB_CANCELED != 101 || PrintJobEvent.JOB_COMPLETE != 102
            || PrintJobEvent.JOB_FAILED != 103 || PrintJobEvent.REQUIRES_ATTENTION != 104
            || PrintJobEvent.NO_MORE_EVENTS != 105
            || PrintJobEvent.DATA_TRANSFER_COMPLETE != 106) {
            return i;
        }
        i++;

        // ---- java.awt.print: Paper ---------------------------------------
        Paper p = new Paper();
        if (p.getWidth() != 612.0 || p.getHeight() != 792.0) {
            return i;
        }
        i++;
        if (p.getImageableX() != 72.0 || p.getImageableY() != 72.0
            || p.getImageableWidth() != 468.0 || p.getImageableHeight() != 648.0) {
            return i;
        }
        i++;
        // No valida: acepta un area negativa y mas grande que la hoja.
        Paper q = new Paper();
        q.setSize(100, 200);
        q.setImageableArea(-5, -5, 500, 500);
        if (q.getImageableX() != -5.0 || q.getImageableWidth() != 500.0) {
            return i;
        }
        i++;
        Paper qc = (Paper) q.clone();
        qc.setSize(1, 1);
        if (q.getWidth() != 100.0) {
            return i;
        }
        i++;

        // ---- java.awt.print: PageFormat ----------------------------------
        if (PageFormat.LANDSCAPE != 0 || PageFormat.PORTRAIT != 1
            || PageFormat.REVERSE_LANDSCAPE != 2) {
            return i;
        }
        i++;
        PageFormat pf = new PageFormat();
        if (pf.getOrientation() != PageFormat.PORTRAIT) {
            return i;
        }
        i++;
        if (pf.getWidth() != 612.0 || pf.getHeight() != 792.0 || pf.getImageableX() != 72.0
            || pf.getImageableWidth() != 468.0 || pf.getImageableHeight() != 648.0) {
            return i;
        }
        i++;
        if (!sameMatrix(pf.getMatrix(), new double[] { 1, 0, 0, 1, 0, 0 })) {
            return i;
        }
        i++;
        // getPaper devuelve una copia: tocarla no cambia el formato.
        Paper got = pf.getPaper();
        got.setSize(1, 1);
        if (pf.getWidth() != 612.0 || pf.getPaper() == pf.getPaper()) {
            return i;
        }
        i++;
        pf.setOrientation(PageFormat.LANDSCAPE);
        if (pf.getWidth() != 792.0 || pf.getHeight() != 612.0) {
            return i;
        }
        i++;
        if (pf.getImageableWidth() != 648.0 || pf.getImageableHeight() != 468.0) {
            return i;
        }
        i++;
        if (!sameMatrix(pf.getMatrix(), new double[] { 0, -1, 1, 0, 0, 792 })) {
            return i;
        }
        i++;
        pf.setOrientation(PageFormat.REVERSE_LANDSCAPE);
        if (!sameMatrix(pf.getMatrix(), new double[] { 0, 1, -1, 0, 612, 0 })) {
            return i;
        }
        i++;
        if (pf.getWidth() != 792.0 || pf.getImageableX() != 72.0 || pf.getImageableY() != 72.0) {
            return i;
        }
        i++;
        // La rotacion se nota con margenes asimetricos, que es donde vale el caso.
        PageFormat asym = new PageFormat();
        Paper ap = asym.getPaper();
        ap.setImageableArea(10, 20, 300, 400);
        asym.setPaper(ap);
        asym.setOrientation(PageFormat.LANDSCAPE);
        if (asym.getImageableX() != 792.0 - (20.0 + 400.0) || asym.getImageableY() != 10.0) {
            return i;
        }
        i++;
        if (asym.getImageableWidth() != 400.0 || asym.getImageableHeight() != 300.0) {
            return i;
        }
        i++;
        asym.setOrientation(PageFormat.REVERSE_LANDSCAPE);
        if (asym.getImageableX() != 20.0 || asym.getImageableY() != 612.0 - (10.0 + 300.0)) {
            return i;
        }
        i++;
        if (!iae(new Runnable() {
            public void run() {
                new PageFormat().setOrientation(7);
            }
        })) {
            return i;
        }
        i++;
        PageFormat pfc = (PageFormat) asym.clone();
        if (pfc.getOrientation() != PageFormat.REVERSE_LANDSCAPE || pfc.getPaper() == asym.getPaper()) {
            return i;
        }
        i++;

        // ---- java.awt.print: Book ----------------------------------------
        if (Pageable.UNKNOWN_NUMBER_OF_PAGES != -1 || Printable.PAGE_EXISTS != 0
            || Printable.NO_SUCH_PAGE != 1) {
            return i;
        }
        i++;
        final Book book = new Book();
        if (book.getNumberOfPages() != 0) {
            return i;
        }
        i++;
        if (!ioobe(new Runnable() {
            public void run() {
                book.getPageFormat(0);
            }
        })) {
            return i;
        }
        i++;
        final Printable painter = new Nothing();
        book.append(painter, new PageFormat());
        book.append(painter, new PageFormat(), 3);
        // La cantidad agrega entradas, no repite una pagina.
        if (book.getNumberOfPages() != 4) {
            return i;
        }
        i++;
        if (book.getPrintable(3) != painter || book.getPageFormat(3) == null) {
            return i;
        }
        i++;
        // Las tres del append con cantidad comparten formato.
        if (book.getPageFormat(1) != book.getPageFormat(3)) {
            return i;
        }
        i++;
        if (!ioobe(new Runnable() {
            public void run() {
                book.setPage(9, painter, new PageFormat());
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                book.append(null, new PageFormat());
            }
        })) {
            return i;
        }
        i++;
        if (!npe(new Runnable() {
            public void run() {
                book.append(painter, null);
            }
        })) {
            return i;
        }
        i++;

        // ---- java.awt.print: excepciones y PrinterJob ---------------------
        IOException cause = new IOException("boom");
        PrinterIOException pio = new PrinterIOException(cause);
        // Las dos formas de sacar la causa dan lo mismo, y no hay mensaje.
        if (pio.getIOException() != cause || pio.getCause() != cause
            || pio.getMessage() != null) {
            return i;
        }
        i++;
        if (!(new PrinterAbortException() instanceof PrinterException)) {
            return i;
        }
        i++;
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job == null || job.getCopies() != 1 || !"Java Printing".equals(job.getJobName())) {
            return i;
        }
        i++;
        job.setJobName("mio");
        job.setCopies(3);
        if (job.getCopies() != 3 || !"mio".equals(job.getJobName()) || job.isCancelled()) {
            return i;
        }
        i++;
        // cancel() solo actua sobre una impresion en curso; fuera de una no hace nada.
        job.cancel();
        if (job.isCancelled()) {
            return i;
        }
        i++;
        PageFormat dp = job.defaultPage();
        if (dp.getWidth() != 612.0 || dp.getImageableX() != 72.0) {
            return i;
        }
        i++;
        // validatePage acota el area imprimible a la hoja.
        PageFormat bad = new PageFormat();
        Paper bp = bad.getPaper();
        bp.setImageableArea(-10, -10, 5000, 5000);
        bad.setPaper(bp);
        PageFormat fixed = job.validatePage(bad);
        if (fixed.getImageableX() != 0.0 || fixed.getImageableY() != 0.0
            || fixed.getImageableWidth() != 612.0 || fixed.getImageableHeight() != 792.0) {
            return i;
        }
        i++;
        final PrinterJob j2 = PrinterJob.getPrinterJob();
        if (!npe(new Runnable() {
            public void run() {
                j2.setPageable(null);
            }
        })) {
            return i;
        }
        i++;
        // Idem: la subclase concreta comprueba la pantalla antes que el null.
        if (!headless(new Runnable() {
            public void run() {
                j2.printDialog(null);
            }
        })) {
            return i;
        }
        i++;
        if (!headless(new Runnable() {
            public void run() {
                j2.pageDialog((javax.print.attribute.PrintRequestAttributeSet) null);
            }
        })) {
            return i;
        }
        i++;
        if (PrinterJob.lookupPrintServices() == null
            || PrinterJob.lookupStreamPrintServices(null) == null) {
            return i;
        }
        i++;

        return -1;
    }

    /**
     * Lo que KajiJDK hace y el JDK 25 no.
     *
     * <p>Un solo caso: {@code PrintServiceLookup.registerServiceProvider(null)}. El JDK devuelve
     * true, o sea afirma haber registrado un proveedor nulo; nosotros devolvemos false, que es lo que
     * dice el contrato del metodo -- "true si el proveedor se registro" -- y lo unico cierto.
     */
    public static int runKaji() {
        int i = 0;
        if (PrintServiceLookup.registerServiceProvider(null)) {
            return i;
        }
        i++;
        return -1;
    }

    /** El dato de un Doc, tragandose la IOException que aca no puede pasar. */
    private static Object get(SimpleDoc d) {
        try {
            return d.getPrintData();
        } catch (IOException e) {
            return null;
        }
    }

    /** Compara dos matrices de seis. */
    private static boolean sameMatrix(double[] got, double[] want) {
        if (got == null || got.length != want.length) {
            return false;
        }
        int k = 0;
        while (k < want.length) {
            if (got[k] != want[k]) {
                return false;
            }
            k++;
        }
        return true;
    }

    private static boolean npe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (NullPointerException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean iae(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean headless(Runnable r) {
        try {
            r.run();
            return false;
        } catch (java.awt.HeadlessException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean ioobe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IndexOutOfBoundsException e) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("kaji")) {
            System.out.println(runKaji());
            return;
        }
        System.out.println(run());
    }
}
