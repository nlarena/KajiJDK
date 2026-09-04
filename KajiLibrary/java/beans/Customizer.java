package java.beans;

// El panel a medida con el que una herramienta edita un bean entero, cuando editar propiedad por
// propiedad no alcanza. Quien lo implementa recibe el bean por setObject y avisa de los cambios
// como cualquier fuente de propiedades ligadas.
//
// En el JDK un Customizer ademas hereda de java.awt.Component; aca no puede, porque java.awt no
// existe en este arbol. La interfaz en si —sus tres metodos— no toca awt y queda completa.
public interface Customizer {

    // El bean a editar. Se llama una sola vez, antes de mostrar el panel.
    void setObject(Object bean);

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
