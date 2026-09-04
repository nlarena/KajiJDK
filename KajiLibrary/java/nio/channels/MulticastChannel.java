package java.nio.channels;

/**
 * KajiLibrary's java.nio.channels.MulticastChannel — un canal de red que puede sumarse a un grupo
 * de multidifusion.
 *
 * <p>Sumarse a un grupo es pedirle a **una placa de red concreta** que empiece a aceptar los paquetes
 * dirigidos a una direccion de grupo. Los dos datos son necesarios: en una maquina con varias placas
 * no hay una respuesta correcta a "por cual", y el sistema no puede elegirla por uno.
 *
 * <h2>Los dos `join`, que antes no estaban</h2>
 *
 * <p>Faltaban por los **tipos** y no por los sockets: los dos toman un `java.net.NetworkInterface`,
 * que no existia en este arbol. Ya existe, y con ella los dos `join` -- y
 * {@link MembershipKey#networkInterface()}, que faltaba por lo mismo.
 *
 * <p>La diferencia entre los dos no es de comodidad. El de tres argumentos pide **solo a ese
 * emisor**: es una membresia especifica de la fuente (SSM), que el sistema tiene que sostener aparte
 * y que no todos sostienen. Por eso el contrato permite que tire
 * {@link UnsupportedOperationException} -- y no es una escapatoria, es informacion: significa "esta
 * pila no filtra por emisor", que es distinto de "no se pudo".
 */
public interface MulticastChannel extends NetworkChannel {

    /**
     * Cierra el canal.
     *
     * <p>Se redeclara --tambien en el JDK-- para documentar que cerrar **da de baja todas las
     * membresias**. Sin eso, un programa que cierra el canal y no llama a `drop()` dejaria a la
     * placa recibiendo trafico de un grupo que ya no le interesa a nadie.
     */
    void close();

    /**
     * Se suma al grupo {@code group} por la placa {@code interf}.
     *
     * <p>Los dos datos son necesarios: en una maquina con varias placas no hay una respuesta correcta
     * a "por cual", y el sistema no puede elegirla por uno.
     *
     * @return el comprobante, con el que despues se da de baja
     * @throws IllegalArgumentException si la direccion no es de multidifusion, o si no es de una
     *     familia que la placa soporte
     * @throws IllegalStateException si el canal ya esta en ese grupo por esa placa
     * @throws java.nio.channels.ClosedChannelException si el canal esta cerrado
     * @throws IOException si fallo la operacion
     */
    MembershipKey join(java.net.InetAddress group, java.net.NetworkInterface interf)
            throws IOException;

    /**
     * Se suma al grupo {@code group} por la placa {@code interf}, **pero solo para** el emisor
     * {@code source}.
     *
     * <p>Es una membresia especifica de la fuente. Sirve cuando el grupo es publico y lo unico que
     * interesa es un emisor conocido: el filtro lo hace el sistema, asi que el trafico de los demas
     * ni siquiera sube.
     *
     * @throws UnsupportedOperationException si la pila no sostiene membresias por emisor. El
     *     contrato lo permite, y decirlo es mas util que fallar como si fuera un error de red
     * @throws IllegalArgumentException si alguna de las dos direcciones no sirve para esto
     * @throws IOException si fallo la operacion
     */
    MembershipKey join(java.net.InetAddress group, java.net.NetworkInterface interf,
            java.net.InetAddress source) throws IOException;
}
