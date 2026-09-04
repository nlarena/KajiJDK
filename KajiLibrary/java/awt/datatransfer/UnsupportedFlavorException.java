package java.awt.datatransfer;

/**
 * Se pidieron los datos en un formato que no se puede entregar.
 *
 * <p>Es una excepción **comprobada** a propósito: pedir un formato que el origen no ofrece no es un
 * error de programación sino una posibilidad normal, y quien pide tiene que estar preparado.
 */
public class UnsupportedFlavorException extends Exception {

    private static final long serialVersionUID = 5383814944251665601L;

    /**
     * Con el formato que no se pudo entregar.
     *
     * <p>El mensaje es el nombre legible del formato y no su tipo MIME: el que va a leerlo es una
     * persona.
     */
    public UnsupportedFlavorException(DataFlavor flavor) {
        super(flavor != null ? flavor.getHumanPresentableName() : null);
    }
}
