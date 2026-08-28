//! The **reducer** (level 2.4): turn the 500-node program the generator happened to emit into one
//! a human will actually read.
//!
//! This is the step that decides whether a campaign produces findings or produces noise. A raw
//! generated program is a wall of parenthesised arithmetic; nobody diagnoses a VM bug from it, and
//! `docs/fuzzer_findings.md` says so outright — *un hallazgo sin caso mínimo reproducible no está
//! terminado*.
//!
//! # Structural, not textual
//!
//! Every cut is made on the AST and the result is re-checked with
//! [`JavaProgram::well_formed`](super::gen::JavaProgram::well_formed) before it is even run. That
//! is the payoff of the level-0 decision that a program is a data structure: a line-based shrinker
//! would spend its whole budget producing source that does not compile, and every one of those is
//! a wasted pair of process spawns.
//!
//! Gating on a checker rather than proving each transform safe is deliberate. "Delete a statement"
//! is only valid if nothing later reads what it declared; "drop a parameter" is only valid if the
//! call sites are fixed too. Encoding each of those preconditions is work that has to be redone
//! correctly for every new transform, and getting one subtly wrong produces a *reduced program that
//! does not mean what the original meant* — the worst possible outcome for a bug report. Cutting
//! freely and throwing away whatever stops type-checking costs one cheap function call per
//! candidate and cannot be got subtly wrong.
//!
//! # The transforms, coarsest first
//!
//! | pass | what it removes |
//! |---|---|
//! | [`Pass::DeleteStatement`] | a whole statement, at any depth |
//! | [`Pass::CollapseIf`] | an `if`, replaced by one of its two arms spliced in |
//! | [`Pass::UnrollLoop`] | a `for`, replaced by its body, or its bound walked toward 1 |
//! | [`Pass::ShrinkArray`] | an array's length, walked toward 1 (or toward `-1` if it is negative) |
//! | [`Pass::DropMethod`] | a helper nobody calls (with the renumbering that implies) |
//! | [`Pass::DropParameter`] | a parameter, zeroed inside the method and cut from every call site |
//! | [`Pass::ExprToConstant`] | a subtree, replaced by `0` of its own type |
//! | [`Pass::ShrinkLiteral`] | a constant, walked toward zero |
//!
//! Coarsest first because deleting a statement can remove hundreds of nodes that the expression
//! passes would otherwise chew through one at a time, and each candidate costs **two process
//! spawns**.
//!
//! # Why it terminates, and why it stops early
//!
//! A candidate is only accepted if its [`weight`] — `(node count, total magnitude of every
//! literal)`, compared lexicographically — is strictly smaller than the current best. That order is
//! well-founded, so the loop cannot cycle, no matter what the transforms do.
//!
//! And it finds a **local** minimum, not a global one. That is the lesson level 0 already wrote
//! down in its own reducer test, and it is the right trade: a greedy shrinker that halves a program
//! in twenty predicate evaluations is worth more than a perfect one that needs twenty thousand,
//! because every evaluation is two child processes.
//!
//! # The predicate is not ours
//!
//! [`campaign`](super::campaign) builds it from the same runner and oracle that found the
//! divergence, and hands it in. A reducer with its own idea of "still fails" is the classic way a
//! shrinker turns a real bug into a fictional one — it minimises happily until the program fails
//! for some entirely different reason, and the report points at the wrong thing.

use super::gen::{Block, Cond, Expr, JavaProgram, Method, Stmt, Ty};
use super::Reducer;

/// The order in which cuts are attempted. Public so a test can drive one pass in isolation, and so
/// the table in the module docs has something to point at.
#[derive(Clone, Copy, PartialEq, Eq, Debug)]
pub enum Pass {
    DeleteStatement,
    CollapseIf,
    UnrollLoop,
    ShrinkArray,
    ShrinkWarmup,
    DropMethod,
    DropParameter,
    ExprToConstant,
    ShrinkLiteral,
}

/// Every pass, coarsest first.
pub const PASSES: &[Pass] = &[
    Pass::DeleteStatement,
    Pass::CollapseIf,
    Pass::UnrollLoop,
    Pass::ShrinkArray,
    Pass::ShrinkWarmup,
    Pass::DropMethod,
    Pass::DropParameter,
    Pass::ExprToConstant,
    Pass::ShrinkLiteral,
];

/// How small a program is, for the purpose of deciding that a cut was an improvement.
///
/// Two components, compared in order:
///
/// 1. **node count** — the thing a human notices;
/// 2. **literal magnitude** — so that shrinking `2147483647` to `1` counts as progress even though
///    the tree is exactly the same shape. Without this, [`Pass::ShrinkLiteral`] could never accept
///    anything and a finding would keep its enormous accidental constants forever.
pub fn weight(program: &JavaProgram) -> (usize, u128) {
    (program.size(), literal_mass(program))
}

