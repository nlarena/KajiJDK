package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.io.Serializable;

/**
 * La distribucion de un {@link JViewport}: ubica al unico hijo, que es la vista.
 *
 * <h2>Tres decisiones</h2>
 *
 * <p>Cada vez que se acomoda decide, en este orden:
 *
 * <ol>
 * <li><strong>Que tamano darle a la vista.</strong> Su tamano preferido, salvo que la vista sea
 * {@link Scrollable} y diga que quiere seguir al ancho o al alto de la ventana.
 * <li><strong>Si hay que correr la posicion.</strong> Si al agrandarse la ventana quedaria espacio
 * vacio despues del final de la vista, se la vuelve a acercar: es lo que hace que al agrandar una
 * ventana desplazada hasta el fondo el contenido se pegue al borde en vez de dejar un hueco.
 * <li><strong>Si hay que estirar la vista.</strong> Una vista comun —no {@code Scrollable}— que
 * esta en el origen y es mas chica que la ventana se agranda hasta llenarla. Es lo que hace que un
 * contenido chico se vea con fondo propio y no con el del viewport.
 * </ol>
 *
 * <p>La instancia es compartida: no guarda nada de ningun viewport.
 */
public class ViewportLayout implements LayoutManager, Serializable {

    static ViewportLayout SHARED_INSTANCE = new ViewportLayout();

    public ViewportLayout() {
    }

    /** Nada: un viewport tiene un solo hijo y no lo distingue por nombre. */
    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
    }

    /** Lo que quiere la vista, o cero si no hay. */
    public Dimension preferredLayoutSize(Container parent) {
        Component view = ((JViewport) parent).getView();
        if (view == null) {
            return new Dimension(0, 0);
        } else if (view instanceof Scrollable) {
            return ((Scrollable) view).getPreferredScrollableViewportSize();
        } else {
            return view.getPreferredSize();
        }
    }

    /** Cuatro por cuatro: una ventana puede achicarse hasta casi nada, la vista no la limita. */
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(4, 4);
    }

    /** Ver la nota de la clase. */
    public void layoutContainer(Container parent) {
        JViewport vp = (JViewport) parent;
        Component view = vp.getView();
        Scrollable scrollableView = null;

        if (view == null) {
            return;
        } else if (view instanceof Scrollable) {
            scrollableView = (Scrollable) view;
        }

        // Todo lo de abajo esta en coordenadas de la vista, salvo vpSize.
        Insets insets = vp.getInsets();
        Dimension viewPrefSize = view.getPreferredSize();
        Dimension vpSize = vp.getSize();
        Dimension extentSize = vp.toViewCoordinates(vpSize);
        Dimension viewSize = new Dimension(viewPrefSize);

        if (scrollableView != null) {
            if (scrollableView.getScrollableTracksViewportWidth()) {
                viewSize.width = vpSize.width;
            }
            if (scrollableView.getScrollableTracksViewportHeight()) {
                viewSize.height = vpSize.height;
            }
        }

        Point viewPosition = vp.getViewPosition();

        // Si sobrara espacio despues del final de la vista, se la acerca al borde.
        if (scrollableView == null || vp.getParent() == null
                || vp.getParent().getComponentOrientation().isLeftToRight()) {
            if ((viewPosition.x + extentSize.width) > viewSize.width) {
                viewPosition.x = Math.max(0, viewSize.width - extentSize.width);
            }
        } else {
            if (extentSize.width > viewSize.width) {
                viewPosition.x = viewSize.width - extentSize.width;
            } else {
                viewPosition.x = Math.max(0,
                        Math.min(viewSize.width - extentSize.width, viewPosition.x));
            }
        }

        if ((viewPosition.y + extentSize.height) > viewSize.height) {
            viewPosition.y = Math.max(0, viewSize.height - extentSize.height);
        }

        // Una vista comun que esta en el origen y no llena, se estira hasta llenar.
        if (scrollableView == null) {
            if ((viewPosition.x == 0) && (vpSize.width > viewPrefSize.width)) {
                viewSize.width = vpSize.width;
            }
            if ((viewPosition.y == 0) && (vpSize.height > viewPrefSize.height)) {
                viewSize.height = vpSize.height;
            }
        }
        vp.setViewPosition(viewPosition);
        vp.setViewSize(viewSize);
    }
}
