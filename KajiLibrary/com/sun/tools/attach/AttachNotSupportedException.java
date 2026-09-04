package com.sun.tools.attach;

/**
 * No se pudo adjuntar.
 *
 * <p>Distinta de {@link AgentLoadException}, y la diferencia es donde se corto. Aca ni siquiera se
 * llego a hablar con la VM destino. Las tres causas posibles son que no haya ningun
 * {@link com.sun.tools.attach.spi.AttachProvider} instalado, que ninguno de los instalados reconozca
 * ese identificador, o que el destino no sea una VM que acepte conexiones.
 *
 * <p>Que un proveedor la tire no es necesariamente una falla del sistema, y por eso
 * {@link VirtualMachine#attach(String)} la atrapa y sigue con el siguiente proveedor. Es la manera
 * que tiene un proveedor de decir "este identificador no es mio".
 */
public class AttachNotSupportedException extends Exception {

    private static final long serialVersionUID = 3391824968260177264L;

    /** Sin detalle. */
    public AttachNotSupportedException() {
        super();
    }

    /** Con un mensaje que explique el caso. */
    public AttachNotSupportedException(String s) {
        super(s);
    }
}
