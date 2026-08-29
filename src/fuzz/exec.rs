//! The **executor** (level 2.1): compile a generated program once, then run it down a [`Path`] and
//! come back with an [`Observation`].
//!
//! It is built first among the refinements because it decides *what can be observed at all*, and
//! both of the other pieces are downstream of that: the oracle can only compare what this produces,
//! and the generator must not let a program depend on anything this cannot see.
//!
//! # Out of process, on purpose
//!
//! Every run is a child process, even though calling the interpreter in-process would be faster.
//! The reason is that for a fuzzer a **crash and a hang are findings, not tool failures**:
//!
//! - in-process, a panic in the VM takes the fuzzer down with it, losing the campaign and the seed;
//! - in-process, a generated infinite loop freezes the fuzzer forever, since a Rust thread cannot
//!   be killed from outside;
//! - `JVM_JIT` and `JVM_THREADS` are read from the environment, and a process that sets them for
//!   itself sets them for every thread in it — which is exactly why the JIT's own differential
//!   tests switch the JIT programmatically instead.
//!
//! A child process makes all three go away: a panic is an exit status, a hang is a `kill`, and the
//! environment is per-child.
//!
//! # What each side prints, and the asymmetry between them
//!
//! Measured, not assumed:
//!
//! | path | invocation | on success | on an uncaught exception |
//! |---|---|---|---|
//! | this VM | `run-headless Foo.class run` | `…Foo.class run()I -> Some(Int(42))` | `… -> None`, exit 0 |
//! | real JDK | `java -cp dir Foo` | `42` (printed by the program's own `main`) | `Exception in thread "main" java.lang.ArithmeticException: …` on stderr |
//!
//! The asymmetry is real and worth stating: **this VM does not surface the exception's class**
//! through `run-headless`. The uncaught-exception report the VM builds goes to its console buffer,
//! which that binary never flushes, so all we learn is "it did not return".
//!
//! The fix belongs to the **generator**, not here: a generated program must be *total* — it catches
//! everything itself and encodes the outcome in the `int` it returns, e.g.
//!
//! ```text
//! static int run() {
//!     try { return body(); }
//!     catch (ArithmeticException e) { return MARKER_ARITHMETIC; }
//!     catch (Throwable t)           { return MARKER_OTHER; }
//! }
//! ```
//!
//! Then both sides return an integer, the comparison is exact, and the oracle never has to reason
//! about exception *messages* — which differ between implementations for perfectly legal reasons.
//! [`Outcome::Threw`] stays in the vocabulary for the programs that escape anyway; it just cannot
//! carry a class name from this VM.

use std::io::Read;
use std::path::PathBuf;
use std::process::{Child, Command, ExitStatus, Stdio};
use std::time::{Duration, Instant};

use super::{Observation, Outcome, Path, Program, Runner};

/// Where the external programs live. Held explicitly rather than looked up per run so a campaign
/// fails immediately and loudly if a tool is missing, instead of reporting thousands of "crashes".
#[derive(Clone, Debug)]
pub struct Toolchain {
    /// The reference `javac`, used to turn generated source into class files. The reference
    /// compiler — not ours — because when the *VM* is under test the input must be known-good; a
    /// campaign against our own `javac` is a different pairing, and swaps this field.
    pub javac: PathBuf,
    /// The reference `java`, for [`Path::ReferenceJdk`].
    pub java: PathBuf,
    /// Our `run-headless` binary, for every other path.
    pub headless: PathBuf,
}

impl Toolchain {
    /// Best-effort discovery of the reference JDK.
    ///
    /// **Never resolves `javac` through the `PATH`**, and that is not paranoia: this repo *builds a
    /// binary called `javac`*, and `cargo test` puts the build directory on the child's `PATH`. A
    /// campaign that trusted the `PATH` would silently compile its programs with the compiler under
    /// test — the exact tool whose bugs it is supposed to be checking against a reference.
    ///
    /// So: `JAVA_HOME` if it is set, otherwise the directory of the first `java` on the `PATH`
    /// (a name this repo does *not* build) with any of our own build directories skipped, and
    /// `javac` taken from beside it.
    pub fn detect() -> Toolchain {
        let bin = std::env::var_os("JAVA_HOME")
            .map(|home| PathBuf::from(home).join("bin"))
            .or_else(|| on_path("java").and_then(|java| java.parent().map(PathBuf::from)))
            .unwrap_or_else(|| PathBuf::from("."));
        Toolchain {
            javac: bin.join("javac"),
            java: bin.join("java"),
            headless: PathBuf::from("target/release/run-headless"),
        }
    }
}

