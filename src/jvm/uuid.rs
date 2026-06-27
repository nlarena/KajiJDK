//! A tiny, dependency-free **UUID version 4** (random) generator.
//!
//! We roll our own — like everything else in this project — instead of pulling in
//! the `uuid`/`rand` crates. The one design rule that matters: the PRNG state is
//! **persistent and seeded exactly once**. Reseeding from the clock on every call
//! is the classic cause of "UUID collisions" (in a tight loop the clock barely
//! moves → same seed → same value). Holding the state across calls sidesteps that.
//!
//! The randomness is *pseudo*-random (SplitMix64), good enough for distinct object
//! identities in a teaching JVM — not for cryptography or distributed uniqueness.

use std::time::{SystemTime, UNIX_EPOCH};

/// A source of UUID v4 strings, backed by a persistent SplitMix64 state.
pub struct UuidGenerator {
    /// The PRNG state — advanced on every draw, seeded once in [`UuidGenerator::new`].
    state: u64,
}

impl UuidGenerator {
    /// Seeds the generator **once** from the system clock. From here on it never
    /// touches the clock again — each UUID comes from advancing the PRNG state.
    pub fn new() -> Self {
        let nanos = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);
        // Mix the raw clock value so a near-zero seed still scrambles well.
        UuidGenerator { state: nanos ^ 0x9E37_79B9_7F4A_7C15 }
    }

    /// SplitMix64: advance the state and return the next 64 pseudo-random bits.
    fn next_u64(&mut self) -> u64 {
        self.state = self.state.wrapping_add(0x9E37_79B9_7F4A_7C15);
        let mut z = self.state;
        z = (z ^ (z >> 30)).wrapping_mul(0xBF58_476D_1CE4_E5B9);
        z = (z ^ (z >> 27)).wrapping_mul(0x94D0_49BB_1331_11EB);
        z ^ (z >> 31)
    }

    /// Draws the next UUID v4 as the canonical hyphenated hex string
    /// (`xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx`).
    pub fn next(&mut self) -> String {
        let mut bytes = [0u8; 16];
        bytes[..8].copy_from_slice(&self.next_u64().to_be_bytes());
        bytes[8..].copy_from_slice(&self.next_u64().to_be_bytes());

        // Stamp the version/variant the way RFC 4122 v4 prescribes:
        //  - byte 6, high nibble = 0100  → version 4
        //  - byte 8, high two bits = 10  → RFC 4122 variant
        bytes[6] = (bytes[6] & 0x0F) | 0x40;
        bytes[8] = (bytes[8] & 0x3F) | 0x80;

        format_uuid(&bytes)
    }
}

impl Default for UuidGenerator {
    fn default() -> Self {
        Self::new()
    }
}

/// Renders 16 bytes as the canonical `8-4-4-4-12` lowercase-hex UUID string.
fn format_uuid(bytes: &[u8; 16]) -> String {
    let mut s = String::with_capacity(36);
    for (i, byte) in bytes.iter().enumerate() {
        if matches!(i, 4 | 6 | 8 | 10) {
            s.push('-');
        }
        s.push_str(&format!("{byte:02x}"));
    }
    s
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn has_the_canonical_v4_shape() {
        let mut gen = UuidGenerator::new();
        let id = gen.next();
        // 36 chars: 32 hex + 4 hyphens, hyphens at 8-13-18-23.
        assert_eq!(id.len(), 36);
        let hyphens: Vec<usize> = id.match_indices('-').map(|(i, _)| i).collect();
        assert_eq!(hyphens, vec![8, 13, 18, 23]);
        // Version nibble is '4', variant nibble is one of 8/9/a/b.
        assert_eq!(id.as_bytes()[14], b'4');
        assert!(matches!(id.as_bytes()[19], b'8' | b'9' | b'a' | b'b'));
    }

    #[test]
    fn consecutive_draws_differ() {
        let mut gen = UuidGenerator::new();
        let a = gen.next();
        let b = gen.next();
        assert_ne!(a, b);
    }
}
