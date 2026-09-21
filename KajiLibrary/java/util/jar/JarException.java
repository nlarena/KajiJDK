package java.util.jar;

import java.util.zip.ZipException;

/**
 * A malformed JAR.
 *
 * <p>It extends `ZipException` --and not a plain `IOException`-- because a JAR **is** a ZIP: what
 * breaks the ZIP breaks the JAR, and whoever catches `ZipException` around a compressed file has to
 * go on catching it when that file turns out to be a JAR.
 */
public class JarException extends ZipException {

    public JarException() {
        super();
    }

    public JarException(String s) {
        super(s);
    }
}
