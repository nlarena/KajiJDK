package java.beans.beancontext;

import java.awt.Component;

/**
 * Lo implementa un {@link BeanContextChild} que ademas tiene una parte visual.
 *
 * <h2>Por que la parte visual va aparte y no en el propio bean</h2>
 *
 * <p>Porque un bean no tiene por que ser visible. Un {@link BeanContextChild} puede ser un servicio,
 * un origen de datos o cualquier cosa sin representacion en pantalla, y obligarlo a extender
 * {@link Component} lo ataria a AWT sin motivo.
 *
 * <p>Esta interfaz es la salida: quien tiene componente lo dice implementandola, y el contenedor
 * pregunta con un {@code instanceof} en vez de suponer.
 *
 * @deprecated el modelo de {@code BeanContext} no tiene reemplazo y quedo en desuso; ver el paquete.
 */
@Deprecated(since = "23", forRemoval = true)
public interface BeanContextChildComponentProxy {

    /** El componente que representa a este bean; nunca {@code null}. */
    Component getComponent();
}
