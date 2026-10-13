$ErrorActionPreference = 'Stop'
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " RideLink Microservices — Full API Verification Test Run" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# Configuration
$driverHost = "http://172.20.10.3:8082"
$accountHost = "http://localhost:8081"
$rideHost = "http://localhost:8083"
$fareHost = "http://localhost:8084"

# 1. Health Checks
Write-Host "`n[1/5] Checking Health of all 4 Services across computers..." -ForegroundColor Yellow
$services = @(
    @{ Name="Account Service"; Url="$accountHost/actuator/health" },
    @{ Name="Driver Service (Remote PC)"; Url="$driverHost/actuator/health" },
    @{ Name="Ride Service"; Url="$rideHost/actuator/health" },
    @{ Name="Fare Service"; Url="$fareHost/actuator/health" }
)
$services | ForEach-Object {
    $h = Invoke-RestMethod -Uri $_.Url
    Write-Host "  $($_.Name) : $($h.status)" -ForegroundColor Green
}

# 2. Account Service (8081) Auth
Write-Host "`n[2/5] Testing Account Service (8081)..." -ForegroundColor Yellow
$ts = (Get-Date).Ticks
$pEmail = "pass_${ts}@test.com"
$dEmail = "driv_${ts}@test.com"
$pass = "Test1234!"

# Register Passenger
$pReg = Invoke-RestMethod -Uri "$accountHost/api/auth/register/passenger" -Method POST -ContentType "application/json" -Body (@{name="Passenger Test"; email=$pEmail; password=$pass; phone="+94771112233"} | ConvertTo-Json)
Write-Host "  Passenger Registered: $($pReg.email) [ID: $($pReg.id)]" -ForegroundColor Green

# Register Driver
$dReg = Invoke-RestMethod -Uri "$accountHost/api/auth/register/driver" -Method POST -ContentType "application/json" -Body (@{name="Driver Test"; email=$dEmail; password=$pass; phone="+94779998877"} | ConvertTo-Json)
Write-Host "  Driver Registered: $($dReg.email) [ID: $($dReg.id)]" -ForegroundColor Green

# Login Passenger
$pLogin = Invoke-RestMethod -Uri "$accountHost/api/auth/login" -Method POST -ContentType "application/json" -Body (@{email=$pEmail; password=$pass} | ConvertTo-Json)
$pToken = $pLogin.accessToken
Write-Host "  Passenger JWT Issued (Role: $($pLogin.role))" -ForegroundColor Green

# Login Driver
$dLogin = Invoke-RestMethod -Uri "$accountHost/api/auth/login" -Method POST -ContentType "application/json" -Body (@{email=$dEmail; password=$pass} | ConvertTo-Json)
$dToken = $dLogin.accessToken
Write-Host "  Driver JWT Issued (Role: $($dLogin.role))" -ForegroundColor Green

# 3. Driver Service (8082 - Remote PC) Profile, Vehicle, Availability, Location
Write-Host "`n[3/5] Testing Driver Service on Remote PC ($driverHost)..." -ForegroundColor Yellow
$dHeaders = @{ Authorization = "Bearer $dToken"; "Content-Type" = "application/json" }

# Create Driver Profile
$dProf = Invoke-RestMethod -Uri "$driverHost/api/drivers/profile" -Method POST -Headers $dHeaders -Body (@{licenseNumber="B$ts"; licenseExpiryDate="2030-12-31"; serviceArea="Colombo"} | ConvertTo-Json)
Write-Host "  Driver Profile Created: License $($dProf.licenseNumber)" -ForegroundColor Green

# Register Vehicle
$veh = Invoke-RestMethod -Uri "$driverHost/api/drivers/me/vehicles" -Method POST -Headers $dHeaders -Body (@{vehicleRegistrationNumber="WP-CAB-$($ts % 10000)"; make="Toyota"; model="Prius"; year=2022; colour="Silver"; vehicleType="CAR"; passengerCapacity=4} | ConvertTo-Json)
Write-Host "  Vehicle Registered: $($veh.make) $($veh.model) [ID: $($veh.vehicleId)]" -ForegroundColor Green

# Activate Vehicle
Invoke-RestMethod -Uri "$driverHost/api/drivers/me/vehicles/$($veh.vehicleId)/active?active=true" -Method PATCH -Headers $dHeaders | Out-Null
Write-Host "  Vehicle Activated: TRUE" -ForegroundColor Green

