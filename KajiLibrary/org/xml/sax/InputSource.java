package org.xml.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

// KajiLibrary's org.xml.sax.InputSource -- "aca esta el XML, y aca esta lo que se puede saber
// de donde vino".
//
// Es una bolsa mutable de cuatro cosas independientes: un identificador publico, un
// identificador de sistema, y a lo sumo uno de un flujo de bytes o un flujo de caracteres, mas
// un nombre de codificacion que solo aplica al flujo de bytes. Hay una precedencia entre ellos y
// es la fuente de casi toda la confusion, asi que:
//
//   1. el flujo de caracteres, si esta, gana: los caracteres ya vienen decodificados, y
//      cualquier codificacion puesta en este objeto se ignora (igual que la de la declaracion
//      XML);
//   2. si no, el flujo de bytes, decodificado con getEncoding() si esta puesta, o olfateando la
//      declaracion si no lo esta;
//   3. si no, el identificador de sistema se abre como URI.
//
// El identificador de sistema importa incluso cuando se da un flujo: es contra lo que resuelven
// las referencias relativas de adentro del documento. Darle a un parser un flujo de bytes y
// ningun identificador de sistema es legal y rutinariamente produce un "cannot resolve relative
// URI" mas adelante.
//
// isEmpty() es la prueba de "el EntityResolver me devolvio nada?": un InputSource sin
// identificadores y sin contenido legible, que es la forma convencional de decir "rechaza esta
// entidad externa". Es conservador a proposito: todo flujo que no pueda rebobinar y probar vacio
// cuenta como no vacio, y necesita soporte de mark/reset para decir otra cosa.
public class InputSource {

    private String publicId;
    private String systemId;
    private InputStream byteStream;
    private String encoding;
    private Reader characterStream;

    // Una fuente vacia; se espera que quien llama la complete.
    public InputSource() {
    }

    // Una fuente que nombra un URI que el parser tiene que abrir por su cuenta.
    public InputSource(String systemId) {
        setSystemId(systemId);
    }

    // Una fuente sobre bytes crudos. Poner tambien el identificador de sistema si el documento
    // tiene referencias relativas, y la codificacion si los bytes no la anuncian.
    public InputSource(InputStream byteStream) {
        setByteStream(byteStream);
    }

    // Una fuente sobre caracteres ya decodificados; la codificacion no se consulta.
    public InputSource(Reader characterStream) {
        setCharacterStream(characterStream);
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getSystemId() {
        return systemId;
    }

    public void setByteStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public InputStream getByteStream() {
        return byteStream;
    }

    // La codificacion de caracteres del flujo de bytes, si se la conoce. Se ignora cuando hay un
    // flujo de caracteres puesto, y se ignora cuando el parser abre el identificador de sistema
    // por su cuenta.
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setCharacterStream(Reader characterStream) {
        this.characterStream = characterStream;
    }

    public Reader getCharacterStream() {
        return characterStream;
    }

    // Verdadero cuando esta fuente no nombra nada y no lleva contenido: sin identificador
    // publico, sin identificador de sistema, y con el flujo que tenga demostrablemente en el
    // final. Ver la nota de la clase.
    public boolean isEmpty() {
        return (publicId == null && systemId == null && isStreamEmpty());
    }

    // El "demostrablemente vacio" esta trabajando en serio aca. Un flujo que no se puede
    // rebobinar tira desde reset(), y uno ilegible tira desde read(); en los dos casos se
    // contesta false, porque lo unico que no se puede hacer es afirmar que una fuente esta vacia
    // cuando lo que paso es que no se pudo mirar.
    private boolean isStreamEmpty() {
        boolean empty = true;
        try {
            if (byteStream != null) {
                byteStream.reset();
                int bytesRead = byteStream.available();
                if (bytesRead > 0) {
                    return false;
                }
            }
            if (characterStream != null) {
                characterStream.reset();
                int c = characterStream.read();
                characterStream.reset();
                if (c != -1) {
                    return false;
                }
            }
        } catch (IOException ex) {
            return false;
        }
        return empty;
    }
}
