package java.nio.file;

import java.io.File;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

// KajiJDK's only `FileSystem`: the one behind `Path.getFileSystem()` and
// `FileSystems.getDefault()`.
//
// **There is a real filesystem behind it** --`jdk.internal.io.Fs`'s natives reach the disk-- and
// that is why this class says `isReadOnly() == false`. Of the three questions *about* that
// filesystem that used to have no answer --what its roots are, what its volumes are, and who its
// users are-- only the last is left:
//
//   - `getRootDirectories()` used to return empty and `getFileStores()` used to throw. Both answer
//     now: `Fs.roots()` enumerates the drives and `Fs.diskTotal`/`diskUsable`/`diskUnallocated`
//     give the space, so `KajiFileStore` can exist without inventing any zero.
//   - `getUserPrincipalLookupService()` still throws `UnsupportedOperationException`: there the
//     return value **is** the datum, and a service that invented principals would assert
//     falsehoods.
//   - `supportedFileAttributeViews()` returns empty, and not `{"basic"}`: see the method's note.
//
// `newWatchService()` fails because there is no native for watching directories. `getPathMatcher()`
// **no longer fails**: the earlier note said `glob:` and `regex:` could be implemented --they are
// string comparisons, they do not touch the disk-- and that it was debt and not a VM ceiling. It
// was paid. The `UnsupportedOperationException` is left for what the spec says it is: a syntax this
// implementation does not know.
final class KajiFileSystem extends FileSystem {

    static final KajiFileSystem INSTANCE = new KajiFileSystem();

    private KajiFileSystem() {
    }

    public FileSystemProvider provider() {
        return KajiFileSystemProvider.INSTANCE;
    }

    public void close() {
    }

    public boolean isOpen() {
        return true;
    }

    /**
     * `false`.
     *
     * <p>`Files.write`, `createDirectory`, `delete` and `move` work over this filesystem, so saying
     * `true` --"permits only read-only access"-- would be false, and of the kind that stops a
     * program even trying to write.
     */
    public boolean isReadOnly() {
        return false;
    }

    public String getSeparator() {
        return File.separator;
    }

    /**
     * The filesystem's roots, one per mounted drive.
     *
     * <p>It used to return an empty list while there was nothing to enumerate them with.
     * `Fs.roots()` exists, so now they are the real ones -- and they are asked for on every call,
     * not kept: a drive being plugged in adds a root.
     */
    public Iterable<Path> getRootDirectories() {
        List<Path> out = new ArrayList<Path>();
        String[] roots = jdk.internal.io.Fs.roots();
        for (int i = 0; i < roots.length; i++) {
            out.add(this.getPath(roots[i]));
        }
        return out;
    }

    // An **empty** set and not `{"basic"}`: saying the basic view is supported would oblige
    // `Files.getFileAttributeView(p, BasicFileAttributeView.class)` to return something, and that
    // view has `setTimes`, which writes all three timestamps at once -- and only the modification
    // one has a native.
    //
    // Mind the asymmetry, which is real and is right: the basic attributes ARE **read**
    // (`Files.readAttributes` takes them from `stat`, `size` and `mtime`). What is not there is the
    // **view**, which is a read-and-write object. See `Files.getFileAttributeView`.
    public Set<String> supportedFileAttributeViews() {
        return new HashSet<String>();
    }

    /**
     * The filesystem's volumes: one per root.
     *
     * <p>It used to throw `UnsupportedOperationException` while there was nothing to answer about a
     * volume's space with. Now there is, and this returns one {@link FileStore} per root that
     * {@link #getRootDirectories} enumerates -- which is what the JDK does, except that its own
     * also lists the mounts that are not roots, and `Fs` does not know how to enumerate those.
     */
    public Iterable<FileStore> getFileStores() {
        java.util.List<FileStore> out = new java.util.ArrayList<FileStore>();
        for (Path root : this.getRootDirectories()) {
            String pathOf = root.toString();
            // A root that cannot be read is skipped rather than break the whole enumeration: on
            // Windows there are drive letters with no medium in them, and an empty floppy drive has
            // no business stopping the rest of the volumes from being seen.
            if (jdk.internal.io.Fs.diskTotal(pathOf) >= 0L) {
                out.add(new KajiFileStore(pathOf, KajiFileStore.volumeName(pathOf)));
            }
        }
        return out;
    }

