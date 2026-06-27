//! `jvm-step`: a teaching front-end for the interpreter.
//!
//! First screen shows the class's disassembly (by shelling out to the frozen
//! `javap-clon.exe`). Pressing Enter clears it and starts a step-by-step view:
//! at each step it redraws a window of the bytecode (a few instructions before
//! the current one and several after, the current one highlighted), the operand
//! stack, and the symbol table (local variables). Each Enter executes exactly
//! one opcode.
//!
//! Usage:  jvm-step <File.class> <method> [intArg ...]
//! e.g.    jvm-step java/Add.class add 2 3
//!
//! The interpreter only does integer work so far, so the target should be a
//! static method over `int`s; pass its arguments in slot order.

use std::io::{BufRead, Write};
use std::path::{Path, PathBuf};
use std::process::{Command, ExitCode};

use jvm::jvm::class_file::ClassFile;
use jvm::jvm::interpreter::bytecode_interpreter::{JVM, Step};
use jvm::jvm::interpreter::frame::{Frame, Value};
use jvm::jvm::interpreter::gc::{self, CompactReport, MarkReport};
use jvm::jvm::interpreter::metaspace::MetaspaceService;
use jvm::jvm::opcode::{self, Instruction};
use jvm::jvm::verifier;

/// Path to the frozen Nivel-0 javap clone, relative to the repo root.
const JAVAP_CLON: &str = "bin/javap-clon.exe";

// --- ANSI control sequences ------------------------------------------------
/// Clear screen + scrollback and home the cursor.
const CLEAR: &str = "\x1b[2J\x1b[3J\x1b[H";
/// Bold bright-yellow: the instruction about to execute (active frame).
const CUR: &str = "\x1b[1;93m";
/// Bright cyan: the paused instruction in a frozen (caller) frame.
const FROZEN: &str = "\x1b[96m";
/// Dim grey: the surrounding context instructions.
const DIM: &str = "\x1b[90m";
/// Bright green / red: a live (reachable) / garbage object after a GC mark pass.
const GREEN: &str = "\x1b[92m";
const RED: &str = "\x1b[91m";
const RESET: &str = "\x1b[0m";

/// How many instructions to show before / after the current one (kept small so
/// two panels fit side by side).
const BEFORE: usize = 2;
const AFTER: usize = 7;

/// Heap hex-dump rows visible at once (8 bytes each). The dump scrolls (↑/↓) so a
/// large heap can't push the instruction panels off the top of the screen — the panel
/// height stays bounded regardless of occupancy.
const HEAP_WINDOW_ROWS: usize = 16;

/// A GC pass to paint in the heap panel — which kind drives the header text and the
/// per-row tints (mark vs sweep share the live/garbage report; compaction reports
/// what it moved/reclaimed).
enum GcView {
    Marked(MarkReport),
    Swept(MarkReport),
    Compacted(CompactReport),
}

