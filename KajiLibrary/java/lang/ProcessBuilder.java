package java.lang;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// The builder of operating system processes: it gathers the command, the working directory, the
// environment, and where the child's three standard streams go.
//
// ===========================================================================================
// WHAT THIS CLASS DOES **NOT** DECLARE, AND WHY
// ===========================================================================================
//
// `start()` and `startPipeline(List)` **are here**, and until recently they were not. It is worth
// telling why for both, because it is the same reason.
//
// They were left out while the VM could not launch processes. `start()`'s contract is "return a
// `Process` representing a running child process", and with no process subsystem that cannot be
// met: `java.lang.Process` is abstract and nobody extended it, there was not one native that would
// launch anything. Declaring it anyway and making it throw `UnsupportedOperationException` --as
// `Runtime.exec` and `ProcessHandle`'s factories do-- would have been worse than the absence: one
// that exists and cannot deliver lies to whoever compiles against it, passes compilation and
// resolution, and blows up only when run. One that does not exist fails in the compiler, which is
// where it gets fixed.
//
// Now the VM does know how: `jdk.internal.proc.Proc` is the seam, with launching, waiting,
// signalling and the three pipes. So `start()` was written, and with it `ChildProcess`, the missing
// implementation of `Process`. The absence belonged to the substrate and not to this class, and the
// substrate was raised instead of the absence being documented better.
//
// What still cannot be delivered **is said**: see `startPipeline`, which builds the chain with
// temporary files and not with direct pipes, and `ProcessHandle`, which still throws.
//
// `final`, as in the JDK: nobody can extend it to "add" a `start()` that could not deliver.
public final class ProcessBuilder {

    // **The list handed to us** is kept, not a copy. It is part of the JDK's contract and it is a
    // trap on purpose: modifying the list afterwards changes the builder. The varargs overloads do
    // copy, because there the array is built by the compiler and there is nothing to share.
    private List<String> command;

    // null means "the current process's working directory", not "none".
    private File directory;

    // Lazy: it is copied from the process's environment only when somebody asks. Two builders never
    // share a map.
    private Map<String, String> environment;

    private boolean redirectErrorStream;

    // Three positions: 0 input, 1 output, 2 error. Lazy as well; null stands for "all three on
    // PIPE", which is the initial state.
    private Redirect[] redirects;