/// A floating literal's contribution to the mass: its **bit pattern with the sign cleared**.
///
/// Not `abs()`, which cannot answer for `NaN` and `Infinity` — precisely the two the reducer most
/// wants to walk away from. The masked bit pattern is a total order that agrees with magnitude on
/// every finite value, puts `Infinity` above every finite one and `NaN` above that, and puts `0.0`
/// at the bottom. So "shrink a NaN toward zero" is a *decreasing* step in the same well-founded
/// order every other shrink uses, with no special case anywhere.
///
/// It also settles a subtlety: `-0.0` weighs more than `0.0`, so [`Expr::zero`] handing back `0.0`
/// is a step the order accepts and `-0.0` would not have been.
fn fp_mass(bits: u64) -> u128 {
    (bits & !(1u64 << 63)) as u128
}

fn literal_mass(program: &JavaProgram) -> u128 {
    let mut total = 0u128;
    let mut copy = program.clone();
    visit_exprs_mut(&mut copy, &mut |e| match e {
        Expr::IntLit(v) => total += v.unsigned_abs() as u128,
        Expr::LongLit(v) => total += v.unsigned_abs() as u128,
        // A `float`'s bits are widened to the `double` scale first, so that the two kinds of
        // literal are comparable and a program cannot shrink by changing which one it uses.
        Expr::FloatLit(bits) => total += fp_mass((f32::from_bits(*bits) as f64).to_bits()),
        Expr::DoubleLit(bits) => total += fp_mass(*bits),
        _ => {}
    });
    visit_blocks_mut(&mut copy, &mut |block| {
        for stmt in block.iter() {
            match stmt {
                Stmt::For { bound, .. } => total += (*bound).unsigned_abs() as u128,
                // An array length is a literal like any other, and it has to be in the weight or
                // [`Pass::ShrinkArray`] could never be accepted.
                Stmt::NewArray { len, .. } => total += (*len).unsigned_abs() as u128,
                _ => {}
            }
        }
    });
    // The warm-up is a loop bound like any other, and it has to be in the weight or
    // [`Pass::ShrinkWarmup`] could never be accepted.
    total += program.warmup.unsigned_abs() as u128;
    total
}

/// Shrinks a failing program to a local minimum.
#[derive(Clone, Debug)]
pub struct StructuralReducer {
    /// A ceiling on accepted cuts, so a pathological program cannot make a campaign run all night.
    pub max_steps: usize,
    /// Cuts accepted. Together with [`StructuralReducer::candidates_tried`] this is what says
    /// whether the reducer earned its process spawns.
    pub steps: usize,
    /// Candidates handed to the predicate — i.e. pairs of child processes spent.
    pub candidates_tried: usize,
    /// Candidates thrown away by [`JavaProgram::well_formed`] before costing anything. A large
    /// number here is healthy: it is the cheap check doing the expensive check's work.
    pub candidates_rejected_unchecked: usize,
}

impl Default for StructuralReducer {
    fn default() -> StructuralReducer {
        StructuralReducer::new(400)
    }
}

impl StructuralReducer {
    pub fn new(max_steps: usize) -> StructuralReducer {
        StructuralReducer {
            max_steps,
            steps: 0,
            candidates_tried: 0,
            candidates_rejected_unchecked: 0,
        }
    }
}

impl Reducer<JavaProgram> for StructuralReducer {
    fn reduce(
        &mut self,
        program: JavaProgram,
        still_fails: &mut dyn FnMut(&JavaProgram) -> bool,
    ) -> JavaProgram {
        let mut best = program;
        'fixpoint: loop {
            for pass in PASSES {
                for candidate in candidates(&best, *pass) {
                    if self.steps >= self.max_steps {
                        break 'fixpoint;
                    }
                    // Two cheap gates before the two expensive process spawns. Order matters: the
                    // weight comparison is free, and it discards the many transforms that produce
                    // a differently-shaped program of the same size.
                    if weight(&candidate) >= weight(&best) {
                        self.candidates_rejected_unchecked += 1;
                        continue;
                    }
                    if candidate.well_formed().is_err() {
                        self.candidates_rejected_unchecked += 1;
                        continue;
                    }
                    self.candidates_tried += 1;
                    if still_fails(&candidate) {
                        best = candidate;
                        self.steps += 1;
                        // Restart from the coarsest pass: a deletion often unlocks another one.
                        continue 'fixpoint;
                    }
                }
            }
            break;
        }
        best
    }
}

