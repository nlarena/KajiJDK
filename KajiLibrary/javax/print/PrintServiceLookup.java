package javax.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.print.attribute.AttributeSet;

/**
 * KajiLibrary's javax.print.PrintServiceLookup -- encuentra impresoras.
 *
 * <p>Tiene dos caras que conviene no confundir. Los metodos <b>estaticos</b> son la API de quien busca
 * una impresora; los <b>abstractos</b> son lo que implementa quien provee impresoras. La misma clase
 * hace de fachada y de contrato de proveedor.
 *
 * <h2>De donde salen los proveedores</h2>
 *
 * <p>De dos lugares que se suman:
 *
 * <ul>
 *   <li>los declarados como servicio {@code javax.print.PrintServiceLookup} y encontrados con
 *       {@link ServiceLoader}. Es como aparecen las impresoras del sistema operativo;
 *   <li>los registrados a mano con {@link #registerServiceProvider}.
 * </ul>
 *
 * <p>{@link #registerService} es distinto de los dos: registra <b>una impresora suelta</b>, sin
 * proveedor. Sirve para agregar algo que se armo en el programa.
 *
 * <h2>Los filtros</h2>
 *
 * <p>Las busquedas toman un {@link DocFlavor} y un {@link AttributeSet}, y los dos aceptan null para
 * decir "no me importa". El conjunto no filtra por igualdad: filtra por lo que la impresora
 * <b>puede</b> dar, asi que pedir dos copias devuelve las que soportan al menos dos.
 *
 * <h2>A KajiLibrary subset</h2>
 *
 * <p>Esta biblioteca no trae ningun proveedor: hablar con el sistema de impresion del sistema
 * operativo pide codigo nativo, y no hay. Los metodos funcionan --recorren el {@link ServiceLoader} y
 * los registrados a mano-- y devuelven vacio o null porque no hay nada que encontrar, que es
 * exactamente lo que la API define para una maquina sin impresoras. Registrando un proveedor, esto
 * anda sin cambios.
 */
public abstract class PrintServiceLookup {

    /** Los proveedores registrados a mano. */
    private static final ArrayList<PrintServiceLookup> REGISTERED =
        new ArrayList<PrintServiceLookup>();

    /** Las impresoras sueltas registradas a mano. */
    private static final ArrayList<PrintService> REGISTERED_SERVICES =
        new ArrayList<PrintService>();

    /** Para las subclases. */
    protected PrintServiceLookup() {
    }

    /**
     * Las impresoras que aceptan ese formato y esos atributos.
     *
     * @param flavor el formato, o null para no filtrar
     * @param attributes lo que se piensa pedir, o null
     * @return las que sirven; nunca null, puede estar vacio
     */
    public static final PrintService[] lookupPrintServices(DocFlavor flavor,
                                                           AttributeSet attributes) {
        ArrayList<PrintService> found = new ArrayList<PrintService>();
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            PrintService[] some = providers.next().getPrintServices(flavor, attributes);
            addAll(found, some);
        }
        int i = 0;
        while (i < REGISTERED_SERVICES.size()) {
            PrintService s = REGISTERED_SERVICES.get(i);
            if (matches(s, flavor, attributes) && !found.contains(s)) {
                found.add(s);
            }
            i = i + 1;
        }
        return found.toArray(new PrintService[found.size()]);
    }

    /**
     * Las que aceptan trabajos de varios documentos.
     *
     * <p>El primer argumento es un <b>arreglo</b> de formatos, y hay que leerlo bien: una impresora
     * califica si soporta <b>todos</b>, no alguno. Es lo que corresponde, porque los documentos de un
     * trabajo van juntos.
     */
    public static final MultiDocPrintService[] lookupMultiDocPrintServices(
        DocFlavor[] flavors, AttributeSet attributes) {
        ArrayList<MultiDocPrintService> found = new ArrayList<MultiDocPrintService>();
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            MultiDocPrintService[] some =
                providers.next().getMultiDocPrintServices(flavors, attributes);
            if (some != null) {
                int i = 0;
                while (i < some.length) {
                    if (some[i] != null && !found.contains(some[i])) {
                        found.add(some[i]);
                    }
                    i = i + 1;
                }
            }
        }
        return found.toArray(new MultiDocPrintService[found.size()]);
    }

    /** La impresora por omision, o null si no hay ninguna. */
    public static final PrintService lookupDefaultPrintService() {
        Iterator<PrintServiceLookup> providers = allProviders();
        while (providers.hasNext()) {
            PrintService s = providers.next().getDefaultPrintService();
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    /**
     * Registra un proveedor.
     *
     * @return si se registro; false si es null o ya estaba
     */
    public static boolean registerServiceProvider(PrintServiceLookup sp) {
        if (sp == null) {
            return false;
        }
        synchronized (REGISTERED) {
            int i = 0;
            while (i < REGISTERED.size()) {
                if (REGISTERED.get(i).getClass() == sp.getClass()) {
                    return false;
                }
                i = i + 1;
            }
            REGISTERED.add(sp);
        }
        return true;
    }

    /**
     * Registra una impresora suelta. Ver la nota de la clase sobre la diferencia con
     * {@link #registerServiceProvider}.
     *
     * @return si se registro; false si es null o ya estaba
     */
    public static boolean registerService(PrintService service) {
        if (service == null || service instanceof StreamPrintService) {
            return false;
        }
        synchronized (REGISTERED_SERVICES) {
            if (REGISTERED_SERVICES.contains(service)) {
                return false;
            }
            REGISTERED_SERVICES.add(service);
        }
        return true;
    }

    /** Las de este proveedor que sirven para ese formato y esos atributos. */
    public abstract PrintService[] getPrintServices(DocFlavor flavor, AttributeSet attributes);

    /** Todas las de este proveedor. */
    public abstract PrintService[] getPrintServices();

    /** Las de varios documentos. Ver {@link #lookupMultiDocPrintServices}: hacen falta todos. */
    public abstract MultiDocPrintService[] getMultiDocPrintServices(DocFlavor[] flavors,
                                                                    AttributeSet attributes);

    /** La por omision de este proveedor, o null. */
    public abstract PrintService getDefaultPrintService();

    /** Los del {@link ServiceLoader} y los registrados a mano, en ese orden. */
    private static Iterator<PrintServiceLookup> allProviders() {
        ArrayList<PrintServiceLookup> all = new ArrayList<PrintServiceLookup>();
        try {
            Iterator<PrintServiceLookup> loaded =
                ServiceLoader.load(PrintServiceLookup.class).iterator();
            while (loaded.hasNext()) {
                all.add(loaded.next());
            }
        } catch (Throwable e) {
            // Un proveedor roto no puede tumbar la busqueda entera; los demas siguen.
        }
        synchronized (REGISTERED) {
            all.addAll(REGISTERED);
        }
        return all.iterator();
    }

    /** Agrega los que no esten repetidos. */
    private static void addAll(ArrayList<PrintService> into, PrintService[] some) {
        if (some == null) {
            return;
        }
        int i = 0;
        while (i < some.length) {
            if (some[i] != null && !into.contains(some[i])) {
                into.add(some[i]);
            }
            i = i + 1;
        }
    }

    /** El filtro que se le aplica a una impresora registrada suelta. */
    private static boolean matches(PrintService s, DocFlavor flavor, AttributeSet attributes) {
        if (flavor != null && !s.isDocFlavorSupported(flavor)) {
            return false;
        }
        if (attributes != null) {
            AttributeSet bad = s.getUnsupportedAttributes(flavor, attributes);
            if (bad != null && !bad.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