    public ProcessBuilder(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        }
        this.command = command;
    }

    public ProcessBuilder(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (int i = 0; i < command.length; i++) {
            this.command.add(command[i]);
        }
    }

    public ProcessBuilder command(List<String> command) {
        if (command == null) {
            throw new NullPointerException();
        }
        this.command = command;
        return this;
    }

    public ProcessBuilder command(String... command) {
        this.command = new ArrayList<String>(command.length);
        for (int i = 0; i < command.length; i++) {
            this.command.add(command[i]);
        }
        return this;
    }

    // The live list, not a copy: writing to what this returns changes the builder.
    public List<String> command() {
        return this.command;
    }

    // The live map of the child's environment, initialised with a copy of the process's.
    //
    // Here `System.getenv()` returns an empty map (KajiJDK does not read the OS environment), so in
    // practice one starts from nothing. What is real is the **map's behaviour**, which is the only
    // observable part of this method while there is no `start()`. See `Environment`.
    //
    // Every call returns the same map, and two builders never share theirs.
    public Map<String, String> environment() {
        if (this.environment == null) {
            Environment env = new Environment();
            env.putAll(System.getenv());
            this.environment = env;
        }
        return this.environment;
    }

    // The child's environment.
    //
    // It is a **case-sensitive** `HashMap`, and that is the counter-intuitive part: on Windows
    // variable names do not distinguish case, but that holds for the process's environment
    // --`System.getenv(String)`, which does look up case-insensitively-- and **not** for this map.
    // The JDK uses two different structures for exactly that reason, and the writing one is copied
    // here. A
    // `env.put("Path", ...)` no pisa a `PATH`.
    //
    // What it does validate, and why the class exists instead of a bare `HashMap`:
    //
    //   - nulls: neither key nor value, neither to write nor to look up. A `HashMap` would accept
    //     them silently and the error would turn up much later.
    //   - the `=`: it is the environment block's separator, so a name containing it cannot be
    //     represented. It is allowed at position 0 because Windows uses magic names of the form
    //     `=C:` to keep each drive's current directory.
    //   - the `\0`: the OS's environment block is null-terminated.
    //
    // What it does not replicate: the views (`keySet`, `values`, `entrySet`) are `HashMap`'s and do
    // not reject a null in `contains`. It is the only observable difference from the JDK and it is
    // noted here instead of being silenced.
    private static final class Environment extends HashMap<String, String> {

        // The null character, as a constant and not as a literal: a real `\0` inside the source
        // turns it into a binary file for grep, diff and any text tool.
        private static final int NUL = 0;

        private static String validName(String name) {
            if (name.indexOf('=', 1) != -1 || name.indexOf(Environment.NUL) != -1) {
                throw new IllegalArgumentException(
                        "Invalid environment variable name: \"" + name + "\"");
            }
            return name;
        }

        private static String validValue(String value) {
            if (value.indexOf(Environment.NUL) != -1) {
                throw new IllegalArgumentException(
                        "Invalid environment variable value: \"" + value + "\"");
            }
            return value;
        }

        private static String nonNull(Object o) {
            if (o == null) {
                throw new NullPointerException();
            }
            return (String) o;
        }

        public String put(String key, String value) {
            return super.put(Environment.validName(key), Environment.validValue(value));
        }

        public String get(Object key) {
            return super.get(Environment.nonNull(key));
        }

        public boolean containsKey(Object key) {
            return super.containsKey(Environment.nonNull(key));
        }

        public boolean containsValue(Object value) {
            return super.containsValue(Environment.nonNull(value));
        }

        public String remove(Object key) {
            return super.remove(Environment.nonNull(key));
        }
    }

    public File directory() {
        return this.directory;
    }

    // `null` is a valid value: it means "inherit the current process's working directory", which is
    // different from "not configured".
    public ProcessBuilder directory(File directory) {
        this.directory = directory;
        return this;
    }

    // ---------------- I/O redirection ----------------

    private Redirect[] redirects() {
        if (this.redirects == null) {
            this.redirects = new Redirect[] {Redirect.PIPE, Redirect.PIPE, Redirect.PIPE};
        }
        return this.redirects;
    }

    // Validation is the only thing separating a correct builder from an absurd one, and it does not
    // depend on being able to launch anything: an input source cannot be a write destination.
    public ProcessBuilder redirectInput(Redirect source) {
        if (source.type() == Redirect.Type.WRITE || source.type() == Redirect.Type.APPEND) {
            throw new IllegalArgumentException("Redirect invalid for reading: " + source);
        }
        this.redirects()[0] = source;
        return this;
    }

    public ProcessBuilder redirectOutput(Redirect destination) {
        if (destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
        this.redirects()[1] = destination;
        return this;
    }

    public ProcessBuilder redirectError(Redirect destination) {
        if (destination.type() == Redirect.Type.READ) {
            throw new IllegalArgumentException("Redirect invalid for writing: " + destination);
        }
        this.redirects()[2] = destination;
        return this;
    }

    public ProcessBuilder redirectInput(File file) {
        return this.redirectInput(Redirect.from(file));
    }

    public ProcessBuilder redirectOutput(File file) {
        return this.redirectOutput(Redirect.to(file));
    }

    public ProcessBuilder redirectError(File file) {
        return this.redirectError(Redirect.to(file));
    }

    public Redirect redirectInput() {
        return this.redirects()[0];
    }

    public Redirect redirectOutput() {
        return this.redirects()[1];
    }

    public Redirect redirectError() {
        return this.redirects()[2];
    }

    public ProcessBuilder inheritIO() {
        Redirect[] rs = this.redirects();
        for (int i = 0; i < 3; i++) {
            rs[i] = Redirect.INHERIT;
        }
        return this;
    }

    public boolean redirectErrorStream() {
        return this.redirectErrorStream;
    }

    // When this is true the error output is merged into the standard one, and that is why
    // `redirectError(...)` has no effect: there are not two streams to send to different places.
    public ProcessBuilder redirectErrorStream(boolean redirectErrorStream) {
        this.redirectErrorStream = redirectErrorStream;
        return this;
    }

    // Where one of the child's standard streams goes -- or comes from.
    //
    // It is a sealed type disguised as an abstract class: the constructor is private, so the only
    // possible instances are the three constants and the three the factories return. Each of those
    // six forms has a different `Type` and that is all the state that matters.
    public abstract static class Redirect {

        // The system's black hole. On Windows it is a reserved device name, not a file; on POSIX it
        // is a real file. It makes no difference: nobody opens it here, because nobody
        // launches processes. It exists so `DISCARD.file()` answers what the contract says.
        private static final File NULL_FILE = new File(
                System.getProperty("os.name", "").startsWith("Windows") ? "NUL" : "/dev/null");

        // A redirection's category.
        //
        // Mind `DISCARD`, which is the confusing exception: its `type()` is `WRITE`, not a value of
        // its own. Throwing away is writing to a file, and the file is the system's null.
        public enum Type {
            PIPE,
            INHERIT,
            READ,
            WRITE,
            APPEND;
        }

        // Private: there is no way of inventing a seventh kind of redirection from outside.
        private Redirect() {
        }

        public abstract Type type();

        // null when the redirection involves no file (PIPE and INHERIT).
        public File file() {
            return null;
        }

        // It only makes sense for the ones that write. The base throws instead of returning `false`
        // because asking a `PIPE` whether it appends is not a question with an answer.
        boolean append() {
            throw new UnsupportedOperationException();
        }

        public static final Redirect PIPE = new Concrete(Type.PIPE, null, false, "PIPE");

        public static final Redirect INHERIT = new Concrete(Type.INHERIT, null, false, "INHERIT");

        // Mind the text: it is "WRITE", not "DISCARD". The three constants print as their `type()`,
        // and this one's is `WRITE`. It looks odd and it is what the JDK does.
        public static final Redirect DISCARD =
                new Concrete(Type.WRITE, Redirect.NULL_FILE, false, Type.WRITE.toString());

        public static Redirect from(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concrete(Type.READ, file, false,
                    "redirect to read from file \"" + file + "\"");
        }

        public static Redirect to(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concrete(Type.WRITE, file, false,
                    "redirect to write to file \"" + file + "\"");
        }

        public static Redirect appendTo(File file) {
            if (file == null) {
                throw new NullPointerException();
            }
            return new Concrete(Type.APPEND, file, true,
                    "redirect to append to file \"" + file + "\"");
        }

        // Two redirections are equal if they are the same object, or if they have the same type
        // and the same file.
        //
        // The second branch is only reached by redirections that have a file: `PIPE` and `INHERIT`
        // are unique constants, so for them the comparison already ended on identity --any other
        // object has a different `type()`-- and the null is never dereferenced.
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Redirect)) {
                return false;
            }
            Redirect r = (Redirect) obj;
            if (r.type() != this.type()) {
                return false;
            }
            return this.file().equals(r.file());
        }

        public int hashCode() {
            File file = this.file();
            if (file == null) {
                return super.hashCode();
            }
            return file.hashCode();
        }

        // The six forms are told apart only by their four fields, so one class is enough. The JDK
        // uses six anonymous classes; the observable result is the same and this reads better.
        private static final class Concrete extends Redirect {

            private final Type type;
            private final File file;
            private final boolean append;
            private final String text;

            Concrete(Type type, File file, boolean append, String text) {
                this.type = type;
                this.file = file;
                this.append = append;
                this.text = text;
            }

            public Type type() {
                return this.type;
            }

            public File file() {
                return this.file;
            }

            boolean append() {
                return this.append;
            }

            public String toString() {
                return this.text;
            }
        }
    }

    // ---- launching ---------------------------------------------------------------------------------

    /**
     * It launches the process with this builder's configuration.
     *
     * <p>Each call launches **a new one**: the builder can be reused, and modifying it afterwards
     * does not touch the ones already out. That forces copying the command and the environment
     * here, and it is on purpose -- were the live list passed, changing it afterwards would change
     * an already running process, which is impossible.
     *
     * @throws java.io.IOException if it could not be launched (no such executable, no permissions)
     * @throws NullPointerException if the command has a null element
     * @throws IndexOutOfBoundsException if the command is empty
     */
    public Process start() throws java.io.IOException {
        List<String> cmd = this.command();
        if (cmd == null || cmd.isEmpty()) {
            throw new IndexOutOfBoundsException("the command is empty");
        }
        String[] argv = new String[cmd.size()];
        for (int i = 0; i < cmd.size(); i++) {
            String a = cmd.get(i);
            if (a == null) {
                throw new NullPointerException("the command has a null element at " + i);
            }
            argv[i] = a;
        }

        // The environment is passed **only if somebody touched it**. The difference matters: an
        // empty array tells the native "inherit mine", and a full one tells it "use exactly this".
        // Always sending the materialised map would strip a child launched by an unconfigured
        // builder of the system variables it ought to inherit.
        String[] env = new String[0];
        if (this.environment != null) {
            Map<String, String> m = this.environment;
            env = new String[m.size() * 2];
            int k = 0;
            for (Map.Entry<String, String> e : m.entrySet()) {
                env[k] = e.getKey();
                env[k + 1] = e.getValue();
                k = k + 2;
            }
        }

        Redirect[] r = this.redirects();
        String[] paths = new String[3];
        int[] modes = new int[3];
        for (int i = 0; i < 3; i++) {
            modes[i] = ProcessBuilder.modeOf(r[i]);
            File f = r[i].file();
            paths[i] = f == null ? null : f.getPath();
        }

        String dir = this.directory == null ? null : this.directory.getPath();
        int h = jdk.internal.proc.Proc.spawn(argv, dir, env, paths, modes, this.redirectErrorStream);
        if (h < 0) {
            // The native returns -1 without distinguishing the reason, so the message names the one
            // thing known for certain: which command was attempted. Inventing "does not exist" or
            // "no permissions" would be guessing which of the two it was.
            throw new java.io.IOException("could not launch the process: " + argv[0]);
        }
        return new ChildProcess(h);
    }

    // The redirection mode the native understands: 0 pipe, 1 inherit, 2 discard, 3 file
    // overwriting, 4 file appending.
    //
    // `DISCARD` is compared by **identity** and not by type, and there lies this class's trap: its
    // `type()` is `WRITE` and its `file()` is the system's null, so by type it is indistinguishable
    // from a `Redirect.to(new File("NUL"))`. It is told apart by identity because it is a unique
    // constant, and that way the native uses its own discard instead of opening a file.
    private static int modeOf(Redirect r) {
        if (r == Redirect.DISCARD) {
            return 2;
        }
        if (r.type() == Redirect.Type.PIPE) {
            return 0;
        }
        if (r.type() == Redirect.Type.INHERIT) {
            return 1;
        }
        if (r.type() == Redirect.Type.APPEND) {
            return 4;
        }
        return 3;
    }

    /**
     * It launches several chained processes: each one's output is the next one's input.
     *
     * <p><strong>The chain is built with temporary files, not with direct pipes</strong>, and that
     * has to be said because it has an observable consequence: the processes **do not run at the
     * same time**. Each finishes before the next starts, instead of flowing in parallel.
     *
     * <p>The final result is the same --the bytes coming out of the last are the ones a pipe would
     * produce-- but there are two differences one can notice: a chain processing an infinite stream
     * never advances, and one moving a lot of volume uses disk instead of memory.
     *
     * <p>It is done this way because connecting one child's output to another's input calls for
     * duplicating descriptors between processes, and this VM's seam hands pipes to the **parent**
     * process, not a descriptor that can be passed to a third. The day `Proc` knows how to chain,
     * this gets rewritten and the observable contract improves without the signature changing.
     *
     * <p>What it **does** deliver: the returned list has one `Process` per builder in the same
     * order, and the last one's `getInputStream()` reads the chain's result. Whatever intermediate
     * redirections the caller set are ignored, as in the JDK.
     *
     * @throws NullPointerException if the list or one of its elements is null
     * @throws java.io.IOException if one of them could not be launched
     */
    public static List<Process> startPipeline(List<ProcessBuilder> builders)
            throws java.io.IOException {
        if (builders == null) {
            throw new NullPointerException("builders");
        }
        if (builders.isEmpty()) {
            // JDK 25 does **not** throw on an empty list: it returns an empty list. This was
            // measured against real `java` (`scratchpad/zz349/Vacia.java`) because older versions'
            // javadoc says `IllegalArgumentException` and the first version of this copied it from
            // there. What the implementation does wins, not what the documentation says it did.
            return new ArrayList<Process>();
        }
        for (ProcessBuilder b : builders) {
            if (b == null) {
                throw new NullPointerException("a builder in the chain is null");
            }
        }

        List<Process> out = new ArrayList<Process>();
        File previous = null;
        int n = builders.size();
        for (int i = 0; i < n; i++) {
            ProcessBuilder b = builders.get(i);
            // It works on a copy: the chain fixes the intermediate redirections, and doing that on
            // the caller's builder would change their object from under them.
            ProcessBuilder c = new ProcessBuilder(b.command());
            c.directory(b.directory());
            c.redirectErrorStream(b.redirectErrorStream());
            if (b.environment != null) {
                c.environment().putAll(b.environment);
            }
            // The first keeps its input; the rest read from the previous one's temporary.
            c.redirectInput(i == 0 ? b.redirectInput() : Redirect.from(previous));
            File own = null;
            if (i == n - 1) {
                c.redirectOutput(b.redirectOutput());
            } else {
                own = ProcessBuilder.chainTempFile(i);
                c.redirectOutput(Redirect.to(own));
            }
            c.redirectError(b.redirectError());
            Process p = c.start();
            // It waits here: the next needs the complete file, and there is no pipe allowing them
            // to overlap. It is the direct consequence of what is said above.
            if (i < n - 1) {
                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("the chain was interrupted", e);
                }
            }
            out.add(p);
            previous = own;
        }
        return out;
    }

    // A file name for one link of the chain. It carries the index and a counter so two simultaneous
    // chains do not collide: without that, two threads building chains at once would use the same
    // name and one would read the other's output.
    private static int chainCounter = 0;

    private static synchronized File chainTempFile(int index) {
        ProcessBuilder.chainCounter = ProcessBuilder.chainCounter + 1;
        String base = System.getProperty("java.io.tmpdir");
        String name = "kaji-pipe-" + ProcessBuilder.chainCounter + "-" + index + ".tmp";
        return base == null ? new File(name) : new File(base, name);
    }
}
