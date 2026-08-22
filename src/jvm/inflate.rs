//! **DEFLATE** decompression (RFC 1951) and its `zlib` wrapper (RFC 1950).
//!
//! Written from scratch because the project has no compression dependency, and needed to
//! read a runtime image built with `jlink --compress`: the JDK stores those resources as
//! zlib streams, so without an inflater a compressed image is unreadable.
//!
//! Two things about the format are easy to get backwards, and both bite silently:
//!
//! - **The stream is read LSB-first**, bit by bit from the low end of each byte — but a
//!   **Huffman code is packed MSB-first**, most significant bit of the code coming first.
//!   The two orders coexist in the same stream.
//! - A back-reference may **overlap** the bytes it is still producing (`distance` smaller
//!   than `length`): copying it as a block would be wrong, it has to go byte by byte. That
//!   is what makes a run like `aaaa…` cost three bytes.

/// Reads bits LSB-first, the order DEFLATE packs its fields in.
struct Bits<'a> {
    bytes: &'a [u8],
    /// Index of the next byte, and how many bits of the current one are already consumed.
    at: usize,
    used: u32,
}

impl<'a> Bits<'a> {
    fn new(bytes: &'a [u8]) -> Self {
        Bits { bytes, at: 0, used: 0 }
    }

    fn bit(&mut self) -> Option<u32> {
        let byte = *self.bytes.get(self.at)? as u32;
        let bit = (byte >> self.used) & 1;
        self.used += 1;
        if self.used == 8 {
            self.used = 0;
            self.at += 1;
        }
        Some(bit)
    }

    /// `n` bits as a little-endian integer — the shape every fixed-width field uses.
    fn bits(&mut self, n: u32) -> Option<u32> {
        let mut value = 0;
        for i in 0..n {
            value |= self.bit()? << i;
        }
        Some(value)
    }

    /// Drops the rest of the current byte; a *stored* block starts byte-aligned.
    fn align(&mut self) {
        if self.used != 0 {
            self.used = 0;
            self.at += 1;
        }
    }
}

/// A canonical Huffman table, kept as the counts per length plus the symbols in canonical
/// order — enough to decode without materialising every code.
struct Huffman {
    counts: [u16; 16],
    symbols: Vec<u16>,
}

impl Huffman {
    /// Builds the table from one code length per symbol (`0` = symbol unused).
    fn new(lengths: &[u8]) -> Huffman {
        let mut counts = [0u16; 16];
        for &l in lengths {
            counts[l as usize] += 1;
        }
        counts[0] = 0; // los simbolos sin codigo no participan
        // Offset of each length inside the canonical ordering.
        let mut offsets = [0u16; 16];
        for l in 1..16 {
            offsets[l] = offsets[l - 1] + counts[l - 1];
        }
        let mut symbols = vec![0u16; lengths.len()];
        for (symbol, &l) in lengths.iter().enumerate() {
            if l != 0 {
                symbols[offsets[l as usize] as usize] = symbol as u16;
                offsets[l as usize] += 1;
            }
        }
        Huffman { counts, symbols }
    }

    /// Decodes one symbol, walking lengths from short to long.
    ///
    /// The code is accumulated **MSB-first** even though the stream is read LSB-first: at
    /// each length the running value is compared against how many codes of that length
    /// exist, which is exactly what makes a canonical table decodable without a lookup.
    fn decode(&self, bits: &mut Bits) -> Option<u16> {
        let (mut code, mut first, mut index) = (0i32, 0i32, 0i32);
        for length in 1..16 {
            code |= bits.bit()? as i32;
            let count = self.counts[length] as i32;
            if code - first < count {
                return self.symbols.get((index + (code - first)) as usize).copied();
            }
            index += count;
            first = (first + count) << 1;
            code <<= 1;
        }
        None // codigo invalido: 15 bits sin cerrar
    }
}

// Length codes 257..=285: base length and how many extra bits follow.
const LENGTH_BASE: [u16; 29] = [
    3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31, 35, 43, 51, 59, 67, 83, 99, 115, 131,
    163, 195, 227, 258,
];
const LENGTH_EXTRA: [u8; 29] = [
    0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0,
];
// Distance codes 0..=29.
const DISTANCE_BASE: [u16; 30] = [
    1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193, 257, 385, 513, 769, 1025, 1537,
    2049, 3073, 4097, 6145, 8193, 12289, 16385, 24577,
];
const DISTANCE_EXTRA: [u8; 30] = [
    0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9, 10, 10, 11, 11, 12, 12, 13,
    13,
];

/// Inflates a raw DEFLATE stream (no zlib header).
pub fn inflate(bytes: &[u8]) -> Option<Vec<u8>> {
    let mut bits = Bits::new(bytes);
    let mut out = Vec::new();
    loop {
        let last = bits.bit()?;
        match bits.bits(2)? {
            0 => stored_block(&mut bits, &mut out)?,
            1 => {
                let (literals, distances) = fixed_tables();
                huffman_block(&mut bits, &mut out, &literals, &distances)?;
            }
            2 => {
                let (literals, distances) = dynamic_tables(&mut bits)?;
                huffman_block(&mut bits, &mut out, &literals, &distances)?;
            }
            _ => return None, // BTYPE 11 no existe
        }
        if last == 1 {
            return Some(out);
        }
    }
}

/// An uncompressed block: byte-aligned `LEN`, its complement, then the bytes verbatim.
fn stored_block(bits: &mut Bits, out: &mut Vec<u8>) -> Option<()> {
    bits.align();
    let len = bits.bits(16)? as usize;
    let nlen = bits.bits(16)?;
    if nlen != !(len as u16) as u32 {
        return None; // NLEN tiene que ser el complemento de LEN
    }
    out.extend_from_slice(bits.bytes.get(bits.at..bits.at + len)?);
    bits.at += len;
    Some(())
}

