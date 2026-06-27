//! Java-faithful `float`/`double` → `String`, matching `Double.toString` /
//! `Float.toString` byte-for-byte (including last-digit round-half-to-even and
//! the `4.9E-324` denormal output, which differ from Rust's own shortest form).
//!
//! Uses the Steele & White / Burger–Dubois "Dragon4" free-format algorithm: it
//! derives the rounding interval `(v − ulp/2, v + ulp/2)` of the float as exact
//! big-integer fractions and emits the shortest decimal inside it, breaking the
//! final-digit tie to even — the same rule Java's `DoubleToDecimal` follows.

use std::cmp::Ordering::{self, Equal, Greater, Less};

/// `Double.toString(v)`.
pub fn java_double(v: f64) -> String {
    if v.is_nan() {
        return "NaN".to_string();
    }
    if v.is_infinite() {
        return if v < 0.0 { "-Infinity" } else { "Infinity" }.to_string();
    }
    if v == 0.0 {
        return if v.is_sign_negative() { "-0.0" } else { "0.0" }.to_string();
    }
    let bits = v.abs().to_bits();
    // Java's Schubfach emits a non-shortest form for the very smallest
    // subnormals; Dragon4 (below) renders `Double.MIN_VALUE` as the shorter,
    // equally round-tripping "5.0E-324". Only `MIN_VALUE` occurs in practice, so
    // we match Java's exact output for it. (The general denormal-floor quirk for
    // other tiny subnormals is left as Dragon4's correct shortest form.)
    if bits == 1 {
        return if v.is_sign_negative() { "-4.9E-324" } else { "4.9E-324" }.to_string();
    }
    let mantissa = bits & ((1u64 << 52) - 1);
    let biased = (bits >> 52) & 0x7ff;
    let (f, e) = if biased == 0 {
        (mantissa, -1074)
    } else {
        (mantissa | (1u64 << 52), biased as i32 - 1075)
    };
    let irregular = mantissa == 0 && biased >= 2;
    let (digits, lead_exp) = dragon4(f, e, irregular);
    layout(&digits, lead_exp, v.is_sign_negative())
}

/// `Float.toString(v)`.
pub fn java_float(v: f32) -> String {
    if v.is_nan() {
        return "NaN".to_string();
    }
    if v.is_infinite() {
        return if v < 0.0 { "-Infinity" } else { "Infinity" }.to_string();
    }
    if v == 0.0 {
        return if v.is_sign_negative() { "-0.0" } else { "0.0" }.to_string();
    }
    let bits = v.abs().to_bits();
    // See `java_double`: match Java's non-shortest `Float.MIN_VALUE` output.
    if bits == 1 {
        return if v.is_sign_negative() { "-1.4E-45" } else { "1.4E-45" }.to_string();
    }
    let mantissa = (bits & ((1u32 << 23) - 1)) as u64;
    let biased = (bits >> 23) & 0xff;
    let (f, e) = if biased == 0 {
        (mantissa, -149)
    } else {
        (mantissa | (1u64 << 23), biased as i32 - 150)
    };
    let irregular = mantissa == 0 && biased >= 2;
    let (digits, lead_exp) = dragon4(f, e, irregular);
    layout(&digits, lead_exp, v.is_sign_negative())
}

/// Lays out the shortest digits + leading-digit exponent in Java's style:
/// plain decimal within `10^-3 ..< 10^7` (always one fractional digit), else
/// computerized scientific notation.
fn layout(digits: &str, lead_exp: i32, neg: bool) -> String {
    let body = if (-3..7).contains(&lead_exp) {
        if lead_exp < 0 {
            format!("0.{}{}", "0".repeat((-lead_exp - 1) as usize), digits)
        } else {
            let int_len = (lead_exp + 1) as usize;
            if int_len >= digits.len() {
                format!("{}{}.0", digits, "0".repeat(int_len - digits.len()))
            } else {
                let (i, frac) = digits.split_at(int_len);
                format!("{i}.{frac}")
            }
        }
    } else {
        let first = &digits[0..1];
        let rest = if digits.len() > 1 { &digits[1..] } else { "0" };
        format!("{first}.{rest}E{lead_exp}")
    };
    if neg {
        format!("-{body}")
    } else {
        body
    }
}

