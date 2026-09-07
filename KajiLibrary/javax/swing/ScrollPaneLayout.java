package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.io.Serializable;

import javax.swing.border.Border;
import javax.swing.plaf.UIResource;

/**
 * La distribucion de un {@link JScrollPane}: nueve lugares, y la decision de si hace falta una
 * barra.
 *
 * <h2>Nueve lugares</h2>
 *
 * <p>La ventana al centro, dos barras, dos cabeceras —una arriba y otra a la izquierda— y cuatro
 * esquinas. Cada pieza se agrega con una de las constantes de {@link ScrollPaneConstants} como
 * restriccion, igual que en un {@code BorderLayout}, y esta clase se queda con una referencia
 * directa a cada una: son pocas y fijas, y buscarlas por nombre en cada acomodada seria trabajo de
 * mas.
 *
 * <h2>Por que la decision es circular</h2>
 *
 * <p>Con la politica "cuando haga falta", saber si hace falta una barra depende de cuanto espacio
 * queda, y cuanto espacio queda depende de si hay barras: poner la horizontal come alto y puede
 * hacer que ahora si haga falta la vertical. Por eso el metodo decide en pasadas —vertical,
 * horizontal, y de nuevo la vertical— y despues, si el contenido es {@link Scrollable}, vuelve a
 * preguntarle con el tamano ya fijado, porque un contenido que sigue al ancho de la ventana puede
 * cambiar de opinion cuando la ventana cambia.
 *
 * <p>El JDK corta ahi y no itera hasta que se estabilice: un contenido malicioso podria no
 * estabilizarse nunca. Dos pasadas alcanzan para todo lo razonable.
 */
public class ScrollPaneLayout implements LayoutManager, ScrollPaneConstants, Serializable {

    /** La ventana; el centro de todo. */
    protected JViewport viewport;

    protected JScrollBar vsb;

    protected JScrollBar hsb;

    /** La cabecera de filas: se desplaza con la ventana, pero solo en vertical. */
    protected JViewport rowHead;

    /** La cabecera de columnas: se desplaza con la ventana, pero solo en horizontal. */
    protected JViewport colHead;

    protected Component lowerLeft;
    protected Component lowerRight;
    protected Component upperLeft;
    protected Component upperRight;

    protected int vsbPolicy = VERTICAL_SCROLLBAR_AS_NEEDED;

    protected int hsbPolicy = HORIZONTAL_SCROLLBAR_AS_NEEDED;

    public ScrollPaneLayout() {
    }

    /** Toma del panel las nueve piezas y las dos politicas. */
    public void syncWithScrollPane(JScrollPane sp) {
        viewport = sp.getViewport();
        vsb = sp.getVerticalScrollBar();
        hsb = sp.getHorizontalScrollBar();
        rowHead = sp.getRowHeader();
        colHead = sp.getColumnHeader();
        lowerLeft = sp.getCorner(LOWER_LEFT_CORNER);
        lowerRight = sp.getCorner(LOWER_RIGHT_CORNER);
        upperLeft = sp.getCorner(UPPER_LEFT_CORNER);
        upperRight = sp.getCorner(UPPER_RIGHT_CORNER);
        vsbPolicy = sp.getVerticalScrollBarPolicy();
        hsbPolicy = sp.getHorizontalScrollBarPolicy();
    }

    /**
     * Saca del panel al que ocupaba ese lugar y devuelve al nuevo.
     *
     * <p>Es lo que hace que poner una barra nueva no deje la vieja debajo.
     */
    protected Component addSingletonComponent(Component oldC, Component newC) {
        if ((oldC != null) && (oldC != newC)) {
            oldC.getParent().remove(oldC);
        }
        return newC;
    }

