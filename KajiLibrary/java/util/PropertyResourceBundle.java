package java.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

// Un ResourceBundle escrito como archivo `.properties` en vez de como clase.
//
// Es la otra mitad de ListResourceBundle, y la que se usa cuando las traducciones las mantiene
// alguien que no compila: un `.properties` se edita con cualquier editor y no necesita pasar por
// javac. La contrapartida es que los valores solo pueden ser cadenas — un `.properties` no sabe
// decir "arreglo de String" ni "entero".
//
// Cargar es un acto unico: el archivo se lee entero en el constructor y despues el bundle es
// inmutable. Por eso el constructor declara `throws IOException` y ningun otro metodo lo hace.
public class PropertyResourceBundle extends ResourceBundle {

    // Los pares leidos del archivo.
    private final Properties lookup;

    // Lee el bundle de `stream`, en ISO-8859-1 como manda el formato.
    //
    // Que sea Latin-1 y no UTF-8 sorprende siempre, y es por compatibilidad: el formato es
    // anterior a que UTF-8 fuera lo normal, y por eso trae `\\uXXXX` — es la unica forma de
    // escribir un caracter que no entra en un byte.
    public PropertyResourceBundle(InputStream stream) throws IOException {
        this.lookup = new Properties();
        this.lookup.load(stream);
    }

    // Lee el bundle de `reader`, que ya viene decodificado.
    //
    // Esta es la sobrecarga que hay que usar para un archivo en UTF-8: el llamador decide la
    // codificacion al construir el Reader, en vez de quedar atado al Latin-1 del stream.
    public PropertyResourceBundle(Reader reader) throws IOException {
        this.lookup = new Properties();
        this.lookup.load(reader);
    }

    // El valor de `key` en ESTE bundle, o null. No consulta al padre: de eso se ocupa getObject.
    public Object handleGetObject(String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        return this.lookup.get(key);
    }

    // Todas las claves visibles: las propias y las que agregue el padre.
    public Enumeration<String> getKeys() {
        Enumeration<String> delPadre = null;
        if (this.parent != null) {
            delPadre = this.parent.getKeys();
        }
        return new BundleKeyEnumeration(this.handleKeySet().iterator(), delPadre);
    }

    // Las claves que este bundle define, sin las del padre.
    protected Set<String> handleKeySet() {
        HashSet<String> out = new HashSet<String>();
        Iterator<String> it = this.lookup.stringPropertyNames().iterator();
        while (it.hasNext()) {
            out.add(it.next());
        }
        return out;
    }
}
