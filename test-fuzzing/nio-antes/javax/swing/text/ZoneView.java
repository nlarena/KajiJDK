package javax.swing.text;

import java.awt.Shape;
import java.util.Vector;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentEvent$ElementChange;

/**
 * A view that builds its children in zones and discards those that are not seen.
 *
 * <h2>For documents that do not fit in memory as views</h2>
 *
 * <p>A document of a million lines would have a million views if they were all built. This class
 * splits the content into <em>zones</em> of a maximum size and has only the last ones used built;
 * the rest are left as an empty view that knows its range and builds itself when needed.
 *
 * <p>The cost is that a zone that is discarded loses its layout and it has to be redone on coming
 * back. Hence the two numbers that govern it: {@link #setMaximumZoneSize}, which decides how much
 * is redone in one go, and {@link #setMaxZonesLoaded}, how many are kept.
 *
 * <p>In this library the zones are built and never discarded: with no huge documents to test on,
 * discarding would be complexity with no measurable benefit. {@link #unloadZone} is there and
 * works; what there is not is anybody calling it by itself.
 */
public class ZoneView extends BoxView {

    int maxZoneSize = 8 * 1024;
    int maxZonesLoaded = 3;
    Vector<View> loadedZones;

    /** A zoned view on that axis. */
    public ZoneView(Element elem, int axis) {
        super(elem, axis);
        loadedZones = new Vector<View>();
    }

    /** How much text fits in a zone. */
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

    /** It notes that that zone was built. */
    protected void zoneWasLoaded(View zone) {
        loadedZones.addElement(zone);
        unloadOldZones();
    }

    /** It discards the old zones; see the class note. */
    void unloadOldZones() {
        while (loadedZones.size() > getMaxZonesLoaded()) {
            View zone = loadedZones.elementAt(0);
            loadedZones.removeElementAt(0);
            unloadZone(zone);
        }
    }

    /** It releases that zone's children; its range is kept. */
    protected void unloadZone(View zone) {
        zone.removeAll();
    }

    protected boolean isZoneLoaded(View zone) {
        return (zone.getViewCount() > 0);
    }

    /** An empty zone covering that stretch. */
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

    /** It splits the content into zones of the maximum size. */
    protected void loadChildren(ViewFactory f) {
        int offs0 = getStartOffset();
        int offs1 = getEndOffset();
        append(createZone(offs0, offs1));
        checkZoneSize(0);
    }

    /** If the zone is larger than the maximum, it is split in half. */
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

    /** It splits a zone in two at that position. */
    void splitZone(int index, int offs0, int offs1) {
        View zona = getView(index);
        int end = zona.getEndOffset();
        View[] newLeaves = new View[2];
        newLeaves[0] = createZone(offs0, offs1);
        newLeaves[1] = createZone(offs1, end);
        replace(index, 1, newLeaves);
    }

    /** How far a zone that starts there should reach. */
    int getDesiredZoneEnd(int index) {
        View v = getView(index);
        int offs0 = v.getStartOffset();
        return Math.min(offs0 + maxZoneSize, getEndOffset());
    }

    protected boolean updateChildren(DocumentEvent$ElementChange ec, DocumentEvent e,
            ViewFactory f) {
        // The zones do not follow the elements one to one: they arrange themselves.
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
     * A zone: a stretch of the document that is built when needed.
     *
     * <p>It is private in the JDK and here too. It keeps its range with two {@link Position}s, so
     * it survives the edits even when it is not built.
     */
    static class Zone extends BoxView {

        private Position start;
        private Position end;
        private final ZoneView parent;

        Zone(Element elem, Position start, Position end, ZoneView parent) {
            super(elem, parent.getAxis());
            this.start = start;
            this.end = end;
            this.parent = parent;
        }

        /** It builds the children only when somebody asks for them. */
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
                parent.zoneWasLoaded(this);
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
            // They are loaded on demand; see load().
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
