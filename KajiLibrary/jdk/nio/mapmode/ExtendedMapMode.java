package jdk.nio.mapmode;

import java.nio.channels.MapModes;
import java.nio.channels.FileChannel;

/**
 * Los modos de mapeo que no estan en {@link FileChannel.MapMode}.
 *
 * <p>Los dos son para memoria **no volatil**: un mapeo hecho con ellos se puede forzar a persistir
 * con `MappedByteBuffer.force()`, que es lo que los distingue de `READ_ONLY` y `READ_WRITE`. La
 * diferencia vive del lado de `map()`, no aca: un `MapMode` es una etiqueta, y estas dos son las
 * etiquetas.
 *
 * <p>Que `map()` las acepte depende del `FileChannel` concreto. El nuestro ya mapea, pero **estas
 * dos las rechaza** con `UnsupportedOperationException`: pedir memoria no volatil y recibir un
 * mapeo comun seria justamente la clase de respuesta que miente. El JDK 25 sobre Windows contesta
 * lo mismo, por la misma razon.
 *
 * @since 14
 */
public class ExtendedMapMode {

    /** Mapeo de solo lectura sobre memoria no volatil. */
    public static final FileChannel.MapMode READ_ONLY_SYNC =
            MapModes.of("READ_ONLY_SYNC");

    /** Mapeo de lectura y escritura sobre memoria no volatil. */
    public static final FileChannel.MapMode READ_WRITE_SYNC =
            MapModes.of("READ_WRITE_SYNC");

    private ExtendedMapMode() {
    }
}
