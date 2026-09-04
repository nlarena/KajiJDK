package com.sun.jdi;

/**
 * Un reflejo de algo que vive en la maquina virtual **de enfrente**.
 *
 * <p>Es la raiz de toda JDI, y la palabra "reflejo" no es adorno: un depurador no tiene los objetos
 * del programa depurado, tiene representantes de ellos. Un `ObjectReference` no *es* el objeto: es
 * un identificador que, cada vez que se lo consulta, cruza el cable JDWP y pregunta.
 *
 * <p>De ahi sale la unica operacion de esta interfaz: dado cualquier reflejo, saber **de que VM**
 * es. Dos reflejos de VM distintas no se pueden mezclar, y sin esto no habria como comprobarlo.
 */
public interface Mirror {

    /** La maquina virtual de la que este reflejo es reflejo. */
    VirtualMachine virtualMachine();

    /** Una descripcion legible; la forma exacta depende de la implementacion. */
    String toString();
}
