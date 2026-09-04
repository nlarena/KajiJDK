package javax.management;

/**
 * El punto donde se cambia la implementacion del agente.
 *
 * <p>Es un objeto y no un metodo estatico por una sola razon, y es la que le da sentido: la fabrica
 * no lo instancia con `new MBeanServerBuilder()` sino cargando la clase que nombre la propiedad
 * `javax.management.builder.initial`. Redefinir esta clase es como se le mete un agente propio
 * --uno que audite, uno que replique-- a un programa que ya esta escrito, sin tocar el programa.
 *
 * <p>Los dos metodos estan separados a proposito. Una subclase que solo quiera cambiar los datos
 * del delegado --el nombre del implementador, la version-- redefine `newMBeanServerDelegate` y deja
 * el agente como esta.
 */
public class MBeanServerBuilder {

    public MBeanServerBuilder() {
    }

    /** El delegado que va a llevar el `MBeanServerId` y a emitir las altas y bajas. */
    public MBeanServerDelegate newMBeanServerDelegate() {
        return new MBeanServerDelegate();
    }

    /**
     * El agente.
     *
     * @param defaultDomain el dominio que se usa cuando un {@link ObjectName} no trae uno
     * @param outer el agente que se le pasa a los MBeans en `preRegister`, para que un envoltorio
     *        pueda hacerse pasar por el agente verdadero. Si es `null`, el agente se pasa a si
     *        mismo, que es el caso normal
     * @param delegate el que devolvio {@link #newMBeanServerDelegate}
     */
    public MBeanServer newMBeanServer(String defaultDomain, MBeanServer outer,
                                      MBeanServerDelegate delegate) {
        return new LocalServer(defaultDomain, outer, delegate);
    }
}