/// Every candidate one pass can produce from `program`. Built eagerly: the list is at most a few
/// thousand small clones, which is nothing next to the process spawns it feeds.
pub fn candidates(program: &JavaProgram, pass: Pass) -> Vec<JavaProgram> {
    match pass {
        Pass::DeleteStatement => statement_edits(program, &|stmt, block, at| {
            let _ = stmt;
            block.remove(at);
            true
        }),
        Pass::CollapseIf => collapse_ifs(program),
        Pass::UnrollLoop => unroll_loops(program),
        Pass::ShrinkArray => shrink_arrays(program),
        Pass::ShrinkWarmup => shrink_warmup(program),
        Pass::DropMethod => drop_methods(program),
        Pass::DropParameter => drop_parameters(program),
        Pass::ExprToConstant => expr_edits(program, &|e| {
            let zero = Expr::zero(e.ty());
            (*e != zero).then_some(vec![zero])
        }),
        Pass::ShrinkLiteral => expr_edits(program, &|e| match e {
            Expr::IntLit(v) => Some(shrink_i32(*v).into_iter().map(Expr::IntLit).collect()),
            Expr::LongLit(v) => Some(shrink_i64(*v).into_iter().map(Expr::LongLit).collect()),
            Expr::FloatLit(bits) => Some(
                shrink_f64(f32::from_bits(*bits) as f64)
                    .into_iter()
                    .map(|v| Expr::FloatLit((v as f32).to_bits()))
                    .collect(),
            ),
            Expr::DoubleLit(bits) => Some(
                shrink_f64(f64::from_bits(*bits))
                    .into_iter()
                    .map(|v| Expr::DoubleLit(v.to_bits()))
                    .collect(),
            ),
            _ => None,
        }),
    }
}

/// The ladder a constant is walked down: straight to zero if the bug tolerates it, then halving,
/// then one step. Halving is what keeps `Integer.MAX_VALUE` from needing two billion predicate
/// evaluations to become `5`.
fn shrink_i32(v: i32) -> Vec<i32> {
    if v == 0 {
        return Vec::new();
    }
    let mut out = vec![0, v / 2, v - v.signum()];
    out.retain(|c| c.unsigned_abs() < v.unsigned_abs());
    out.dedup();
    out
}

/// The ladder a floating constant is walked down. Three rungs, coarsest first: `0.0`, then `1.0`,
/// then halving.
///
/// # Why halving and not the same `v/2, v-1` ladder the integers use
///
/// Because "one step toward zero" has no meaning for a `double` — the step is an ulp, and walking
/// `Double.MAX_VALUE` down an ulp at a time is 2^63 predicate evaluations. Halving is exact in IEEE
/// (it decrements the exponent and leaves the mantissa alone) until the subnormal range, and it
/// reaches `1.0` from either end of the range in about 2000 steps rather than never.
///
/// `NaN` and both infinities have no half, so for them the ladder is just the two constants — which
/// is the right answer anyway: a finding that needs a `NaN` will refuse both rungs and keep it, and
/// a finding that does not will land on `0.0` in one step.
fn shrink_f64(v: f64) -> Vec<f64> {
    if v == 0.0 && v.is_sign_positive() {
        return Vec::new();
    }
    let mut out = vec![0.0, 1.0];
    if v.is_finite() {
        out.push(v * 0.5);
    }
    // The same well-founded order [`weight`] compares on, so a rung that is not an improvement is
    // never offered and the reducer cannot cycle.
    let mass = fp_mass(v.to_bits());
    out.retain(|c| fp_mass(c.to_bits()) < mass);
    out.dedup();
    out
}

fn shrink_i64(v: i64) -> Vec<i64> {
    if v == 0 {
        return Vec::new();
    }
    let mut out = vec![0, v / 2, v - v.signum()];
    out.retain(|c| c.unsigned_abs() < v.unsigned_abs());
    out.dedup();
    out
}

// ---------------------------------------------------------------------------------------------
// The transforms
// ---------------------------------------------------------------------------------------------

/// One candidate per `(block, position)` in the program, with `edit` applied there.
fn statement_edits(
    program: &JavaProgram,
    edit: &dyn Fn(&Stmt, &mut Block, usize) -> bool,
) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for block_index in 0..count_blocks(program) {
        let length = block_length(program, block_index);
        for at in 0..length {
            let mut candidate = program.clone();
            let mut seen = 0;
            let mut applied = false;
            visit_blocks_mut(&mut candidate, &mut |block| {
                if seen == block_index && !applied && at < block.len() {
                    let stmt = block[at].clone();
                    applied = edit(&stmt, block, at);
                }
                seen += 1;
            });
            if applied {
                out.push(candidate);
            }
        }
    }
    out
}

/// An `if` becomes whichever arm is spliced in — or nothing at all, which
/// [`Pass::DeleteStatement`] already covers, so only the two arms are offered here.
fn collapse_ifs(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for take_then in [true, false] {
        out.extend(statement_edits(program, &|stmt, block, at| {
            let Stmt::If { then, otherwise, .. } = stmt else {
                return false;
            };
            let arm = if take_then { then } else { otherwise };
            block.remove(at);
            for (offset, s) in arm.iter().enumerate() {
                block.insert(at + offset, s.clone());
            }
            true
        }));
    }
    out
}

