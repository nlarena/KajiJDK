//! Level 2 assembled: generator + executor + oracle + reducer, wired into
//! [`campaign`](super::campaign).
//!
//! Everything below this line is a convenience. The four pieces are independent by design and the
//! loop takes them as arguments; this module just spares every caller from repeating the same eight
//! lines, and gives the `#[ignore]`d end-to-end tests one place to live.
//!
//! # Which pairings are worth running
//!
//! | pair | what it pins | cost |
//! |---|---|---|
//! | [`Path::Interpreter`] vs [`Path::Jit`] | the JIT against the interpreter, which is the VM's own correctness oracle | two of our processes |
//! | [`Path::Jit`] vs [`Path::ReferenceJdk`] | this VM against a real `java` | needs the known-divergence list to be honest |
//! | [`Path::Jit`] vs [`Path::OsGil`] | the threading substrates against each other | cheap, but this grammar has no threads yet, so it can only find engine differences |
//!
//! The first is the one to run by default: it is the pairing where a disagreement is unambiguously
//! a bug in *this* project, with no reference-implementation judgement calls in the way.

use std::path::PathBuf;
use std::time::Duration;

use super::exec::{ProcessRunner, Toolchain};
use super::gen::{GenConfig, JavaGenerator};
use super::oracle::ExactOracle;
use super::reduce::StructuralReducer;
use super::{campaign, Path, Report, Seed};

/// The four pieces, assembled.
pub struct Campaign {
    pub generator: JavaGenerator,
    pub runner: ProcessRunner,
    pub oracle: ExactOracle,
    pub reducer: StructuralReducer,
}

impl Campaign {
    /// A campaign against the toolchain on this machine.
    ///
    /// The time budget is per *run*, not per seed, and it has to clear the slowest thing a
    /// generated program can legitimately do (a few thousand statements through the interpreter,
    /// plus JVM startup) by a wide margin — a budget set too tight turns every slow-but-correct
    /// program into a `Timeout` on one side and a value on the other, which the oracle is obliged
    /// to report as a divergence. That failure mode looks exactly like a real finding, so it is
    /// worth over-provisioning.
    pub fn detect(workdir: impl Into<PathBuf>, budget: Duration) -> Campaign {
        Campaign {
            generator: JavaGenerator::default(),
            runner: ProcessRunner::new(Toolchain::detect(), workdir, budget),
            oracle: ExactOracle::new(),
            reducer: StructuralReducer::default(),
        }
    }

    /// Narrows the grammar. Useful for a first campaign against a pairing nobody has tried, where
    /// small programs make any finding readable even before the reducer runs.
    pub fn with_config(mut self, config: GenConfig) -> Campaign {
        self.generator.config = config;
        self
    }

    /// The same program, `repeats` times, on **one** path — see
    /// [`repetition_campaign`][crate::fuzz::repetition_campaign].
    /// El lazo de **pertenencia** — ver
    /// [`membership_campaign`][crate::fuzz::membership_campaign].
    ///
    /// Sin reductor, y no por pereza: el conjunto admisible es una propiedad de la **forma** del
    /// programa, y un reductor que borrara la carrera dejaría el conjunto viejo colgado de un
    /// programa que ya no lo produce. Minimizar acá exige recalcular el conjunto, que es trabajo
    /// del generador y no del reductor.
    pub fn run_membership(
        &mut self,
        path: Path,
        repeats: usize,
        seeds: u64,
        stop_after: usize,
    ) -> Report {
        crate::fuzz::membership_campaign(
            &mut self.generator,
            &mut self.runner,
            &mut crate::fuzz::NoReduction,
            path,
            repeats,
            (0..seeds).map(Seed),
            stop_after,
        )
    }

    pub fn run_repeated(
        &mut self,
        path: Path,
        repeats: usize,
        seeds: u64,
        stop_after: usize,
    ) -> Report {
        crate::fuzz::repetition_campaign(
            &mut self.generator,
            &mut self.runner,
            &self.oracle,
            &mut self.reducer,
            path,
            repeats,
            (0..seeds).map(Seed),
            stop_after,
        )
    }

    pub fn run(&mut self, paths: (Path, Path), seeds: u64, stop_after: usize) -> Report {
        campaign(
            &mut self.generator,
            &mut self.runner,
            &self.oracle,
            &mut self.reducer,
            paths,
            (0..seeds).map(Seed),
            stop_after,
        )
    }
}

/// A campaign report as a human reads it: the health of the generator first, because a campaign
/// with a bad `unusable` rate found nothing for a reason that has nothing to do with the VM.
pub fn describe(report: &Report, paths: (Path, Path)) -> String {
    use std::fmt::Write as _;
    let mut out = String::new();
    let _ = writeln!(out, "{} vs {}", paths.0, paths.1);
    let _ = writeln!(
        out,
        "  {} seeds, {} usable ({:.0}%), {} threw a marker ({:.0}%), {} divergences",
        report.seeds_run,
        report.seeds_run - report.unusable,
        report.usable_fraction() * 100.0,
        report.marked,
        report.marked_fraction() * 100.0,
        report.divergences.len()
    );
    // Sólo las campañas de pertenencia tienen conjunto que cubrir, así que en las de pareo esta
    // línea no aparece en vez de aparecer en cero — un cero acá se leería como un problema.
    if let Some(frac) = report.coverage_fraction() {
        let ganadores: Vec<String> =
            report.wins.iter().map(|(i, n)| format!("#{i}x{n}")).collect();
        let _ = writeln!(
            out,
            "  {:.0}% del conjunto admisible visitado, {} ({})",
            frac * 100.0,
            match report.saw_more_than_one() {
                true => "alguna semilla vio más de un valor",
                false => "NINGUNA semilla vio más de un valor",
            },
            ganadores.join(" ")
        );
    }
    for (why, count) in &report.unusable_reasons {
        let _ = writeln!(out, "  unusable x{count}: {why}");
    }
    for divergence in &report.divergences {
        let _ = writeln!(out, "---\n{divergence}");
    }
    out
}

#[cfg(test)]
mod tests {
    use super::*;

    fn workdir(name: &str) -> PathBuf {
        std::env::temp_dir().join(format!("kaji-fuzz-{name}"))
    }

    /// `FUZZ_SEEDS` if it is set, otherwise the smoke-test default. One environment variable so a
    /// campaign can be widened without editing the test that runs it — the same knob
    /// [`a_long_campaign_over_every_pairing`] already used.
    fn seed_count(default: u64) -> u64 {
        std::env::var("FUZZ_SEEDS").ok().and_then(|s| s.parse().ok()).unwrap_or(default)
    }

