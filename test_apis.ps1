[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " RideLink Microservices - Full API Verification Test" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Health Checks
Write-Host "`n[1/5] Health Checks on all 4 Ports..." -ForegroundColor Yellow
$h1 = Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method GET
$h2 = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method GET
$h3 = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health" -Method GET
$h4 = Invoke-RestMethod -Uri "http://localhost:8084/actuator/health" -Method GET
Write-Host "  Account (Port 8081) :" $h1.status -ForegroundColor Green
Write-Host "  Driver  (Port 8082) :" $h2.status -ForegroundColor Green
Write-Host "  Ride    (Port 8083) :" $h3.status -ForegroundColor Green
Write-Host "  Fare    (Port 8084) :" $h4.status -ForegroundColor Green

# 2. Register and Login (8081)
Write-Host "`n[2/5] Account Service (8081) - Registration and JWT Authentication..." -ForegroundColor Yellow
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$pEmail = "pass_" + $ts + "@test.com"
$dEmail = "driv_" + $ts + "@test.com"
$pass = "Password123!"

$pRegBody = @{ name="Passenger Test"; email=$pEmail; phone="+94771112233"; password=$pass } | ConvertTo-Json
$pReg = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register/passenger" -Method POST -ContentType "application/json" -Body $pRegBody
Write-Host "  Passenger Registered:" $pReg.email "[ID:" $pReg.id "]" -ForegroundColor Green

$dRegBody = @{ name="Driver Test"; email=$dEmail; phone="+94779998877"; password=$pass } | ConvertTo-Json
$dReg = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/register/driver" -Method POST -ContentType "application/json" -Body $dRegBody
Write-Host "  Driver Registered   :" $dReg.email "[ID:" $dReg.id "]" -ForegroundColor Green

$pLogBody = @{ email=$pEmail; password=$pass } | ConvertTo-Json
$pLogin = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -ContentType "application/json" -Body $pLogBody
$pToken = $pLogin.accessToken
Write-Host "  Passenger JWT Issued (Role:" $pLogin.role ")" -ForegroundColor Green

$dLogBody = @{ email=$dEmail; password=$pass } | ConvertTo-Json
$dLogin = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" -Method POST -ContentType "application/json" -Body $dLogBody
$dToken = $dLogin.accessToken
Write-Host "  Driver JWT Issued    (Role:" $dLogin.role ")" -ForegroundColor Green

# 3. Driver Service (8082) Profile, Vehicle, Location, Availability
Write-Host "`n[3/5] Driver Service (8082) - Profile, Fleet and Availability..." -ForegroundColor Yellow
$dHeaders = @{ Authorization = "Bearer " + $dToken; "Content-Type" = "application/json" }
$pHeaders = @{ Authorization = "Bearer " + $pToken; "Content-Type" = "application/json" }

$profBody = @{ licenseNumber="B" + $ts; licenseExpiryDate="2030-12-31"; serviceArea="Colombo" } | ConvertTo-Json
$prof = Invoke-RestMethod -Uri "http://localhost:8082/api/drivers/profile" -Method POST -Headers $dHeaders -Body $profBody
Write-Host "  Driver Profile Created. License:" $prof.licenseNumber -ForegroundColor Green

$vehBody = @{ registrationNumber="WP-CAB-" + ($ts % 10000); make="Toyota"; model="Prius"; manufactureYear=2022; colour="Silver"; vehicleType="CAR"; passengerCapacity=4 } | ConvertTo-Json
$veh = Invoke-RestMethod -Uri "http://localhost:8082/api/drivers/me/vehicles" -Method POST -Headers $dHeaders -Body $vehBody
Write-Host "  Vehicle Registered:" $veh.make $veh.model "[ID:" $veh.id "]" -ForegroundColor Green

$vehActiveUrl = "http://localhost:8082/api/drivers/me/vehicles/" + $veh.id + "/active?active=true"
Invoke-RestMethod -Uri $vehActiveUrl -Method PATCH -Headers $dHeaders | Out-Null
Write-Host "  Vehicle Activated : True" -ForegroundColor Green

$locBody = @{ serviceArea="Colombo"; currentLatitude=6.9344; currentLongitude=79.8428 } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8082/api/drivers/me/location" -Method PATCH -Headers $dHeaders -Body $locBody | Out-Null
Write-Host "  Simulated Location: (6.9344, 79.8428)" -ForegroundColor Green

$availBody = @{ availabilityStatus="AVAILABLE" } | ConvertTo-Json
$avail = Invoke-RestMethod -Uri "http://localhost:8082/api/drivers/me/availability" -Method PATCH -Headers $dHeaders -Body $availBody
Write-Host "  Availability Status:" $avail.availabilityStatus -ForegroundColor Green

# 4. Fare Service (8084) Estimation
Write-Host "`n[4/5] Fare Service (8084) - Fare Estimation..." -ForegroundColor Yellow
$estBody = @{ distanceKm=5.5; durationMinutes=15; vehicleType="CAR"; pickupTime=[DateTime]::UtcNow.ToString("o") } | ConvertTo-Json
$est = Invoke-RestMethod -Uri "http://localhost:8084/api/fares/estimate" -Method POST -Headers $pHeaders -Body $estBody
Write-Host "  Fare Estimate Calculated: LKR" $est.estimatedFare -ForegroundColor Green

# 5. Ride Service (8083) Full Lifecycle and Payment Settlement (8084)
Write-Host "`n[5/5] Ride Management (8083) and Settlement (8084) - Full Lifecycle..." -ForegroundColor Yellow
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
} | ConvertTo-Json