/// A `for` either loses iterations or disappears entirely, leaving its body behind. The second one
/// is only valid when the body never reads the counter — and rather than checking that here,
/// [`JavaProgram::well_formed`] rejects the candidate, which is the whole argument of this module.
fn unroll_loops(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = statement_edits(program, &|stmt, block, at| {
        let Stmt::For { body, .. } = stmt else {
            return false;
        };
        block.remove(at);
        for (offset, s) in body.iter().enumerate() {
            block.insert(at + offset, s.clone());
        }
        true
    });
    for target in [1i32, 2] {
        out.extend(statement_edits(program, &|stmt, block, at| {
            let Stmt::For { var, bound, body } = stmt else {
                return false;
            };
            if *bound <= target {
                return false;
            }
            block[at] = Stmt::For { var: var.clone(), bound: target, body: body.clone() };
            true
        }));
    }
    out
}

/// Walks an array's length toward its own smallest interesting value.
///
/// **Toward `1`, not toward `0`**, for a positive length: a zero-length array makes every index out
/// of range, so a shrink that reached it would silently convert a finding about a *value* into one
/// about [`marks::BOUNDS`](super::gen::marks::BOUNDS) — the classic way a shrinker replaces a real
/// bug with a fictional one. And a **negative** length walks toward `-1` rather than up through
/// zero, because a negative length is not a small array, it is a
/// `NegativeArraySizeException`, and a finding that needs one must be able to keep it.
fn shrink_arrays(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for target in [1i32, 2, -1] {
        out.extend(statement_edits(program, &|stmt, block, at| {
            let Stmt::NewArray { name, elem, len } = stmt else {
                return false;
            };
            // Same sign, strictly smaller magnitude. Crossing zero would change which exception
            // the statement can raise.
            if len.signum() != target.signum() || len.unsigned_abs() <= target.unsigned_abs() {
                return false;
            }
            block[at] =
                Stmt::NewArray { name: name.clone(), elem: *elem, len: target };
            true
        }));
    }
    out
}

/// Walks the warm-up count down toward one.
///
/// A minimal case should not carry a 40-iteration loop it does not need — but it may well need it,
/// because the warm-up is the only reason the JIT compiled anything at all. So this is offered as a
/// candidate like every other cut and the predicate decides: a bug that only appears in compiled
/// code will simply refuse every shrink below the threshold, and the reduced case will say so by
/// keeping its loop.
fn shrink_warmup(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for target in [1i32, 2, program.warmup / 2, program.warmup - 1] {
        if target < 1 || target >= program.warmup {
            continue;
        }
        let mut candidate = program.clone();
        candidate.warmup = target;
        out.push(candidate);
    }
    out
}

/// Removes a helper nobody calls, and renumbers what is left. The emitter writes a call site as
/// `m<index>`, so the name and the position have to stay in step; `well_formed` checks that, which
/// is why this transform can be written plainly instead of carefully.
fn drop_methods(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for victim in 0..program.methods.len() {
        if calls_anywhere(program, victim) {
            continue;
        }
        let mut candidate = program.clone();
        candidate.methods.remove(victim);
        for (position, method) in candidate.methods.iter_mut().enumerate() {
            method.name = format!("m{position}");
        }
        candidate.entry.name = format!("m{}", candidate.methods.len());
        visit_exprs_mut(&mut candidate, &mut |e| {
            if let Expr::Call(index, _, _) = e {
                if *index > victim {
                    *index -= 1;
                }
            }
        });
        out.push(candidate);
    }
    out
}

fn calls_anywhere(program: &JavaProgram, target: usize) -> bool {
    let mut found = false;
    let mut copy = program.clone();
    visit_exprs_mut(&mut copy, &mut |e| {
        if let Expr::Call(index, _, _) = e {
            if *index == target {
                found = true;
            }
        }
    });
    found
}

/// A parameter is zeroed inside its method, then removed, then cut from every call site. Doing all
/// three at once is what makes this a *shrink* rather than a way to produce broken programs: a
/// version that only removed unused parameters would almost never fire, because the generator
/// prefers reading a variable over inventing a constant.
fn drop_parameters(program: &JavaProgram) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for method_index in 0..program.methods.len() {
        for param_index in 0..program.methods[method_index].params.len() {
            let mut candidate = program.clone();
            let (name, ty) = candidate.methods[method_index].params[param_index].clone();
            substitute_var(&mut candidate.methods[method_index], &name, ty);
            candidate.methods[method_index].params.remove(param_index);
            visit_exprs_mut(&mut candidate, &mut |e| {
                if let Expr::Call(index, args, _) = e {
                    if *index == method_index && param_index < args.len() {
                        args.remove(param_index);
                    }
                }
            });
            out.push(candidate);
        }
    }
    out
}

