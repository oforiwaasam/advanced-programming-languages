"Ride Sharing System (Pharo Smalltalk) - MSCS632 OOP assignment.
 File-in with:  CodeImporter evaluateFileNamed: '/path/to/RideSharing.st'
 Then run:      RideSharingDemo run   (output appears in the Transcript)"!

Object subclass: #Ride
	instanceVariableNames: 'rideID pickupLocation dropoffLocation distance'
	classVariableNames: ''
	package: 'RideSharing'!

!Ride class methodsFor: 'instance creation'!
id: anInteger from: pickup to: dropoff distance: miles
	^ self new setId: anInteger from: pickup to: dropoff distance: miles! !

!Ride methodsFor: 'initialization'!
setId: anInteger from: pickup to: dropoff distance: miles
	rideID := anInteger.
	pickupLocation := pickup.
	dropoffLocation := dropoff.
	distance := miles! !

!Ride methodsFor: 'accessing'!
rideID
	^ rideID!
pickupLocation
	^ pickupLocation!
dropoffLocation
	^ dropoffLocation!
distance
	^ distance! !

!Ride methodsFor: 'behavior'!
fare
	"Abstract: every concrete ride type must define its own fare rule."
	^ self subclassResponsibility!
rideType
	^ self subclassResponsibility!
rideDetails
	^ '[', self rideType, '] Ride #', rideID printString, ': ',
		pickupLocation, ' -> ', dropoffLocation, ', ',
		(distance printShowingDecimalPlaces: 2), ' mi, fare $',
		(self fare printShowingDecimalPlaces: 2)! !

Ride subclass: #StandardRide
	instanceVariableNames: ''
	classVariableNames: ''
	package: 'RideSharing'!

!StandardRide methodsFor: 'behavior'!
fare
	^ 2.5 + (1.2 * distance)!
rideType
	^ 'Standard'! !

Ride subclass: #PremiumRide
	instanceVariableNames: 'carModel'
	classVariableNames: ''
	package: 'RideSharing'!

!PremiumRide methodsFor: 'accessing'!
carModel: aString
	carModel := aString! !

!PremiumRide methodsFor: 'behavior'!
fare
	^ 5.0 + (2.75 * distance)!
rideType
	^ 'Premium'!
rideDetails
	^ super rideDetails, ' (car: ', carModel, ')'! !

Ride subclass: #SharedRide
	instanceVariableNames: 'passengers'
	classVariableNames: ''
	package: 'RideSharing'!

!SharedRide methodsFor: 'accessing'!
passengers: anInteger
	passengers := anInteger max: 1! !

!SharedRide methodsFor: 'behavior'!
fare
	"Each extra passenger gets 15% off, capped at 45% total discount."
	| discount |
	discount := (0.15 * (passengers - 1)) min: 0.45.
	^ (2.0 + (0.9 * distance)) * (1 - discount)!
rideType
	^ 'Shared'!
rideDetails
	^ super rideDetails, ' (', passengers printString, ' passengers)'! !

Object subclass: #Driver
	instanceVariableNames: 'driverID name rating assignedRides'
	classVariableNames: ''
	package: 'RideSharing'!

!Driver class methodsFor: 'instance creation'!
id: anInteger name: aString rating: aNumber
	^ self new setId: anInteger name: aString rating: aNumber! !

!Driver methodsFor: 'initialization'!
setId: anInteger name: aString rating: aNumber
	driverID := anInteger.
	name := aString.
	rating := aNumber.
	assignedRides := OrderedCollection new! !

!Driver methodsFor: 'accessing'!
driverID
	^ driverID!
name
	^ name!
rating
	^ rating!
rideCount
	^ assignedRides size! !

!Driver methodsFor: 'behavior'!
addRide: aRide
	assignedRides add: aRide!
totalEarnings
	^ assignedRides inject: 0 into: [ :sum :each | sum + each fare ]!
getDriverInfo
	^ 'Driver #', driverID printString, ' ', name,
		' | rating ', (rating printShowingDecimalPlaces: 1),
		' | rides completed: ', assignedRides size printString,
		' | earnings $', (self totalEarnings printShowingDecimalPlaces: 2)! !

Object subclass: #Rider
	instanceVariableNames: 'riderID name requestedRides'
	classVariableNames: ''
	package: 'RideSharing'!

!Rider class methodsFor: 'instance creation'!
id: anInteger name: aString
	^ self new setId: anInteger name: aString! !

!Rider methodsFor: 'initialization'!
setId: anInteger name: aString
	riderID := anInteger.
	name := aString.
	requestedRides := OrderedCollection new! !

!Rider methodsFor: 'accessing'!
riderID
	^ riderID!
name
	^ name! !

!Rider methodsFor: 'behavior'!
requestRide: aRide
	requestedRides add: aRide!
viewRides
	Transcript show: 'Ride history for ', name, ' (Rider #', riderID printString, '):'; cr.
	requestedRides isEmpty
		ifTrue: [ Transcript show: '  (no rides yet)'; cr ]
		ifFalse: [ requestedRides do: [ :each |
			Transcript show: '  ', each rideDetails; cr ] ]! !

Object subclass: #RideSharingDemo
	instanceVariableNames: ''
	classVariableNames: ''
	package: 'RideSharing'!

!RideSharingDemo class methodsFor: 'demo'!
run
	| rides total ama kofi maya daniel premium shared |
	premium := (PremiumRide id: 102 from: 'Buckhead' to: 'Downtown' distance: 6.8)
		carModel: 'Tesla Model S'; yourself.
	shared := (SharedRide id: 103 from: 'Decatur' to: 'Midtown' distance: 5.1)
		passengers: 3; yourself.
	rides := OrderedCollection new.
	rides
		add: (StandardRide id: 101 from: 'Midtown' to: 'Airport' distance: 12.4);
		add: premium;
		add: shared;
		add: (StandardRide id: 104 from: 'Campus' to: 'Train Station' distance: 3.2).

	"Polymorphism: same messages sent to different ride classes."
	Transcript show: '=== Polymorphic fare and rideDetails ==='; cr.
	total := 0.
	rides do: [ :each |
		Transcript show: each rideDetails; cr.
		total := total + each fare ].
	Transcript show: 'Total fares: $', (total printShowingDecimalPlaces: 2); cr; cr.

	ama := Driver id: 1 name: 'Ama Mensah' rating: 4.9.
	kofi := Driver id: 2 name: 'Kofi Boateng' rating: 4.7.
	maya := Rider id: 1 name: 'Maya Chen'.
	daniel := Rider id: 2 name: 'Daniel Osei'.

	ama addRide: (rides at: 1); addRide: (rides at: 2).
	kofi addRide: (rides at: 3); addRide: (rides at: 4).
	maya requestRide: (rides at: 1); requestRide: (rides at: 2).
	daniel requestRide: (rides at: 3); requestRide: (rides at: 4).

	Transcript show: '=== Drivers ==='; cr.
	Transcript show: ama getDriverInfo; cr; show: kofi getDriverInfo; cr; cr.
	Transcript show: '=== Rider histories ==='; cr.
	maya viewRides.
	daniel viewRides! !