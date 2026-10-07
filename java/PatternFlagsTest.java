import java.util.regex.Pattern;

/**
 * Behaviour test of the flag argument of {@code Pattern.compile(String, int)}, written to run
 * **the same** in this VM and in the real JDK.
 *
 * <p>Each check has an index. {@code run()} returns -1 if they all passed, or the index of the
 * first one that failed, which is enough to compare the two VMs without depending on the console
 * output matching character by character.
 *
 * <p>It exists because differential fuzzing against OpenJDK 25 found that the flag word was not
 * validated at all: only the three flags this engine cannot deliver were rejected, and every
 * other bit was accepted and stored. {@code compile("a", 512)} returned a pattern whose
 * {@code flags()} read back 512, and {@code compile("a", Integer.MIN_VALUE)} one that read back
 * the negative word, where the JDK rejects both.
 *
 * <p>The order of the checks inside the constructor is observable, so it is pinned here too: an
 * invalid bit is rejected **before** a null pattern is noticed and **before** the pattern is
 * parsed. That is why {@code compile(null, 512)} has to be an {@code IllegalArgumentException}
 * and not a {@code NullPointerException}, and {@code compile("[", 512)} not a syntax error.
 *
 * <p>UNICODE_CASE, CANON_EQ and UNICODE_CHARACTER_CLASS are deliberately absent from these
 * checks. This engine refuses them and the JDK honors them, so any assertion about them would
 * fail on one of the two sides by design; see the class comment on {@code Pattern}.
 */
public class PatternFlagsTest {

  /** The flags this engine honors, so every combination below stays comparable. */
  static final int HONORED = Pattern.UNIX_LINES | Pattern.CASE_INSENSITIVE | Pattern.COMMENTS
          | Pattern.MULTILINE | Pattern.LITERAL | Pattern.DOTALL;

  static boolean accepts(String p, int f, int expectedFlags) {
    try {
      return Pattern.compile(p, f).flags() == expectedFlags;
    } catch (Throwable t) {
      return false;
    }
  }

  static boolean rejects(String p, int f, Class<?> want) {
    try {
      Pattern.compile(p, f);
      return false;
    } catch (Throwable t) {
      return t.getClass() == want;
    }
  }

  public static int run() {
    // --- every honored flag on its own is accepted, and flags() echoes it back ---
    if (!accepts("a", 0, 0)) return 0;
    if (!accepts("a", Pattern.UNIX_LINES, Pattern.UNIX_LINES)) return 1;
    if (!accepts("a", Pattern.CASE_INSENSITIVE, Pattern.CASE_INSENSITIVE)) return 2;
    if (!accepts("a", Pattern.COMMENTS, Pattern.COMMENTS)) return 3;
    if (!accepts("a", Pattern.MULTILINE, Pattern.MULTILINE)) return 4;
    if (!accepts("a", Pattern.LITERAL, Pattern.LITERAL)) return 5;
    if (!accepts("a", Pattern.DOTALL, Pattern.DOTALL)) return 6;
    if (!accepts("a", HONORED, HONORED)) return 7;

    // --- a bit outside the declared set is rejected ---
    if (!rejects("a", 0x200, IllegalArgumentException.class)) return 8;
    if (!rejects("a", 0x400, IllegalArgumentException.class)) return 9;
    if (!rejects("a", 1 << 30, IllegalArgumentException.class)) return 10;
    if (!rejects("a", -1, IllegalArgumentException.class)) return 11;
    if (!rejects("a", -5, IllegalArgumentException.class)) return 12;
    if (!rejects("a", Integer.MIN_VALUE, IllegalArgumentException.class)) return 13;
    // 543 is 0x200 set alongside five honored flags: one bad bit is enough.
    if (!rejects("a", 543, IllegalArgumentException.class)) return 14;

    // --- the invalid bit is seen before the null pattern and before the parser ---
    if (!rejects(null, 0x200, IllegalArgumentException.class)) return 15;
    if (!rejects("[", 0x200, IllegalArgumentException.class)) return 16;
    if (!rejects(null, 0, NullPointerException.class)) return 17;

    // --- an honored flag still changes what matches, so the check did not break the engine ---
    if (!Pattern.compile("hola", Pattern.CASE_INSENSITIVE).matcher("HOLA").matches()) return 18;
    if (Pattern.compile("hola", 0).matcher("HOLA").matches()) return 19;
    if (!Pattern.compile("a+b", Pattern.LITERAL).matcher("a+b").matches()) return 20;

    return -1;
  }

  public static void main(String[] args) {
    int bad = run();
    System.out.println(bad < 0 ? "ok" : ("FAILED at check " + bad));
  }
}