fn substitute_var(method: &mut Method, name: &str, ty: Ty) {
    let mut visit = |e: &mut Expr| {
        if let Expr::Var(var, _) = e {
            if var == name {
                *e = Expr::zero(ty);
            }
        }
    };
    visit_block_exprs(&mut method.body, &mut visit);
    visit_expr(&mut method.result, &mut visit);
    // An assignment *to* the vanished parameter has nowhere to go; drop it. The statement passes
    // would get there eventually, but leaving a dangling name would make every candidate from this
    // pass fail `well_formed` and the pass would look like it does nothing.
    strip_assignments(&mut method.body, name);
}

fn strip_assignments(block: &mut Block, name: &str) {
    block.retain(|stmt| !matches!(stmt, Stmt::Assign { name: target, .. } if target == name));
    for stmt in block.iter_mut() {
        match stmt {
            Stmt::If { then, otherwise, .. } => {
                strip_assignments(then, name);
                strip_assignments(otherwise, name);
            }
            Stmt::For { body, .. } => strip_assignments(body, name),
            _ => {}
        }
    }
}


/// One candidate per `(expression, replacement)` that `replacements` offers.
fn expr_edits(
    program: &JavaProgram,
    replacements: &dyn Fn(&Expr) -> Option<Vec<Expr>>,
) -> Vec<JavaProgram> {
    let mut out = Vec::new();
    for index in 0..count_exprs(program) {
        let Some(original) = nth_expr(program, index) else {
            break;
        };
        let Some(options) = replacements(&original) else {
            continue;
        };
        for option in options {
            if option == original {
                continue;
            }
            let mut candidate = program.clone();
            let mut seen = 0;
            let mut applied = false;
            visit_exprs_mut(&mut candidate, &mut |e| {
                if seen == index && !applied {
                    *e = option.clone();
                    applied = true;
                }
                seen += 1;
            });
            if applied {
                out.push(candidate);
            }
        }
    }
    out
}

// ---------------------------------------------------------------------------------------------
// Traversal
// ---------------------------------------------------------------------------------------------

fn count_exprs(program: &JavaProgram) -> usize {
    let mut n = 0;
    let mut copy = program.clone();
    visit_exprs_mut(&mut copy, &mut |_| n += 1);
    n
}

fn nth_expr(program: &JavaProgram, index: usize) -> Option<Expr> {
    let mut seen = 0;
    let mut found = None;
    let mut copy = program.clone();
    visit_exprs_mut(&mut copy, &mut |e| {
        if seen == index {
            found = Some(e.clone());
        }
        seen += 1;
    });
    found
}

fn count_blocks(program: &JavaProgram) -> usize {
    let mut n = 0;
    let mut copy = program.clone();
    visit_blocks_mut(&mut copy, &mut |_| n += 1);
    n
}

fn block_length(program: &JavaProgram, index: usize) -> usize {
    let mut seen = 0;
    let mut length = 0;
    let mut copy = program.clone();
    visit_blocks_mut(&mut copy, &mut |block| {
        if seen == index {
            length = block.len();
        }
        seen += 1;
    });
    length
}

/// Pre-order over every expression in the program, including the ones inside conditions.
///
/// Pre-order and not post-order on purpose: a transform replaces the node it is handed and the walk
/// then descends into the *replacement*. That is harmless — the caller has already stopped editing
/// — and it keeps the numbering that [`nth_expr`] and [`expr_edits`] share identical between the
/// counting pass and the editing pass, which is the only property that has to hold.
fn visit_exprs_mut(program: &mut JavaProgram, f: &mut dyn FnMut(&mut Expr)) {
    for method in &mut program.methods {
        visit_block_exprs(&mut method.body, f);
        visit_expr(&mut method.result, f);
    }
    visit_block_exprs(&mut program.entry.body, f);
    visit_expr(&mut program.entry.result, f);
}

fn visit_block_exprs(block: &mut Block, f: &mut dyn FnMut(&mut Expr)) {
    for stmt in block.iter_mut() {
        match stmt {
            Stmt::Declare { init, .. } => visit_expr(init, f),
            Stmt::Assign { expr, .. } => visit_expr(expr, f),
            Stmt::If { cond, then, otherwise } => {
                visit_cond(cond, f);
                visit_block_exprs(then, f);
                visit_block_exprs(otherwise, f);
            }
            Stmt::For { body, .. } => visit_block_exprs(body, f),
            Stmt::NewArray { .. } => {}
            Stmt::ArrayStore { index, value, .. } => {
                visit_expr(index, f);
                visit_expr(value, f);
            }
        }
    }
}