# Update Simulated GPS Location
Invoke-RestMethod -Uri "$driverHost/api/drivers/me/location" -Method PATCH -Headers $dHeaders -Body (@{serviceArea="Colombo"; currentLatitude=6.9344; currentLongitude=79.8428} | ConvertTo-Json) | Out-Null
Write-Host "  Driver GPS Location Set: (6.9344, 79.8428)" -ForegroundColor Green

# Set Availability to AVAILABLE
$avail = Invoke-RestMethod -Uri "$driverHost/api/drivers/me/availability" -Method PATCH -Headers $dHeaders -Body (@{status="AVAILABLE"} | ConvertTo-Json)
Write-Host "  Driver Availability Status: $($avail.availabilityStatus)" -ForegroundColor Green

# 4. Fare Service (8084) Estimate
Write-Host "`n[4/5] Testing Fare Service (8084) Estimation..." -ForegroundColor Yellow
$pHeaders = @{ Authorization = "Bearer $pToken"; "Content-Type" = "application/json" }
$est = Invoke-RestMethod -Uri "$fareHost/api/fares/estimate" -Method POST -Headers $pHeaders -Body (@{distanceKm=5.5; durationMinutes=15; vehicleType="CAR"; pickupTime=(Get-Date).ToString("o")} | ConvertTo-Json)
Write-Host "  Fare Estimate Calculated: LKR $($est.estimatedFare)" -ForegroundColor Green

# 5. Ride Service (8083) Full Lifecycle and Payment
Write-Host "`n[5/5] Testing Ride Service (8083) Lifecycle and Payment..." -ForegroundColor Yellow

# Create Ride Request
$rideReq = @{
    pickupName = "Colombo Fort";
    pickupLatitude = 6.9344;
    pickupLongitude = 79.8428;
    destinationName = "Galle Face Green";
    destinationLatitude = 6.9271;
    destinationLongitude = 79.8446;
    serviceArea = "Colombo";
    requestedVehicleType = "CAR";
    estimatedDistanceKm = 5.5;
    estimatedDurationMinutes = 15
}
$ride = Invoke-RestMethod -Uri "$rideHost/api/rides" -Method POST -Headers $pHeaders -Body ($rideReq | ConvertTo-Json)
Write-Host "  [Ride Created and MATCHED across PCs!] ID: $($ride.id) | Status: $($ride.status) | Driver: $($ride.driverAccountId)" -ForegroundColor Green

# Driver Accepts Ride
$accRide = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/accept" -Method POST -Headers $dHeaders
Write-Host "  [Ride Accepted] Status: $($accRide.status)" -ForegroundColor Green

# Driver Starts Ride
$startRide = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/start" -Method POST -Headers $dHeaders
Write-Host "  [Trip Started] Status: $($startRide.status)" -ForegroundColor Green

# Driver Completes Ride
$compReq = @{ actualDistanceKm = 5.8; actualDurationMinutes = 16 }
$compRide = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/complete" -Method POST -Headers $dHeaders -Body ($compReq | ConvertTo-Json)
Write-Host "  [Trip Completed] Status: $($compRide.status) | Final Fare ID: $($compRide.finalFareId) | Amount: LKR $($compRide.finalFareAmount)" -ForegroundColor Green

# Settle Payment with Idempotency Key (8084)
$idempotencyKey = [System.Guid]::NewGuid().ToString()
$payHeaders = @{ Authorization = "Bearer $pToken"; "Content-Type" = "application/json"; "Idempotency-Key" = $idempotencyKey }
$payReq = @{
    fareId = $compRide.finalFareId;
    amount = $compRide.finalFareAmount;
    paymentMethod = "CARD";
    idempotencyKey = $idempotencyKey
}
$payment = Invoke-RestMethod -Uri "$fareHost/api/payments" -Method POST -Headers $payHeaders -Body ($payReq | ConvertTo-Json)
Write-Host "  [Payment Processed] Status: $($payment.status) | TxRef: $($payment.transactionReference) | Amount: LKR $($payment.amount)" -ForegroundColor Green

# Retrieve Receipt (8084)
$receipt = Invoke-RestMethod -Uri "$fareHost/api/receipts/ride/$($ride.id)" -Method GET -Headers $pHeaders
Write-Host "  [Digital Receipt] Receipt ID: $($receipt.receiptId) | Status: $($receipt.status)" -ForegroundColor Green

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " ALL 4 SERVICES CONNECTED AND WORKING ACROSS BOTH COMPUTERS!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