/// The first `name` on the `PATH`, skipping anything inside a Cargo `target` directory — see
/// [`Toolchain::detect`] for why that exclusion is load-bearing.
fn on_path(name: &str) -> Option<PathBuf> {
    let path = std::env::var_os("PATH")?;
    for dir in std::env::split_paths(&path) {
        if dir.components().any(|c| c.as_os_str() == "target") {
            continue;
        }
        for candidate in [dir.join(format!("{name}.exe")), dir.join(name)] {
            if candidate.is_file() {
                return Some(candidate);
            }
        }
    }
    None
}

/// Runs programs as child processes.
///
/// Compilation is cached on the **source text**: the loop asks for the same program down two paths
/// back to back, and the reducer asks for a great many nearly-identical ones, so recompiling per
/// path would double the cost of every campaign for nothing.
pub struct ProcessRunner {
    tools: Toolchain,
    workdir: PathBuf,
    budget: Duration,
    /// The source of the program currently sitting compiled in `workdir`, if any.
    compiled: Option<String>,
}

impl ProcessRunner {
    pub fn new(tools: Toolchain, workdir: impl Into<PathBuf>, budget: Duration) -> ProcessRunner {
        ProcessRunner { tools, workdir: workdir.into(), budget, compiled: None }
    }

    /// Writes and compiles `program` unless the identical source is already built. Returns the
    /// compiler's diagnostics on failure.
    fn ensure_compiled<P: Program>(&mut self, program: &P) -> Result<(), String> {
        let source = program.to_java();
        if self.compiled.as_deref() == Some(source.as_str()) {
            return Ok(());
        }
        self.compiled = None; // whatever is on disk is about to stop matching

        std::fs::create_dir_all(&self.workdir).map_err(|e| format!("workdir: {e}"))?;
        let file = self.workdir.join(format!("{}.java", program.class_name()));
        std::fs::write(&file, source.as_bytes()).map_err(|e| format!("write: {e}"))?;

        let out = Command::new(&self.tools.javac)
            .arg("-d")
            .arg(&self.workdir)
            .arg(&file)
            .output()
            .map_err(|e| format!("spawn javac: {e}"))?;
        if !out.status.success() {
            return Err(String::from_utf8_lossy(&out.stderr).trim().to_string());
        }
        self.compiled = Some(source);
        Ok(())
    }

    /// The command that runs an already-compiled program down `path`.
    fn command<P: Program>(&self, program: &P, path: Path) -> Command {
        let class_file = self.workdir.join(format!("{}.class", program.class_name()));
        match path {
            Path::ReferenceJdk => {
                let mut c = Command::new(&self.tools.java);
                c.arg("-cp").arg(&self.workdir).arg(program.class_name());
                c
            }
            _ => {
                let mut c = Command::new(&self.tools.headless);
                c.arg(&class_file).arg("run");
                // The engine is chosen entirely by the child's environment, which is the other half
                // of why this is a child at all.
                match path {
                    Path::Interpreter => {
                        c.env("JVM_JIT", "0").env("JVM_THREADS", "green");
                    }
                    Path::Jit => {
                        c.env_remove("JVM_JIT").env("JVM_THREADS", "green");
                    }
                    Path::OsGil => {
                        c.env_remove("JVM_JIT").env("JVM_THREADS", "os-gil");
                    }
                    Path::OsParallel => {
                        c.env_remove("JVM_JIT").env("JVM_THREADS", "os");
                    }
                    Path::ReferenceJdk => unreachable!("handled above"),
                }
                c
            }
        }
    }
}

