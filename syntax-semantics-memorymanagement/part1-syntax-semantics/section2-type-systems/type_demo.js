function add(a, b) { return a + b; }
 
console.log(add(2, 3));         // 5
console.log(add("2", "3"));     // "23"
console.log(add(2, "3"));       // "23"
console.log(add(2, null));      // 2
console.log(add(2, undefined)); // NaN
console.log(add([1], [2]));     // "1,2"