    /**
     * It fails.
     *
     * <p>There is no native that queries the system's user database, and a service that returned a
     * principal for any name would be inventing identities.
     */
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException("KajiJDK has no principal lookup service");
    }

    public Path getPath(String first, String... more) {
        return Path.of(first, more);
    }

    /**
     * A path matcher, by `glob:` or by `regex:`.
     *
     * <p>Both end up in a {@link java.util.regex.Pattern} over `path.toString()`. The difference is
     * who writes the expression: with `regex:` the caller writes it, and with `glob:`
     * {@link #globToRegex} translates it.
     *
     * @throws IllegalArgumentException if the `:` is missing or the pattern is malformed
     * @throws UnsupportedOperationException if the syntax is neither of the two
     * @throws java.util.regex.PatternSyntaxException if the expression does not compile
     */
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        int cut = syntaxAndPattern.indexOf(':');
        if (cut <= 0 || cut == syntaxAndPattern.length() - 1) {
            throw new IllegalArgumentException(syntaxAndPattern);
        }
        String syntax = syntaxAndPattern.substring(0, cut);
        String pattern = syntaxAndPattern.substring(cut + 1);
        String expression;
        if (syntax.equalsIgnoreCase("glob")) {
            expression = globToRegex(pattern);
        } else if (syntax.equalsIgnoreCase("regex")) {
            expression = pattern;
        } else {
            throw new UnsupportedOperationException("sintaxis desconocida: " + syntax);
        }
        final java.util.regex.Pattern compiled = java.util.regex.Pattern.compile(expression);
        return new PathMatcher() {
            public boolean matches(Path path) {
                // Against the **whole** path and not against the name: it is what the spec says,
                // and it is what makes `**\/*.java` distinguishable from `*.java`. Whoever wants to
                // compare only the name hands it `path.getFileName()`, which is what `Files` does.
                return path != null && compiled.matcher(path.toString()).matches();
            }
        };
    }

    /**
     * It translates a glob into a regular expression.
     *
     * <p>The rule that defines all the rest: **`*` does not cross separators and `**` does**. That
     * is where the two different translations come from, and why the next character has to be
     * looked at before deciding.
     *
     * <p>The rest is mechanical: `?` is a character that is not a separator, `[...]` passes through
     * almost as it stands --with `!` instead of `^` to negate--, `{a,b}` is an alternation, and
     * `\\` escapes. Every character the regular expression treats specially and the glob does not
     * is escaped.
     *
     * <p>On Windows both separators count, so "is not a separator" is `[^\\/]` and not `[^/]`.
     */
    private static String globToRegex(String glob) {
        StringBuilder re = new StringBuilder();
        // `\Q...\E` is not used on purpose: metacharacters of ours have to be interleaved with the
        // user's text, and nested quoting becomes unreadable at once. Escaping is done character by
        // character, which is longer to write and much easier to read.
        int i = 0;
        int braces = 0;
        while (i < glob.length()) {
            char c = glob.charAt(i);
            i = i + 1;
            if (c == '\\') {
                if (i >= glob.length()) {
                    throw new java.util.regex.PatternSyntaxException(
                            "the pattern ends in an escape backslash", glob, i - 1);
                }
                re.append(java.util.regex.Pattern.quote(String.valueOf(glob.charAt(i))));
                i = i + 1;
            } else if (c == '/') {
                re.append(SEPARATOR);
            } else if (c == '*') {
                if (i < glob.length() && glob.charAt(i) == '*') {
                    // `**` crosses separators. It is the only difference from `*`, and it is the
                    // whole point.
                    re.append(".*");
                    i = i + 1;
                } else {
                    re.append(NON_SEPARATOR).append('*');
                }
            } else if (c == '?') {
                re.append(NON_SEPARATOR);
            } else if (c == '[') {
                i = characterClass(glob, i, re);
            } else if (c == '{') {
                if (braces > 0) {
                    throw new java.util.regex.PatternSyntaxException(
                            "a glob's groups do not nest", glob, i - 1);
                }
                braces = braces + 1;
                re.append('(');
            } else if (c == ',' && braces > 0) {
                re.append('|');
            } else if (c == '}') {
                if (braces == 0) {
                    throw new java.util.regex.PatternSyntaxException(
                            "it closes a group it did not open", glob, i - 1);
                }
                braces = braces - 1;
                re.append(')');
            } else {
                escape(re, c);
            }
        }
        if (braces > 0) {
            throw new java.util.regex.PatternSyntaxException(
                    "a group is left unclosed", glob, glob.length());
        }
        return re.toString();
    }

    // The `[...]` class. It returns where the pattern carries on after the `]`.
    private static int characterClass(String glob, int from, StringBuilder re) {
        int i = from;
        re.append('[');
        if (i < glob.length() && (glob.charAt(i) == '!' || glob.charAt(i) == '^')) {
            // A glob negates with `!`; a regular expression with `^`. A literal `^` at the start of
            // a glob class does **not** negate, but writing it that way is so unusual that the JDK
            // does not tell it apart either.
            re.append('^');
            i = i + 1;
        }
        boolean empty = true;
        while (i < glob.length() && (glob.charAt(i) != ']' || empty)) {
            char c = glob.charAt(i);
            i = i + 1;
            empty = false;
            if (c == '\\' && i < glob.length()) {
                re.append('\\').append(glob.charAt(i));
                i = i + 1;
            } else if (c == '-' || c == ']') {
                re.append('\\').append(c);
            } else if (c == '[' || c == '&' || c == '^') {
                // `&&` is intersection in a Java class and means nothing in a glob.
                re.append('\\').append(c);
            } else {
                re.append(c);
            }
        }
        if (i >= glob.length()) {
            throw new java.util.regex.PatternSyntaxException(
                    "a character class is left unclosed", glob, glob.length());
        }
        re.append(']');
        return i + 1;
    }

    private static void escape(StringBuilder re, char c) {
        if ("\\.[]{}()*+-?^$|".indexOf(c) >= 0) {
            re.append('\\');
        }
        re.append(c);
    }

    // What counts as a separator. On Windows both count, which is why a glob written with `/`
    // matches a path the system writes with `\\`.
    private static final String SEPARATOR =
            java.io.File.separatorChar == '\\' ? "[\\\\/]" : "/";
    private static final String NON_SEPARATOR =
            java.io.File.separatorChar == '\\' ? "[^\\\\/]" : "[^/]";

    public WatchService newWatchService() {
        throw new UnsupportedOperationException("KajiJDK has no watch service");
    }
}
