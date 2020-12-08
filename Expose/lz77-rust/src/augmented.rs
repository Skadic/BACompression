use std::{fmt::Debug, vec, write};

use bio::data_structures::{smallints::SmallInts, suffix_array::{lcp, suffix_array}};



pub struct AugmentedString {
    underlying: String,
    suffix_array: Vec<usize>,
    inverse_suffix_array: Vec<usize>,
    lcp: Vec<isize>,
    psv: Vec<usize>,
    nsv: Vec<usize>,
    sentinel: char
}

impl AugmentedString {
    pub fn new(mut s: String, sentinel: char) -> Self {
        s.push(sentinel);
        let sa = suffix_array(s.as_bytes());
        let isa = invert_suffix_array(&sa[..]);
        let lcp = lcp(s.as_bytes(), &sa).decompress();
        let (psv, nsv) = auxiliaries(&sa[..]);
        Self {
            underlying: s,
            suffix_array: sa,
            inverse_suffix_array: isa,
            lcp,
            psv,
            nsv,
            sentinel
        }
    }

    pub fn sentinel(&self) -> char {
        self.sentinel
    }

    pub fn suffix_array_index(&self, index: usize) -> usize {
        self.inverse_suffix_array[index]
    }

    pub fn suffix_index(&self, index: usize) -> usize {
        // In case this index is out of bounds, return the length of the string, which is the index
        //
        *self.suffix_array.get(index).unwrap_or(&self.underlying.len())
    }

    pub fn lcp(&self, index: usize) -> isize {
        self.lcp[index]
    }

    pub fn psv(&self, index: usize) -> usize {
        self.psv[index]
    }

    pub fn nsv(&self, index: usize) -> usize {
        self.nsv[index]
    }

    pub fn string_len(&self) -> usize {
        self.underlying.len()
    }

    pub fn underlying(&self) -> &str {
        &self.underlying[..]
    }
}


fn invert_suffix_array(suffix_array: &[usize]) -> Vec<usize> {
    let mut inverse = vec![0; suffix_array.len()];

    for (i, &suffix) in suffix_array.iter().enumerate() {
        inverse[suffix] = i;
    }

    inverse
}

fn auxiliaries(suffix_array: &[usize]) -> (Vec<usize>, Vec<usize>) {
    // Create empty vectors of appropriate size
    let mut psv = vec![0usize; suffix_array.len()];
    let mut nsv = vec![0usize; suffix_array.len()];

    // Gets the suffix array value if the index is in range
    // Otherwise returns 0, which is the index of the empty suffix in the suffix array
    let get_sa_value = |index: usize| suffix_array.get(index).map(|&i| i as isize).unwrap_or(0);

    for i in 1..=suffix_array.len() {
        let mut j = i - 1;

        while get_sa_value(i) < get_sa_value(j) as isize {
            nsv[j] = i;
            j = psv[j];

            if j == 0 {
                break;
            }
        }

        if i < psv.len() { psv[i] = j }

    }

    (psv, nsv)
}



impl Debug for AugmentedString {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}, SA: {:?}, ISA: {:?}, LCP: {:?}, PSV: {:?}, NSV: {:?}", self.underlying, self.suffix_array, self.inverse_suffix_array, self.lcp, self.psv, self.nsv)
    }
}