fn visit_expr(expr: &mut Expr, f: &mut dyn FnMut(&mut Expr)) {
    f(expr);
    match expr {
        Expr::IntLit(_)
        | Expr::LongLit(_)
        | Expr::FloatLit(_)
        | Expr::DoubleLit(_)
        | Expr::ArrayLength(_)
        | Expr::Var(_, _) => {}
        Expr::Neg(a)
        | Expr::Not(a)
        | Expr::Cast(_, a)
        | Expr::Classify(a)
        | Expr::ArrayLoad(_, _, a) => visit_expr(a, f),
        Expr::Bin(_, a, b) | Expr::Shift(_, a, b) => {
            visit_expr(a, f);
            visit_expr(b, f);
        }
        Expr::Ternary(c, a, b) => {
            visit_cond(c, f);
            visit_expr(a, f);
            visit_expr(b, f);
        }
        Expr::Call(_, args, _) => {
            for arg in args.iter_mut() {
                visit_expr(arg, f);
            }
        }
    }
}

fn visit_cond(cond: &mut Cond, f: &mut dyn FnMut(&mut Expr)) {
    match cond {
        Cond::Cmp(_, a, b) => {
            visit_expr(a, f);
            visit_expr(b, f);
        }
        Cond::And(a, b) | Cond::Or(a, b) => {
            visit_cond(a, f);
            visit_cond(b, f);
        }
        Cond::Not(a) => visit_cond(a, f),
    }
}

/// Every block in the program, outermost first, in a stable order.
fn visit_blocks_mut(program: &mut JavaProgram, f: &mut dyn FnMut(&mut Block)) {
    for method in &mut program.methods {
        visit_block(&mut method.body, f);
    }
    visit_block(&mut program.entry.body, f);
}