impl<P: Program> Runner<P> for ProcessRunner {
    fn run(&mut self, program: &P, path: Path) -> Observation {
        if let Err(diagnostics) = self.ensure_compiled(program) {
            return Observation { outcome: Outcome::CompileError(diagnostics), stdout: String::new() };
        }
        let mut command = self.command(program, path);
        command.stdout(Stdio::piped()).stderr(Stdio::piped()).stdin(Stdio::null());

        let child = match command.spawn() {
            Ok(child) => child,
            Err(e) => {
                let why = format!("spawn {:?}: {e}", command.get_program());
                return Observation { outcome: Outcome::Crashed(why), stdout: String::new() };
            }
        };
        match wait_with_budget(child, self.budget) {
            Completed::Timeout => Observation { outcome: Outcome::Timeout, stdout: String::new() },
            Completed::Exited { status, stdout, stderr } => {
                interpret(path, status, &stdout, &stderr)
            }
        }
    }
}

/// How a child ended, with everything it printed.
enum Completed {
    Exited { status: ExitStatus, stdout: String, stderr: String },
    Timeout,
}

/// Waits for `child`, killing it once `budget` elapses.
///
/// The two pipes are drained by threads rather than read after the wait: a child that prints more
/// than the pipe buffer holds blocks *writing* while we block *waiting*, and the two of us sit
/// there until the budget expires — a deadlock that would look exactly like a hung VM, which is the
/// one thing this tool must never be wrong about.
fn wait_with_budget(mut child: Child, budget: Duration) -> Completed {
    let mut out_pipe = child.stdout.take();
    let mut err_pipe = child.stderr.take();
    let drain_out = std::thread::spawn(move || drain(&mut out_pipe));
    let drain_err = std::thread::spawn(move || drain(&mut err_pipe));

    let deadline = Instant::now() + budget;
    let status = loop {
        match child.try_wait() {
            Ok(Some(status)) => break Some(status),
            Ok(None) if Instant::now() >= deadline => {
                let _ = child.kill();
                let _ = child.wait(); // reap it, so the pipes close and the drains finish
                break None;
            }
            Ok(None) => std::thread::sleep(Duration::from_millis(2)),
            Err(_) => break None,
        }
    };
    let stdout = drain_out.join().unwrap_or_default();
    let stderr = drain_err.join().unwrap_or_default();
    match status {
        Some(status) => Completed::Exited { status, stdout, stderr },
        None => Completed::Timeout,
    }
}

fn drain(pipe: &mut Option<impl Read>) -> String {
    let mut text = String::new();
    if let Some(pipe) = pipe.as_mut() {
        let _ = pipe.read_to_string(&mut text);
    }
    text
}

/// Turns what a child printed into an [`Observation`]. Split out from the process handling so the
/// parsing — the part with the interesting edge cases — is testable without spawning anything.
pub(crate) fn interpret(path: Path, status: ExitStatus, stdout: &str, stderr: &str) -> Observation {
    // A VM that panicked is always a finding, whichever side it happened on, and it is worth
    // separating from an ordinary non-zero exit (which `java` uses for an uncaught exception).
    if stderr.contains("panicked at") || stderr.contains("RUST_BACKTRACE") {
        let line = stderr.lines().find(|l| l.contains("panicked at")).unwrap_or("panic");
        return Observation { outcome: Outcome::Crashed(line.trim().to_string()), stdout: stdout.to_string() };
    }
    let outcome = match path {
        Path::ReferenceJdk => {
            if let Some(class) = uncaught_class(stderr) {
                Outcome::Threw(class)
            } else if let Some(value) = stdout.lines().rev().find_map(|l| l.trim().parse::<i32>().ok()) {
                Outcome::Returned(value)
            } else if status.success() {
                Outcome::Crashed("the reference JDK printed nothing parseable".to_string())
            } else {
                Outcome::Crashed(format!("java exited with {status}"))
            }
        }
        // `run-headless` prints `<path> run()I -> Some(Int(42))`, or `-> None` when the entry
        // thread died without returning. `None` is all this VM can tell us — see the module docs.
        _ => match parse_headless(stdout) {
            Some(value) => Outcome::Returned(value),
            None if stdout.contains("-> None") => Outcome::Threw(String::new()),
            None if !status.success() => Outcome::Crashed(format!("run-headless exited with {status}")),
            None => Outcome::Crashed("run-headless printed nothing parseable".to_string()),
        },
    };
    Observation { outcome, stdout: stdout.to_string() }
}