fn main() -> ExitCode {
    enable_ansi();

    let args: Vec<String> = std::env::args().skip(1).collect();
    if args.len() < 2 {
        eprintln!("usage: jvm-step <File.class> <method> [intArg ...]");
        return ExitCode::FAILURE;
    }
    let path = &args[0];
    let method_name = &args[1];
    // The rest are integer arguments for the method, in slot order.
    let call_args: Vec<Value> = args[2..]
        .iter()
        .map(|a| Value::Int(a.parse().unwrap_or(0)))
        .collect();

    // --- 1. Parse the entry class; find the requested method's descriptor. ----
    let class_file = match ClassFile::from_path(path) {
        Ok(cf) => cf,
        Err(e) => {
            eprintln!("error parsing '{path}': {e}");
            return ExitCode::FAILURE;
        }
    };
    let class_name = match class_file.class_name(class_file.this_class) {
        Some(name) => name.to_string(),
        None => {
            eprintln!("could not read the class name of {path}");
            return ExitCode::FAILURE;
        }
    };
    // The CLI gives only a method name; look up its descriptor to resolve it.
    let descriptor = class_file
        .methods
        .iter()
        .find(|m| class_file.utf8(m.name_index) == Some(method_name.as_str()))
        .and_then(|m| class_file.utf8(m.descriptor_index));
    let descriptor = match descriptor {
        Some(d) => d.to_string(),
        None => {
            eprintln!("method '{method_name}' not found in {path}");
            return ExitCode::FAILURE;
        }
    };

    // --- 2. Build the metaspace and verify the entry method (Linking §5.4.1).
    // Two loaders, parent-first: the bootstrap loader serves the core classes from
    // `boot/` (java.lang.*), the application loader the user classes from the entry
    // class's own directory. Verification runs *before* execution — the real JVM
    // rejects a method that fails to verify — on the still-separate `class_file`, so
    // `is_subtype` can use the metaspace freely. We keep the outcome to show it on
    // the intro screen (one pause for the whole intro, not two).
    let app: Vec<PathBuf> = Path::new(path).parent().map(PathBuf::from).into_iter().collect();
    let mut metaspace = MetaspaceService::new(vec![PathBuf::from("boot")], app);
    let verify_line = match class_file.methods.iter().find(|m| {
        class_file.utf8(m.name_index) == Some(method_name.as_str())
            && class_file.utf8(m.descriptor_index) == Some(descriptor.as_str())
    }) {
        Some(member) => match verifier::verify_method(&mut metaspace, &class_file, member) {
            Ok(()) => format!("\x1b[92m✓ verificado: {method_name}{descriptor}\x1b[0m"),
            // The verifier doesn't model this opcode yet — warn and run anyway.
            Err(e) if e.unsupported => {
                format!("\x1b[90m⚠ verificación parcial: {} (pc {}) — se ejecuta igual\x1b[0m", e.message, e.pc)
            }
            // A real type-safety violation: reject, like a JVM `VerifyError`.
            Err(e) => {
                eprintln!("VerifyError en {}{} @pc {}: {}", e.method, descriptor, e.pc, e.message);
                return ExitCode::FAILURE;
            }
        },
        None => String::new(),
    };

    // --- 3. Intro screen: the javap-clone disassembly + the verification result,
    // then a single Enter to start stepping.
    show_disassembly(path);
    println!("{verify_line}");
    print!("\n[Enter para empezar la ejecución paso a paso]  ");
    let _ = std::io::stdout().flush();
    wait_for_enter();

    metaspace.add(class_name.clone(), class_file);
    let entry = match metaspace.resolve_method(&class_name, method_name, &descriptor) {
        Some(id) => id,
        None => {
            eprintln!("method '{method_name}' has no Code (abstract or native?)");
            return ExitCode::FAILURE;
        }
    };
    let max_locals = metaspace.max_locals(entry);
    let frame = Frame::new(entry, max_locals, call_args);
    let mut interp = JVM::new(metaspace, frame);

    // The most recent GC pass, if any — kept so the heap panel can paint live vs
    // garbage. Cleared on the next step (the marks go stale once state changes).
    let mut last_gc: Option<GcView> = None;
    // First visible row of the heap hex-dump (in 8-byte rows). Persists across steps;
    // ↑/↓ scroll it so a large heap never crowds out the instruction panels.
    let mut heap_scroll: usize = 0;
    loop {
        heap_scroll = heap_scroll.min(max_heap_scroll(&interp)); // clamp if the heap shrank (e.g. compaction)
        render(&interp, last_gc.as_ref(), heap_scroll);
        print!("\n[↑/↓ = scroll heap · espacio = marcar · s = barrer · c = compactar · otra tecla = avanzar opcode]  ");
        let _ = std::io::stdout().flush();

        // Single keypress (no Enter). ↑/↓ scroll the heap and a GC key paints a
        // read-only view — none of these advance the program, and scrolling keeps the
        // current GC paint. Any *other* key steps one opcode.
        match read_key() {
            Key::Up => heap_scroll = heap_scroll.saturating_sub(1),
            Key::Down => heap_scroll = (heap_scroll + 1).min(max_heap_scroll(&interp)),
            Key::Char(b' ') => last_gc = Some(GcView::Marked(interp.gc_mark())),
            Key::Char(b's') | Key::Char(b'S') => last_gc = Some(GcView::Swept(interp.gc_sweep())),
            Key::Char(b'c') | Key::Char(b'C') => last_gc = Some(GcView::Compacted(interp.gc_compact())),
            Key::Char(_) => {
                last_gc = None; // stepping moves the state on, so any GC paint is now stale
                if let Step::Return(value) = interp.step() {
                    // The entry method returned: the call stack is now empty, so
                    // there's no frame left to draw — just announce the result.
                    match value {
                        Some(v) => println!("\n=> return {}", show(&v)),
                        None => println!("\n=> return (void)"),
                    }
                    break;
                }
            }
        }
    }

    ExitCode::SUCCESS
}