fn visit_block(block: &mut Block, f: &mut dyn FnMut(&mut Block)) {
    f(block);
    for stmt in block.iter_mut() {
        match stmt {
            Stmt::If { then, otherwise, .. } => {
                visit_block(then, f);
                visit_block(otherwise, f);
            }
            Stmt::For { body, .. } => visit_block(body, f),
            _ => {}
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::fuzz::gen::{BinOp, JavaGenerator, Malformed, ShiftOp};
    use crate::fuzz::{Generator as _, Program as _, Seed};

    fn program(seed: u64) -> JavaProgram {
        JavaGenerator::default().generate(Seed(seed))
    }

    /// Walks the AST looking for the shape a planted bug is defined by. The predicates below are
    /// structural rather than textual so that a shrink cannot satisfy them by accident — `"m3"` is
    /// a substring of a lot of things, `Expr::Call(3, …)` is not.
    fn contains_op(program: &JavaProgram, wanted: BinOp) -> bool {
        let mut found = false;
        let mut copy = program.clone();
        visit_exprs_mut(&mut copy, &mut |e| {
            if let Expr::Bin(op, _, _) = e {
                if *op == wanted {
                    found = true;
                }
            }
        });
        found
    }

    fn contains_shift(program: &JavaProgram, wanted: ShiftOp) -> bool {
        let mut found = false;
        let mut copy = program.clone();
        visit_exprs_mut(&mut copy, &mut |e| {
            if let Expr::Shift(op, _, _) = e {
                if *op == wanted {
                    found = true;
                }
            }
        });
        found
    }

    fn largest_literal(program: &JavaProgram) -> u64 {
        let mut biggest = 0u64;
        let mut copy = program.clone();
        visit_exprs_mut(&mut copy, &mut |e| match e {
            Expr::IntLit(v) => biggest = biggest.max(v.unsigned_abs() as u64),
            Expr::LongLit(v) => biggest = biggest.max(v.unsigned_abs()),
            _ => {}
        });
        biggest
    }

    /// A seed whose program contains the shape a test wants to plant a bug on.
    fn seed_containing(predicate: impl Fn(&JavaProgram) -> bool) -> JavaProgram {
        (0..200)
            .map(program)
            .find(|p| predicate(p))
            .expect("200 seeds must produce at least one program with the wanted shape")
    }

    #[test]
    fn a_planted_operator_bug_shrinks_to_almost_nothing() {
        // The plant: "the two paths disagree whenever the program divides". A real reducer must
        // strip away everything that is not a division and leave a case a human can read.
        let original = seed_containing(|p| contains_op(p, BinOp::Div));
        let before = original.size();
        let mut reducer = StructuralReducer::default();
        let mut predicate = |p: &JavaProgram| contains_op(p, BinOp::Div);
        let reduced = reducer.reduce(original, &mut predicate);

        assert!(contains_op(&reduced, BinOp::Div), "the shrink must preserve the bug");
        assert!(reduced.well_formed().is_ok(), "a reduced program must still be Java");
        // Absolute *and* relative. `seed_containing` takes the first seed with the wanted shape,
        // so the starting size is an accident of the RNG and a pure ratio would be a test of that
        // accident; what a finding needs is a case small enough that somebody reads it.
        assert!(
            reduced.size() <= 20 && reduced.size() * 4 < before,
            "{} nodes down to {} is not a reduction worth reporting",
            before,
            reduced.size()
        );
        assert!(
            reducer.steps > 5,
            "it accepted only {} cuts — the passes are not firing",
            reducer.steps
        );
    }

    #[test]
    fn a_planted_shift_bug_shrinks_and_keeps_its_shape() {
        // `-1 >>> 1` is one of the bugs this project found by hand, so `>>>` is a realistic plant.
        let original = seed_containing(|p| contains_shift(p, ShiftOp::Unsigned));
        let before = original.size();
        let mut reducer = StructuralReducer::default();
        let mut predicate = |p: &JavaProgram| contains_shift(p, ShiftOp::Unsigned);
        let reduced = reducer.reduce(original, &mut predicate);

        assert!(contains_shift(&reduced, ShiftOp::Unsigned));
        assert!(reduced.well_formed().is_ok());
        // Absolute, not just a ratio. `seed_containing` takes the *first* seed with the wanted
        // shape, so how big the original happened to be is an accident of the RNG; what a finding
        // needs is a case small enough that somebody reads it.
        assert!(
            reduced.size() <= 20 && reduced.size() * 4 < before,
            "{before} nodes down to {} is not a case a human will read",
            reduced.size()
        );
    }

    #[test]
    fn a_planted_threshold_walks_the_constant_down_to_the_edge() {
        // The other half of shrinking: not just fewer nodes but *smaller numbers*. Without the
        // literal-magnitude half of `weight`, a finding would keep whatever accidental
        // `2147483647` the generator happened to draw.
        let original = seed_containing(|p| largest_literal(p) >= 1000);
        let mut reducer = StructuralReducer::default();
        let mut predicate = |p: &JavaProgram| largest_literal(p) >= 5;
        let reduced = reducer.reduce(original, &mut predicate);

        assert!(largest_literal(&reduced) >= 5, "the shrink must preserve the bug");
        assert_eq!(
            largest_literal(&reduced),
            5,
            "halving then stepping should land exactly on the threshold, got {}",
            largest_literal(&reduced)
        );
    }

    #[test]
    fn a_predicate_nothing_satisfies_leaves_the_program_alone() {
        // The control. A reducer that shrinks when the predicate never holds is one that has quietly
        // stopped consulting it — and would report a minimal program that does not reproduce.
        let original = program(4);
        let before = original.clone();
        let mut reducer = StructuralReducer::default();
        let reduced = reducer.reduce(original, &mut |_| false);
        assert_eq!(reduced, before, "no candidate held, so nothing may be cut");
        assert_eq!(reducer.steps, 0);
        assert!(reducer.candidates_tried > 0, "it must at least have asked");
    }

    #[test]
    fn a_predicate_everything_satisfies_reaches_the_floor() {
        // With nothing to preserve, the reducer should arrive at the smallest program the grammar
        // can express: an entry method that returns a constant, and nothing else.
        let mut reducer = StructuralReducer::new(10_000);
        let reduced = reducer.reduce(program(11), &mut |_| true);
        assert!(reduced.methods.is_empty(), "every helper was droppable");
        assert!(reduced.entry.body.is_empty(), "every statement was droppable");
        assert_eq!(reduced.entry.result, Expr::IntLit(0));
        assert!(reduced.well_formed().is_ok());
        let source = reduced.to_java();
        assert!(source.contains("static int m0() {"), "{source}");
        assert!(source.contains("return 0;"), "{source}");
    }

    #[test]
    fn every_pass_produces_a_healthy_share_of_valid_candidates() {
        // The claim this module rests on is *not* that candidates are always well-formed — they are
        // not, and `well_formed` is precisely the gate that makes cutting freely safe. The claim is
        // that a healthy share survives; a pass whose candidates never type-check is a pass that
        // only burns process spawns.
        //
        // Measured in aggregate rather than per seed, because a small program can legitimately have
        // one candidate and have it be invalid — deleting the only declaration a later statement
        // reads. That is the checker doing its job, not the pass failing.
        for pass in PASSES {
            let (mut total, mut good) = (0usize, 0usize);
            for seed in 0..30 {
                for candidate in candidates(&program(seed), *pass) {
                    total += 1;
                    good += usize::from(candidate.well_formed().is_ok());
                }
            }
            assert!(total > 0, "{pass:?} produced no candidates at all across 30 seeds");
            assert!(
                good * 4 >= total,
                "{pass:?}: only {good} of {total} candidates type-check — the pass is mostly waste"
            );
        }
    }

    #[test]
    fn dropping_a_parameter_fixes_every_call_site() {
        let p = seed_containing(|p| {
            p.methods.iter().any(|m| !m.params.is_empty())
                && p.methods.iter().enumerate().any(|(i, _)| {
                    let mut called = false;
                    let mut copy = p.clone();
                    visit_exprs_mut(&mut copy, &mut |e| {
                        if let Expr::Call(index, _, _) = e {
                            if *index == i {
                                called = true;
                            }
                        }
                    });
                    called
                })
        });
        let all = candidates(&p, Pass::DropParameter);
        assert!(!all.is_empty());
        for candidate in &all {
            // A candidate whose call sites were not fixed would fail exactly here — which is the
            // point of having the checker rather than trusting the transform.
            if let Err(Malformed::BadArguments(index)) = candidate.well_formed() {
                panic!("a call to m{index} kept an argument the method no longer takes");
            }
        }
    }

    #[test]
    fn dropping_a_method_renumbers_what_is_left() {
        let p = seed_containing(|p| p.methods.len() >= 2);
        for candidate in candidates(&p, Pass::DropMethod) {
            assert!(candidate.methods.len() < p.methods.len());
            assert!(
                !matches!(candidate.well_formed(), Err(Malformed::MisnamedMethod { .. })),
                "the emitter writes call sites as m<index>, so names must follow positions"
            );
        }
    }

    #[test]
    fn the_step_ceiling_is_honoured() {
        let mut reducer = StructuralReducer::new(3);
        let reduced = reducer.reduce(program(11), &mut |_| true);
        assert_eq!(reducer.steps, 3, "a campaign must not be able to run all night on one finding");
        assert!(reduced.well_formed().is_ok(), "stopping early must still leave a valid program");
    }

    #[test]
    fn weight_is_a_well_founded_order() {
        // Why the loop cannot cycle: every accepted candidate is strictly smaller in this order,
        // and the order is bounded below.
        let p = program(6);
        let (nodes, mass) = weight(&p);
        assert!(nodes > 0);
        let smaller = candidates(&p, Pass::DeleteStatement);
        for candidate in smaller {
            assert!(weight(&candidate) < (nodes, mass), "deletion must always shrink");
        }
    }

    #[test]
    fn shrinking_a_constant_never_grows_it() {
        for v in [i32::MIN, -1000, -1, 0, 1, 7, 255, i32::MAX] {
            for candidate in shrink_i32(v) {
                assert!(
                    candidate.unsigned_abs() < v.unsigned_abs(),
                    "{v} offered {candidate}, which is not smaller"
                );
            }
        }
        for v in [i64::MIN, -1, 0, 1, i64::MAX] {
            for candidate in shrink_i64(v) {
                assert!(candidate.unsigned_abs() < v.unsigned_abs());
            }
        }
        assert!(shrink_i32(0).is_empty(), "zero has nowhere left to go");
    }

    #[test]
    fn the_reduced_program_still_serves_both_sides() {
        // A minimal case is only useful if it can still be run down both paths — `run-headless`
        // calls `run`, `java` calls `main`.
        let mut reducer = StructuralReducer::new(10_000);
        let reduced = reducer.reduce(program(2), &mut |_| true);
        let source = reduced.to_java();
        assert!(source.contains("static int run() {"));
        assert!(source.contains("public static void main(String[] a)"));
    }
}

#[cfg(test)]
mod demo {
    //! What a reduction actually looks like, printed. Not an assertion — the assertions live in
    //! [`super::tests`] — but a finding is only worth writing up if the case fits on a screen, and
    //! this is how that claim gets checked by eye rather than by adjective.
    //!
    //! `cargo test --release --lib fuzz::reduce::demo -- --ignored --nocapture`

    use super::*;
    use crate::fuzz::gen::{BinOp, JavaGenerator};
    use crate::fuzz::{Generator as _, Program as _, Seed};

    #[test]
    #[ignore]
    fn show_a_reduction() {
        let mut generator = JavaGenerator::default();
        let original = (0..200)
            .map(|s| generator.generate(Seed(s)))
            .find(|p| {
                let mut found = false;
                let mut copy = p.clone();
                visit_exprs_mut(&mut copy, &mut |e| {
                    if matches!(e, Expr::Bin(BinOp::Rem, _, _)) {
                        found = true;
                    }
                });
                found
            })
            .expect("some seed uses `%`");

        let before = original.clone();
        let mut reducer = StructuralReducer::new(10_000);
        let reduced = reducer.reduce(original, &mut |p| {
            let mut found = false;
            let mut copy = p.clone();
            visit_exprs_mut(&mut copy, &mut |e| {
                if matches!(e, Expr::Bin(BinOp::Rem, _, _)) {
                    found = true;
                }
            });
            found
        });

        println!(
            "{} nodes -> {} nodes in {} accepted cuts ({} candidates evaluated, {} rejected free)",
            before.size(),
            reduced.size(),
            reducer.steps,
            reducer.candidates_tried,
            reducer.candidates_rejected_unchecked
        );
        println!("==== before ====\n{}", before.to_java());
        println!("==== after ====\n{}", reduced.to_java());
    }
}
