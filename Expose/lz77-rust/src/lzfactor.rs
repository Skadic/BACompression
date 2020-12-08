
use std::fmt::Display;

use crate::lzfactor::LZFactor::{Char, Factor};


/// An enum representing a LZ Factor
/// The Char variant represents a single symbol. Its attributes are the byte representation of the symbol and the position of its occurrence
/// The Factor variant represents a pointer to a repeating subsequence.
/// Its attributes are the start of the original pattern, the length of the pattern and the index of the start of the next pattern
#[derive(Debug)]
pub enum LZFactor {
    Char(u8, usize),
    Factor(usize, usize, usize)
}

impl LZFactor {

    pub fn new_char(character : u8, start_index: usize) -> Self {
        Char(character, start_index)
    }

    pub fn new_factor(pattern_start_index: usize, pattern_length: usize, current_index: usize) -> Self {
        Factor(pattern_start_index, pattern_length, current_index + pattern_length)
    }

    pub fn is_char(&self) -> bool {
        match self {
            Char(_, _) => true,
            Factor(_, _, _) => false
        }
    } 

    pub fn factor_values(&self) -> Option<(usize, usize, usize)> {
        match self {
            Char(_, _) => None,
            Factor(pattern_start, pattern_len, next_index) => Some((*pattern_start, *pattern_len, *next_index))
        }
    }

    pub fn char_values(&self) -> Option<(u8, usize)> {
        match self {
            Char(character, char_index) => Some((*character, *char_index)),
            Factor(_, _, _) => None
        }
    }

    pub fn length(&self) -> usize {
        match self {
            Char(_, _) => 0,
            Factor(_, len, _) => *len
        }
    }

    pub fn next_index(&self) -> usize {
        match self {
            Char(_, index) => index + 1,
            Factor(_, _, next_index) => *next_index
        }
    }

}

impl Display for LZFactor {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            Char(c, _) => write!(f, "{}", String::from_utf8(vec![*c]).unwrap()),
            Factor(pattern_start, len, _) => write!(f, "({}, {})", *pattern_start, *len),
        }
    }
}