/// Clears the screen and draws the call stack: a breadcrumb of every frame, then a
/// row of panels — the two deepest frames (caller frozen, current active) and, on
/// the far right, the heap. With a single frame there's no caller, so just the
/// current method + the heap. Going deeper shifts older callers off-screen.
fn render(interp: &JVM, gc: Option<&GcView>, heap_scroll: usize) {
    print!("{CLEAR}");
    let frames = interp.frames();
    let depth = frames.len();

    // Thread row (only once there's more than one green thread): each thread with its
    // current method and state; ▶ marks the one running this step, so the cooperative
    // context switches are visible as you step.
    let threads = interp.thread_views();
    if threads.len() > 1 {
        let cells: Vec<String> = threads
            .iter()
            .map(|t| {
                let mark = if t.current { "▶" } else { " " };
                format!("{mark}T{} {} [{}]", t.id, t.method, t.status)
            })
            .collect();
        println!("threads:  {}\n", cells.join("    "));
    }

    // Breadcrumb of the whole call chain, so off-screen ancestors stay visible.
    let chain: Vec<&str> = frames.iter().map(|f| interp.method_name_of(f)).collect();
    println!("call stack:  {}\n", chain.join("  ›  "));

    // Panels left→right: caller (if any), current method, heap.
    let mut panels: Vec<Vec<String>> = Vec::new();
    if depth >= 2 {
        panels.push(panel_lines(interp, &frames[depth - 2], true)); // caller, frozen
    }
    panels.push(panel_lines(interp, &frames[depth - 1], false)); // current, active
    panels.push(heap_panel_lines(interp, gc, heap_scroll)); // the heap, far right
    print_columns(&panels);

    // The program's stdout (what native methods printed), shown persistently — the
    // screen clears every frame, so raw stdout would just flicker and vanish.
    let console = interp.console();
    if !console.is_empty() {
        println!("\n{DIM}── salida (stdout) ──{RESET}");
        print!("{console}");
    }
}

/// Prints several panels side by side, separated by a vertical rule. Each column
/// is padded (ANSI-aware) to its own widest line; trailing empty columns on a row
/// are dropped so there are no dangling separators.
fn print_columns(panels: &[Vec<String>]) {
    let widths: Vec<usize> = panels
        .iter()
        .map(|p| p.iter().map(|l| visible_len(l)).max().unwrap_or(0))
        .collect();
    let rows = panels.iter().map(Vec::len).max().unwrap_or(0);
    for i in 0..rows {
        // Rightmost column with content on this row (skip trailing blanks).
        let last = panels.iter().rposition(|p| p.get(i).is_some_and(|l| !l.is_empty()));
        let Some(last) = last else {
            println!();
            continue;
        };
        let mut line = String::new();
        for (j, width) in widths.iter().enumerate().take(last + 1) {
            if j > 0 {
                line.push_str("  │  ");
            }
            let cell = panels[j].get(i).map(String::as_str).unwrap_or("");
            line.push_str(cell);
            if j < last {
                line.push_str(&" ".repeat(width.saturating_sub(visible_len(cell))));
            }
        }
        println!("{line}");
    }
}

/// The highest valid heap scroll offset (in rows) for the current occupancy — so the
/// last `HEAP_WINDOW_ROWS` rows sit at the bottom and you can't scroll past the end.
/// `0` when the whole dump already fits in the window.
fn max_heap_scroll(interp: &JVM) -> usize {
    let rows = interp.heap().bytes().len().div_ceil(8);
    rows.saturating_sub(HEAP_WINDOW_ROWS)
}