/// The fixed tables of RFC 1951 §3.2.6 — the ones a `BTYPE=01` block implies.
fn fixed_tables() -> (Huffman, Huffman) {
    let mut lengths = [0u8; 288];
    for (symbol, l) in lengths.iter_mut().enumerate() {
        *l = match symbol {
            0..=143 => 8,
            144..=255 => 9,
            256..=279 => 7,
            _ => 8,
        };
    }
    (Huffman::new(&lengths), Huffman::new(&[5u8; 30]))
}

/// The tables a `BTYPE=10` block carries: code lengths, themselves Huffman-coded.
fn dynamic_tables(bits: &mut Bits) -> Option<(Huffman, Huffman)> {
    let hlit = bits.bits(5)? as usize + 257;
    let hdist = bits.bits(5)? as usize + 1;
    let hclen = bits.bits(4)? as usize + 4;
    // The lengths of the *code-length* alphabet arrive in this scrambled order, chosen so
    // that the ones most likely to be zero come last and can be omitted.
    const ORDER: [usize; 19] = [16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15];
    let mut code_lengths = [0u8; 19];
    for &position in ORDER.iter().take(hclen) {
        code_lengths[position] = bits.bits(3)? as u8;
    }
    let code_table = Huffman::new(&code_lengths);

    // Literal and distance lengths share one run: 16 repeats the previous length, 17 and 18
    // are runs of zeros of different spans.
    let mut lengths = vec![0u8; hlit + hdist];
    let mut i = 0;
    while i < lengths.len() {
        let symbol = code_table.decode(bits)?;
        match symbol {
            0..=15 => {
                lengths[i] = symbol as u8;
                i += 1;
            }
            16 => {
                let previous = *lengths.get(i.checked_sub(1)?)?;
                for _ in 0..3 + bits.bits(2)? {
                    *lengths.get_mut(i)? = previous;
                    i += 1;
                }
            }
            17 => i += 3 + bits.bits(3)? as usize,
            18 => i += 11 + bits.bits(7)? as usize,
            _ => return None,
        }
    }
    if i > lengths.len() {
        return None;
    }
    let (literal, distance) = lengths.split_at(hlit);
    Some((Huffman::new(literal), Huffman::new(distance)))
}

/// The body of a Huffman block: literals, and back-references into what is already out.
fn huffman_block(
    bits: &mut Bits,
    out: &mut Vec<u8>,
    literals: &Huffman,
    distances: &Huffman,
) -> Option<()> {
    loop {
        let symbol = literals.decode(bits)?;
        match symbol {
            256 => return Some(()), // fin del bloque
            0..=255 => out.push(symbol as u8),
            257..=285 => {
                let i = symbol as usize - 257;
                let length = LENGTH_BASE[i] as usize + bits.bits(LENGTH_EXTRA[i] as u32)? as usize;
                let d = distances.decode(bits)? as usize;
                if d >= DISTANCE_BASE.len() {
                    return None;
                }
                let distance =
                    DISTANCE_BASE[d] as usize + bits.bits(DISTANCE_EXTRA[d] as u32)? as usize;
                let from = out.len().checked_sub(distance)?;
                // Byte a byte a proposito: la referencia puede **solaparse** con lo que
                // todavia se esta escribiendo (distancia menor que longitud), y copiar el
                // rango de una vez daria otra cosa.
                for k in 0..length {
                    let byte = *out.get(from + k)?;
                    out.push(byte);
                }
            }
            _ => return None,
        }
    }
}

/// Inflates a **zlib** stream: two header bytes, the DEFLATE data, and an Adler-32.
pub fn zlib_inflate(bytes: &[u8]) -> Option<Vec<u8>> {
    if bytes.len() < 2 || (((bytes[0] as u32) << 8) | bytes[1] as u32) % 31 != 0 {
        return None;
    }
    if bytes[0] & 0x0F != 8 {
        return None; // el unico metodo definido es deflate
    }
    inflate(&bytes[2..])
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::jvm::jimage::zlib_stored;

    #[test]
    fn a_stored_stream_round_trips() {
        // Los bloques *stored* que escribe nuestro `--compress zip-0` tienen que pasar por
        // el inflater general igual que por el especifico.
        let original = b"bloques stored, sin comprimir".to_vec();
        assert_eq!(zlib_inflate(&zlib_stored(&original)).as_deref(), Some(&original[..]));
    }

    #[test]
    fn a_stream_spanning_several_stored_blocks_round_trips() {
        let original: Vec<u8> = (0..200_000u32).map(|i| (i % 251) as u8).collect();
        assert_eq!(zlib_inflate(&zlib_stored(&original)), Some(original));
    }

    #[test]
    fn a_zlib_header_that_does_not_check_is_refused() {
        assert!(zlib_inflate(&[0x78, 0x00]).is_none(), "el check % 31 tiene que fallar");
        assert!(zlib_inflate(&[]).is_none());
        // Metodo distinto de deflate (nibble bajo != 8).
        assert!(zlib_inflate(&[0x79, 0x9C, 0x00]).is_none());
    }

    #[test]
    fn a_truncated_stream_is_refused_instead_of_returning_half() {
        // Devolver lo que se alcanzo a inflar seria peor que fallar: el llamador no puede
        // distinguir un recurso a medias de uno completo.
        let full = zlib_stored(b"algo suficientemente largo para cortar por la mitad");
        assert!(zlib_inflate(&full[..full.len() / 2]).is_none());
    }
}
