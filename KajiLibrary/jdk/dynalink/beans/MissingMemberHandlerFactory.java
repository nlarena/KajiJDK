package jdk.dynalink.beans;

import java.lang.invoke.MethodHandle;

import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;

/**
 * Que hacer cuando se pide un miembro que la clase no tiene.
 *
 * <h2>Por que es configurable</h2>
 *
 * <p>Porque cada lenguaje contesta distinto a {@code obj.noExiste}. Java no compila. JavaScript
 * devuelve {@code undefined}. Otro puede querer una excepcion con un mensaje en su propio idioma, o
 * consultar un objeto prototipo antes de rendirse.
 *
 * <p>Sin este enganche, {@link BeansLinker} tendria que elegir una de esas respuestas para todos, y
 * cualquiera que eligiera estaria mal para la mayoria.
 *
 * <h2>Devolver {@code null} no es lo mismo que fallar</h2>
 *
 * <p>{@code null} significa "no tengo nada que aportar para este caso", y el enlace sigue su curso
 * normal — que termina en un {@link jdk.dynalink.NoSuchDynamicMethodException}. Devolver un handle,
 * en cambio, hace que el sitio quede enlazado a el, y esa es la forma de que {@code obj.noExiste}
 * pase a valer algo en vez de reventar.
 *
 * @since 9
 */
@FunctionalInterface
public interface MissingMemberHandlerFactory {

    /**
     * El metodo con el que responder a un miembro que no existe.
     *
     * @param linkRequest el pedido que no se pudo satisfacer
     * @param linkerServices los servicios del que hospeda
     * @return el metodo, o {@code null} para dejar que falle como siempre
     * @throws Exception si la construccion falla
     */
    MethodHandle createMissingMemberHandler(LinkRequest linkRequest, LinkerServices linkerServices)
            throws Exception;
}
