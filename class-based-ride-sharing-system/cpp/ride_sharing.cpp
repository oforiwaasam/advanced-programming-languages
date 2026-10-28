// Ride Sharing System (C++17) - MSCS632 OOP assignment
// Demonstrates encapsulation, inheritance, and polymorphism.
#include <iomanip>
#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <vector>

// ---------------------------------------------------------------------------
// Ride: abstract base class. Data is protected (visible to subclasses only);
// outside code reads it through accessors (encapsulation).
// ---------------------------------------------------------------------------
class Ride {
public:
    Ride(int rideID, std::string pickup, std::string dropoff, double distance)
        : rideID_(rideID), pickup_(std::move(pickup)),
          dropoff_(std::move(dropoff)), distance_(distance) {}

    virtual ~Ride() = default;  // needed so deleting via Ride* is safe

    // Pure virtual: every concrete ride type MUST define its own fare rule.
    virtual double fare() const = 0;
    virtual std::string rideType() const = 0;

    // Shared behavior; calls the virtual fare() so output is type-specific.
    virtual std::string rideDetails() const {
        std::ostringstream out;
        out << std::fixed << std::setprecision(2);
        out << "[" << rideType() << "] Ride #" << rideID_ << ": " << pickup_
            << " -> " << dropoff_ << ", " << distance_ << " mi, fare $"
            << fare();
        return out.str();
    }

    int rideID() const { return rideID_; }
    const std::string& pickupLocation() const { return pickup_; }
    const std::string& dropoffLocation() const { return dropoff_; }
    double distance() const { return distance_; }

protected:
    int rideID_;
    std::string pickup_;
    std::string dropoff_;
    double distance_;
};

// ---------------------------------------------------------------------------
// Subclasses (inheritance) override fare() (polymorphism).
// ---------------------------------------------------------------------------
class StandardRide : public Ride {
public:
    using Ride::Ride;
    double fare() const override { return 2.50 + 1.20 * distance_; }
    std::string rideType() const override { return "Standard"; }
};

class PremiumRide : public Ride {
public:
    PremiumRide(int id, std::string from, std::string to, double dist,
                std::string carModel)
        : Ride(id, std::move(from), std::move(to), dist),
          carModel_(std::move(carModel)) {}

    double fare() const override { return 5.00 + 2.75 * distance_; }
    std::string rideType() const override { return "Premium"; }

    // Extends the base output with subclass-specific detail.
    std::string rideDetails() const override {
        return Ride::rideDetails() + " (car: " + carModel_ + ")";
    }

private:
    std::string carModel_;
};

class SharedRide : public Ride {
public:
    SharedRide(int id, std::string from, std::string to, double dist,
               int passengers)
        : Ride(id, std::move(from), std::move(to), dist),
          passengers_(passengers < 1 ? 1 : passengers) {}

    // Each extra passenger gets 15% off, capped at 45% total discount.
    double fare() const override {
        double discount = 0.15 * (passengers_ - 1);
        if (discount > 0.45) discount = 0.45;
        return (2.00 + 0.90 * distance_) * (1.0 - discount);
    }
    std::string rideType() const override { return "Shared"; }
    std::string rideDetails() const override {
        return Ride::rideDetails() + " (" + std::to_string(passengers_) +
               " passengers)";
    }

private:
    int passengers_;
};

using RidePtr = std::shared_ptr<Ride>;

// ---------------------------------------------------------------------------
// Driver: assignedRides is PRIVATE. The only ways in/out are addRide() and the
// read-only methods below (encapsulation).
// ---------------------------------------------------------------------------
class Driver {
public:
    Driver(int driverID, std::string name, double rating)
        : driverID_(driverID), name_(std::move(name)), rating_(rating) {}

    void addRide(const RidePtr& ride) {
        if (ride) assignedRides_.push_back(ride);
    }

    double totalEarnings() const {
        double total = 0;
        for (const auto& r : assignedRides_) total += r->fare();  // polymorphic
        return total;
    }

    std::string getDriverInfo() const {
        std::ostringstream out;
        out << std::fixed << std::setprecision(2);
        out << "Driver #" << driverID_ << " " << name_ << " | rating "
            << std::setprecision(1) << rating_ << std::setprecision(2)
            << " | rides completed: " << assignedRides_.size()
            << " | earnings $" << totalEarnings();
        return out.str();
    }

    int driverID() const { return driverID_; }
    const std::string& name() const { return name_; }
    double rating() const { return rating_; }
    std::size_t rideCount() const { return assignedRides_.size(); }

private:
    int driverID_;
    std::string name_;
    double rating_;
    std::vector<RidePtr> assignedRides_;  // hidden from outside code
};

// ---------------------------------------------------------------------------
// Rider: requestedRides is also private.
// ---------------------------------------------------------------------------
class Rider {
public:
    Rider(int riderID, std::string name)
        : riderID_(riderID), name_(std::move(name)) {}

    void requestRide(const RidePtr& ride) {
        if (ride) requestedRides_.push_back(ride);
    }

    void viewRides() const {
        std::cout << "Ride history for " << name_ << " (Rider #" << riderID_
                  << "):\n";
        if (requestedRides_.empty()) {
            std::cout << "  (no rides yet)\n";
            return;
        }
        for (const auto& r : requestedRides_)
            std::cout << "  " << r->rideDetails() << "\n";
    }

    int riderID() const { return riderID_; }
    const std::string& name() const { return name_; }

private:
    int riderID_;
    std::string name_;
    std::vector<RidePtr> requestedRides_;
};

// ---------------------------------------------------------------------------
// Demo
// ---------------------------------------------------------------------------
int main() {
    std::cout << std::fixed << std::setprecision(2);

    // 1. Polymorphism: one container of base-class pointers, mixed ride types.
    std::vector<RidePtr> rides = {
        std::make_shared<StandardRide>(101, "Midtown", "Airport", 12.4),
        std::make_shared<PremiumRide>(102, "Buckhead", "Downtown", 6.8,
                                      "Tesla Model S"),
        std::make_shared<SharedRide>(103, "Decatur", "Midtown", 5.1, 3),
        std::make_shared<StandardRide>(104, "Campus", "Train Station", 3.2),
    };

    std::cout << "=== Polymorphic fare() and rideDetails() ===\n";
    double total = 0;
    for (const auto& r : rides) {
        std::cout << r->rideDetails() << "\n";  // dispatches to the real type
        total += r->fare();
    }
    std::cout << "Total fares: $" << total << "\n\n";

    // 2. Drivers and riders.
    Driver ama(1, "Ama Mensah", 4.9);
    Driver kofi(2, "Kofi Boateng", 4.7);
    Rider maya(1, "Maya Chen");
    Rider daniel(2, "Daniel Osei");

    ama.addRide(rides[0]);
    ama.addRide(rides[1]);
    kofi.addRide(rides[2]);
    kofi.addRide(rides[3]);

    maya.requestRide(rides[0]);
    maya.requestRide(rides[1]);
    daniel.requestRide(rides[2]);
    daniel.requestRide(rides[3]);

    std::cout << "=== Drivers ===\n"
              << ama.getDriverInfo() << "\n"
              << kofi.getDriverInfo() << "\n\n";

    std::cout << "=== Rider histories ===\n";
    maya.viewRides();
    daniel.viewRides();

    // 3. Encapsulation check: the next line would NOT compile if uncommented.
    // ama.assignedRides_.clear();   // error: 'assignedRides_' is private
    return 0;
}