    /** Guarda la pieza en el lugar que nombra la restriccion. */
    public void addLayoutComponent(String s, Component c) {
        if (s.equals(VIEWPORT)) {
            viewport = (JViewport) addSingletonComponent(viewport, c);
        } else if (s.equals(VERTICAL_SCROLLBAR)) {
            vsb = (JScrollBar) addSingletonComponent(vsb, c);
        } else if (s.equals(HORIZONTAL_SCROLLBAR)) {
            hsb = (JScrollBar) addSingletonComponent(hsb, c);
        } else if (s.equals(ROW_HEADER)) {
            rowHead = (JViewport) addSingletonComponent(rowHead, c);
        } else if (s.equals(COLUMN_HEADER)) {
            colHead = (JViewport) addSingletonComponent(colHead, c);
        } else if (s.equals(LOWER_LEFT_CORNER)) {
            lowerLeft = addSingletonComponent(lowerLeft, c);
        } else if (s.equals(LOWER_RIGHT_CORNER)) {
            lowerRight = addSingletonComponent(lowerRight, c);
        } else if (s.equals(UPPER_LEFT_CORNER)) {
            upperLeft = addSingletonComponent(upperLeft, c);
        } else if (s.equals(UPPER_RIGHT_CORNER)) {
            upperRight = addSingletonComponent(upperRight, c);
        } else {
            throw new IllegalArgumentException("invalid layout key " + s);
        }
    }

    public void removeLayoutComponent(Component c) {
        if (c == viewport) {
            viewport = null;
        } else if (c == vsb) {
            vsb = null;
        } else if (c == hsb) {
            hsb = null;
        } else if (c == rowHead) {
            rowHead = null;
        } else if (c == colHead) {
            colHead = null;
        } else if (c == lowerLeft) {
            lowerLeft = null;
        } else if (c == lowerRight) {
            lowerRight = null;
        } else if (c == upperLeft) {
            upperLeft = null;
        } else if (c == upperRight) {
            upperRight = null;
        }
    }

    public int getVerticalScrollBarPolicy() {
        return vsbPolicy;
    }

    /** Cambia la politica; el panel la guarda tambien, y es el suyo el que manda al acomodar. */
    public void setVerticalScrollBarPolicy(int x) {
        if (x != VERTICAL_SCROLLBAR_AS_NEEDED && x != VERTICAL_SCROLLBAR_NEVER
                && x != VERTICAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid verticalScrollBarPolicy");
        }
        vsbPolicy = x;
    }

    public int getHorizontalScrollBarPolicy() {
        return hsbPolicy;
    }

    public void setHorizontalScrollBarPolicy(int x) {
        if (x != HORIZONTAL_SCROLLBAR_AS_NEEDED && x != HORIZONTAL_SCROLLBAR_NEVER
                && x != HORIZONTAL_SCROLLBAR_ALWAYS) {
            throw new IllegalArgumentException("invalid horizontalScrollBarPolicy");
        }
        hsbPolicy = x;
    }

    public JViewport getViewport() {
        return viewport;
    }

    public JScrollBar getHorizontalScrollBar() {
        return hsb;
    }

    public JScrollBar getVerticalScrollBar() {
        return vsb;
    }

    public JViewport getRowHeader() {
        return rowHead;
    }

    public JViewport getColumnHeader() {
        return colHead;
    }

    /** La pieza de esa esquina; las esquinas "inicial" y "final" dependen del idioma. */
    public Component getCorner(String key) {
        if (key.equals(LOWER_LEFT_CORNER)) {
            return lowerLeft;
        } else if (key.equals(LOWER_RIGHT_CORNER)) {
            return lowerRight;
        } else if (key.equals(UPPER_LEFT_CORNER)) {
            return upperLeft;
        } else if (key.equals(UPPER_RIGHT_CORNER)) {
            return upperRight;
        }
        return null;
    }

