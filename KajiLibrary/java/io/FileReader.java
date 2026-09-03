package java.io;

import java.nio.charset.Charset;

// KajiLibrary's java.io.FileReader -- caracteres leidos de un archivo.
//
// No agrega nada: es exactamente `new InputStreamReader(new FileInputStream(f), cs)` con un nombre
// mas corto. Que la clase exista igual no es redundancia, es lo que hace que el caso comun --leer
// un archivo de texto-- se escriba en una linea y sin nombrar dos clases intermedias.
//
// **Hereda la lectura entera de `FileInputStream`**: el archivo se lee completo al construirse, y
// lo que otro proceso le escriba despues no se ve. Ver la nota de esa clase; cuando el sustrato
// tenga descriptores, esto se arregla solo.
//
// El constructor por `Charset` es el que conviene usar. Los que no lo reciben toman el charset por
// omision de la plataforma, y esa dependencia del entorno es justamente lo que hace que un
// programa lea bien en una maquina y mal en otra.
public class FileReader extends InputStreamReader {

    /**
     * Abre `fileName` para leer texto con el charset por omision.
     *
     * @throws FileNotFoundException si no existe, es un directorio, o no se puede leer
     */
    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }

    /**
     * Abre `file` para leer texto con el charset por omision.
     *
     * @throws FileNotFoundException si no existe, es un directorio, o no se puede leer
     */
    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

    /** Lee por descriptor. Esta biblioteca no modela descriptores; ver `FileInputStream`. */
    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

    /**
     * Abre `fileName` para leer texto con el charset dado.
     *
     * @throws IOException si no existe, es un directorio, o no se puede leer
     */
    public FileReader(String fileName, Charset charset) throws IOException {
        super(new FileInputStream(fileName), charset);
    }

    /**
     * Abre `file` para leer texto con el charset dado.
     *
     * @throws IOException si no existe, es un directorio, o no se puede leer
     */
    public FileReader(File file, Charset charset) throws IOException {
        super(new FileInputStream(file), charset);
    }
}
