import java.util.regex.Pattern;

/**
 * Behaviour test of the inline flag groups {@code (?idmsux)} and {@code (?idmsux-idmsux:X)},
 * written to run **the same** in this VM and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed, which is enough to compare the two VMs without depending on the console
 * output matching character by character.
 *
 * <p>It exists because differential fuzzing against OpenJDK 25 found that the parser rejected
 * every inline form outright: {@code (?i)hola}, {@code (?s).+}, {@code (?m)^b$} and
 * {@code (?x) a b} all raised {@code PatternSyntaxException} where the JDK compiles them. The
 * flags themselves were honored — the engine has always read them when building nodes — but
 * only when they arrived through the {@code compile(String, int)} argument.
 *
 * <p>Most of what is pinned here is the **reach** of a change, which is the part that is easy to
 * get wrong and was measured against the JDK rather than assumed: {@code (?i)} applies from
 * where it appears to the end of the group that encloses it, so {@code (a(?i)b)c} matches
 * {@code "aBc"} and not {@code "aBC"}. {@code (?i:X)} applies to X alone. {@code flags()}
 * reports the word as it stands when the top level finishes, which is why {@code (?i)a} reports
 * CASE_INSENSITIVE and {@code ((?i)a)b} reports nothing.
 *
 * <p>{@code (?u)} and {@code (?U)} are deliberately absent. This engine refuses to turn
 * UNICODE_CASE or UNICODE_CHARACTER_CLASS on and the JDK honors them, so an assertion either
 * way would fail on one of the two sides by design; see the class comment on {@code Pattern}.
 * Turning them *off* is a no-op here and is accepted, so {@code (?i-u)} is checked.
 */
public class InlineFlagsTest {

  static boolean finds(String regex, String subject) {
    return Pattern.compile(regex).matcher(subject).find();
  }

  static boolean compiles(String regex) {
    try {
      Pattern.compile(regex);
      return true;
    } catch (Throwable t) {
      return false;
    }
  }

  static boolean rejects(String regex, Class<?> want) {
    try {
      Pattern.compile(regex);
      return false;
    } catch (Throwable t) {
      return t.getClass() == want;
    }
  }

  public static int run() {
    String nl = String.valueOf((char) 10);

    // --- each honored letter does what the corresponding flag does ---
    if (!finds("(?i)a", "A")) return 0;
    if (!finds("(?i)a", "a")) return 1;
    if (!finds("(?s).", nl)) return 2;
    if (finds(".", nl)) return 3;
    if (!finds("(?m)^b$", "a" + nl + "b")) return 4;
    if (finds("^b$", "a" + nl + "b")) return 5;
    if (!finds("(?x) a b", "ab")) return 6;
    if (finds("(?x) a b", "a b")) return 7;
    if (!finds("(?x)a #comment", "a")) return 8;
    if (!compiles("(?d)a")) return 9;
    if (!finds("(?is)a.", "A" + nl)) return 10;

    // --- the reach of a change: to the end of the enclosing group, and no further ---
    if (!finds("a(?i)b", "aB")) return 11;
    if (finds("a(?i)b", "Ab")) return 12;
    if (!finds("(a(?i)b)c", "aBc")) return 13;
    if (finds("(a(?i)b)c", "aBC")) return 14;
    if (!finds("(?:(?i)a)b", "Ab")) return 15;
    if (finds("(?:(?i)a)b", "AB")) return 16;
    if (!finds("(?i:a)b", "Ab")) return 17;
    if (finds("(?i:a)b", "AB")) return 18;

    // --- the off switch ---
    if (finds("(?i-i)a", "A")) return 19;
    if (finds("(?i)(?-i)a", "A")) return 20;
    if (!finds("(?i)(?-i)a", "a")) return 21;
    if (!compiles("(?i-u)a")) return 22;

    // --- empty but legal forms ---
    if (!compiles("(?)")) return 23;
    if (!compiles("(?-)")) return 24;
    if (!compiles("(?i-)")) return 25;
    if (!compiles("(?i:)b")) return 26;
    if (!compiles("(?ii)a")) return 27;

    // --- malformed forms. Only the class is asserted: the JDK's index in the message and
    // --- ours differ by one, and the text is not part of the contract.
    if (!rejects("(?z)", java.util.regex.PatternSyntaxException.class)) return 28;
    if (!rejects("(?i", java.util.regex.PatternSyntaxException.class)) return 29;

    // --- a flag group is not a capturing group, and does not disturb the numbering ---
    if (Pattern.compile("(?i)(a)(b)").matcher("ab").groupCount() != 2) return 30;
    if (Pattern.compile("(?i:a)(b)").matcher("ab").groupCount() != 1) return 31;

    // --- flags() reports the word as it stands when the top level finishes ---
    if (Pattern.compile("(?i)a").flags() != Pattern.CASE_INSENSITIVE) return 32;
    if (Pattern.compile("a(?i)").flags() != Pattern.CASE_INSENSITIVE) return 33;
    if (Pattern.compile("((?i)a)b").flags() != 0) return 34;
    if (Pattern.compile("(?i:a)").flags() != 0) return 35;
    if (Pattern.compile("(?i)(?-i)a").flags() != 0) return 36;
    if (Pattern.compile("(?i)(a(?s)b)c").flags() != Pattern.CASE_INSENSITIVE) return 37;
    if (Pattern.compile("(?=(?i)a)b").flags() != 0) return 38;
    if (Pattern.compile("(?i)a", Pattern.MULTILINE).flags()
            != (Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)) return 39;

    // --- the flag reaches the constructs that read it when a node is built ---
    if (!Pattern.compile("(?i)a+").matcher("AAA").matches()) return 40;
    if (!Pattern.compile("(?i)[a-c]").matcher("B").matches()) return 41;
    if (!Pattern.compile("(?i)(a)\\1").matcher("aA").matches()) return 42;
    if (!finds("(?i)a|b", "A")) return 43;
    if (!finds("(?i)a|b", "B")) return 44;
    if (Pattern.compile("((?i)a)b").matcher("AB").matches()) return 45;

    // --- pattern() still reports what was handed in ---
    if (!Pattern.compile("(?i)a").pattern().equals("(?i)a")) return 46;

    return -1;
  }

  public static void main(String[] args) {
    int bad = run();
    System.out.println(bad < 0 ? "ok" : ("FAILED at check " + bad));
  }
}