    /**
     * Lo que el panel querria medir: la ventana, mas las cabeceras, mas las barras que ya se sabe
     * que van a hacer falta.
     */
    public Dimension preferredLayoutSize(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int prefWidth = insets.left + insets.right;
        int prefHeight = insets.top + insets.bottom;

        Dimension extentSize = null;
        Dimension viewSize = null;
        Component view = null;

        if (viewport != null) {
            extentSize = viewport.getPreferredSize();
            view = viewport.getView();
            if (view != null) {
                viewSize = view.getPreferredSize();
            } else {
                viewSize = new Dimension(0, 0);
            }
        }

        if (extentSize != null) {
            prefWidth = prefWidth + extentSize.width;
            prefHeight = prefHeight + extentSize.height;
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            prefWidth = prefWidth + vpbInsets.left + vpbInsets.right;
            prefHeight = prefHeight + vpbInsets.top + vpbInsets.bottom;
        }

        if ((rowHead != null) && rowHead.isVisible()) {
            prefWidth = prefWidth + rowHead.getPreferredSize().width;
        }
        if ((colHead != null) && colHead.isVisible()) {
            prefHeight = prefHeight + colHead.getPreferredSize().height;
        }

        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
                prefWidth = prefWidth + vsb.getPreferredSize().width;
            } else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable) view).getScrollableTracksViewportHeight();
                }
                if (canScroll && (viewSize.height > extentSize.height)) {
                    prefWidth = prefWidth + vsb.getPreferredSize().width;
                }
            }
        }

        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
                prefHeight = prefHeight + hsb.getPreferredSize().height;
            } else if ((viewSize != null) && (extentSize != null)) {
                boolean canScroll = true;
                if (view instanceof Scrollable) {
                    canScroll = !((Scrollable) view).getScrollableTracksViewportWidth();
                }
                if (canScroll && (viewSize.width > extentSize.width)) {
                    prefHeight = prefHeight + hsb.getPreferredSize().height;
                }
            }
        }

        return new Dimension(prefWidth, prefHeight);
    }

    /**
     * Lo minimo: la ventana puede achicarse a nada, pero las barras y las cabeceras no.
     *
     * <p>Con la politica "siempre" la barra suma su minimo; con "cuando haga falta" tambien, y no
     * es un error: si el panel se achica hasta el minimo, la barra seguro hara falta.
     */
    public Dimension minimumLayoutSize(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Insets insets = parent.getInsets();
        int minWidth = insets.left + insets.right;
        int minHeight = insets.top + insets.bottom;

        if (viewport != null) {
            Dimension size = viewport.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = minHeight + size.height;
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        if (viewportBorder != null) {
            Insets vpbInsets = viewportBorder.getBorderInsets(parent);
            minWidth = minWidth + vpbInsets.left + vpbInsets.right;
            minHeight = minHeight + vpbInsets.top + vpbInsets.bottom;
        }

        if ((rowHead != null) && rowHead.isVisible()) {
            Dimension size = rowHead.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = Math.max(minHeight, size.height);
        }
        if ((colHead != null) && colHead.isVisible()) {
            Dimension size = colHead.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight = minHeight + size.height;
        }
        if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
            Dimension size = vsb.getMinimumSize();
            minWidth = minWidth + size.width;
            minHeight = Math.max(minHeight, size.height);
        }
        if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
            Dimension size = hsb.getMinimumSize();
            minWidth = Math.max(minWidth, size.width);
            minHeight = minHeight + size.height;
        }

        return new Dimension(minWidth, minHeight);
    }

    /** Ver la nota de la clase sobre por que la decision es circular. */
    public void layoutContainer(Container parent) {
        JScrollPane scrollPane = (JScrollPane) parent;
        vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
        hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

        Rectangle availR = scrollPane.getBounds();
        availR.x = 0;
        availR.y = 0;

        Insets insets = parent.getInsets();
        availR.x = insets.left;
        availR.y = insets.top;
        availR.width = availR.width - (insets.left + insets.right);
        availR.height = availR.height - (insets.top + insets.bottom);

        boolean leftToRight = scrollPane.getComponentOrientation().isLeftToRight();

        // La cabecera de columnas se lleva su alto de arriba de todo.
        Rectangle colHeadR = new Rectangle(0, availR.y, 0, 0);
        if ((colHead != null) && (colHead.isVisible())) {
            int colHeadHeight = Math.min(availR.height, colHead.getPreferredSize().height);
            colHeadR.height = colHeadHeight;
            availR.y = availR.y + colHeadHeight;
            availR.height = availR.height - colHeadHeight;
        }

        // La de filas, su ancho del lado por el que empieza la linea.
        Rectangle rowHeadR = new Rectangle(0, 0, 0, 0);
        if ((rowHead != null) && (rowHead.isVisible())) {
            int rowHeadWidth = Math.min(availR.width, rowHead.getPreferredSize().width);
            rowHeadR.width = rowHeadWidth;
            availR.width = availR.width - rowHeadWidth;
            if (leftToRight) {
                rowHeadR.x = availR.x;
                availR.x = availR.x + rowHeadWidth;
            } else {
                rowHeadR.x = availR.x + availR.width;
            }
        }

        Border viewportBorder = scrollPane.getViewportBorder();
        Insets vpbInsets;
        if (viewportBorder != null) {
            vpbInsets = viewportBorder.getBorderInsets(parent);
            availR.x = availR.x + vpbInsets.left;
            availR.y = availR.y + vpbInsets.top;
            availR.width = availR.width - (vpbInsets.left + vpbInsets.right);
            availR.height = availR.height - (vpbInsets.top + vpbInsets.bottom);
        } else {
            vpbInsets = new Insets(0, 0, 0, 0);
        }

        // El tamano preferido del contenido, no el que pide como ventana: aca se decide si el
        // contenido entra, y lo que tiene que entrar es el contenido entero. El
        // `getPreferredScrollableViewportSize` es para cuanto querria medir el panel, y se usa en
        // {@link #preferredLayoutSize}.
        Component view = (viewport != null) ? viewport.getView() : null;
        Dimension viewPrefSize = (view != null) ? view.getPreferredSize() : new Dimension(0, 0);

        Dimension extentSize = (viewport != null)
                ? viewport.toViewCoordinates(availR.getSize()) : new Dimension(0, 0);

        boolean viewTracksViewportWidth = false;
        boolean viewTracksViewportHeight = false;
        boolean isEmpty = (availR.width < 0 || availR.height < 0);
        Scrollable sv;
        if (!isEmpty && view instanceof Scrollable) {
            sv = (Scrollable) view;
            viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
            viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
        } else {
            sv = null;
        }

        Rectangle vsbR = new Rectangle(0, availR.y - vpbInsets.top, 0, 0);

        boolean vsbNeeded;
        if (isEmpty) {
            vsbNeeded = false;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
            vsbNeeded = true;
        } else if (vsbPolicy == VERTICAL_SCROLLBAR_NEVER) {
            vsbNeeded = false;
        } else {
            vsbNeeded = !viewTracksViewportHeight && (viewPrefSize.height > extentSize.height);
        }

        if ((vsb != null) && vsbNeeded) {
            adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
            extentSize = viewport.toViewCoordinates(availR.getSize());
        }

        Rectangle hsbR = new Rectangle(availR.x - vpbInsets.left, 0, 0, 0);
        boolean hsbNeeded;
        if (isEmpty) {
            hsbNeeded = false;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
            hsbNeeded = true;
        } else if (hsbPolicy == HORIZONTAL_SCROLLBAR_NEVER) {
            hsbNeeded = false;
        } else {
            hsbNeeded = !viewTracksViewportWidth && (viewPrefSize.width > extentSize.width);
        }

        if ((hsb != null) && hsbNeeded) {
            adjustForHSB(true, availR, hsbR, vpbInsets);

            // Poner la horizontal quito alto: puede que ahora haga falta la vertical.
            if ((vsb != null) && !vsbNeeded && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                extentSize = viewport.toViewCoordinates(availR.getSize());
                vsbNeeded = viewPrefSize.height > extentSize.height;
                if (vsbNeeded) {
                    adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                }
            }
        }

        // Con el tamano ya fijado se le vuelve a preguntar al contenido; ver la nota de la clase.
        if (viewport != null) {
            viewport.setBounds(availR);

            if (sv != null) {
                extentSize = viewport.toViewCoordinates(availR.getSize());

                boolean oldHSBNeeded = hsbNeeded;
                boolean oldVSBNeeded = vsbNeeded;
                viewTracksViewportWidth = sv.getScrollableTracksViewportWidth();
                viewTracksViewportHeight = sv.getScrollableTracksViewportHeight();
                if (vsb != null && vsbPolicy == VERTICAL_SCROLLBAR_AS_NEEDED) {
                    boolean newVSBNeeded = !viewTracksViewportHeight
                            && (viewPrefSize.height > extentSize.height);
                    if (newVSBNeeded != vsbNeeded) {
                        vsbNeeded = newVSBNeeded;
                        adjustForVSB(vsbNeeded, availR, vsbR, vpbInsets, leftToRight);
                        extentSize = viewport.toViewCoordinates(availR.getSize());
                    }
                }
                if (hsb != null && hsbPolicy == HORIZONTAL_SCROLLBAR_AS_NEEDED) {
                    boolean newHSBNeeded = !viewTracksViewportWidth
                            && (viewPrefSize.width > extentSize.width);
                    if (newHSBNeeded != hsbNeeded) {
                        hsbNeeded = newHSBNeeded;
                        adjustForHSB(hsbNeeded, availR, hsbR, vpbInsets);
                        if ((vsb != null) && !vsbNeeded
                                && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
                            extentSize = viewport.toViewCoordinates(availR.getSize());
                            vsbNeeded = viewPrefSize.height > extentSize.height;
                            if (vsbNeeded) {
                                adjustForVSB(true, availR, vsbR, vpbInsets, leftToRight);
                            }
                        }
                    }
                }
                if (oldHSBNeeded != hsbNeeded || oldVSBNeeded != vsbNeeded) {
                    viewport.setBounds(availR);
                }
            }
        }

        vsbR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        hsbR.width = availR.width + vpbInsets.left + vpbInsets.right;
        rowHeadR.height = availR.height + vpbInsets.top + vpbInsets.bottom;
        rowHeadR.y = availR.y - vpbInsets.top;
        colHeadR.width = availR.width + vpbInsets.left + vpbInsets.right;
        colHeadR.x = availR.x - vpbInsets.left;

        if (rowHead != null) {
            rowHead.setBounds(rowHeadR);
        }
        if (colHead != null) {
            colHead.setBounds(colHeadR);
        }

        if (vsb != null) {
            if (vsbNeeded) {
                vsb.setVisible(true);
                vsb.setBounds(vsbR);
            } else {
                vsb.setVisible(false);
            }
        }

        if (hsb != null) {
            if (hsbNeeded) {
                hsb.setVisible(true);
                hsb.setBounds(hsbR);
            } else {
                hsb.setVisible(false);
            }
        }

        if (lowerLeft != null) {
            lowerLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, hsbR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, hsbR.height);
        }
        if (lowerRight != null) {
            lowerRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, hsbR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, hsbR.height);
        }
        if (upperLeft != null) {
            upperLeft.setBounds(leftToRight ? rowHeadR.x : vsbR.x, colHeadR.y,
                    leftToRight ? rowHeadR.width : vsbR.width, colHeadR.height);
        }
        if (upperRight != null) {
            upperRight.setBounds(leftToRight ? vsbR.x : rowHeadR.x, colHeadR.y,
                    leftToRight ? vsbR.width : rowHeadR.width, colHeadR.height);
        }
    }

    /** Le saca al espacio disponible lo que ocupa la barra vertical, o se lo devuelve. */
    private void adjustForVSB(boolean wantsVSB, Rectangle available, Rectangle vsbR,
            Insets vpbInsets, boolean leftToRight) {
        int oldWidth = vsbR.width;
        if (wantsVSB) {
            int vsbWidth = Math.max(0, Math.min(vsb.getPreferredSize().width, available.width));
            available.width = available.width - vsbWidth;
            vsbR.width = vsbWidth;

            if (leftToRight) {
                vsbR.x = available.x + available.width + vpbInsets.right;
            } else {
                vsbR.x = available.x - vpbInsets.left;
                available.x = available.x + vsbWidth;
            }
        } else {
            available.width = available.width + oldWidth;
        }
    }

    /** Lo mismo con la horizontal. */
    private void adjustForHSB(boolean wantsHSB, Rectangle available, Rectangle hsbR,
            Insets vpbInsets) {
        int oldHeight = hsbR.height;
        if (wantsHSB) {
            int hsbHeight = Math.max(0,
                    Math.min(available.height, hsb.getPreferredSize().height));
            available.height = available.height - hsbHeight;
            hsbR.y = available.y + available.height + vpbInsets.bottom;
            hsbR.height = hsbHeight;
        } else {
            available.height = available.height + oldHeight;
        }
    }

    /** @deprecated es {@link JScrollPane#getViewportBorderBounds}. */
    @Deprecated
    public Rectangle getViewportBorderBounds(JScrollPane scrollpane) {
        return scrollpane.getViewportBorderBounds();
    }

    /** La misma distribucion, marcada como puesta por un aspecto; ver {@link UIResource}. */
    public static class UIResource extends ScrollPaneLayout
            implements javax.swing.plaf.UIResource {

        public UIResource() {
        }
    }
}
