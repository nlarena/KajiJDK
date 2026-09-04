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
 * KajiLibrary's java.awt.print.PrinterJob -- un trabajo de impresion del sistema viejo.
 *
 * <p>El punto de entrada de {@code java.awt.print}: se pide uno con {@link #getPrinterJob}, se le dice
 * que imprimir con {@link #setPrintable} o {@link #setPageable}, y se llama {@link #print}.
 *
 * <h2>Los dos sistemas de impresion</h2>
 *
 * <p>Este paquete y {@code javax.print} conviven y no son lo mismo. Este esta orientado a
 * <b>dibujar</b> --se le da un objeto que pinta paginas--; el otro esta orientado a <b>documentos</b>
 * --se le da un PDF o un PostScript ya hecho--.
 *
 * <p>Se cruzan en un punto: {@link #setPrintService} y {@link #lookupPrintServices} usan los
 * {@link PrintService} del sistema nuevo. Asi que se puede elegir la impresora con la API nueva y
 * dibujar con la vieja, que es lo que hace la mayoria del codigo que imprime graficos.
 *
 * <h2>{@link #print} bloquea</h2>
 *
 * <p>Vuelve cuando el trabajo se entrego, y mientras tanto llama a {@code Printable.print} muchas
 * veces --varias por pagina; ver {@link Printable}--. Hay que llamarla fuera del hilo de la interfaz.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no habla con el sistema de impresion del sistema operativo, que pide codigo
 * nativo. {@link #getPrinterJob} devuelve un trabajo de verdad --lleva su nombre, sus copias, se deja
 * cancelar-- que al imprimir lanza {@link PrinterException} con "No print service found", que es lo
 * mismo que hace el JDK en una maquina sin impresoras. Los dialogos lanzan {@link HeadlessException},
 * que es lo que ya declaran y lo que corresponde sin pantalla.
 *
 * <p>Todo lo que no depende del sistema esta implementado de verdad, incluido
 * {@link #getPageFormat(PrintRequestAttributeSet)}, que traduce atributos a un {@link PageFormat}.
 */
public abstract class PrinterJob {

    /** Para las subclases. */
    public PrinterJob() {
    }

    /**
     * Un trabajo nuevo, asociado a la impresora por omision.
     *
     * <p>Nunca devuelve null. Ver la nota de la clase sobre que puede hacer el que devuelve aca.
     */
    public static PrinterJob getPrinterJob() {
        return new ServicelessPrinterJob();
    }

    /**
     * Las impresoras que aceptan dibujos de este sistema.
     *
     * <p>Es un atajo de {@code PrintServiceLookup.lookupPrintServices} filtrando por el formato
     * {@code SERVICE_FORMATTED.PAGEABLE}, que es el que corresponde a dibujar.
     */
    public static PrintService[] lookupPrintServices() {
        return PrintServiceLookup.lookupPrintServices(
            javax.print.DocFlavor.SERVICE_FORMATTED.PAGEABLE, null);
    }

    /**
     * Las fabricas que convierten dibujos a ese tipo MIME.
     *
     * @param mimeType que escribir, o null para todas
     */
    public static StreamPrintServiceFactory[] lookupStreamPrintServices(String mimeType) {
        return StreamPrintServiceFactory.lookupStreamPrintServiceFactories(
            javax.print.DocFlavor.SERVICE_FORMATTED.PAGEABLE, mimeType);
    }

    /** A que impresora va, o null si no se eligio ninguna. */
    public PrintService getPrintService() {
        return null;
    }

    /**
     * Elige la impresora.
     *
     * @throws PrinterException si esa impresora no sirve para este trabajo
     */
    public void setPrintService(PrintService service) throws PrinterException {
        throw new PrinterException("Setting a service is not supported on this class");
    }

    /** Que dibujar, con el formato por omision. */
    public abstract void setPrintable(Printable painter);

    /** Que dibujar, con ese formato para todas las paginas. */
    public abstract void setPrintable(Printable painter, PageFormat format);

    /**
     * Que dibujar, con formato por pagina.
     *
     * @throws NullPointerException si es null
     */
    public abstract void setPageable(Pageable document) throws NullPointerException;

    /**
     * Muestra el dialogo de impresion.
     *
     * @return si el usuario acepto
     * @throws HeadlessException si no hay pantalla
     */
    public abstract boolean printDialog() throws HeadlessException;

    /**
     * Idem, rellenando desde esos atributos y devolviendo en ellos lo elegido.
     *
     * <p>Igual que en {@code javax.print.ServiceUI}, el conjunto es de entrada <b>y</b> de salida.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public boolean printDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        if (attributes == null) {
            throw new NullPointerException("attributes");
        }
        return printDialog();
    }

    /**
     * Muestra el dialogo de configuracion de pagina.
     *
     * @return el formato elegido, o el mismo que se paso si se cancelo
     * @throws HeadlessException si no hay pantalla
     */
    public abstract PageFormat pageDialog(PageFormat page) throws HeadlessException;

    /**
     * Idem, partiendo de atributos.
     *
     * @throws HeadlessException si no hay pantalla
     */
    public PageFormat pageDialog(PrintRequestAttributeSet attributes) throws HeadlessException {
        if (attributes == null) {
            throw new NullPointerException("attributes");
        }
        return pageDialog(defaultPage());
    }

    /** Una copia de ese formato ajustada a lo que la impresora puede. */
    public abstract PageFormat defaultPage(PageFormat page);

    /** El formato por omision de la impresora. */
    public PageFormat defaultPage() {
        return defaultPage(new PageFormat());
    }

    /**
     * El {@link PageFormat} que describen esos atributos.
     *
     * <p>Mira tres: {@link Media} para el tamano de hoja, {@link MediaPrintableArea} para el area
     * imprimible y {@link OrientationRequested} para la orientacion. Los que no esten quedan como en
     * {@link #defaultPage}.
     *
     * <p>Solo aplica los que la impresora elegida soporte, asi que sin impresora devuelve el formato
     * por omision tal cual.
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
     * Una copia de ese formato con el area imprimible acotada a lo que la impresora puede.
     *
     * <p>Es lo que corrige un {@link Paper} armado a mano, que no valida nada.
     */
    public abstract PageFormat validatePage(PageFormat page);

    /**
     * Imprime. Bloquea; ver la nota de la clase.
     *
     * @throws PrinterException si fallo
     */
    public abstract void print() throws PrinterException;

    /**
     * Idem, con esos atributos.
     *
     * @throws PrinterException si fallo
     */
    public void print(PrintRequestAttributeSet attributes) throws PrinterException {
        print();
    }

    /** Cuantas copias. */
    public abstract void setCopies(int copies);

    /** Cuantas copias. */
    public abstract int getCopies();

    /** A nombre de quien va el trabajo. */
    public abstract String getUserName();

    /** El nombre que se ve en la cola. */
    public abstract void setJobName(String jobName);

    /** El nombre que se ve en la cola. */
    public abstract String getJobName();

    /**
     * Pide cancelar.
     *
     * <p>Es asincronico y se llama desde otro hilo: el que llamo {@link #print} esta bloqueado. Lo que
     * pasa es que la proxima llamada a {@code Printable.print} no ocurre y {@code print} sale con
     * {@link PrinterAbortException}.
     */
    public abstract void cancel();

    /** Si se pidio cancelar. */
    public abstract boolean isCancelled();
}
