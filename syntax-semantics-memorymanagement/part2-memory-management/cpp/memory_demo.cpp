// C++: manual memory management (no GC, no ownership checker)
#include <iostream>
using namespace std;
 
int main() {
    // 1. Manual allocation
    int* buffer = new int[1000000];
    for (int i = 0; i < 1000000; i++) buffer[i] = i;
    cout << "First element: " << buffer[0] << endl;
    cout << "Last element: "  << buffer[999999] << endl;
 
    // 2. Manual deallocation. The programmer, not the language, is
    //    responsible for this. Forgetting it is a memory LEAK.
    delete[] buffer;
 
    // 3. `buffer` is now a DANGLING POINTER: it still holds the old
    //    address, but that memory is no longer owned by this program.
    //    Dereferencing it is undefined behavior. (Left commented out
    //    because UB may crash, corrupt memory, or "appear to work";
    //    that unpredictability is exactly the danger.)
    // cout << buffer[0] << endl;   // UNDEFINED BEHAVIOR: use-after-free
 
    buffer = nullptr;   // best practice: neutralize the dangling pointer
 
    // 4. A genuine leak, left in for discussion purposes: this
    //    allocation is never freed anywhere in the program.
    int* leaked = new int[1000000];
    leaked[0] = 42;
    cout << "Leaked buffer's first element: " << leaked[0] << endl;
    // no matching delete[] leaked; -> 4,000,000 bytes leak until process exit
 
    return 0;
}
