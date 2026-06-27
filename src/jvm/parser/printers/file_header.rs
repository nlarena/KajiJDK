//! The 4-line file-metadata block javap prints before the class declaration:
//! `Classfile <path>`, `Last modified …; size … bytes`, `SHA-256 checksum …`,
//! and `Compiled from "X.java"`.

use sha2::{Digest, Sha256};

use super::dump_common;
use crate::jvm::class_file::ClassFile;

/// Prints the file-metadata header. Reads the `.class` off disk for its size,
/// modification time and SHA-256 — best-effort, silently skipped if unreadable.
pub fn print(cf: &ClassFile, path: &str) {
    crate::pln!("Classfile {}", clean_path(path));
    if let Ok(bytes) = std::fs::read(path) {
        let modified = std::fs::metadata(path)
            .ok()
            .and_then(|m| m.modified().ok())
            .and_then(|t| t.duration_since(std::time::UNIX_EPOCH).ok())
            .map(|d| format_date(d.as_secs() as i64))
            .unwrap_or_else(|| "unknown".to_string());
        crate::pln!("  Last modified {}; size {} bytes", modified, bytes.len());
        let hex: String = Sha256::digest(&bytes).iter().map(|b| format!("{b:02x}")).collect();
        crate::pln!("  SHA-256 checksum {hex}");
    }
    if let Some(src) = dump_common::source_file(cf) {
        crate::pln!("  Compiled from \"{src}\"");
    }
}

/// Cleans a Windows canonical path to javap's `/D:/.../X.class` style.
fn clean_path(path: &str) -> String {
    match std::fs::canonicalize(path) {
        Ok(p) => {
            let s = p.to_string_lossy().replace('\\', "/");
            let s = s.strip_prefix("//?/").unwrap_or(&s).to_string();
            if s.as_bytes().get(1) == Some(&b':') {
                format!("/{s}") // leading slash before the drive, as javap shows
            } else {
                s
            }
        }
        Err(_) => path.to_string(),
    }
}

/// Formats epoch seconds as javap-style `D mon YYYY` (UTC, Spanish month abbr.).
fn format_date(secs: i64) -> String {
    const MONTHS: [&str; 12] = [
        "ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic",
    ];
    let (y, m, d) = ymd_from_epoch(secs);
    format!("{} {} {}", d, MONTHS[(m - 1) as usize], y)
}

/// Civil date (year, month, day) from Unix epoch seconds (UTC). Howard Hinnant's
/// `civil_from_days` algorithm.
fn ymd_from_epoch(secs: i64) -> (i64, i64, i64) {
    let days = secs.div_euclid(86_400);
    let z = days + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097;
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let y = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let d = doy - (153 * mp + 2) / 5 + 1;
    let m = if mp < 10 { mp + 3 } else { mp - 9 };
    let year = if m <= 2 { y + 1 } else { y };
    (year, m, d)
}
