package javax.accessibility;

/**
 * Lo implementa todo objeto que quiera ser accesible.
 *
 * <p>Es deliberadamente mínima: un solo método. Toda la información vive en el
 * {@link AccessibleContext}, y no en el objeto mismo, para que una clase no tenga que llenarse de
 * métodos de accesibilidad para participar.
 *
 * <p>Esa indirección es la decisión central del paquete: un componente **tiene** un contexto en vez
 * de **ser** accesible, y así el costo de la accesibilidad —que es real, en memoria y en trabajo—
 * se paga sólo cuando alguien la pide.
 */
public interface Accessible {

    /** La información de accesibilidad de este objeto. */
    AccessibleContext getAccessibleContext();
}
