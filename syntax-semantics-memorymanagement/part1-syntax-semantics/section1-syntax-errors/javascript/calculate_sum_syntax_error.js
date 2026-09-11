// JavaScript : Calculate the sum of an array (with error)
function calculateSum(arr) {
    let total = 0;
    for (let num of arr) {
        total += num;
    }
    return total;
}
 
let numbers = [1, 2, 3, 4, 5];
let result = calculate Sum(numbers);
console.log("Sum in JavaScript:", result);