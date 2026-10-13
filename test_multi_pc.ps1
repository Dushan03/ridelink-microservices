$ErrorActionPreference = 'Stop'

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " RideLink Microservices - Multi-PC API Verification Test" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$driverHost  = "http://172.20.10.3:8082"
$accountHost = "http://localhost:8081"
$rideHost    = "http://localhost:8083"
$fareHost    = "http://localhost:8084"

# 1. Health Checks
Write-Host "`n[1/5] Checking Health of Services across computers..." -ForegroundColor Yellow
$hAcc = Invoke-RestMethod -Uri "$accountHost/actuator/health"
Write-Host "  Account Service (Local 8081): $($hAcc.status)" -ForegroundColor Green

$hDrv = Invoke-RestMethod -Uri "$driverHost/actuator/health"
Write-Host "  Driver Service (Remote 172.20.10.3:8082): $($hDrv.status)" -ForegroundColor Green

$hRid = Invoke-RestMethod -Uri "$rideHost/actuator/health"
Write-Host "  Ride Service (Local 8083): $($hRid.status)" -ForegroundColor Green

$hFar = Invoke-RestMethod -Uri "$fareHost/actuator/health"
Write-Host "  Fare Service (Local 8084): $($hFar.status)" -ForegroundColor Green

# 2. Account Service
Write-Host "`n[2/5] Registering & Logging in Passenger and Driver on Account Service (8081)..." -ForegroundColor Yellow
$ts = (Get-Date).Ticks
$pEmail = "pass_$ts@test.com"
$dEmail = "driv_$ts@test.com"
$pwd = "Password123!"

$pReg = Invoke-RestMethod -Uri "$accountHost/api/auth/register/passenger" -Method POST -ContentType "application/json" -Body (@{name="Passenger Test"; email=$pEmail; password=$pwd; phone="+94771112233"} | ConvertTo-Json)
Write-Host "  Passenger Registered: $($pReg.email)" -ForegroundColor Green

$dReg = Invoke-RestMethod -Uri "$accountHost/api/auth/register/driver" -Method POST -ContentType "application/json" -Body (@{name="Driver Test"; email=$dEmail; password=$pwd; phone="+94779998877"} | ConvertTo-Json)
Write-Host "  Driver Registered: $($dReg.email)" -ForegroundColor Green

$pLog = Invoke-RestMethod -Uri "$accountHost/api/auth/login" -Method POST -ContentType "application/json" -Body (@{email=$pEmail; password=$pwd} | ConvertTo-Json)
$pToken = $pLog.accessToken
Write-Host "  Passenger JWT Issued: OK" -ForegroundColor Green

$dLog = Invoke-RestMethod -Uri "$accountHost/api/auth/login" -Method POST -ContentType "application/json" -Body (@{email=$dEmail; password=$pwd} | ConvertTo-Json)
$dToken = $dLog.accessToken
Write-Host "  Driver JWT Issued: OK" -ForegroundColor Green

# 3. Driver & Vehicle Service on Remote PC
Write-Host "`n[3/5] Setting up Driver on Remote PC ($driverHost)..." -ForegroundColor Yellow
$dHeaders = @{ Authorization = "Bearer $dToken"; "Content-Type" = "application/json" }

$area = "Colombo_$($ts % 10000)"

$dProf = Invoke-RestMethod -Uri "$driverHost/api/drivers/profile" -Method POST -Headers $dHeaders -Body (@{licenseNumber="B$ts"; licenseExpiryDate="2030-12-31"; serviceArea=$area} | ConvertTo-Json)
Write-Host "  Driver Profile Created on Remote PC: License $($dProf.licenseNumber)" -ForegroundColor Green

$vehReq = @{
    registrationNumber = "WP-CAB-$($ts % 10000)";
    make = "Toyota";
    model = "Prius";
    manufactureYear = 2022;
    colour = "Silver";
    vehicleType = "CAR";
    passengerCapacity = 4
}
$veh = Invoke-RestMethod -Uri "$driverHost/api/drivers/me/vehicles" -Method POST -Headers $dHeaders -Body ($vehReq | ConvertTo-Json)
Write-Host "  Vehicle Registered: $($veh.make) $($veh.model) [ID: $($veh.id)]" -ForegroundColor Green

Invoke-RestMethod -Uri "$driverHost/api/drivers/me/vehicles/$($veh.id)/active?active=true" -Method PATCH -Headers $dHeaders | Out-Null
Write-Host "  Vehicle Activated: OK" -ForegroundColor Green

