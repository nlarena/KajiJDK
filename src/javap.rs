//! `javap`: a faithful reimplementation of the `javap` tool. Like a `main`, this
//! file only routes to the right dump; the actual renderers live in the
//! `printers` module: `Brief` (default) and `Verbose` (`-v`).

use crate::jvm::class_file::ClassFile;
use crate::jvm::parser::printers::{Brief, Verbose, Visibility};

pub struct Javap<'a> {
    cf: &'a ClassFile,
    path: &'a str,
}

impl<'a> Javap<'a> {
    pub fn new(cf: &'a ClassFile, path: &'a str) -> Self {
        Self { cf, path }
    }

    /// Mimics the `javap` CLI: brief listing by default, full verbose dump
    /// (constant pool + bytecode + line numbers) when `verbose` (`-v`). The
    /// `visibility` filter (`-public`/`-protected`/`-package`/`-private`)
    /// applies to both modes.
    pub fn run(&self, verbose: bool, visibility: Visibility) {
        if verbose {
            Verbose::new(self.cf, self.path, visibility).print();
        } else {
            Brief::new(self.cf, visibility).print();
        }
    }
}