/// Builds the heap panel: a header with usage, then the visible window of the hex dump
/// of the allocated region (8 bytes per row, with offsets), starting at row `scroll`.
/// Empty until `new` starts allocating.
fn heap_panel_lines(interp: &JVM, gc: Option<&GcView>, scroll: usize) -> Vec<String> {
    let heap = interp.heap();
    let used = heap.used();
    let mirrors = interp.class_objects(); // (Class ID, class name, offset), by offset
    let mut header = format!("heap  ({} / {} bytes)", used, heap.size());
    match gc {
        // Mark-only: garbage is just *identified*. Sweep: reclaimed into the free
        // list. Compact: live objects relocated, holes squeezed out.
        Some(GcView::Marked(r)) => header.push_str(&format!(
            "   {GREEN}{} vivos{RESET} · {RED}{} basura{RESET}",
            r.live.len(),
            r.garbage.len()
        )),
        Some(GcView::Swept(r)) => header.push_str(&format!(
            "   {GREEN}{} vivos{RESET} · {DIM}{} barridos → free list{RESET}",
            r.live.len(),
            r.garbage.len()
        )),
        Some(GcView::Compacted(c)) => header.push_str(&format!(
            "   {GREEN}compactado{RESET}: {} movidos · {} B recuperados",
            c.moved, c.reclaimed
        )),
        None => {}
    }
    // Mark and sweep share a live/garbage report that drives the per-row tints.
    let report = match gc {
        Some(GcView::Marked(r)) | Some(GcView::Swept(r)) => Some(r),
        _ => None,
    };
    let mut lines = vec![header, String::new()];
    if used == 0 {
        lines.push("(sin objetos — aparecen con `new`)".to_string());
        return lines;
    }
    // Free-list holes are a *persistent* property of the heap (not a stale GC view):
    // we read them every frame, so a reclaimed hole stays marked `· libre` while you
    // keep stepping — and you watch it vanish (hex changing) the moment a `new`
    // reuses it. A row counts as free if its offset falls inside any free block.
    let free = heap.free_blocks();
    let in_free = |offset: usize| free.iter().any(|&(off, size)| offset >= off && offset < off + size);

    // Hex dump, annotating the row that *starts* a class's `Class<…>` mirror with the
    // class's Class ID, so the `class_id` offsets in object headers become legible.
    // Tints: green (live, last mark) · dim (a free hole) · red (garbage not yet swept).
    // Built in full, then windowed to one page so a big heap doesn't tower over the
    // instruction panels (page with `n`/`p`).
    let mut dump: Vec<String> = Vec::new();
    for (row, chunk) in heap.bytes().chunks(8).enumerate() {
        let offset = row * 8;
        let hex: Vec<String> = chunk.iter().map(|b| format!("{b:02x}")).collect();
        let label = mirrors
            .iter()
            .find(|&&(_, _, off)| off == offset)
            .map(|(uuid, name, _)| format!("  ← {uuid} Class<{name}>"))
            .unwrap_or_default();
        let (color, mark) = if report.is_some_and(|r| r.live.contains(&offset)) {
            (GREEN, "  ✓ vivo")
        } else if in_free(offset) {
            (DIM, "  · libre") // a reclaimed hole, until a malloc reuses it
        } else if report.is_some_and(|r| r.garbage.contains(&offset)) {
            (RED, "  ✗ basura") // identified by a mark pass, not yet swept
        } else {
            ("", "")
        };
        dump.push(format!("{color}{offset:04x}  {}{}{}{RESET}", hex.join(" "), label, mark));
    }

    // Window the dump to the visible rows starting at `scroll`; annotate the byte range
    // and what's hidden above/below so the panel stays short and the instructions remain
    // on screen.
    let total = dump.len();
    let start = scroll.min(total);
    let end = (start + HEAP_WINDOW_ROWS).min(total);
    if total > HEAP_WINDOW_ROWS {
        lines.push(format!(
            "{DIM}─ heap {:#06x}–{:#06x} de {:#06x} · ↑/↓ scroll ─{RESET}",
            start * 8,
            end * 8,
            total * 8
        ));
        if start > 0 {
            lines.push(format!("{DIM}   ⋮ {} filas arriba (↑){RESET}", start));
        }
    }
    lines.extend(dump.drain(start..end));
    if end < total {
        lines.push(format!("{DIM}   ⋮ {} filas abajo (↓){RESET}", total - end));
    }
    // The free list, shown persistently whenever it has holes (so you see them get
    // consumed as allocations reuse the space).
    if !free.is_empty() {
        let bytes: usize = free.iter().map(|&(_, size)| size).sum();
        // Show the compaction policy live: how many of those free bytes are
        // *fragments* (holes too small to reuse) and whether that trips the rule.
        let policy = interp.gc_policy();
        let frag = gc::fragmented_bytes(heap, policy);
        let verdict = if gc::should_compact(heap, policy) {
            format!("{RED}sí{RESET}")
        } else {
            format!("{DIM}no{RESET}")
        };
        lines.push(String::new());
        lines.push(format!("free list: {} huecos · {bytes} B", free.len()));
        lines.push(format!("fragmentado: {frag} B · compactaría: {verdict}"));
    }
    // Legend: the class → mirror-offset map, so a `class_id` of 0 reads as "the
    // class whose mirror is at offset 0", not "empty".
    if !mirrors.is_empty() {
        lines.push(String::new());
        lines.push("clase → offset del mirror:".to_string());
        for (_uuid, name, off) in &mirrors {
            lines.push(format!("  {name:<8} @{off}"));
        }
    }
    lines
}

