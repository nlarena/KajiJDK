package java.beans.beancontext;

import java.awt.Container;

/**
 * Lo implementa un {@link BeanContext} que ademas es un contenedor visual.
 *
 * <p>La contraparte de {@link BeanContextChildComponentProxy} del lado del contenedor, y por la
 * misma razon: un contexto no tiene por que ser visible, asi que la relacion con AWT se declara en
 * vez de heredarse.
 *
 * <p>Sirve para lo obvio una vez dicho: si el contexto es un contenedor y sus hijos tienen
 * componente, la jerarquia de beans y la de la interfaz grafica pueden mantenerse alineadas solas.
 *
 * @deprecated ver {@link BeanContextChildComponentProxy}.
 */
@Deprecated(since = "23", forRemoval = true)
public interface BeanContextContainerProxy {

    /** El contenedor que representa a este contexto; nunca {@code null}. */
    Container getContainer();
}
