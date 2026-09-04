package java.awt;

/**
 * La marca que lleva un {@link Graphics} que está dibujando **sobre una impresión**.
 *
 * <p>Es cómo un componente se entera de que lo están imprimiendo y no pintando en pantalla, sin
 * mirar la clase concreta: si el `Graphics` que recibió es un `PrintGraphics`, va a papel. Sirve
 * para no dibujar lo que no tiene sentido impreso —un cursor, un resaltado de selección—.
 */
public interface PrintGraphics {

    /** El trabajo de impresión al que pertenece. */
    PrintJob getPrintJob();
}