    /// **The campaign.** Ignored because it compiles and spawns thousands of processes; this is the
    /// one to run by hand.
    ///
    /// `cargo build --release && cargo test --release --lib fuzz::campaigns -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn the_interpreter_and_the_jit_agree_on_generated_programs() {
        let paths = (Path::Interpreter, Path::Jit);
        let mut it = Campaign::detect(workdir("campaign"), Duration::from_secs(25));
        let report = it.run(paths, seed_count(120), 10);
        println!("{}", describe(&report, paths));
        println!(
            "reducer: {} cuts accepted, {} candidates run, {} rejected for free",
            it.reducer.steps, it.reducer.candidates_tried, it.reducer.candidates_rejected_unchecked
        );
        assert!(
            report.usable_fraction() > 0.9,
            "a campaign that cannot compile or cannot finish its own programs tests nothing: {}",
            describe(&report, paths)
        );
        assert!(
            report.divergences.is_empty(),
            "the JIT and the interpreter must agree:\n{}",
            describe(&report, paths)
        );
    }

    /// The same programs against a real `java`. Kept separate because a disagreement here needs a
    /// judgement call — the reference implementation is right by definition, but some differences
    /// are known and legitimate, which is what the oracle's list is for.
    #[test]
    #[ignore]
    fn this_vm_and_the_reference_jdk_agree_on_generated_programs() {
        let paths = (Path::Jit, Path::ReferenceJdk);
        let mut it = Campaign::detect(workdir("campaign-ref"), Duration::from_secs(25));
        let report = it.run(paths, seed_count(120), 10);
        println!("{}", describe(&report, paths));
        assert!(
            report.divergences.is_empty(),
            "this VM disagrees with the reference implementation:\n{}",
            describe(&report, paths)
        );
    }

    /// Strings against the reference JDK.
    ///
    /// The pairing is the point. `string_share` is zero by default and no string opcode is inside
    /// the JIT's subset, so against [`Path::Jit`] a probe is inert weight — it only displaces
    /// arithmetic the compiled arm would otherwise have seen. What is worth asking is whether **this
    /// VM** agrees with a real JDK about the three things the stage generates: interning of
    /// literals (JLS §3.10.5), constant folding of a concatenation of two literals (§15.28, which
    /// decides whether `("a" + "b") == "ab"`), and `equals` over contents.
    ///
    /// The first of those spent time on the oracle's known-divergence list as an accepted
    /// difference before it turned out to be a conformance bug, which is the reason this campaign
    /// exists rather than a note saying strings are fine.
    #[test]
    #[ignore]
    fn strings_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { string_share: 40, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-strings"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        // **Expected to fail while FZ-008 is open**, and deliberately not suppressed. The last
        // time this difference was made to go away it was by adding it to the oracle's
        // known-divergence list, and the list then hid the live bug for as long as it stood. A red
        // campaign naming its finding is the honest state; a green one would have to be bought
        // with the thing that caused the problem.
        assert!(
            report.divergences.is_empty(),
            "this VM disagrees with the reference implementation about strings. If the minimal              case is `ssame(\"x\", \"x\")`, this is **FZ-008** (literals are not interned, JLS              §3.10.5) and it is open — not a new finding:
{}",
            describe(&report, paths)
        );
    }

    /// Narrowing round trips against the reference JDK.
    ///
    /// Paired against the interpreter and not the JIT on purpose: `i2b`, `i2s` and `i2c` are absent
    /// from `burst::compile`'s opcode scan, so a method carrying one is refused whole. Running this
    /// against [`Path::Jit`] would compare the interpreter with itself and call the result
    /// agreement — FZ-004 in a different costume.
    ///
    /// What it asks is whether `conversion_operations` truncates and extends the way a real JDK
    /// does, including the asymmetry that makes `(char) -1` equal 65535 while `(byte) -1` stays -1.
    #[test]
    #[ignore]
    fn narrowing_agrees_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { narrowing_share: 40, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-narrowing"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(
            report.divergences.is_empty(),
            "this VM disagrees with the reference implementation about narrowing:\n{}",
            describe(&report, paths)
        );
    }

    /// **La gramática entera**, y vive en una funcion por una razón de corrección y no de estilo:
    /// la semilla nombra un programa **sólo junto a la config que la consumió**. Cambiar una
    /// perilla corre el flujo del RNG y la semilla 725 pasa a ser otro programa, así que la
    /// campaña que encuentra un hallazgo y el test que lo reproduce tienen que leer literalmente
    /// la misma config o el segundo mide otra cosa creyendo que mide la primera.
    fn wide() -> GenConfig {
        GenConfig {
            max_methods: 5,
            max_params: 4,
            max_stmts: 7,
            max_expr_depth: 4,
            max_block_depth: 3,
            max_loop_bound: 6,
            budget: 6_000,
            // **Todas las perillas prendidas**, y esto es lo que la primera corrida larga corrigió.
            //
            // Acá decía que cada construcción nueva se sumaba sola «el día que aterriza, y no el día
            // que alguien se acuerda de agregar un campo», porque heredaba su share por defecto. Es
            // falso para cualquiera cuyo default sea **cero**, y lo son catorce: las de `K5` y media
            // `K6` enteras. Medido: la corrida de 2000 semillas × 4 pareos dio 0 divergencias sin
            // haber generado ni un string, ni una matriz, ni un NaN, ni una recursión, ni un hilo,
            // ni una etiqueta, ni un local angosto, ni un campo de referencia.
            //
            // Un default en cero no es un descuido — cada uno tiene su razón, casi siempre cobertura
            // compilada— pero heredarlo acá convierte la campaña larga en una campaña larga sobre un
            // pedazo de la gramática, que es la forma FZ-004/FZ-005 aplicada al instrumento.
            //
            // Los valores no son los máximos de cada censo sino más bajos: con quince construcciones
            // compitiendo por las mismas sentencias, los shares altos de un censo de una sola
            // construcción se pisarían entre sí.
            throw_share: 10,
            narrowing_share: 25,
            string_share: 30,
            matrix_share: 25,
            null_array_share: 20,
            nan_share: 25,
            recursion_share: 20,
            narrow_local_share: 25,
            do_while_share: 40,
            label_share: 35,
            ref_field_share: 30,
            workers: 3,
            // `race_threads` queda en cero y **no** por olvido: una carrera tiene más de una
            // respuesta correcta, así que su oráculo es pertenencia y no igualdad. Metida acá, cada
            // semilla sería una divergencia fabricada. Vive en su propia campaña.
            race_threads: 0,
            ..GenConfig::default()
        }
    }

    /// An afternoon's campaign rather than a smoke test: more seeds, a wider grammar, every
    /// pairing. `FUZZ_SEEDS` sets the count so the same test serves both a ten-minute run and an
    /// overnight one.
    ///
    /// `FUZZ_SEEDS=1000 cargo test --release --lib fuzz::campaigns::tests::a_long_campaign -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn a_long_campaign_over_every_pairing() {
        let seeds = seed_count(300);
        // Wider than the default: more helpers to call, deeper expressions, more statements. The
        // budget goes up with them, because the point of a long campaign is to reach shapes the
        // smoke tests cannot.
        let wide = wide();
        let pairings = [
            (Path::Interpreter, Path::Jit),
            (Path::Jit, Path::ReferenceJdk),
            (Path::Jit, Path::OsGil),
            (Path::Jit, Path::OsParallel),
        ];
        let mut findings = Vec::new();
        let mut marked_rates = Vec::new();
        for paths in pairings {
            let mut it = Campaign::detect(workdir("long"), Duration::from_secs(30))
                .with_config(wide);
            let report = it.run(paths, seeds, 5);
            println!("{}", describe(&report, paths));
            marked_rates.push((report.marked, report.seeds_run, paths));
            if !report.divergences.is_empty() {
                findings.push(describe(&report, paths));
            }
        }
        assert!(findings.is_empty(), "{}", findings.join("\n"));
        // **And a campaign can fail on coverage, not only on divergences.** A seed that answers a
        // bare marker threw on its first warm-up iteration and did nothing else; the oracle is
        // right to call that agreement, and it is still not coverage. Before the wrapper caught per
        // iteration this ran between 6% and 51% depending on the configuration and **nothing in the
        // report showed it** — which is FZ-005's whole shape.
        for (marked, seeds, paths) in marked_rates {
            let share = marked as f64 / seeds as f64;
            assert!(
                share < 0.10,
                "{} vs {}: {marked}/{seeds} seeds ({:.0}%) threw instead of computing — the clean \
                 report above is over programs that mostly did nothing",
                paths.0,
                paths.1,
                share * 100.0
            );
        }
    }

    /// **FZ-012 — cuál de los dos miente.**
    ///
    /// El pareo que lo encontró es intérprete contra JIT, y ese pareo dice que difieren y **no**
    /// quién tiene razón: los dos son nuestros. El desempate es un tercero que no es nuestro, así
    /// que esto corre la misma semilla por los cinco caminos y deja que el JDK de referencia diga
    /// cuál de las dos respuestas es la de Java.
    ///
    /// La config sale de [`wide`] y no de una copia: la semilla nombra un programa sólo junto a la
    /// config que la consumió.
    ///
    /// ```text
    ///     cargo test --release --lib fz012_la_semilla -- --ignored --nocapture
    ///     FZ_SEED=725 cargo test --release --lib fz012_la_semilla -- --ignored --nocapture
    /// ```
    #[test]
    #[ignore = "repro: corre una semilla de la campaña larga por los cinco caminos"]
    fn fz012_la_semilla_725_por_todos_los_caminos() {
        use crate::fuzz::{Generator as _, Program as _, Runner as _};

        let seed = Seed(
            std::env::var("FZ_SEED").ok().and_then(|s| s.parse().ok()).unwrap_or(725),
        );
        let mut it =
            Campaign::detect(workdir("fz012"), Duration::from_secs(60)).with_config(wide());
        let program = it.generator.generate(seed);

        println!("== semilla {} ==", seed.0);
        println!("{}", program.to_java());
        for path in [
            Path::ReferenceJdk,
            Path::Interpreter,
            Path::Jit,
            Path::OsGil,
            Path::OsParallel,
        ] {
            let observed = it.runner.run(&program, path);
            println!("  {:>13} -> {:?}", path.to_string(), observed.outcome);
            if !observed.stdout.trim().is_empty() {
                println!("                  stdout: {:?}", observed.stdout.trim());
            }
        }
    }

    /// **FZ-012, la minimización** — la misma semilla, pero pasada por el reductor con el pareo
    /// que la encontró, para quedarse con el programa más chico que todavía diverge. Se imprime el
    /// fuente entero porque lo que sigue —decidir *qué* construcción está mal compilada— se hace
    /// leyéndolo, y el reductor es lo único que lo deja legible.
    ///
    /// ```text
    ///     cargo test --release --lib fz012_minimizada -- --ignored --nocapture
    /// ```
    #[test]
    #[ignore = "repro: reduce una semilla de la campaña larga"]
    fn fz012_minimizada() {
        let seed = Seed(
            std::env::var("FZ_SEED").ok().and_then(|s| s.parse().ok()).unwrap_or(725),
        );
        let mut it =
            Campaign::detect(workdir("fz012min"), Duration::from_secs(60)).with_config(wide());
        let report = crate::fuzz::campaign(
            &mut it.generator,
            &mut it.runner,
            &it.oracle,
            &mut it.reducer,
            (Path::Interpreter, Path::Jit),
            [seed],
            1,
        );
        assert!(!report.divergences.is_empty(), "la semilla {} ya no diverge", seed.0);
        for d in &report.divergences {
            println!("{d}");
        }
    }

    /// **`os-parallel` against itself.** The one campaign shape that needs no reference: it does
    /// not know the right answer and does not have to, because a program whose result is fixed by
    /// construction answering two different things is a finding whichever answer was right.
    ///
    /// That is the shape of FZ-002 and of the stale-reference heisenbug behind it — a wrong answer
    /// in roughly one run of ten, on a program nobody disputes. A *pairing* is blind to it: the
    /// same coin is flipped on both sides, so the two agree most of the time and the finding is
    /// diluted rather than detected. Repetition turns every extra run into another chance, at
    /// linear cost.
    ///
    /// **Today it is expected to find nothing, and that is the point of running it now.** The
    /// grammar has no threads, so every generated program is deterministic *by construction* —
    /// which makes this the control that says the instrument is silent when it should be, before
    /// there is anything for it to hear. The detector is built and validated first; the grammar
    /// that can make it speak comes after.
    ///
    /// `cargo test --release --lib os_parallel_agrees_with_itself -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn os_parallel_agrees_with_itself_on_deterministic_programs() {
        let seeds = seed_count(60);
        let repeats = std::env::var("FUZZ_REPEATS")
            .ok()
            .and_then(|v| v.parse().ok())
            .unwrap_or(10);
        let mut it = Campaign::detect(workdir("self"), Duration::from_secs(30));
        let report = it.run_repeated(Path::OsParallel, repeats, seeds, 3);
        println!(
            "os-parallel vs itself: {} seeds x {repeats} runs, {} usable, {} divergences",
            report.seeds_run,
            report.seeds_run - report.unusable,
            report.divergences.len()
        );
        for finding in &report.divergences {
            println!("{finding}");
        }
        assert!(
            report.divergences.is_empty(),
            "a program the grammar makes deterministic answered two different things"
        );
        // The other half, and the one a clean report cannot be trusted without: a campaign whose
        // programs never ran says nothing, and would say it just as quietly.
        assert!(
            report.usable_fraction() > 0.9,
            "only {:.0}% of seeds were usable — the silence above is the generator's, not the VM's",
            report.usable_fraction() * 100.0
        );
    }

    /// **Una carrera de verdad, juzgada por pertenencia.**
    ///
    /// La única campaña cuyo oráculo no es la igualdad. `Stmt::Fork` compró determinismo siendo
    /// rígido —slots disjuntos, joins antes de leer, reducción en orden fijo— y con eso dejó afuera
    /// todo lo que un programa concurrente real hace y la JLS deja abierto. Ésta es la campaña de
    /// lo que quedó afuera: K hilos escriben constantes **distintas** al **mismo** slot sin
    /// sincronizarse, y después de joinear a todos el lector ve *alguna* de ellas.
    ///
    /// Que sea una y no otra es lo que el modelo de memoria deja libre, así que un pareo reportaría
    /// como divergencia dos motores eligiendo distinto, y la repetición lo mismo entre corridas. Lo
    /// que sí es un bug en cualquier motor es un resultado **fuera** del conjunto.
    ///
    /// `cargo test --release --lib a_real_race -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn a_real_race_stays_inside_the_results_the_memory_model_allows() {
        let repeats =
            std::env::var("FUZZ_REPEATS").ok().and_then(|v| v.parse().ok()).unwrap_or(10);
        let cfg = GenConfig { race_threads: 4, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-race"), Duration::from_secs(30))
            .with_config(cfg);
        let report = it.run_membership(Path::OsParallel, repeats, seed_count(40), 3);
        println!(
            "carrera: {} semillas x {repeats} corridas, {} con conjunto declarado, {} fuera",
            report.seeds_run,
            report.seeds_run - report.unusable,
            report.divergences.len()
        );
        println!("{}", describe(&report, (Path::OsParallel, Path::OsParallel)));
        for finding in &report.divergences {
            println!("{finding}");
        }
        assert!(
            report.divergences.is_empty(),
            "un resultado fuera del conjunto que el modelo de memoria permite"
        );
        // El piso, y es un aserto sobre **esta corrida**, no sobre el motor: un motor que siempre
        // elige la misma respuesta admisible es correcto. Lo que no es aceptable es que la campaña
        // termine sin haber observado una sola diferencia de interleaving, porque entonces de las K
        // respuestas que declaró probar probó una. En una máquina de un solo core esto puede fallar
        // legítimamente, y el mensaje lo dice para que no se lea como un bug de la VM.
        assert!(
            report.saw_more_than_one(),
            "ninguna semilla vio más de un valor admisible: la campaña no midió interleaving              (legítimo en una máquina de un core; en cualquier otra, la forma de la carrera no              está compitiendo)"
        );
        // Y el piso por posición, que es el objetivo de la barrera: que el store de **cada** worker
        // se llegue a observar. Con 4 workers el reparto parejo da 25%; el umbral está cinco veces
        // más abajo a propósito, para que falle sólo cuando el sesgo volvió, no cuando el scheduler
        // tuvo un día raro. Sin barrera esto medía 0,25%.
        let piso = report.least_visited_share().unwrap_or(0.0);
        assert!(
            piso > 0.05,
            "la posición menos visitada del conjunto ganó el {:.1}% de las corridas: el store de              ese worker no se está probando ({:?})",
            piso * 100.0,
            report.wins
        );
        assert!(report.usable_fraction() > 0.9, "{:.0}% con conjunto", report.usable_fraction() * 100.0);
    }

    /// **Locales angostos contra el JDK de referencia.**
    ///
    /// `byte`, `short` y `char` son `int` en el bytecode, así que lo que se pregunta no es el
    /// truncado —eso ya lo cubren las conversiones— sino que la conversión ocurra **en la
    /// asignación**: una VM que perdiera el `i2b` antes del `istore` daría el valor equivocado en
    /// una lectura posterior. Y `boolean`, que es el único cuya lectura es una condición.
    ///
    /// `cargo test --release --lib narrow_locals_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn narrow_locals_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { narrow_local_share: 30, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-narrow"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **`do`/`while` y saltos etiquetados contra el JDK de referencia.**
    ///
    /// Las dos formas van juntas porque las dos preguntan lo mismo: **cuándo** se evalúa la guarda.
    /// Un `do` la evalúa después del cuerpo, y un salto etiquetado la saltea entera hacia un bucle
    /// de más afuera. Una VM que emitiera el salto al revés —o que tratara un `break L;` como un
    /// `break;`— corre un número distinto de vueltas, y como el acumulador va multiplicando por 31
    /// en cada una, una sola vuelta de diferencia cambia el resultado.
    ///
    /// Un `Timeout` acá sería la propiedad 3 rota: querría decir que una etiqueta dejó salir de un
    /// bucle a un lugar desde donde se vuelve a entrar. El oráculo lo reporta como divergencia, que
    /// es exactamente lo que corresponde.
    ///
    /// `cargo test --release --lib labelled_loops_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn labelled_loops_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig {
            do_while_share: 50,
            label_share: 45,
            while_share: 60,
            jump_share: 45,
            switch_share: 25,
            ..GenConfig::default()
        };
        let mut it = Campaign::detect(workdir("campaign-labelled"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **El cast que lee el campo ancho, contra el JDK de referencia.**
    ///
    /// Dos cosas en la misma sentencia y por eso vale la campaña: un `checkcast` que puede fallar y,
    /// si no falla, un `getfield` de 64 bits sobre el objeto recién estrechado. El resultado sale
    /// por un local `long`, así que el valor cruza un `lstore` y un `lload` antes de que alguien lo
    /// mire — que es donde una VM que perdiera la mitad alta lo perdería.
    ///
    /// `cargo test --release --lib wide_casts_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn wide_casts_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { cast_share: 40, object_share: 35, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-wide-cast"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Dos sitios paralelos, contra el JDK de referencia y contra sí mismo.**
    ///
    /// Lo que un segundo sitio agrega no es más paralelismo: es una **segunda clase `Thread`** que
    /// se carga, se resuelve y se instancia, y una segunda tanda de `start()`/`join()` después de
    /// que la primera terminó. Con un solo sitio todo eso pasa una vez por programa, y una vez no
    /// distingue «funciona» de «funciona la primera vez» — que es justo la clase de bug que vive en
    /// una tabla de clases cargadas o en el estado que un `join()` deja atrás.
    ///
    /// El programa sigue siendo determinista por construcción —slots disjuntos, joins antes de las
    /// lecturas, reducción en orden de índice— así que el oráculo sigue siendo igualdad y no hace
    /// falta el conjunto admisible.
    ///
    /// `cargo test --release --lib two_parallel_sites -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn two_parallel_sites_agree_with_the_reference_jdk() {
        let paths = (Path::OsParallel, Path::ReferenceJdk);
        let cfg = GenConfig { workers: 3, parallel_sites: 2, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-two-sites"), Duration::from_secs(30))
            .with_config(cfg);
        let report = it.run(paths, seed_count(60), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Recursión contra el JDK de referencia.**
    ///
    /// Lo que se pone a prueba no es el opcode —una llamada estática es una llamada estática— sino
    /// que el argumento de terminación se sostenga en programas generados: que el descenso llegue
    /// al caso base y devuelva lo mismo de los dos lados. Un `Timeout` acá sería la propiedad 3
    /// rota, y el oráculo lo reporta como divergencia, que es lo correcto.
    ///
    /// `cargo test --release --lib recursion_agrees -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn recursion_agrees_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { recursion_share: 30, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-recursion"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Payloads de NaN contra el JDK de referencia.**
    ///
    /// Lo que se pregunta es lo único que la JLS **no** deja libre en esta esquina: un patrón de
    /// bits que entra por `longBitsToDouble` tiene que salir igual por `doubleToRawLongBits`,
    /// incluso después de pasar por un local. Qué NaN devuelve una *operación* sí es definido por
    /// la implementación (§4.2.3), y por eso el generador nunca sondea uno calculado.
    ///
    /// `cargo test --release --lib nan_payloads_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn nan_payloads_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { nan_share: 35, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-nan"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Matrices against the reference JDK.**
    ///
    /// El pareo es el punto, y es el mismo argumento que hacen `narrowing_share` y los campos de
    /// referencia: `multianewarray` está fuera del subconjunto de `burst::compile`, y también lo
    /// está el `aaload` que necesita toda lectura de una matriz, así que contra [`Path::Jit`] los
    /// dos brazos serían el intérprete. Contra un JDK real no hay tal problema, y lo que se
    /// pregunta es si esta VM coincide sobre las tres cosas que la forma genera: la allocation
    /// rectangular, dos chequeos de cota sobre **dos arrays distintos**, y `m[i].length`.
    ///
    /// `cargo test --release --lib matrices_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn matrices_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        // Las dos perillas juntas: la matriz es lo que hace que `m[i] = null` tenga dónde ir, y
        // anular una fila es lo único que produce una matriz **dentada** en esta gramática.
        let cfg =
            GenConfig { matrix_share: 50, null_array_share: 40, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-matrices"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Reference fields against the reference JDK.**
    ///
    /// The pairing is the point, and it is the same argument `narrowing_share` makes in the other
    /// direction. A `putfield` of a reference is answered `Ineligible` by `burst::compile`, and the
    /// refusal is **per method**: against [`Path::Jit`] one of these anywhere in the entry method
    /// makes both arms the interpreter, so the campaign would be comparing an engine with itself
    /// and calling the result agreement — FZ-004 in a costume. Against a real JDK there is no such
    /// problem, and what is being asked is whether this VM agrees about the two things the shape
    /// generates: a reference field read back through a hop, and a chain whose middle is `null`.
    ///
    /// `cargo test --release --lib reference_fields_agree -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn reference_fields_agree_with_the_reference_jdk() {
        let paths = (Path::Interpreter, Path::ReferenceJdk);
        let cfg = GenConfig { ref_field_share: 60, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("campaign-refs"), Duration::from_secs(25))
            .with_config(cfg);
        let report = it.run(paths, seed_count(80), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// **Reference fields under a collector, compared with itself.**
    ///
    /// This is the pairing the construct exists for. A reference field is the only thing in the
    /// grammar that builds an edge from one heap object to another, which is what the GC's write
    /// barrier and remembered set are for — and an Old object holding a young pointer is the exact
    /// shape of the field in FZ-002's report. Threads on as well, so the edges are built by several
    /// OS threads while the collector runs.
    ///
    /// Worth running with the GC actually collecting, which is **not** the default:
    /// `JVM_GC_AUTO=1 JVM_GC_OCCUPANCY=0.2 JVM_GC_TENURE=1 JVM_GC_VERIFY=1`.
    #[test]
    #[ignore]
    fn reference_fields_agree_with_themselves_under_a_collector() {
        let repeats =
            std::env::var("FUZZ_REPEATS").ok().and_then(|v| v.parse().ok()).unwrap_or(10);
        let cfg = GenConfig { ref_field_share: 60, workers: 4, ..GenConfig::default() };
        let mut it = Campaign::detect(workdir("refs-self"), Duration::from_secs(40))
            .with_config(cfg);
        let report = it.run_repeated(Path::OsParallel, repeats, seed_count(40), 3);
        println!(
            "refs vs themselves: {} seeds x {repeats} runs, {} usable, {} divergences",
            report.seeds_run,
            report.seeds_run - report.unusable,
            report.divergences.len()
        );
        for finding in &report.divergences {
            println!("{finding}");
        }
        assert!(report.divergences.is_empty(), "a deterministic program answered two things");
        assert!(report.usable_fraction() > 0.9, "solo {:.0}% usables", report.usable_fraction() * 100.0);
    }

    /// The grammar for [`GenConfig::workers`]: threads on, everything else as usual.
    ///
    /// Deliberately not the default configuration. A thread is the most expensive thing this
    /// grammar can ask for, and the shape that makes one *usable* to a differential oracle is
    /// rigid — so it earns its place in the campaigns that are about threads, and pays for itself
    /// nowhere else.
    fn concurrent() -> GenConfig {
        GenConfig { workers: 4, ..GenConfig::default() }
    }

    /// **The threads level, against itself.** Concurrent programs, the same one run many times on
    /// `os-parallel`, checked for agreement with itself.
    ///
    /// This is the pair the level was built for: a grammar that can *make* threads and an oracle
    /// that needs no reference to judge them. What it looks for is not a wrong answer — it has no
    /// idea what the right one is — but **two different answers from one program**, which for a
    /// program the grammar makes deterministic is a finding whichever of the two was right.
    ///
    /// `FUZZ_SEEDS=200 FUZZ_REPEATS=20 cargo test --release --lib threads_agree_with_themselves -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn threads_agree_with_themselves_across_runs() {
        let seeds = seed_count(40);
        let repeats =
            std::env::var("FUZZ_REPEATS").ok().and_then(|v| v.parse().ok()).unwrap_or(10);
        let mut it = Campaign::detect(workdir("threads-self"), Duration::from_secs(40))
            .with_config(concurrent());
        let report = it.run_repeated(Path::OsParallel, repeats, seeds, 3);
        println!(
            "threads vs themselves: {} seeds x {repeats} runs, {} usable, {} divergences",
            report.seeds_run,
            report.seeds_run - report.unusable,
            report.divergences.len()
        );
        for (why, count) in &report.unusable_reasons {
            println!("  unusable x{count}: {why}");
        }
        for finding in &report.divergences {
            println!("{finding}");
        }
        assert!(
            report.divergences.is_empty(),
            "a concurrent program the grammar makes deterministic answered two different things"
        );
        assert!(
            report.usable_fraction() > 0.9,
            "only {:.0}% usable — the silence is the generator's, not the VM's",
            report.usable_fraction() * 100.0
        );
    }

    /// **The threads level, against a real JDK.** Agreeing with itself is not the same as being
    /// right: a substrate that dropped every worker's result would be perfectly self-consistent.
    ///
    /// So the same programs go through the pairing oracle too, where the answer is decided by an
    /// implementation nobody here wrote.
    ///
    /// `cargo test --release --lib threads_agree_with_the_reference_jdk -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn threads_agree_with_the_reference_jdk() {
        let paths = (Path::OsParallel, Path::ReferenceJdk);
        let mut it = Campaign::detect(workdir("threads-jdk"), Duration::from_secs(40))
            .with_config(concurrent());
        let report = it.run(paths, seed_count(60), 5);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
        assert!(report.usable_fraction() > 0.9, "{}", describe(&report, paths));
    }

    /// Green threads against OS threads behind the GIL. The grammar has no threads in it yet, so
    /// this can only catch an engine difference — but it is nearly free to run alongside the others.
    #[test]
    #[ignore]
    fn the_threading_substrates_agree_on_generated_programs() {
        let paths = (Path::Jit, Path::OsGil);
        let mut it = Campaign::detect(workdir("campaign-threads"), Duration::from_secs(25));
        let report = it.run(paths, 60, 10);
        println!("{}", describe(&report, paths));
        assert!(report.divergences.is_empty(), "{}", describe(&report, paths));
    }
}

#[cfg(test)]
mod jit_coverage {
    //! Does the JIT actually run on what the generator emits?
    //!
    //! This is not a rhetorical question, and it is the single most important thing to check about
    //! the [`Path::Interpreter`] / [`Path::Jit`] pairing. A method is only compiled after
    //! `JitCache::THRESHOLD` (32) invocations, or on-stack at a loop header once the loop has gone
    //! round enough times. A generated program calls `run()` **once**. If nothing crosses either
    //! threshold, then `JVM_JIT=0` and `JVM_JIT` unset are running *the same engine*, the campaign
    //! is comparing the interpreter against itself, and a clean report means nothing at all.
    //!
    //! This is the same genre of tool bug as FZ-003: a campaign that looks like it is testing
    //! something and is not. So it gets measured rather than assumed, and the measurement is
    //! written down in `docs/fuzzer_findings/`.
    //!
    //! Running in this process rather than through [`ProcessRunner`] is what makes the measurement
    //! possible: `run-headless` never prints the JIT's counters, but
    //! [`execute_counting_tuned`][crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned]
    //! returns them. It is also very much faster, which is why the differential below can cover far
    //! more seeds than the process-spawning campaign — at the cost of the one thing the child
    //! process was for: a VM panic here takes the test down with it instead of being reported.

    use super::*;
    use crate::burst::code_cache::JitStats;
    use crate::fuzz::gen::{marks, JavaProgram};
    use crate::fuzz::{Generator as _, Program as _};
    use crate::jvm::class_file::ClassFile;
    use crate::jvm::interpreter::bytecode_interpreter::execute_counting_tuned;
    use crate::jvm::interpreter::frame::{Frame, Value};
    use crate::jvm::interpreter::metaspace::MetaspaceService;

    /// Compiles `program` with the reference `javac` and leaves the class file in `dir`.
    fn compile(program: &JavaProgram, dir: &std::path::Path) -> PathBuf {
        std::fs::create_dir_all(dir).expect("workdir");
        let source = dir.join(format!("{}.java", program.class_name()));
        std::fs::write(&source, program.to_java()).expect("write source");
        let out = std::process::Command::new(Toolchain::detect().javac)
            .arg("-d")
            .arg(dir)
            .arg(&source)
            .output()
            .expect("spawn javac");
        assert!(
            out.status.success(),
            "the generator emitted source javac rejects:\n{}",
            String::from_utf8_lossy(&out.stderr)
        );
        dir.join(format!("{}.class", program.class_name()))
    }

    /// Whether a returned value is one of the total wrapper's exception markers rather than a
    /// computed result — i.e. whether the program threw instead of finishing its warm-up.
    fn is_marker(value: i32) -> bool {
        [
            marks::ARITHMETIC,
            marks::BOUNDS,
            marks::NULL,
            marks::CLASS_CAST,
            marks::STACK_OVERFLOW,
            marks::NEGATIVE_SIZE,
            marks::OTHER,
        ]
        .contains(&value)
    }

    /// `run()` on the green engine with the JIT forced on or off, plus the JIT's counters.
    fn execute(class_file: &std::path::Path, jit: bool) -> (Option<i32>, usize, JitStats) {
        execute_method(class_file, jit, "run")
    }

    /// El mismo motor, invocando el metodo que se le pida. Existe por el canal de lanzamientos:
    /// la sonda `kjthrew()` corre el programa entero y devuelve el contador, asi que medirlo es
    /// **una** invocacion mas y ni un proceso mas.
    fn execute_method(
        class_file: &std::path::Path,
        jit: bool,
        method: &str,
    ) -> (Option<i32>, usize, JitStats) {
        let class = ClassFile::from_path(class_file.to_str().expect("utf-8 path")).expect("load");
        let name = class.class_name(class.this_class).unwrap().to_string();
        let mut metaspace = MetaspaceService::new(
            vec![PathBuf::from("KajiLibrary"), PathBuf::from("boot")],
            vec![class_file.parent().map(PathBuf::from).unwrap_or_default()],
        );
        metaspace.add(name.clone(), class);
        let entry = metaspace.resolve_method(&name, method, "()I").expect("()I");
        let max_locals = metaspace.max_locals(entry);
        let frame = Frame::new(entry, max_locals, Vec::new());
        let (value, steps, stats) =
            execute_counting_tuned(metaspace, frame, Some(jit), None, |_| {});
        let value = match value {
            Some(Value::Int(v)) => Some(v),
            _ => None,
        };
        (value, steps, stats)
    }

    /// **Cuantas iteraciones del calentamiento lanzan**, que es lo que `marked` no puede decir.
    ///
    /// # Por que `marked` no alcanzaba
    ///
    /// Pregunta si el **resultado** es una marca. Desde que el envoltorio atrapa por iteracion, el
    /// resultado es `((0*31 + r0)*31 + r1)*31 + …`, y eso solo es una marca pelada cuando
    /// `warmup == 1`. Con el warmup de una campana de verdad —40— una semilla que lanza en las 40
    /// iteraciones y una que no lanza nunca dan **el mismo cero**. Cobertura leida como cero por
    /// construccion, que es FZ-005 con el envoltorio nuevo.
    ///
    /// # Por que el canal es un campo y no un `println`
    ///
    /// Porque la consola era el diseno obvio y no sobrevivio al primer intento: `System.out`
    /// crashea esta VM (`FZ-010`), y con el canal encendido las 80 semillas se volvian
    /// divergencias. Un campo estatico y una sonda que lo devuelve no necesitan nada de `java.io`.
    ///
    /// # Que se aserta, y lo que la medicion corrigio del enunciado
    ///
    /// El hito pedia distinguir «lanzo una vez» de «lanzo siempre». Medido, **«una vez» no existe**:
    /// 31 semillas en cero, 29 en las 40, y ninguna en el medio. La razon es la propiedad 2. El
    /// programa es determinista y cada iteracion del calentamiento es la **misma llamada sin
    /// argumentos** a un metodo sin estado entre llamadas, asi que si una iteracion lanza, lanzan
    /// todas. La distribucion es binaria por construccion.
    ///
    /// Eso no debilita el canal, lo aclara: lo que distingue no es *cuantas veces* lanzo sino si la
    /// semilla **ejercito algo o no**, que es exactamente lo que `marked` no podia decir. Y el
    /// numero que sale es el que importaba — con `throw_share: 25`, **29 de 60 semillas no calculan
    /// nada** y la campana las reporta como acuerdo.
    ///
    /// Para que «una vez» fuera alcanzable haria falta que una iteracion cambie lo que la siguiente
    /// hace: estado que sobreviva entre llamadas. La gramatica no lo tiene y no es un descuido —
    /// seria estado mutable compartido entre iteraciones, o sea otra forma de no-determinismo.
    ///
    /// Se asertan **los dos modos**, cada uno por su lado: sin el de arriba el canal no estaria
    /// midiendo nada, y sin el de abajo la tasa de lanzamiento estaria tan alta que la campana
    /// entera seria basura.
    ///
    /// `cargo test --release --lib el_canal_de_lanzamientos -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn el_canal_de_lanzamientos_distingue_una_vez_de_siempre() {
        const SEEDS: u64 = 60;
        let dir = std::env::temp_dir().join("kaji-throw-channel");
        let _ = std::fs::remove_dir_all(&dir);
        let cfg = GenConfig { throw_channel: true, throw_share: 25, ..GenConfig::default() };
        let warmup = cfg.warmup.max(0) as u32;
        let mut generator = JavaGenerator::new(cfg);

        let mut cuentas: Vec<u32> = Vec::new();
        for seed in 0..SEEDS {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            // El resultado de `run()` no interesa aca: lo compara la campana. Lo que interesa es
            // cuantas de sus iteraciones murieron antes de calcular nada.
            let (valor, _, _) = execute_method(&class_file, true, "kjthrew");
            if let Some(n) = valor {
                cuentas.push(n.max(0) as u32);
            }
        }

        let nunca = cuentas.iter().filter(|&&n| n == 0).count();
        let una = cuentas.iter().filter(|&&n| n == 1).count();
        let siempre = cuentas.iter().filter(|&&n| n >= warmup).count();
        let total: u64 = cuentas.iter().map(|&n| u64::from(n)).sum();
        println!(
            "{} semillas con canal (warmup {warmup}): nunca {nunca}, una vez {una}, \
             siempre {siempre}, {:.0}% de las iteraciones lanzaron",
            cuentas.len(),
            total as f64 / (cuentas.len().max(1) as u64 * u64::from(warmup)) as f64 * 100.0
        );

        assert!(!cuentas.is_empty(), "ninguna semilla publico el canal");
        assert!(siempre > 0, "ninguna semilla lanzo en todas las iteraciones");
        assert!(nunca > 0, "ninguna semilla corrio limpia: la tasa de lanzamiento esta muy alta");
        // Y la forma de la distribucion, que es el hallazgo: binaria. Si apareciera una semilla en
        // el medio, alguna iteracion estaria haciendo algo distinto de la anterior — o sea que la
        // propiedad 2 dejo de valer, y eso hay que enterarse aca y no en una divergencia rara.
        assert_eq!(
            una + cuentas.iter().filter(|&&n| n > 0 && n < warmup).count(),
            0,
            "una semilla lanzo en algunas iteraciones y no en otras: el programa dejo de ser \
             determinista entre llamadas"
        );
    }

    /// The measurement. Prints the share of generated programs on which the JIT compiles anything
    /// at all, and fails if that share is so low that the process-spawning campaign is theatre.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::the_jit -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn the_jit_actually_compiles_something_on_generated_programs() {
        const SEEDS: u64 = 60;
        let dir = std::env::temp_dir().join("kaji-fuzz-jitcov");
        let mut generator = JavaGenerator::default();

        let (mut compiled_any, mut entered_any, mut osr_any) = (0, 0, 0);
        for seed in 0..SEEDS {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            let (_, _, stats) = execute(&class_file, true);
            compiled_any += usize::from(stats.compiled > 0);
            entered_any += usize::from(stats.native_calls > 0);
            osr_any += usize::from(stats.osr_entries > 0);
        }
        println!(
            "of {SEEDS} generated programs: {compiled_any} compiled something, \
             {entered_any} entered native code, {osr_any} entered on-stack"
        );
        assert!(
            entered_any * 4 >= SEEDS as usize,
            "only {entered_any}/{SEEDS} programs ever entered native code — the \
             interpreter-vs-JIT campaign is comparing the interpreter against itself. Raise the \
             call counts the generator produces, or lower JVM_JIT_THRESHOLD for the JIT path."
        );
    }

    /// **FZ-005, as a regression test.** A program that throws on warm-up iteration 1 is never
    /// scanned by the JIT at all, and the signature that says so is `rejected == 0`.
    ///
    /// That last part is the whole diagnostic value. "The JIT did not run" has two causes with
    /// opposite fixes — it refused the method (`rejected` goes up: legitimate information about the
    /// subset) or it never saw the method (`rejected` stays at zero: a bug in the generator, which
    /// is producing programs that die before they are hot). Both look identical in
    /// `native_calls == 0`, which is why FZ-005 hid behind a green campaign for a whole stage.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::a_program_that_throws -- --ignored`
    #[test]
    #[ignore]
    fn a_program_that_throws_before_the_threshold_is_never_even_scanned() {
        use crate::fuzz::gen::{Expr, Method, Stmt, Ty};

        // The exact minimal case in `docs/fuzzer_findings/FZ-005-arrays-mueren-antes-del-jit.md`,
        // built from the AST so the two halves cannot drift apart: `int[] a0 = new int[2];
        // return a0[<index>];`
        let at = |index: i32, class: &str| JavaProgram {
            class: class.to_string(),
            // El caso mínimo de FZ-005 no aloca ningún objeto de la jerarquía, así que su grafo de
            // clases es el vacío y nada se emite.
            hierarchy: Default::default(),
            throw_channel: false,
            methods: Vec::new(),
            entry: Method {
                name: "m0".to_string(),
                params: Vec::new(),
                returns: Ty::Int,
                body: vec![Stmt::NewArray { name: "a0".into(), elem: Ty::Int, len: 2 }],
                result: Expr::ArrayLoad("a0".into(), Ty::Int, Box::new(Expr::IntLit(index))),
                cost: 4,
            },
            recursive_body: None,
            admissible: Vec::new(),
            warmup: GenConfig::default().warmup,
        };
        let dir = std::env::temp_dir().join("kaji-fuzz-fz005");

        let out_of_range = at(5, "FzFive");
        assert!(out_of_range.well_formed().is_ok(), "the fixture must be valid Java");
        let (value, _, stats) = execute(&compile(&out_of_range, &dir), true);
        assert_eq!(value, Some(marks::BOUNDS), "it must throw, or it proves nothing");
        assert_eq!(stats.native_calls, 0, "one invocation cannot reach a threshold of 32");
        assert_eq!(
            stats.rejected, 0,
            "`rejected` must stay at zero: the JIT never *saw* this method, and a campaign that \
             cannot tell that from a refusal cannot tell a generator bug from a subset boundary"
        );

        // The identical program with an index the array actually has. One character apart, and it
        // is the difference between a campaign that tests the JIT and one that does not.
        let in_range = at(1, "FzOne");
        let (value, _, stats) = execute(&compile(&in_range, &dir), true);
        assert_eq!(value, Some(0), "a0[1] of a fresh int[2] is the zero `newarray` left there");
        assert!(
            stats.native_calls > 0,
            "the same program that survives its warm-up must reach native code: {stats:?}"
        );
    }

    /// What each grammar extension costs in JIT coverage — measured per configuration rather than
    /// assumed, which is the whole lesson of FZ-004.
    ///
    /// The one that matters is `fp_narrowing`. `f2i`/`f2l`/`d2i`/`d2l` are outside the JIT's subset
    /// on purpose (`burst::compile`, JLS §5.1.3), and the refusal is **per method**: one narrowing
    /// conversion anywhere in the entry method and the whole thing runs interpreted, on both arms
    /// of an interpreter-versus-JIT campaign. So the two settings are not two flavours of the same
    /// campaign — they are two different campaigns, and this prints the number that says so.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::what_each -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn what_each_grammar_setting_costs_in_jit_coverage() {
        const SEEDS: u64 = 80;
        let dir = std::env::temp_dir().join("kaji-fuzz-jitcov-cfg");
        let base = GenConfig::default();
        let settings = [
            ("scalars, integral only ", GenConfig { fp_share: 0, array_share: 0, ..base }),
            ("scalars, with floats   ", GenConfig { array_share: 0, ..base }),
            ("arrays, no floats     ", GenConfig { fp_share: 0, ..base }),
            ("arrays of int only    ", GenConfig { wide_array_elements: false, ..base }),
            ("no narrowing conversion", GenConfig { fp_narrowing: false, ..base }),
            // Stage 3. `new`, `getfield`, `putfield` and `invokevirtual` are all **inside** the
            // JIT's subset, so unlike arrays and narrowing conversions these should not cost
            // anything — which is exactly the kind of expectation worth measuring rather than
            // stating. The two rows that isolate the risk: the planted probe alone (a `new` in a
            // loop, whose `invokespecial` is inlined into its caller) and the `long` field, which
            // is outside the subset in a way that could take the caller with it.
            (
                "no objects at all      ",
                GenConfig { object_share: 0, dispatch_probe: false, ..base },
            ),
            ("objects, no planted probe", GenConfig { dispatch_probe: false, ..base }),
            ("objects, int fields only", GenConfig { wide_fields: false, ..base }),
            // The row that says whether interface dispatch is free. `invokeinterface` (0xb9) is
            // inside `burst::compile`'s scan — it shares every arm with `invokevirtual` and differs
            // only in instruction length — so the prediction is that this row matches the default.
            // A drop here would mean the compiled arm is quietly the interpreter on any program
            // that dispatches through an interface, which is FZ-004 exactly.
            ("objects, no interfaces", GenConfig { interface_share: 0, ..base }),
            // The row that prices the reference field, rather than asserting what it costs. The
            // prediction is that this one is **much worse** than the default: `burst::compile`
            // answers `Ineligible` to a `putfield` of a reference and the refusal is per method,
            // so every entry method carrying one runs interpreted on both arms. Measuring it is
            // what turns "do not pair this against the JIT" from advice into a number.
            ("objects + reference fields", GenConfig { ref_field_share: 60, ..base }),
            // El par que le pone precio al **cast que lee el campo ancho**. Las dos filas dejan la
            // jerarquía idéntica —`long b` declarado y escrito por el constructor en las dos— y
            // varían una sola cosa: si un cast puede llegar a ese campo. Ese aislamiento es el
            // punto, y costó una perilla: apagar `wide_fields` habría sacado el campo de todos
            // lados y la diferencia habría vuelto a medir la mitad ancha entera.
            //
            // Es el único camino de esta gramática por el que un cast que el JIT **puede** compilar
            // termina leyendo un campo que **no** puede: `checkcast` está adentro del subconjunto,
            // el resolver contesta un offset para un `int` de instancia y se niega con el resto, y
            // la negativa es por método.
            ("casts, sólo campo int ", GenConfig { cast_share: 40, wide_cast: false, ..base }),
            ("casts, campo long      ", GenConfig { cast_share: 40, ..base }),
            ("everything (the default)", base),
        ];
        let mut rows = Vec::new();
        for (label, config) in settings {
            let mut generator = JavaGenerator::new(config);
            let (mut entered, mut osr) = (0, 0);
            let (mut compiled, mut refused, mut deopts, mut threw) = (0, 0, 0, 0);
            for seed in 0..SEEDS {
                let program = generator.generate(Seed(seed));
                let class_file = compile(&program, &dir);
                let (value, _, stats) = execute(&class_file, true);
                entered += usize::from(stats.native_calls > 0);
                osr += usize::from(stats.osr_entries > 0);
                compiled += stats.compiled;
                refused += stats.rejected;
                deopts += stats.deopts;
                // A program that threw on its **first** warm-up iteration never reached
                // `JitCache::THRESHOLD`, so the JIT never even scanned it. That is a completely
                // different failure from "the JIT refused it", and telling the two apart is the
                // only way to know which one a grammar change actually caused.
                threw += usize::from(matches!(value, Some(v) if is_marker(v)));
            }
            // `rejected` is the number that actually answers the question, because the refusal is
            // per method: a program can enter native code through its classifier helper while the
            // method doing the floating arithmetic was turned away.
            println!(
                "{label}: {entered}/{SEEDS} entered native code, {osr} on-stack, \
                 {compiled} methods compiled, {refused} refused, {deopts} deopts,                  {threw} died on a marker"
            );
            rows.push((label, entered));
        }
        // Not an assertion about which is larger — that is the measurement, and pinning it would
        // turn a finding into a test failure. What must hold is that the integral grammar still
        // clears the FZ-004 floor, so a regression there is not blamed on floating point.
        let integral = rows[0].1;
        assert!(
            integral * 4 >= SEEDS as usize,
            "the integral grammar itself dropped to {integral}/{SEEDS} — FZ-004 again, and \
             floating point is not the cause"
        );
    }

    /// The **itable census**: does `javac` actually emit `invokeinterface` for what the generator
    /// produces?
    ///
    /// A campaign reporting zero divergences over a construct it never generated is
    /// indistinguishable from a campaign that works, which is the failure FZ-003 and FZ-005 both
    /// were. For interface dispatch the gap is unusually easy to fall into: the generator never
    /// names the opcode. It declares a local as `…I` instead of `…B` and lets `javac` choose, so
    /// "we generate interface calls" is a claim about a *compiler*, not about our own code — and
    /// the only honest way to hold it is to go and look in the class file.
    ///
    /// So this counts `invokeinterface` sites directly: opcode `0xb9`, whose operand must resolve
    /// to a `CONSTANT_InterfaceMethodref` and which is the one call opcode carrying a trailing zero
    /// byte (JVMS §6.5). Requiring all three makes a stray `0xb9` inside another instruction's
    /// operands vanishingly unlikely to be miscounted.
    ///
    /// `cargo test --release --lib fuzz::campaigns::jit_coverage::interface_calls -- --ignored --nocapture`
    #[test]
    #[ignore]
    fn interface_calls_actually_reach_the_class_file() {
        let dir = std::env::temp_dir().join("kaji-itable-census");
        let _ = std::fs::remove_dir_all(&dir);
        let mut sites = 0usize;
        let mut seeds_with_a_site = 0usize;
        const SEEDS: u64 = 60;
        let mut generator = JavaGenerator::new(GenConfig::default());

        for seed in 0..SEEDS {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            let class = ClassFile::from_path(class_file.to_str().expect("utf-8 path")).expect("load");

            let mut here = 0usize;
            for method in &class.methods {
                let Some(code) = class.member_code(method) else { continue };
                let body = &code.code;
                for i in 0..body.len().saturating_sub(4) {
                    if body[i] != 0xb9 || body[i + 4] != 0 {
                        continue;
                    }
                    let index = u16::from_be_bytes([body[i + 1], body[i + 2]]);
                    if class.methodref_name_and_type(index).is_some() {
                        here += 1;
                    }
                }
            }
            sites += here;
            if here > 0 {
                seeds_with_a_site += 1;
            }
        }

        let share = 100 * seeds_with_a_site / SEEDS as usize;
        println!(
            "invokeinterface: {sites} sites across {seeds_with_a_site}/{SEEDS} seeds ({share}%)"
        );
        // A **floor**, not a target. Measured at the default `interface_share` of 50 it sits at
        // roughly 35% of seeds and 40 sites; what this number has to catch is the construct
        // silently going to zero — a knob defaulted back off, a `javac` change that picks
        // `invokevirtual`, a scope rule that stops the locals being read — because that is the
        // state in which a clean campaign report says nothing about itables and looks identical to
        // one that says something.
        assert!(
            share >= 20,
            "only {share}% of seeds carry an interface call ({sites} sites):              the campaigns are no longer covering itable dispatch"
        );
        let _ = std::fs::remove_dir_all(&dir);
    }

    /// The same differential the process-spawning campaign runs, but in this process and therefore
    /// over far more seeds. Not a replacement — it cannot survive a VM panic, and a panic is a
    /// finding — but it is what a wide sweep can afford.
    #[test]
    #[ignore]
    fn the_jit_and_the_interpreter_agree_in_process() {
        let seeds: u64 = std::env::var("FUZZ_SEEDS")
            .ok()
            .and_then(|s| s.parse().ok())
            .unwrap_or(200);
        let dir = std::env::temp_dir().join("kaji-fuzz-inproc");
        let mut generator = JavaGenerator::default();

        let (mut disagreements, mut exercised) = (Vec::new(), 0);
        for seed in 0..seeds {
            let program = generator.generate(Seed(seed));
            let class_file = compile(&program, &dir);
            let (off, _, off_stats) = execute(&class_file, false);
            let (on, _, on_stats) = execute(&class_file, true);
            assert_eq!(off_stats, JitStats::default(), "seed {seed}: the JIT ran with it off");
            exercised += usize::from(on_stats.native_calls > 0);
            if off != on {
                disagreements.push(format!(
                    "seed {seed}: interpreter {off:?}, jit {on:?} ({on_stats:?})\n{}",
                    program.to_java()
                ));
            }
        }
        println!("{seeds} seeds, {exercised} of them entered native code");
        assert!(disagreements.is_empty(), "{}", disagreements.join("\n---\n"));
    }
}