/// Builds one frame's panel as lines: a header, the bytecode window (its current
/// instruction highlighted — actively, or in a paused colour if it's the
/// suspended caller), the operand stack and the locals.
fn panel_lines(interp: &JVM, frame: &Frame, frozen: bool) -> Vec<String> {
    let name = interp.method_name_of(frame);
    let tag = if frozen { "❄ esperando" } else { "▶ ejecutando" };
    let mut lines = vec![format!("{name}()  {tag}"), String::new()];

    let bytes = interp.code_of(frame);
    let instructions = opcode::disassemble(bytes);
    let current = instructions.iter().position(|i| i.pc == frame.pc());
    lines.extend(window_lines(bytes, &instructions, current, frozen));

    lines.push(String::new());
    lines.push(format!("pila (tope →): {}", show_list(frame.stack())));
    lines.push(String::new());
    lines.push("locales:".to_string());
    for (slot, value) in frame.locals().iter().enumerate() {
        lines.push(format!("  [{slot}] = {}", show(value)));
    }
    lines
}

/// Builds the bytecode window's lines: `BEFORE` before `current`, the current one
/// highlighted, `AFTER` after. Each row shows the instruction's raw bytes. The
/// `frozen` flag picks the highlight colour (a paused caller vs the active frame).
fn window_lines(bytes: &[u8], instructions: &[Instruction], current: Option<usize>, frozen: bool) -> Vec<String> {
    // After a `*return` the pc may not match any instruction; fall back to the tail.
    let current = current.unwrap_or(instructions.len().saturating_sub(1));
    let start = current.saturating_sub(BEFORE);
    let end = (current + AFTER + 1).min(instructions.len());

    // Pre-render each row's hex bytes so we can align the mnemonics to the widest.
    let rows: Vec<(usize, String, &Instruction)> = (start..end)
        .map(|i| {
            let ins = &instructions[i];
            let raw = bytes.get(ins.pc..ins.pc + ins.length).unwrap_or(&[]);
            (i, hex_bytes(raw), ins)
        })
        .collect();
    let width = rows.iter().map(|(_, hex, _)| hex.len()).max().unwrap_or(0);

    let (hi_color, hi_marker) = if frozen { (FROZEN, "▷") } else { (CUR, "▶") };
    rows.iter()
        .map(|(i, hex, ins)| {
            let ops = ins.operands.replace('\n', " ");
            let (color, marker) = if *i == current { (hi_color, hi_marker) } else { (DIM, " ") };
            format!("{color} {marker} {:>4}: {hex:<width$}  {} {}{RESET}", ins.pc, ins.mnemonic, ops)
        })
        .collect()
}

/// Visible width of a string, ignoring ANSI escape sequences, so the side-by-side
/// padding lines up even with colour codes embedded in the line.
fn visible_len(s: &str) -> usize {
    let mut len = 0;
    let mut in_escape = false;
    for c in s.chars() {
        if in_escape {
            in_escape = c != 'm';
        } else if c == '\x1b' {
            in_escape = true;
        } else {
            len += 1;
        }
    }
    len
}

/// Runs `javap-clon.exe -v <path>` and echoes its output. A missing clone is a
/// warning, not a fatal error — the stepping below is the main event.
fn show_disassembly(path: &str) {
    match Command::new(JAVAP_CLON).arg("-v").arg(path).output() {
        Ok(out) => {
            print!("{}", String::from_utf8_lossy(&out.stdout));
            if !out.stderr.is_empty() {
                eprint!("{}", String::from_utf8_lossy(&out.stderr));
            }
        }
        Err(e) => eprintln!("(no pude correr {JAVAP_CLON}: {e})"),
    }
}

