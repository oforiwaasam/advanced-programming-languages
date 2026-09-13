// Rust: ownership and borrowing (compile-time memory safety, no GC)
fn sum_vector(v: &Vec<i32>) -> i32 {
    // `v` is a BORROW (a reference). This function reads the data
    // but never takes ownership of it, so the caller keeps using it.
    v.iter().sum()
}
 
fn main() {
    let data = vec![1, 2, 3, 4, 5]; // heap-allocated buffer, OWNED by `data`
 
    let total = sum_vector(&data);  // pass an immutable borrow
    println!("Sum: {}", total);
    println!("Still usable after borrow: {:?}", data);
 
    let moved = data;               // ownership MOVES from `data` to `moved`
    // println!("{:?}", data);      // <-- uncommenting this is a COMPILE ERROR:
                                     //     "value borrowed here after move"
    println!("Owner is now `moved`: {:?}", moved);
 
} // `moved` goes out of scope here; Rust automatically calls its Drop
  // implementation and frees the heap memory -- deterministically,
  // with no garbage collector involved.