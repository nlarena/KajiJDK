package javax.swing.text;

import java.awt.Shape;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * Una vista que arma sus hijos de a zonas y descarta las que no se ven.
 *
 * <h2>Para documentos que no entran en memoria como vistas</h2>
 *
 * <p>Un documento de un millon de lineas tendria un millon de vistas si se armaran todas. Esta
 * clase parte el contenido en <em>zonas</em> de un tamano maximo y solo tiene armadas las ultimas
 * que se usaron; las demas quedan como una vista vacia que sabe su rango y se arma sola cuando
 * hace falta.
 *
 * <p>El costo es que una zona que se descarta pierde su maquetado y hay que rehacerlo al volver.
 * De ahi los dos numeros que la gobiernan: {@link #setMaximumZoneSize}, que decide cuanto se
 * rehace de una vez, y {@link #setMaxZonesLoaded}, cuantas se guardan.
 *
 * <p>En esta biblioteca las zonas se arman y no se descartan nunca: sin documentos enormes que
 * probar, descartar seria complejidad sin beneficio medible. {@link #unloadZone} esta y funciona;
 * lo que no hay es quien la llame sola.
 */
public class ZoneView extends BoxView {

    int maxZoneSize = 8 * 1024;
    int maxZonesLoaded = 3;
    Vector<View> loadedZones;

    /** Una vista por zonas sobre ese eje. */
    public ZoneView(Element elem, int axis) {
        super(elem, axis);
        loadedZones = new Vector<View>();
    }

    /** Cuanto texto entra en una zona. */
    public int getMaximumZoneSize() {
        return maxZoneSize;
    }

    public void setMaximumZoneSize(int size) {
        maxZoneSize = size;
    }

    public int getMaxZonesLoaded() {
        return maxZonesLoaded;
    }

    public void setMaxZonesLoaded(int mzl) {
        if (mzl < 1) {
            throw new IllegalArgumentException("ZoneView.setMaxZonesLoaded must be greater than 0.");
        }
        maxZonesLoaded = mzl;
        unloadOldZones();
    }

    /** Anota que esa zona quedo armada. */
    protected void zoneWasLoaded(View zone) {
        loadedZones.addElement(zone);
        unloadOldZones();
    }

    /** Descarta las zonas viejas; ver la nota de la clase. */
    void unloadOldZones() {
        while (loadedZones.size() > getMaxZonesLoaded()) {
            View zone = loadedZones.elementAt(0);
            loadedZones.removeElementAt(0);
            unloadZone(zone);
        }
    }

    /** Suelta los hijos de esa zona; su rango se conserva. */
    protected void unloadZone(View zone) {
        zone.removeAll();
    }

    protected boolean isZoneLoaded(View zone) {
        return (zone.getViewCount() > 0);
    }

    /** Una zona vacia que cubre ese tramo. */
    protected View createZone(int p0, int p1) {
        Document doc = getDocument();
        View zone;
        try {
            zone = new Zone(getElement(), doc.createPosition(p0), doc.createPosition(p1), this);
        } catch (BadLocationException ble) {
            throw new StateInvariantError("ZoneView.createZone");
        }
        return zone;
    }

    /** Parte el contenido en zonas del tamano maximo. */
    protected void loadChildren(ViewFactory f) {
        int offs0 = getStartOffset();
        int offs1 = getEndOffset();
        append(createZone(offs0, offs1));
        checkZoneSize(0);
    }

    /** Si la zona es mas grande que el maximo, se parte al medio. */
    private void checkZoneSize(int index) {
        View zone = getView(index);
        int offs0 = zone.getStartOffset();
        int offs1 = zone.getEndOffset();
        if ((offs1 - offs0) > maxZoneSize) {
            int offs = (offs1 + offs0) / 2;
            Element elem = getElement();
            offs = elem.getElement(elem.getElementIndex(offs)).getStartOffset();
            if (offs > offs0 && offs < offs1) {
                splitZone(index, offs0, offs);
            }
        }
    }

    protected int getViewIndexAtPosition(int pos) {
        if (pos < getStartOffset() || pos >= getEndOffset()) {
            return -1;
        }
        for (int counter = getViewCount() - 1; counter >= 0; counter--) {
            View v = getView(counter);
            if (pos >= v.getStartOffset() && pos < v.getEndOffset()) {
                return counter;
            }
        }
        return -1;
    }

    void handleInsert(int pos, int length) {
        int index = getViewIndex(pos, Position.Bias.Forward);
        if (index >= 0) {
            View v = getView(index);
            if (isZoneLoaded(v)) {
                checkZoneSize(index);
            }
        }
    }

    void handleRemove(int pos, int length) {
        int index = getViewIndex(pos, Position.Bias.Forward);
        if (index >= 0) {
            View v = getView(index);
            if (v.getStartOffset() == v.getEndOffset()) {
                remove(index);
            }
        }
    }

    /** Parte una zona en dos por esa posicion. */
    void splitZone(int index, int offs0, int offs1) {
        View zona = getView(index);
        int fin = zona.getEndOffset();
        View[] nuevas = new View[2];
        nuevas[0] = createZone(offs0, offs1);
        nuevas[1] = createZone(offs1, fin);
        replace(index, 1, nuevas);
    }

    /** Hasta donde deberia llegar una zona que empieza ahi. */
    int getDesiredZoneEnd(int index) {
        View v = getView(index);
        int offs0 = v.getStartOffset();
        return Math.min(offs0 + maxZoneSize, getEndOffset());
    }

    protected boolean updateChildren(DocumentEvent$ElementChange ec, DocumentEvent e,
            ViewFactory f) {
        // Las zonas no siguen a los elementos uno a uno: se acomodan solas.
        return false;
    }

    public void insertUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        handleInsert(changes.getOffset(), changes.getLength());
        super.insertUpdate(changes, a, f);
    }

    public void removeUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        handleRemove(changes.getOffset(), changes.getLength());
        super.removeUpdate(changes, a, f);
    }

    /**
     * Una zona: un tramo del documento que se arma cuando hace falta.
     *
     * <p>Es privada en el JDK y aca tambien. Guarda su rango con dos {@link Position}, asi que
     * sobrevive a las ediciones aunque no este armada.
     */
    static class Zone extends BoxView {

        private Position start;
        private Position end;
        private final ZoneView padre;

        Zone(Element elem, Position start, Position end, ZoneView padre) {
            super(elem, padre.getAxis());
            this.start = start;
            this.end = end;
            this.padre = padre;
        }

        /** Arma los hijos recien cuando alguien los pide. */
        public void load() {
            if (!isLoaded()) {
                setEstimatedMajorSpan(true);
                Element e = getElement();
                ViewFactory f = getViewFactory();
                int index0 = e.getElementIndex(getStartOffset());
                int index1 = e.getElementIndex(getEndOffset());
                View[] added = new View[index1 - index0 + 1];
                for (int i = index0; i <= index1; i++) {
                    added[i - index0] = f.create(e.getElement(i));
                }
                replace(0, 0, added);
                padre.zoneWasLoaded(this);
            }
        }

        boolean isLoaded() {
            return (getViewCount() != 0);
        }

        void setEstimatedMajorSpan(boolean isEstimated) {
        }

        public int getStartOffset() {
            return start.getOffset();
        }

        public int getEndOffset() {
            return end.getOffset();
        }

        protected void loadChildren(ViewFactory f) {
            // Se cargan por demanda; ver load().
        }

        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            load();
            return super.modelToView(pos, a, b);
        }

        public int viewToModel(float x, float y, Shape a, Position.Bias[] bias) {
            load();
            return super.viewToModel(x, y, a, bias);
        }

        public void paint(java.awt.Graphics g, Shape a) {
            load();
            super.paint(g, a);
        }

        protected void childAllocation(int index, java.awt.Rectangle a) {
            load();
            super.childAllocation(index, a);
        }
    }
}
