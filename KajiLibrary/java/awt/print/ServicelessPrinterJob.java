package java.awt.print;

import java.awt.HeadlessException;
import javax.print.attribute.PrintRequestAttributeSet;

/**
 * El trabajo que devuelve {@code PrinterJob.getPrinterJob()} cuando no hay sistema de impresion.
 *
 * <p>De acceso de paquete: no es API, es el detalle que hace que {@code getPrinterJob} pueda cumplir su
 * contrato de no devolver null. Ver la nota de {@link PrinterJob} sobre el alcance.
 *
 * <p>Lo que depende de la maquina falla de forma declarada; el resto --el nombre, las copias, la
 * cancelacion, el ajuste de formatos-- funciona de verdad, porque no necesita impresora.
 */
final class ServicelessPrinterJob extends PrinterJob {

    /** Lo que se pidio imprimir; se guarda aunque no se pueda. */
    private Pageable document;

    /** Cuantas copias. */
    private int copies = 1;

    /** El nombre en la cola. */
    private String jobName = "Java Printing";

    /**
     * Si hay una impresion en curso. Nunca lo hay, porque {@link #print} falla enseguida.
     *
     * <p>Existe porque {@link #cancel} solo tiene efecto durante una impresion, y sin este campo el
     * motivo de que no haga nada no se leeria en el codigo.
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

    /** No hay pantalla. */
    @Override
    public boolean printDialog() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * No hay pantalla, y eso gana sobre el control de null de la clase base.
     *
     * <p>Es lo que hace el JDK sin pantalla: su subclase concreta comprueba primero y por eso
     * {@code printDialog(null)} alla tampoco da {@code NullPointerException}.
     */
    @Override
    public boolean printDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        throw new HeadlessException();
    }

    /** No hay pantalla. */
    @Override
    public PageFormat pageDialog(PageFormat page) throws HeadlessException {
        throw new HeadlessException();
    }

    /** Idem {@link #printDialog(PrintRequestAttributeSet)}. */
    @Override
    public PageFormat pageDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        throw new HeadlessException();
    }

    /** Sin impresora contra que ajustar, una copia tal cual. */
    @Override
    public PageFormat defaultPage(PageFormat page) {
        return (PageFormat) page.clone();
    }

    /**
     * Acota el area imprimible a la hoja.
     *
     * <p>Es lo unico que se puede validar sin saber los margenes mecanicos de una impresora concreta, y
     * ya alcanza para arreglar el error tipico: un area negativa o mas grande que el papel.
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

    /** No hay a donde mandarlo. */
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

    /** El usuario del sistema, si se puede leer. */
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
     * Cancela la impresion en curso, si la hubiera.
     *
     * <p>No hay ninguna nunca, asi que no hace nada. Es lo mismo que hace el JDK cuando se lo llama
     * fuera de una impresion --se comprobo contra el JDK 25--, y lo que dice la documentacion del
     * metodo: cancela un trabajo <b>en curso</b>.
     */
    @Override
    public void cancel() {
        if (this.printing) {
            this.printing = false;
        }
    }

    /** Siempre false; ver {@link #cancel}. */
    @Override
    public boolean isCancelled() {
        return false;
    }
}
