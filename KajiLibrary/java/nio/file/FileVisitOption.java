package java.nio.file;

// How to configure a walk of the file tree. It has had a single constant since it existed.
//
// This note used to say KajiJDK does not walk trees and that no method takes it; `Fs.list` arrived
// and `Files.walkFileTree`, `walk` and `find` all take it. It changes nothing, because this VM has
// no symbolic links to follow -- which is said where the option is read.
public enum FileVisitOption {

    /** Follow the symbolic links on the way down. */
    FOLLOW_LINKS
}
