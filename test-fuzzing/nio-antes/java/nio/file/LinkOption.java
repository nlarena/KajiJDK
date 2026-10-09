package java.nio.file;

// KajiLibrary's java.nio.file.LinkOption -- how symbolic links are handled. Its sole constant,
// NOFOLLOW_LINKS, says "do not follow a link". It is both an OpenOption and a CopyOption.
public enum LinkOption implements OpenOption, CopyOption {
    NOFOLLOW_LINKS
}
