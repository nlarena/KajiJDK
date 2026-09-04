package java.awt.datatransfer;

import java.util.Map;

/**
 * La traducción entre los formatos de Java y los nombres que usa el sistema operativo.
 *
 * <p>Cada plataforma nombra los formatos del portapapeles a su manera: Windows dice `CF_TEXT`, X11
 * dice `STRING`, macOS dice otra cosa. Un {@link DataFlavor} es el nombre de Java. Esta interfaz es
 * el diccionario entre los dos.
 *
 * <p>Sin ella, todo programa Java que quisiera intercambiar datos con un programa nativo tendría que
 * conocer los nombres de cada sistema.
 */
public interface FlavorMap {

    /** Qué nombre nativo le corresponde a cada uno de esos formatos. */
    Map<DataFlavor, String> getNativesForFlavors(DataFlavor[] flavors);

    /** Qué formato le corresponde a cada uno de esos nombres nativos. */
    Map<String, DataFlavor> getFlavorsForNatives(String[] natives);
}
