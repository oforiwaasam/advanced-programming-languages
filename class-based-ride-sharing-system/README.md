# Ride Sharing System (MSCS632)

Two implementations of the same class-based design: `Ride` (abstract) with
`StandardRide`, `PremiumRide`, `SharedRide`, plus `Driver` and `Rider`.

## C++ (C++17)
    cd cpp
    clang++ -std=c++17 -Wall -Wextra -o ride_sharing ride_sharing.cpp
    ./ride_sharing

## Smalltalk (Pharo)
1. Open Pharo, then open a Playground (Ctrl+O W).
2. File in the code:  `CodeImporter evaluateFileNamed: '/full/path/to/smalltalk/RideSharing.st'`
3. Open the Transcript (Ctrl+O T), then run in the Playground:  `RideSharingDemo run`