/// Blocks until the user presses Enter, returning what they typed on the line. Used
/// for the one-shot intro prompt ("[Enter para empezar…]").
fn wait_for_enter() -> String {
    let mut line = String::new();
    let _ = std::io::stdin().lock().read_line(&mut line);
    line
}

/// One keypress from the step loop: the up/down arrows (which scroll the heap) or any
/// other key (which the caller turns into a step / GC action).
enum Key {
    Up,
    Down,
    Char(u8),
}

/// Reads a single keypress with no echo and no Enter, so the arrows can scroll the
/// heap live. On Windows the CRT's `_getch` delivers extended keys (arrows, F-keys) as
/// a two-byte sequence: a `0x00` or `0xE0` prefix, then the scan code (↑ = 0x48,
/// ↓ = 0x50). Any non-arrow key comes back as `Key::Char` for the caller to act on.
#[cfg(windows)]
fn read_key() -> Key {
    extern "C" {
        fn _getch() -> i32;
    }
    let c = unsafe { _getch() };
    if c == 0x00 || c == 0xE0 {
        match unsafe { _getch() } {
            0x48 => Key::Up,
            0x50 => Key::Down,
            other => Key::Char(other as u8), // some other extended key → treat as a step
        }
    } else {
        Key::Char(c as u8)
    }
}

/// Non-Windows fallback: no raw console here, so read a line (a bare Enter steps; the
/// first typed byte drives GC keys). Arrow scrolling is Windows-only.
#[cfg(not(windows))]
fn read_key() -> Key {
    match wait_for_enter().trim_end_matches(['\r', '\n']).bytes().next() {
        Some(b) => Key::Char(b),
        None => Key::Char(b'\n'),
    }
}

/// Renders one `Value` the way a human reads it (just the number for an int).
fn show(value: &Value) -> String {
    match value {
        Value::Int(n) => n.to_string(),
        Value::Long(n) => format!("{n}L"),
        Value::Double(n) => format!("{n}d"),
        Value::Float(n) => format!("{n}f"),
        Value::Reference(0) => "null".to_string(),
        Value::Reference(offset) => format!("ref@{offset}"),
    }
}

/// Renders a slice of `Value`s as `[a, b, c]`.
fn show_list(values: &[Value]) -> String {
    let parts: Vec<String> = values.iter().map(show).collect();
    format!("[{}]", parts.join(", "))
}

/// Renders an instruction's raw bytes as space-separated hex (`b8 00 07`). Long
/// operands (switch tables) are capped so a row never blows up the window.
fn hex_bytes(bytes: &[u8]) -> String {
    const MAX: usize = 6;
    let shown: Vec<String> = bytes.iter().take(MAX).map(|b| format!("{b:02x}")).collect();
    let mut text = shown.join(" ");
    if bytes.len() > MAX {
        text.push_str(" …");
    }
    text
}

/// Turns on ANSI escape processing for the console. Windows Terminal has it on
/// by default, but the classic console needs `ENABLE_VIRTUAL_TERMINAL_PROCESSING`
/// or our clear/colour codes would print as literal `←[2J` garbage. A no-op on
/// non-Windows targets.
#[cfg(windows)]
fn enable_ansi() {
    const STD_OUTPUT_HANDLE: u32 = -11i32 as u32;
    const ENABLE_VIRTUAL_TERMINAL_PROCESSING: u32 = 0x0004;
    #[link(name = "kernel32")]
    extern "system" {
        fn GetStdHandle(n_std_handle: u32) -> *mut core::ffi::c_void;
        fn GetConsoleMode(h: *mut core::ffi::c_void, mode: *mut u32) -> i32;
        fn SetConsoleMode(h: *mut core::ffi::c_void, mode: u32) -> i32;
    }
    unsafe {
        let handle = GetStdHandle(STD_OUTPUT_HANDLE);
        let mut mode = 0u32;
        if GetConsoleMode(handle, &mut mode) != 0 {
            SetConsoleMode(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
        }
    }
}

#[cfg(not(windows))]
fn enable_ansi() {}
