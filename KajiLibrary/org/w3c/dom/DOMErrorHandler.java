package org.w3c.dom;

/**
 * KajiLibrary's org.w3c.dom.DOMErrorHandler -- quien recibe los {@link DOMError}.
 *
 * <p>Se registra en la {@link DOMConfiguration} del documento bajo el parametro
 * {@code "error-handler"}. Un solo metodo, y el valor que devuelve **invierte el control**: con
 * {@code true} el procesador sigue adelante, con {@code false} para. Es la unica forma que tiene el
 * llamador de imponer su politica --lo que para un servidor es fatal para un editor es un aviso--
 * porque el procesador no puede saberla.
 *
 * <p>La regla que se rompe siempre: devolver {@code true} ante un
 * {@link DOMError#SEVERITY_FATAL_ERROR} **no** hace que el procesador continue. Un error fatal es
 * fatal; la norma dice que el procesador puede ignorar la respuesta, y lo que salga de seguir sobre
 * un estado que el mismo declaro inutilizable no significa nada.
 *
 * <p>Interfaz declarada entera.
 */
public interface DOMErrorHandler {

    /**
     * @param error el problema, que solo es valido durante esta llamada: guardarse la referencia y
     *     leerla despues no esta garantizado
     * @return {@code true} para continuar, {@code false} para parar
     */
    public boolean handleError(DOMError error);
}
