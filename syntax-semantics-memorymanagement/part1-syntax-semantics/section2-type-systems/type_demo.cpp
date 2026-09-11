#include <iostream>
using namespace std;

int add(int a, int b) { return a + b; }
 
int main() {
    cout << add(2, 3) << endl;      // 5
    cout << add(2, 3.9) << endl;    // 5 -- narrowed to int at compile time
    // add("2", "3");                -- would not compile
    return 0;
}