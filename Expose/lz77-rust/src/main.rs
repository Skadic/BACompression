use std::fs::File;
use std::io::Read;

mod augmented;
mod lzfactor;
mod lz;

fn main() {

    // Get the arguments and skip 1, since the first argument is the application path
    let mut args = std::env::args().skip(1);

    // Check if 1 argment has been given
    if args.len() != 1 {
        println!("Please input a file");
        return;
    }

    let file_name = args.next().unwrap();
    let mut file = String::new();
    File::open(file_name).expect("Unable to open file").read_to_string(&mut file);

    println!("{}", lz::factorized_string(file));

}