/// Pulls the integer out of `run-headless`'s one-line report.
fn parse_headless(stdout: &str) -> Option<i32> {
    let marker = stdout.rfind("-> Some(Int(")?;
    let rest = &stdout[marker + "-> Some(Int(".len()..];
    let end = rest.find(')')?;
    rest[..end].parse().ok()
}

/// The binary class name in `Exception in thread "main" java.lang.Foo: message`, if that is what
/// the reference JDK printed. The message is deliberately dropped: two implementations may word it
/// differently and still both be right, so comparing messages manufactures divergences.
fn uncaught_class(stderr: &str) -> Option<String> {
    let line = stderr.lines().find(|l| l.starts_with("Exception in thread"))?;
    let after_quote = line.rfind('"')?;
    let tail = line[after_quote + 1..].trim();
    let class = tail.split(':').next()?.trim();
    (!class.is_empty()).then(|| class.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::fuzz::{Seed, Verdict};

    /// A hand-written program, so the executor can be tested before a generator exists.
    #[derive(Clone)]
    struct Fixed {
        name: &'static str,
        body: &'static str,
    }

    impl Program for Fixed {
        fn to_java(&self) -> String {
            format!(
                "public class {name} {{\n    static int run() {{ {body} }}\n\
                 \x20   public static void main(String[] a) {{ System.out.println(run()); }}\n}}\n",
                name = self.name,
                body = self.body
            )
        }
        fn class_name(&self) -> &str {
            self.name
        }
    }

    /// Builds an `ExitStatus` without spawning anything real. There is no public constructor, so
    /// the cheapest honest way is to run a process that is guaranteed to exist and succeed.
    fn ok_status() -> ExitStatus {
        Command::new(if cfg!(windows) { "cmd" } else { "true" })
            .args(if cfg!(windows) { vec!["/C", "exit 0"] } else { vec![] })
            .status()
            .expect("a trivially successful process")
    }

    #[test]
    fn a_headless_report_yields_the_returned_value() {
        let line = "C:/tmp/Fz0.class run()I -> Some(Int(42))\n";
        let seen = interpret(Path::Jit, ok_status(), line, "");
        assert_eq!(seen.outcome, Outcome::Returned(42));
    }

    #[test]
    fn a_negative_headless_value_survives_the_parse() {
        let line = "Fz.class run()I -> Some(Int(-2147483648))\n";
        let seen = interpret(Path::Interpreter, ok_status(), line, "");
        assert_eq!(seen.outcome, Outcome::Returned(-2147483648));
    }

    #[test]
    fn a_headless_none_is_read_as_a_throw_without_a_class_name() {
        // This VM cannot tell us *which* exception — the whole reason generated programs must
        // catch their own and return a marker instead.
        let seen = interpret(Path::Jit, ok_status(), "Fz.class run()I -> None\n", "");
        assert_eq!(seen.outcome, Outcome::Threw(String::new()));
    }

    #[test]
    fn the_reference_jdk_reports_the_exception_class_without_its_message() {
        let stderr = "Exception in thread \"main\" java.lang.ArithmeticException: / by zero\n\tat Fz1.run(Fz1.java:2)\n";
        let seen = interpret(Path::ReferenceJdk, ok_status(), "", stderr);
        assert_eq!(
            seen.outcome,
            Outcome::Threw("java.lang.ArithmeticException".to_string()),
            "the message must be dropped: two implementations may word it differently and both be right"
        );
    }

    #[test]
    fn a_rust_panic_on_either_side_is_a_crash_not_a_throw() {
        let stderr = "thread 'main' panicked at src/jvm/x.rs:12:9:\nassertion failed\n";
        let seen = interpret(Path::Jit, ok_status(), "", stderr);
        match seen.outcome {
            Outcome::Crashed(why) => assert!(why.contains("panicked at"), "got {why}"),
            other => panic!("a panic must be a crash, got {other:?}"),
        }
    }

    #[test]
    fn the_reference_jdk_reads_the_last_printed_integer() {
        // A program may print other things first; the value its `main` prints is the last line.
        let seen = interpret(Path::ReferenceJdk, ok_status(), "noise\n7\n", "");
        assert_eq!(seen.outcome, Outcome::Returned(7));
    }

    /// End to end, against the real toolchain. Ignored because it needs `javac` on the machine and
    /// a built `target/release/run-headless`, neither of which `cargo test --lib` provides.
    ///
    /// `cargo build --release && cargo test --release --lib the_executor -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn the_executor_agrees_with_the_reference_jdk_on_a_real_program() {
        let workdir = std::env::temp_dir().join("kaji-fuzz-exec");
        let mut runner =
            ProcessRunner::new(Toolchain::detect(), &workdir, Duration::from_secs(20));
        let program = Fixed { name: "FzExec", body: "int a = 7; return a * 6;" };

        let ours = runner.run(&program, Path::Jit);
        let theirs = runner.run(&program, Path::ReferenceJdk);
        assert_eq!(ours.outcome, Outcome::Returned(42), "our VM: {ours:?}");
        assert_eq!(theirs.outcome, Outcome::Returned(42), "reference: {theirs:?}");
    }

    /// The property the whole tool rests on: a program that never ends is reported as a timeout
    /// rather than hanging the fuzzer. Ignored for the same reason as above.
    #[test]
    #[ignore]
    fn a_program_that_never_terminates_times_out_instead_of_hanging_the_fuzzer() {
        let workdir = std::env::temp_dir().join("kaji-fuzz-exec");
        let mut runner = ProcessRunner::new(Toolchain::detect(), &workdir, Duration::from_secs(3));
        // `while (x == 0)` on a field javac cannot fold — a loop it must actually emit.
        let program = Fixed {
            name: "FzHang",
            body: "int x = 0; while (x == 0) { x = 0; } return x;",
        };
        let started = Instant::now();
        let seen = runner.run(&program, Path::Jit);
        assert_eq!(seen.outcome, Outcome::Timeout);
        assert!(started.elapsed() < Duration::from_secs(30), "the kill must actually happen");
    }

    /// Source that does not compile is reported as such, not as a crash — it is a bug in the
    /// *generator*, and a campaign that cannot tell the two apart chases its own tail.
    #[test]
    #[ignore]
    fn source_that_does_not_compile_says_so() {
        let workdir = std::env::temp_dir().join("kaji-fuzz-exec");
        let mut runner = ProcessRunner::new(Toolchain::detect(), &workdir, Duration::from_secs(20));
        let program = Fixed { name: "FzBad", body: "return \"not an int\";" };
        match runner.run(&program, Path::Jit).outcome {
            Outcome::CompileError(diagnostics) => {
                assert!(!diagnostics.is_empty(), "the compiler's complaint must be kept")
            }
            other => panic!("expected a compile error, got {other:?}"),
        }
    }

    /// The executor is the half of the loop that was missing: with it, [`crate::fuzz::campaign`]
    /// runs real programs. Ignored — it compiles and spawns.
    #[test]
    #[ignore]
    fn the_loop_runs_real_programs_through_the_executor() {
        struct One;
        impl crate::fuzz::Generator for One {
            type Program = Fixed;
            fn generate(&mut self, _seed: Seed) -> Fixed {
                Fixed { name: "FzLoop", body: "return 1 + 2;" }
            }
        }
        struct Exact;
        impl crate::fuzz::Oracle for Exact {
            fn verdict(&self, a: &Observation, b: &Observation) -> Verdict {
                if a.outcome == b.outcome {
                    Verdict::Agree
                } else {
                    Verdict::Differ(format!("{:?} vs {:?}", a.outcome, b.outcome))
                }
            }
        }
        let workdir = std::env::temp_dir().join("kaji-fuzz-exec");
        let report = crate::fuzz::campaign(
            &mut One,
            &mut ProcessRunner::new(Toolchain::detect(), &workdir, Duration::from_secs(20)),
            &Exact,
            &mut crate::fuzz::NoReduction,
            (Path::Interpreter, Path::ReferenceJdk),
            vec![Seed(1)],
            1,
        );
        assert_eq!(report.seeds_run, 1);
        assert!(
            report.divergences.is_empty(),
            "1 + 2 had better be 3 on both sides: {:?}",
            report.divergences
        );
    }
}