$ride = Invoke-RestMethod -Uri "http://localhost:8083/api/rides" -Method POST -Headers $pHeaders -Body $rideReq
Write-Host "  1. Ride Created   : ID:" $ride.id "| Status:" $ride.status "| Driver:" $ride.driverAccountId -ForegroundColor Green

$accUrl = "http://localhost:8083/api/rides/" + $ride.id + "/accept"
$acc = Invoke-RestMethod -Uri $accUrl -Method POST -Headers $dHeaders
Write-Host "  2. Ride Accepted  : Status:" $acc.status -ForegroundColor Green

$startUrl = "http://localhost:8083/api/rides/" + $ride.id + "/start"
$start = Invoke-RestMethod -Uri $startUrl -Method POST -Headers $dHeaders
Write-Host "  3. Trip Started   : Status:" $start.status -ForegroundColor Green

$compBody = @{ actualDistanceKm=5.8; actualDurationMinutes=16 } | ConvertTo-Json
$compUrl = "http://localhost:8083/api/rides/" + $ride.id + "/complete"
$comp = Invoke-RestMethod -Uri $compUrl -Method POST -Headers $dHeaders -Body $compBody
Write-Host "  4. Trip Completed : Status:" $comp.status "| Final Fare ID:" $comp.finalFareId "| Amount: LKR" $comp.finalFareAmount -ForegroundColor Green

$idempotencyKey = [Guid]::NewGuid().ToString()
$payHeaders = @{ Authorization = "Bearer " + $pToken; "Content-Type" = "application/json"; "Idempotency-Key" = $idempotencyKey }
$payBody = @{
    fareId = $comp.finalFareId;
    amount = $comp.finalFareAmount;
    paymentMethod = "CARD";
    idempotencyKey = $idempotencyKey
} | ConvertTo-Json

$pay = Invoke-RestMethod -Uri "http://localhost:8084/api/payments" -Method POST -Headers $payHeaders -Body $payBody
Write-Host "  5. Payment Settled: Status:" $pay.status "| TxRef:" $pay.transactionReference "| Amount: LKR" $pay.amount -ForegroundColor Green

$rcptUrl = "http://localhost:8084/api/receipts/payment/" + $pay.paymentId
$rcpt = Invoke-RestMethod -Uri $rcptUrl -Method GET -Headers $pHeaders
Write-Host "  6. Digital Receipt: Receipt ID:" $rcpt.receiptId "| Status:" $rcpt.receiptNumber -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " ALL 4 MICROSERVICES AND APIS ARE 100% OPERATIONAL!" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Cyan