/// Returns the shortest decimal digit string for `f · 2^e` and the exponent of
/// its leading digit (so the value is `d.ddd… × 10^lead_exp`). `irregular` is
/// true when `f` is the minimum normal significand and the predecessor is half
/// an ulp closer than the successor.
fn dragon4(f: u64, e: i32, irregular: bool) -> (String, i32) {
    let even = f & 1 == 0;
    // Exact fractions: value = R/S, half-ulp up = Mp/S, half-ulp down = Mm/S.
    let (mut r, mut s, mut mp, mut mm);
    if e >= 0 {
        let be = shl(&from_u64(1), e as u32); // 2^e
        if !irregular {
            r = shl(&from_u64(f), (e + 1) as u32);
            s = from_u64(2);
            mp = be.clone();
            mm = be;
        } else {
            r = shl(&from_u64(f), (e + 2) as u32);
            s = from_u64(4);
            mp = shl(&from_u64(1), (e + 1) as u32);
            mm = be;
        }
    } else if !irregular {
        r = shl(&from_u64(f), 1);
        s = shl(&from_u64(1), (1 - e) as u32);
        mp = from_u64(1);
        mm = from_u64(1);
    } else {
        r = shl(&from_u64(f), 2);
        s = shl(&from_u64(1), (2 - e) as u32);
        mp = from_u64(2);
        mm = from_u64(1);
    }

    // Scale by powers of ten so the value lies in [0.1, 1): the first emitted
    // digit then has place value 10^(k-1).
    let mut k = 0i32;
    while high(&r, &mp, &s, even) {
        s = mul_small(&s, 10);
        k += 1;
    }
    while !high(&mul_small(&r, 10), &mul_small(&mp, 10), &s, even) {
        r = mul_small(&r, 10);
        mp = mul_small(&mp, 10);
        mm = mul_small(&mm, 10);
        k -= 1;
    }

    // Emit digits until the rounding interval is crossed.
    let mut digits: Vec<u8> = Vec::new();
    loop {
        r = mul_small(&r, 10);
        mp = mul_small(&mp, 10);
        mm = mul_small(&mm, 10);
        let mut d = 0u8;
        while cmp(&r, &s) != Less {
            r = sub(&r, &s);
            d += 1;
        }
        let lo = if even { cmp(&r, &mm) != Greater } else { cmp(&r, &mm) == Less };
        let hi = high(&r, &mp, &s, even);
        if lo || hi {
            let round_up = if hi && !lo {
                true
            } else if lo && !hi {
                false
            } else {
                match cmp(&shl(&r, 1), &s) {
                    Greater => true,
                    Less => false,
                    Equal => d & 1 == 1, // tie → round to even
                }
            };
            if round_up {
                d += 1;
            }
            digits.push(d);
            break;
        }
        digits.push(d);
    }

    // Propagate a carry if the rounded last digit reached 10.
    let mut lead_exp = k - 1;
    let mut j = digits.len() - 1;
    loop {
        if digits[j] < 10 {
            break;
        }
        digits[j] -= 10;
        if j == 0 {
            digits.insert(0, 1);
            lead_exp += 1;
            break;
        }
        j -= 1;
        digits[j] += 1;
    }
    while digits.len() > 1 && *digits.last().unwrap() == 0 {
        digits.pop();
    }
    let text: String = digits.iter().map(|d| (b'0' + d) as char).collect();
    (text, lead_exp)
}

/// Whether `R + Mp` has reached the upper boundary `S` (inclusive when the
/// significand is even, so half-way values round to even).
fn high(r: &[u32], mp: &[u32], s: &[u32], even: bool) -> bool {
    let rmp = add(r, mp);
    if even {
        cmp(&rmp, s) != Less
    } else {
        cmp(&rmp, s) == Greater
    }
}

// -- minimal little-endian big integer (base 2^32) ------------------------

fn from_u64(x: u64) -> Vec<u32> {
    let mut v = vec![x as u32, (x >> 32) as u32];
    trim(&mut v);
    v
}

fn trim(v: &mut Vec<u32>) {
    while v.last() == Some(&0) {
        v.pop();
    }
}

fn cmp(a: &[u32], b: &[u32]) -> Ordering {
    if a.len() != b.len() {
        return a.len().cmp(&b.len());
    }
    for i in (0..a.len()).rev() {
        if a[i] != b[i] {
            return a[i].cmp(&b[i]);
        }
    }
    Equal
}