Invoke-RestMethod -Uri "$driverHost/api/drivers/me/location" -Method PATCH -Headers $dHeaders -Body (@{serviceArea=$area; currentLatitude=6.9344; currentLongitude=79.8428} | ConvertTo-Json) | Out-Null
Write-Host "  Driver Location Set: $area (6.9344, 79.8428)" -ForegroundColor Green

$avail = Invoke-RestMethod -Uri "$driverHost/api/drivers/me/availability" -Method PATCH -Headers $dHeaders -Body (@{availabilityStatus="AVAILABLE"} | ConvertTo-Json)
Write-Host "  Driver Status on Remote PC: $($avail.availabilityStatus)" -ForegroundColor Green

# 4. Fare Estimation
Write-Host "`n[4/5] Testing Fare Estimation on Fare Service (8084)..." -ForegroundColor Yellow
$pHeaders = @{ Authorization = "Bearer $pToken"; "Content-Type" = "application/json" }
$est = Invoke-RestMethod -Uri "$fareHost/api/fares/estimate" -Method POST -Headers $pHeaders -Body (@{distanceKm=5.5; durationMinutes=15; vehicleType="CAR"; pickupTime=(Get-Date).ToString("o")} | ConvertTo-Json)
Write-Host "  Fare Estimate: LKR $($est.estimatedFare)" -ForegroundColor Green

# 5. Ride Service Lifecycle across PCs
Write-Host "`n[5/5] Testing Ride Lifecycle & Payment across Computers..." -ForegroundColor Yellow
$rideReq = @{
    pickupName = "Colombo Fort";
    pickupLatitude = 6.9344;
    pickupLongitude = 79.8428;
    destinationName = "Galle Face Green";
    destinationLatitude = 6.9271;
    destinationLongitude = 79.8446;
    serviceArea = $area;
    requestedVehicleType = "CAR";
    estimatedDistanceKm = 5.5;
    estimatedDurationMinutes = 15.0
}
$ride = Invoke-RestMethod -Uri "$rideHost/api/rides" -Method POST -Headers $pHeaders -Body ($rideReq | ConvertTo-Json)
Write-Host "  [Step 1] Ride Created & MATCHED across PCs! ID: $($ride.id) | Status: $($ride.status) | Driver: $($ride.driverAccountId)" -ForegroundColor Green

$acc = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/accept" -Method POST -Headers $dHeaders
Write-Host "  [Step 2] Remote Driver Accepted Ride: $($acc.status)" -ForegroundColor Green

$st = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/start" -Method POST -Headers $dHeaders
Write-Host "  [Step 3] Remote Driver Started Trip: $($st.status)" -ForegroundColor Green

$comp = Invoke-RestMethod -Uri "$rideHost/api/rides/$($ride.id)/complete" -Method POST -Headers $dHeaders -Body (@{ actualDistanceKm = 5.8; actualDurationMinutes = 16.0 } | ConvertTo-Json)
Write-Host "  [Step 4] Trip Completed! Final Fare ID: $($comp.finalFareId) | Amount: LKR $($comp.finalFareAmount)" -ForegroundColor Green

$idempotencyKey = [System.Guid]::NewGuid().ToString()
$payHeaders = @{ Authorization = "Bearer $pToken"; "Content-Type" = "application/json"; "Idempotency-Key" = $idempotencyKey }
$payment = Invoke-RestMethod -Uri "$fareHost/api/payments" -Method POST -Headers $payHeaders -Body (@{ fareId = "$($comp.finalFareId)"; paymentMethod = "DIGITAL_WALLET" } | ConvertTo-Json)
Write-Host "  [Step 5] Payment Processed on Fare Service (8084): Status $($payment.status)" -ForegroundColor Green

$receiptId = $payment.receipt.receiptId
$receipt = Invoke-RestMethod -Uri "$fareHost/api/receipts/$receiptId" -Method GET -Headers $pHeaders
Write-Host "  [Step 6] Digital Receipt Verified: ID $($receipt.receiptId) | Total: LKR $($receipt.totalAmountCharged)" -ForegroundColor Green

Write-Host "`n==========================================================" -ForegroundColor Cyan
Write-Host " SUCCESS: ALL 4 MICROSERVICES WORKED ACROSS BOTH PCS!" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
