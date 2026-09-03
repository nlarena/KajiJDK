package java.nio.file.attribute;

// La raiz de las vistas de atributos: solo sabe decir como se llama.
//
// Es una interfaz vacia salvo por `name()`, y eso es todo lo que hace falta para que el resto del
// paquete se pueda escribir: el nombre es la clave con la que `Files.getAttribute("posix:owner")`
// elige la vista. KajiJDK no provee ninguna implementacion --no hay de donde sacar los atributos--
// pero el tipo tiene que existir igual, porque es el parametro de metodos que si existen.
public interface AttributeView {

    /** El nombre de la vista, el prefijo que la identifica en `"vista:atributo"`. */
    String name();
}
