package java.nio.file;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// KajiJDK's single concrete Path: a normalized path string plus the NIO.2 path algebra over its
// root and name elements. No file system is touched; register() is unsupported and toRealPath()
// falls back to normalization.
final class KajiPath implements Path {

    private static final char SEP = File.separatorChar;

    private final String path;

    KajiPath(String p) {
        this.path = clean(p);
    }

    private static String clean(String p) {
        if (p == null) {
            throw new NullPointerException("path cannot be null");
        }
        StringBuilder sb = new StringBuilder();
        char prev = 0;
        int i = 0;
        while (i < p.length()) {
            char c = p.charAt(i);
            if (c == '/' || c == '\\') {
                c = SEP;
            }
            if (!(c == SEP && prev == SEP)) {
                sb.append(c);
                prev = c;
            }
            i = i + 1;
        }
        int len = sb.length();
        if (len > 1 && sb.charAt(len - 1) == SEP) {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }

    // Length of the root prefix (0 if relative).
    private int rootLen() {
        if (this.path.length() == 0) {
            return 0;
        }
        if (SEP == '\\') {
            if (this.path.length() >= 2 && this.path.charAt(1) == ':') {
                return (this.path.length() >= 3 && this.path.charAt(2) == SEP) ? 3 : 2;
            }
            return this.path.charAt(0) == SEP ? 1 : 0;
        }
        return this.path.charAt(0) == SEP ? 1 : 0;
    }

    private List<String> segs() {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        int i = this.rootLen();
        while (i < this.path.length()) {
            char c = this.path.charAt(i);
            if (c == SEP) {
                if (cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
            i = i + 1;
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    public FileSystem getFileSystem() {
        return KajiFileSystem.INSTANCE;
    }

    public boolean isAbsolute() {
        return this.rootLen() > 0;
    }

    public Path getRoot() {
        int r = this.rootLen();
        return (r == 0) ? null : new KajiPath(this.path.substring(0, r));
    }

    public Path getFileName() {
        List<String> s = this.segs();
        return s.isEmpty() ? null : new KajiPath(s.get(s.size() - 1));
    }

    public Path getParent() {
        List<String> s = this.segs();
        int r = this.rootLen();
        if (s.isEmpty()) {
            return null;
        }
        if (s.size() == 1) {
            return (r > 0) ? new KajiPath(this.path.substring(0, r)) : null;
        }
        StringBuilder sb = new StringBuilder(this.path.substring(0, r));
        int i = 0;
        while (i < s.size() - 1) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != SEP) {
                sb.append(SEP);
            }
            sb.append(s.get(i));
            i = i + 1;
        }
        return new KajiPath(sb.toString());
    }

    public int getNameCount() {
        return this.segs().size();
    }

    public Path getName(int index) {
        List<String> s = this.segs();
        if (index < 0 || index >= s.size()) {
            throw new IllegalArgumentException("index: " + index);
        }
        return new KajiPath(s.get(index));
    }

    public Path subpath(int beginIndex, int endIndex) {
        List<String> s = this.segs();
        if (beginIndex < 0 || beginIndex >= endIndex || endIndex > s.size()) {
            throw new IllegalArgumentException();
        }
        StringBuilder sb = new StringBuilder();
        int i = beginIndex;
        while (i < endIndex) {
            if (i > beginIndex) {
                sb.append(SEP);
            }
            sb.append(s.get(i));
            i = i + 1;
        }
        return new KajiPath(sb.toString());
    }

    public boolean startsWith(Path other) {
        KajiPath o = (KajiPath) other;
        if (this.isAbsolute() != o.isAbsolute()) {
            return false;
        }
        if (this.isAbsolute()
                && !this.path.substring(0, this.rootLen()).equals(o.path.substring(0, o.rootLen()))) {
            return false;
        }
        List<String> a = this.segs();
        List<String> b = o.segs();
        if (b.size() > a.size()) {
            return false;
        }
        int i = 0;
        while (i < b.size()) {
            if (!a.get(i).equals(b.get(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public boolean endsWith(Path other) {
        KajiPath o = (KajiPath) other;
        if (o.isAbsolute()) {
            return this.equals(o);
        }
        List<String> a = this.segs();
        List<String> b = o.segs();
        if (b.size() > a.size()) {
            return false;
        }
        int off = a.size() - b.size();
        int i = 0;
        while (i < b.size()) {
            if (!a.get(i + off).equals(b.get(i))) {
                return false;
            }
            i = i + 1;
        }
        return true;
    }

    public Path normalize() {
        List<String> out = new ArrayList<String>();
        boolean abs = this.isAbsolute();
        for (String seg : this.segs()) {
            if (seg.equals(".")) {
                continue;
            }
            if (seg.equals("..")) {
                if (!out.isEmpty() && !out.get(out.size() - 1).equals("..")) {
                    out.remove(out.size() - 1);
                } else if (!abs) {
                    out.add("..");
                }
                continue;
            }
            out.add(seg);
        }
        StringBuilder sb = new StringBuilder(this.path.substring(0, this.rootLen()));
        int i = 0;
        while (i < out.size()) {
            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != SEP) {
                sb.append(SEP);
            }
            sb.append(out.get(i));
            i = i + 1;
        }
        String res = sb.toString();
        if (res.length() == 0) {
            res = ".";
        }
        return new KajiPath(res);
    }

    public Path resolve(Path other) {
        KajiPath o = (KajiPath) other;
        if (o.path.length() == 0) {
            return this;
        }
        if (o.isAbsolute() || this.path.length() == 0) {
            return o;
        }
        String base = this.path;
        String res = (base.charAt(base.length() - 1) == SEP) ? base + o.path : base + SEP + o.path;
        return new KajiPath(res);
    }

    public Path relativize(Path other) {
        KajiPath o = (KajiPath) other;
        if (this.isAbsolute() != o.isAbsolute()) {
            throw new IllegalArgumentException("'other' is a different type of Path");
        }
        List<String> a = this.segs();
        List<String> b = o.segs();
        int common = 0;
        while (common < a.size() && common < b.size() && a.get(common).equals(b.get(common))) {
            common = common + 1;
        }
        List<String> out = new ArrayList<String>();
        int i = common;
        while (i < a.size()) {
            out.add("..");
            i = i + 1;
        }
        i = common;
        while (i < b.size()) {
            out.add(b.get(i));
            i = i + 1;
        }
        StringBuilder sb = new StringBuilder();
        i = 0;
        while (i < out.size()) {
            if (i > 0) {
                sb.append(SEP);
            }
            sb.append(out.get(i));
            i = i + 1;
        }
        return new KajiPath(sb.toString());
    }

    public URI toUri() {
        String abs = this.toAbsolutePath().toString();
        StringBuilder sb = new StringBuilder("file:");
        if (abs.length() == 0 || abs.charAt(0) != SEP) {
            sb.append('/');
        }
        int i = 0;
        while (i < abs.length()) {
            char c = abs.charAt(i);
            sb.append(c == SEP ? '/' : c);
            i = i + 1;
        }
        return URI.create(sb.toString());
    }

    public Path toAbsolutePath() {
        if (this.isAbsolute()) {
            return this;
        }
        String cwd = System.getProperty("user.dir");
        if (cwd == null || cwd.length() == 0) {
            cwd = File.separator;
        }
        return new KajiPath(cwd + SEP + this.path);
    }

    public Path toRealPath(LinkOption... options) throws IOException {
        return this.toAbsolutePath().normalize();
    }

    public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
            WatchEvent.Modifier... modifiers) throws IOException {
        throw new UnsupportedOperationException("KajiJDK has no watch service");
    }

    public int compareTo(Path other) {
        return this.path.compareTo(((KajiPath) other).path);
    }

    public boolean equals(Object other) {
        return (other instanceof KajiPath) && this.path.equals(((KajiPath) other).path);
    }

    public int hashCode() {
        return this.path.hashCode();
    }

    public String toString() {
        return this.path;
    }
}
