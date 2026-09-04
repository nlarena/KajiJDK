package java.awt;

/**
 * Quien decide dónde va y cuánto mide cada hijo de un contenedor.
 *
 * <p>Separar la distribución del contenedor es la decisión de diseño que hace que AWT funcione en
 * pantallas y fuentes que no existían cuando se escribió el programa: el contenedor no sabe dónde
 * van sus hijos, se lo pregunta a otro, y cambiar ese otro cambia la interfaz entera.
 *
 * <p>La interfaz tiene dos mitades. {@link #layoutContainer} es la que hace el trabajo; las otras
 * tres contestan **cuánto necesita** el contenedor, y son las que permiten que la decisión se
 * propague hacia arriba en el árbol.
 *
 * <p>{@link #addLayoutComponent} recibe un nombre y no un objeto, y esa firma se quedó vieja: sirve
 * para {@link CardLayout} —donde el nombre identifica la tarjeta— y para poco más.
 * {@link LayoutManager2} la reemplaza por una que toma un objeto cualquiera.
 */
public interface LayoutManager {

    /** Avisa que se agregó un hijo con ese nombre. */
    void addLayoutComponent(String name, Component comp);

    /** Avisa que se sacó un hijo. */
    void removeLayoutComponent(Component comp);

    /** Cuánto necesita el contenedor para que sus hijos queden cómodos. */
    Dimension preferredLayoutSize(Container parent);

    /** Lo mínimo con lo que el contenedor puede funcionar. */
    Dimension minimumLayoutSize(Container parent);

    /** Ubica y dimensiona a los hijos. */
    void layoutContainer(Container parent);
}