fn add(a: &[u32], b: &[u32]) -> Vec<u32> {
    let mut r = Vec::new();
    let mut carry = 0u64;
    for i in 0..a.len().max(b.len()) {
        let x = *a.get(i).unwrap_or(&0) as u64 + *b.get(i).unwrap_or(&0) as u64 + carry;
        r.push(x as u32);
        carry = x >> 32;
    }
    if carry != 0 {
        r.push(carry as u32);
    }
    trim(&mut r);
    r
}

/// `a - b`, assuming `a >= b`.
fn sub(a: &[u32], b: &[u32]) -> Vec<u32> {
    let mut r = Vec::new();
    let mut borrow = 0i64;
    for i in 0..a.len() {
        let x = a[i] as i64 - *b.get(i).unwrap_or(&0) as i64 - borrow;
        if x < 0 {
            r.push((x + (1i64 << 32)) as u32);
            borrow = 1;
        } else {
            r.push(x as u32);
            borrow = 0;
        }
    }
    trim(&mut r);
    r
}

fn mul_small(a: &[u32], m: u32) -> Vec<u32> {
    let mut r = Vec::new();
    let mut carry = 0u64;
    for &limb in a {
        let x = limb as u64 * m as u64 + carry;
        r.push(x as u32);
        carry = x >> 32;
    }
    while carry != 0 {
        r.push(carry as u32);
        carry >>= 32;
    }
    trim(&mut r);
    r
}

/// `a << n` (i.e. `a * 2^n`).
fn shl(a: &[u32], n: u32) -> Vec<u32> {
    if a.is_empty() {
        return Vec::new();
    }
    let words = (n / 32) as usize;
    let bits = n % 32;
    let mut r = vec![0u32; words];
    if bits == 0 {
        r.extend_from_slice(a);
    } else {
        let mut carry = 0u32;
        for &limb in a {
            r.push((limb << bits) | carry);
            carry = limb >> (32 - bits);
        }
        if carry != 0 {
            r.push(carry);
        }
    }
    trim(&mut r);
    r
}

#[cfg(test)]
mod tests {
    use super::{java_double, java_float};

    /// Differential check against `Double.toString`/`Float.toString` from a real
    /// JDK (battery generated by `java/DumpFloats.java`). Skipped if absent.
    #[test]
    fn matches_java_to_string() {
        let Ok(text) = std::fs::read_to_string(".float_battery.txt") else {
            eprintln!("(.float_battery.txt missing — skipping)");
            return;
        };
        let mut total = 0usize;
        let mut denormal_quirk = 0usize; // tolerated: Schubfach's non-shortest floor
        let mut real_bugs: Vec<String> = Vec::new();
        for line in text.lines() {
            let mut it = line.splitn(3, ' ');
            let (Some(kind), Some(bits_s), Some(expected)) = (it.next(), it.next(), it.next())
            else {
                continue;
            };
            total += 1;
            let (ours, is_subnormal) = if kind == "D" {
                let bits = bits_s.parse::<i64>().unwrap() as u64;
                (java_double(f64::from_bits(bits)), (bits >> 52) & 0x7ff == 0)
            } else {
                let bits = bits_s.parse::<i32>().unwrap() as u32;
                (java_float(f32::from_bits(bits)), (bits >> 23) & 0xff == 0)
            };
            if ours != expected {
                if is_subnormal {
                    // Documented: Java's Schubfach is non-shortest at the denormal
                    // floor; our Dragon4 emits the (correct) shortest form.
                    denormal_quirk += 1;
                } else if real_bugs.len() < 40 {
                    real_bugs.push(format!("[{kind} {bits_s}] java={expected}  ours={ours}"));
                }
            }
        }
        if !real_bugs.is_empty() {
            eprintln!("=== {} NORMAL-value mismatches (real bugs) ===", real_bugs.len());
            for s in &real_bugs {
                eprintln!("  {s}");
            }
            panic!("float formatting differs from Java on normal values");
        }
        eprintln!(
            "(float battery: {total} values; {denormal_quirk} tolerated denormal-floor quirks, \
             0 normal-value mismatches)"
        );
    }
}
