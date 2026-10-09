package jdk.internal.vm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * KajiLibrary's jdk.internal.vm.ThreadDumper -- it dumps the live threads, as text or as JSON.
 *
 * <p>It is what is behind `jcmd Thread.dump_to_file`. The two forms are not the same thing written
 * differently: the text one is meant to be read by a person, and the JSON one by a tool, with the
 * tree of containers explicit.
 *
 * <p>The threads are enumerated by walking the root `ThreadGroup`, which is what there is: this VM
 * keeps no central registry of live threads.
 *
 * <p><strong>And today that walk finds nothing.</strong> On this VM `ThreadGroup` keeps no record
 * of its members: `activeCount()` returns 0 and `enumerate()` writes nothing, even with threads
 * running. (The note cited a repro at `scratchpad/zz350/A5.java`; that file is not in the tree.) So
 * the dump brings **only the thread that asks for it**, which is added separately and by hand
 * --just as in {@link ThreadContainers#root()}, and for the same reason: a dump that says there is
 * no live thread while somebody is asking for it is not an incomplete dump, it is a wrong one--.
 * The day `ThreadGroup` keeps a record, this starts bringing all of them without touching anything.
 *
 * <p><strong>The stacks go empty.</strong> {@link Thread#getStackTrace} on **another** thread needs
 * the VM to stop it at a safepoint and read its frames, and this VM does not expose that. Each
 * thread appears with its name, its state and its identity; the array of frames is left empty
 * instead of bringing the stack of the wrong thread, which is the error that would make the whole
 * dump useless.
 */
public class ThreadDumper {

    private ThreadDumper() {
    }

    /**
     * It dumps as text to a file.
     *
     * @param outputFile where to write
     * @param okToOverwrite whether a file that is already there may be overwritten
     * @return the reply message, which is what `jcmd` shows the user
     */
    public static byte[] dumpThreads(String outputFile, boolean okToOverwrite) {
        return ThreadDumper.toFile(outputFile, okToOverwrite, ThreadDumper.threadsAsText());
    }

    /** It dumps as JSON to a file. */
    public static byte[] dumpThreadsToJson(String outputFile, boolean okToOverwrite) {
        return ThreadDumper.toFile(outputFile, okToOverwrite, ThreadDumper.threadsAsJson());
    }

    /** It dumps as text to a stream. */
    public static void dumpThreads(OutputStream out) throws IOException {
        out.write(ThreadDumper.threadsAsText().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    /** It dumps as JSON to a stream. */
    public static void dumpThreadsToJson(OutputStream out) throws IOException {
        out.write(ThreadDumper.threadsAsJson().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    // The error is returned as a message and not as an exception because the caller is `jcmd` from
    // outside the process: the only thing it can do with a failure is show it to the user.
    private static byte[] toFile(String outputFile, boolean okToOverwrite, String content) {
        if (outputFile == null) {
            return "no output file given".getBytes(StandardCharsets.UTF_8);
        }
        Path p = Path.of(outputFile);
        if (!okToOverwrite && Files.exists(p)) {
            return ("file already exists: " + outputFile).getBytes(StandardCharsets.UTF_8);
        }
        try {
            Files.write(p, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ("could not write " + outputFile + ": " + e).getBytes(StandardCharsets.UTF_8);
        }
        return ("dump written to " + outputFile).getBytes(StandardCharsets.UTF_8);
    }

    // At least the asking thread is guaranteed, just like
    // `ThreadContainers.RootContainer.threads()` and for the same reason: on this VM `ThreadGroup`
    // keeps no record of its members --`activeCount()` gives 0 and `enumerate()` returns nothing
    // even with threads running--, so without this guarantee the walk gave an **empty** list and
    // the dump came out without a single thread while somebody was asking for it. A dump that says
    // there are no live threads is not a partial dump, it is one that lies.
    private static List<Thread> liveThreads() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g != null && g.getParent() != null) {
            g = g.getParent();
        }
        List<Thread> out = new ArrayList<Thread>();
        if (g != null) {
            // With slack: threads can appear between counting and enumerating, and an exact array
            // would lose them.
            Thread[] buf = new Thread[g.activeCount() + 16];
            int n = g.enumerate(buf, true);
            for (int i = 0; i < n; i++) {
                if (buf[i] != null) {
                    out.add(buf[i]);
                }
            }
        }
        Thread self = Thread.currentThread();
        if (!out.contains(self)) {
            out.add(self);
        }
        return out;
    }

    private static String threadsAsText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread dump - ").append(System.currentTimeMillis()).append('\n');
        for (Thread t : ThreadDumper.liveThreads()) {
            sb.append('\n').append('"').append(t.getName()).append('"')
              .append(" #").append(t.threadId())
              .append(t.isDaemon() ? " daemon" : "")
              .append(" prio=").append(t.getPriority())
              .append('\n')
              .append("   java.lang.Thread.State: ").append(t.getState()).append('\n');
        }
        return sb.toString();
    }

    // JSON written by hand and not with a library: the dump has to be able to come out when the
    // process is already in trouble, and that is no time for loading new classes or allocating more
    // than needed.
    private static String threadsAsJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"threadDump\": {\n");
        sb.append("    \"time\": \"").append(System.currentTimeMillis()).append("\",\n");
        sb.append("    \"runtimeVersion\": \"")
          .append(ThreadDumper.escape(System.getProperty("java.version", "unknown")))
          .append("\",\n");
        sb.append("    \"threadContainers\": [\n      {\n");
        sb.append("        \"container\": \"<root>\",\n");
        sb.append("        \"parent\": null,\n");
        sb.append("        \"threads\": [\n");
        List<Thread> threads = ThreadDumper.liveThreads();
        for (int i = 0; i < threads.size(); i++) {
            Thread t = threads.get(i);
            sb.append("          {\n");
            sb.append("            \"tid\": \"").append(t.threadId()).append("\",\n");
            sb.append("            \"name\": \"").append(ThreadDumper.escape(t.getName())).append("\",\n");
            sb.append("            \"state\": \"").append(t.getState()).append("\",\n");
            sb.append("            \"stack\": []\n");
            sb.append("          }").append(i + 1 < threads.size() ? "," : "").append('\n');
        }
        sb.append("        ]\n      }\n    ]\n  }\n}\n");
        return sb.toString();
    }

    // A thread name is chosen by the program: it may bring quotes or backslashes and break the
    // JSON.
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c < ' ') {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
