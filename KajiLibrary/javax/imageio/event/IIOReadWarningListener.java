package javax.imageio.event;

import java.util.EventListener;
import javax.imageio.ImageReader;

/**
 * KajiLibrary's javax.imageio.event.IIOReadWarningListener -- avisa de un problema no fatal al leer.
 *
 * <p>Una advertencia es algo que esta mal en el archivo y de lo que el lector <b>pudo</b> recuperarse:
 * un campo de metadatos corrupto, una suma de verificacion que no da, un valor fuera de rango que se
 * recorto. La imagen sale igual.
 *
 * <p>Sin escuchas registrados esas advertencias <b>se pierden en silencio</b>, y esa es la razon de
 * ser de esta interfaz. Un programa que decodifica archivos de origen desconocido y no registra uno se
 * queda sin saber que la mitad de sus imagenes venian rotas.
 *
 * <p>El mensaje viene traducido segun el idioma que se le haya puesto al lector con
 * {@code ImageReader.setLocale}.
 */
public interface IIOReadWarningListener extends EventListener {

    /** Algo estaba mal y se pudo seguir. Ver la nota de la clase. */
    void warningOccurred(ImageReader source, String warning);
}
