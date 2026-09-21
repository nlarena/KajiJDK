package java.io;

import java.nio.charset.Charset;

// KajiLibrary's java.io.FileReader -- characters read from a file.
//
// It adds nothing: it is exactly `new InputStreamReader(new FileInputStream(f), cs)` under a
// shorter name. That the class exists all the same is not redundancy, it is what makes the common
// case --reading a text file-- one line long and free of two intermediate class names.
//
// **It inherits `FileInputStream`'s whole-file reading**: the file is read complete on
// construction, and what another process writes to it afterwards is not seen. See that class's
// note; when the substrate has descriptors, this fixes itself.
//
// The `Charset` constructor is the one worth using. The ones that do not receive it take the
// platform's default charset, and that dependence on the environment is precisely what makes a
// program read well on one machine and badly on another.
public class FileReader extends InputStreamReader {

    /**
     * Opens `fileName` for reading text with the default charset.
     *
     * @throws FileNotFoundException if it does not exist, is a directory, or cannot be read
     */
    public FileReader(String fileName) throws FileNotFoundException {
        super(new FileInputStream(fileName));
    }

    /**
     * Opens `file` for reading text with the default charset.
     *
     * @throws FileNotFoundException if it does not exist, is a directory, or cannot be read
     */
    public FileReader(File file) throws FileNotFoundException {
        super(new FileInputStream(file));
    }

    /** Reads by descriptor. This library does not model descriptors; see `FileInputStream`. */
    public FileReader(FileDescriptor fd) {
        super(new FileInputStream(fd));
    }

    /**
     * Opens `fileName` for reading text with the given charset.
     *
     * @throws IOException if it does not exist, is a directory, or cannot be read
     */
    public FileReader(String fileName, Charset charset) throws IOException {
        super(new FileInputStream(fileName), charset);
    }

    /**
     * Opens `file` for reading text with the given charset.
     *
     * @throws IOException if it does not exist, is a directory, or cannot be read
     */
    public FileReader(File file, Charset charset) throws IOException {
        super(new FileInputStream(file), charset);
    }
}
