package javax.print.attribute.standard;

import java.util.AbstractSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintServiceAttribute;

/**
 * Las condiciones que tiene la impresora encima, cada una con su gravedad.
 *
 * <p>Es un mapa de {@link PrinterStateReason} a {@link Severity} y no un conjunto de razones,
 * porque la misma condicion no siempre pesa igual: {@code MEDIA_LOW} es un {@code WARNING} cuando
 * quedan diez hojas y un {@code ERROR} cuando no queda ninguna. La gravedad la decide la impresora
 * y por eso viaja pegada a la razon.
 *
 * <p>Como {@link JobStateReasons}, el atributo <b>es</b> la coleccion --extiende
 * {@link HashMap}-- y por lo tanto es mutable, a diferencia del resto del paquete.
 *
 * <p>La vista por gravedad ({@link #printerStateReasonSet}) es lo unico con logica de verdad aca, y
 * es una <b>vista viva</b>, no una copia: no recorre el mapa al construirse sino que filtra al
 * iterar, asi que refleja los cambios posteriores del mapa. Es de solo lectura --hereda de
 * {@link AbstractSet} el {@code add} que tira {@code UnsupportedOperationException}-- porque
 * agregar una razon a la vista de los errores no tendria donde guardar la gravedad.
 *
 * <p>Que sea perezosa tiene un costo que conviene saber: {@code size()} no es O(1) sino que recorre
 * el mapa entero contando los que coinciden.
 */
public final class PrinterStateReasons extends HashMap<PrinterStateReason, Severity>
    implements PrintServiceAttribute {

    private static final long serialVersionUID = -3731791085163619457L;

    public PrinterStateReasons() {
        super();
    }

    public PrinterStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public PrinterStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    /**
     * Copia otro mapa entrada por entrada --y no con el constructor de {@link HashMap}-- para que
     * cada par pase por {@link #put} y se rechacen los null.
     */
    public PrinterStateReasons(Map<PrinterStateReason, Severity> map) {
        this();
        for (Map.Entry<PrinterStateReason, Severity> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public Severity put(PrinterStateReason reason, Severity severity) {
        if (reason == null) {
            throw new NullPointerException("reason is null");
        }
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return super.put(reason, severity);
    }

    public final Class<? extends Attribute> getCategory() {
        return PrinterStateReasons.class;
    }

    public final String getName() {
        return "printer-state-reasons";
    }

    /** Las razones que tienen exactamente esa gravedad, como vista viva y de solo lectura. */
    public Set<PrinterStateReason> printerStateReasonSet(Severity severity) {
        if (severity == null) {
            throw new NullPointerException("severity is null");
        }
        return new PrinterStateReasonSet(severity, this);
    }

    // La vista. No guarda elementos: guarda el criterio y el mapa de afuera.
    //
    // Guarda el mapa y no su entrySet --que es lo que hace el JDK-- para que la vista siga siendo
    // viva sobre el HashMap de KajiLibrary, cuyo entrySet() devuelve una copia y no una vista.
    // Pidiendolo de nuevo en cada iterator() el resultado es el mismo que en el JDK, donde las dos
    // formas coinciden porque ahi el entrySet si es vista.
    //
    // Declaradas `static` y no internas por el finding #440 del compilador: adentro de una interna
    // no se sintetiza el argumento de la instancia externa. Da igual, porque el mapa lo reciben por
    // constructor.
    private static class PrinterStateReasonSet extends AbstractSet<PrinterStateReason> {

        private Severity mySeverity;
        private Map<PrinterStateReason, Severity> myMap;

        PrinterStateReasonSet(Severity severity, Map<PrinterStateReason, Severity> map) {
            this.mySeverity = severity;
            this.myMap = map;
        }

        // Contar cuesta recorrer, porque el filtro no esta materializado.
        public int size() {
            int result = 0;
            Iterator<PrinterStateReason> iter = iterator();
            while (iter.hasNext()) {
                iter.next();
                ++result;
            }
            return result;
        }

        public Iterator<PrinterStateReason> iterator() {
            return new PrinterStateReasonSetIterator(this.mySeverity,
                                                     this.myMap.entrySet().iterator());
        }
    }

    // El filtro. Mantiene adelantada la proxima entrada que coincide, que es lo que deja a
    // hasNext() contestar sin consumir nada.
    private static class PrinterStateReasonSetIterator implements Iterator<PrinterStateReason> {

        private Severity mySeverity;
        private Iterator<Map.Entry<PrinterStateReason, Severity>> myIterator;
        private Map.Entry<PrinterStateReason, Severity> myEntry;

        PrinterStateReasonSetIterator(
                Severity severity,
                Iterator<Map.Entry<PrinterStateReason, Severity>> iterator) {
            this.mySeverity = severity;
            this.myIterator = iterator;
            goToNext();
        }

        // La gravedad se compara por identidad y no con equals: son singletons de EnumSyntax.
        private void goToNext() {
            this.myEntry = null;
            while (this.myEntry == null && this.myIterator.hasNext()) {
                this.myEntry = this.myIterator.next();
                if (this.myEntry.getValue() != this.mySeverity) {
                    this.myEntry = null;
                }
            }
        }

        public boolean hasNext() {
            return this.myEntry != null;
        }

        public PrinterStateReason next() {
            if (this.myEntry == null) {
                throw new NoSuchElementException();
            }
            PrinterStateReason result = this.myEntry.getKey();
            goToNext();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
