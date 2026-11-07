"Ride Sharing System (Pharo Smalltalk) - MSCS632 OOP assignment.
 This file is a plain script. Load it with:
     CodeImporter evaluateFileNamed: '/full/path/to/RideSharing.st'
 (or paste the whole file into a Playground and Do it).
 Then run:  RideSharingDemo run   -- output appears in the Transcript."

Object << #Ride
	slots: { #rideID . #pickupLocation . #dropoffLocation . #distance };
	package: 'RideSharing';
	install.

(Smalltalk at: #Ride) class compile: 'id: anInteger from: pickup to: dropoff distance: miles
	^ self new setId: anInteger from: pickup to: dropoff distance: miles'.
(Smalltalk at: #Ride) compile: 'setId: anInteger from: pickup to: dropoff distance: miles
	rideID := anInteger.
	pickupLocation := pickup.
	dropoffLocation := dropoff.
	distance := miles'.
(Smalltalk at: #Ride) compile: 'rideID
	^ rideID'.
(Smalltalk at: #Ride) compile: 'pickupLocation
	^ pickupLocation'.
(Smalltalk at: #Ride) compile: 'dropoffLocation
	^ dropoffLocation'.
(Smalltalk at: #Ride) compile: 'distance
	^ distance'.
(Smalltalk at: #Ride) compile: 'fare
	"Abstract: every concrete ride type must define its own fare rule."
	^ self subclassResponsibility'.
(Smalltalk at: #Ride) compile: 'rideType
	^ self subclassResponsibility'.
(Smalltalk at: #Ride) compile: 'rideDetails
	^ ''['', self rideType, ''] Ride #'', rideID printString, '': '',
		pickupLocation, '' -> '', dropoffLocation, '', '',
		(distance printShowingDecimalPlaces: 2), '' mi, fare $'',
		(self fare printShowingDecimalPlaces: 2)'.
(Smalltalk at: #Ride) << #StandardRide
	slots: {  };
	package: 'RideSharing';
	install.

(Smalltalk at: #StandardRide) compile: 'fare
	^ 2.5 + (1.2 * distance)'.
(Smalltalk at: #StandardRide) compile: 'rideType
	^ ''Standard'''.
(Smalltalk at: #Ride) << #PremiumRide
	slots: { #carModel };
	package: 'RideSharing';
	install.

(Smalltalk at: #PremiumRide) compile: 'carModel: aString
	carModel := aString'.
(Smalltalk at: #PremiumRide) compile: 'fare
	^ 5.0 + (2.75 * distance)'.
(Smalltalk at: #PremiumRide) compile: 'rideType
	^ ''Premium'''.
(Smalltalk at: #PremiumRide) compile: 'rideDetails
	^ super rideDetails, '' (car: '', carModel, '')'''.
(Smalltalk at: #Ride) << #SharedRide
	slots: { #passengers };
	package: 'RideSharing';
	install.

(Smalltalk at: #SharedRide) compile: 'passengers: anInteger
	passengers := anInteger max: 1'.
(Smalltalk at: #SharedRide) compile: 'fare
	"Each extra passenger gets 15% off, capped at 45% total discount."
	| discount |
	discount := (0.15 * (passengers - 1)) min: 0.45.
	^ (2.0 + (0.9 * distance)) * (1 - discount)'.
(Smalltalk at: #SharedRide) compile: 'rideType
	^ ''Shared'''.
(Smalltalk at: #SharedRide) compile: 'rideDetails
	^ super rideDetails, '' ('', passengers printString, '' passengers)'''.
Object << #Driver
	slots: { #driverID . #name . #rating . #assignedRides };
	package: 'RideSharing';
	install.

(Smalltalk at: #Driver) class compile: 'id: anInteger name: aString rating: aNumber
	^ self new setId: anInteger name: aString rating: aNumber'.
(Smalltalk at: #Driver) compile: 'setId: anInteger name: aString rating: aNumber
	driverID := anInteger.
	name := aString.
	rating := aNumber.
	assignedRides := OrderedCollection new'.
(Smalltalk at: #Driver) compile: 'driverID
	^ driverID'.
(Smalltalk at: #Driver) compile: 'name
	^ name'.
(Smalltalk at: #Driver) compile: 'rating
	^ rating'.
(Smalltalk at: #Driver) compile: 'rideCount
	^ assignedRides size'.
(Smalltalk at: #Driver) compile: 'addRide: aRide
	assignedRides add: aRide'.
(Smalltalk at: #Driver) compile: 'totalEarnings
	^ assignedRides inject: 0 into: [ :sum :each | sum + each fare ]'.
(Smalltalk at: #Driver) compile: 'getDriverInfo
	^ ''Driver #'', driverID printString, '' '', name,
		'' | rating '', (rating printShowingDecimalPlaces: 1),
		'' | rides completed: '', assignedRides size printString,
		'' | earnings $'', (self totalEarnings printShowingDecimalPlaces: 2)'.
Object << #Rider
	slots: { #riderID . #name . #requestedRides };
	package: 'RideSharing';
	install.

(Smalltalk at: #Rider) class compile: 'id: anInteger name: aString
	^ self new setId: anInteger name: aString'.
(Smalltalk at: #Rider) compile: 'setId: anInteger name: aString
	riderID := anInteger.
	name := aString.
	requestedRides := OrderedCollection new'.
(Smalltalk at: #Rider) compile: 'riderID
	^ riderID'.
(Smalltalk at: #Rider) compile: 'name
	^ name'.
(Smalltalk at: #Rider) compile: 'requestRide: aRide
	requestedRides add: aRide'.
(Smalltalk at: #Rider) compile: 'viewRides
	Transcript show: ''Ride history for '', name, '' (Rider #'', riderID printString, ''):''; cr.
	requestedRides isEmpty
		ifTrue: [ Transcript show: ''  (no rides yet)''; cr ]
		ifFalse: [ requestedRides do: [ :each |
			Transcript show: ''  '', each rideDetails; cr ] ]'.
Object << #RideSharingDemo
	slots: {  };
	package: 'RideSharing';
	install.

(Smalltalk at: #RideSharingDemo) class compile: 'run
	| rides total ama kofi maya daniel premium shared |
	premium := (PremiumRide id: 102 from: ''Buckhead'' to: ''Downtown'' distance: 6.8)
		carModel: ''Tesla Model S''; yourself.
	shared := (SharedRide id: 103 from: ''Decatur'' to: ''Midtown'' distance: 5.1)
		passengers: 3; yourself.
	rides := OrderedCollection new.
	rides
		add: (StandardRide id: 101 from: ''Midtown'' to: ''Airport'' distance: 12.4);
		add: premium;
		add: shared;
		add: (StandardRide id: 104 from: ''Campus'' to: ''Train Station'' distance: 3.2).

	"Polymorphism: same messages sent to different ride classes."
	Transcript show: ''=== Polymorphic fare and rideDetails ===''; cr.
	total := 0.
	rides do: [ :each |
		Transcript show: each rideDetails; cr.
		total := total + each fare ].
	Transcript show: ''Total fares: $'', (total printShowingDecimalPlaces: 2); cr; cr.

	ama := Driver id: 1 name: ''Ama Mensah'' rating: 4.9.
	kofi := Driver id: 2 name: ''Kofi Boateng'' rating: 4.7.
	maya := Rider id: 1 name: ''Maya Chen''.
	daniel := Rider id: 2 name: ''Daniel Osei''.

	ama addRide: (rides at: 1); addRide: (rides at: 2).
	kofi addRide: (rides at: 3); addRide: (rides at: 4).
	maya requestRide: (rides at: 1); requestRide: (rides at: 2).
	daniel requestRide: (rides at: 3); requestRide: (rides at: 4).

	Transcript show: ''=== Drivers ===''; cr.
	Transcript show: ama getDriverInfo; cr; show: kofi getDriverInfo; cr; cr.
	Transcript show: ''=== Rider histories ===''; cr.
	maya viewRides.
	daniel viewRides'.