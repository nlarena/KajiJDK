package java.awt.dnd;

/**
 * Puente para volver un {@link DropTargetContext} a su estado inicial desde otro paquete.
 *
 * <p>No es una clase del JDK: es andamiaje nuestro, del mismo tipo que
 * {@code java.nio.channels.FabricaMapMode}. {@code DropTargetContext.reset()} es de paquete a
 * proposito --nadie de afuera deberia poder borrar el estado de un arrastre en curso-- pero el
 * puente con otros juegos de herramientas graficas, {@code jdk.swing.interop}, tiene que poder
 * hacerlo entre un arrastre y el siguiente. En el JDK ese permiso lo da {@code AWTAccessor}.
 */
public final class AccesoDnD {

    private AccesoDnD() {
    }

    /**
     * Vuelve el contexto a su estado inicial.
     *
     * @param contexto el contexto
     * @throws NullPointerException si el contexto es {@code null}
     */
    public static void reiniciar(DropTargetContext contexto) {
        contexto.reset();
    }
}
