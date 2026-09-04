package java.awt.datatransfer;

import java.util.List;

/**
 * Un {@link FlavorMap} que admite traducciones **de uno a varios**.
 *
 * <p>La correspondencia entre formatos y nombres nativos no es una biyección: un texto de Java puede
 * entregarse como varios formatos nativos distintos, y un formato nativo puede corresponder a varias
 * clases de Java. El mapa base devuelve uno solo, el mejor; esta interfaz devuelve la lista entera,
 * ordenada de mejor a peor.
 */
public interface FlavorTable extends FlavorMap {

    /** Todos los nombres nativos que sirven para ese formato, del mejor al peor. */
    List<String> getNativesForFlavor(DataFlavor flav);

    /** Todos los formatos que sirven para ese nombre nativo, del mejor al peor. */
    List<DataFlavor> getFlavorsForNative(String nat);
}
