/**
 * RideLink — Unified Frontend Application
 * Interacts seamlessly with all 4 backend microservices:
 * - Service 1: Account Service (Port 8081)
 * - Service 2: Driver & Vehicle Service (Port 8082)
 * - Service 3: Ride Management Service (Port 8083)
 * - Service 4: Fare & Payment Service (Port 8084)
 */

// Global State
const state = {
    token: localStorage.getItem('ridelink_jwt') || null,
    account: JSON.parse(localStorage.getItem('ridelink_user') || 'null'),
    theme: localStorage.getItem('ridelink_theme') || 'dark',
    activeRideId: localStorage.getItem('ridelink_active_ride_id') || null,
    endpoints: JSON.parse(localStorage.getItem('ridelink_endpoints') || JSON.stringify({
        account: 'http://localhost:8081',
        driver: 'http://localhost:8082',
        ride: 'http://localhost:8083',
        fare: 'http://localhost:8084'
    }))
};

// ==========================================================================
// Initialization
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    initTheme();
    initTabs();
    initSwaggerMenu();
    initEndpointsConfig();
    updateAuthDisplay();
    setupEventListeners();
    generateNewIdempotencyKey();
    checkAllServiceHealth();
    
    // Auto-poll health every 15 seconds
    setInterval(checkAllServiceHealth, 15000);
});

// ==========================================================================
// Theme Management
// ==========================================================================
function initTheme() {
    if (state.theme === 'light') {
        document.body.setAttribute('data-theme', 'light');
    }
    const themeToggle = document.getElementById('themeToggle');
    themeToggle?.addEventListener('click', () => {
        const isLight = document.body.getAttribute('data-theme') === 'light';
        if (isLight) {
            document.body.removeAttribute('data-theme');
            state.theme = 'dark';
        } else {
            document.body.setAttribute('data-theme', 'light');
            state.theme = 'light';
        }
        localStorage.setItem('ridelink_theme', state.theme);
    });
}

// ==========================================================================
// Navigation & Tabs
// ==========================================================================
function initTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    const tabPanes = document.querySelectorAll('.tab-pane');

    tabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const targetId = btn.getAttribute('data-tab');
            tabBtns.forEach(b => b.classList.remove('active'));
            tabPanes.forEach(p => p.classList.remove('active'));

            btn.classList.add('active');
            document.getElementById(targetId)?.classList.add('active');

            // Auto-refresh relevant data on tab open
            if (targetId === 'tab-profile' && state.token) loadUserProfile();
            if (targetId === 'tab-rides' && state.token) {
                if (state.activeRideId) refreshActiveRide();
                loadPassengerRides();
            }
            if (targetId === 'tab-drivers' && state.token) {
                loadDriverProfile();
                loadDriverRides();
            }
            if (targetId === 'tab-payments' && state.token) loadPaymentHistory();
        });
    });

    // Subtabs (Login / Register)
    const loginBtn = document.getElementById('subtab-login-btn');
    const registerBtn = document.getElementById('subtab-register-btn');
    const loginForm = document.getElementById('loginFormContainer');
    const registerForm = document.getElementById('registerFormContainer');

    loginBtn?.addEventListener('click', () => {
        loginBtn.classList.add('active');
        registerBtn.classList.remove('active');
        loginForm.classList.remove('hidden');
        registerForm.classList.add('hidden');
    });

    registerBtn?.addEventListener('click', () => {
        registerBtn.classList.add('active');
        loginBtn.classList.remove('active');
        registerForm.classList.remove('hidden');
        loginForm.classList.add('hidden');
    });
}

// Swagger dropdown menu toggle
function initSwaggerMenu() {
    const btn = document.getElementById('btnSwaggerDropdown');
    const menu = document.getElementById('swaggerMenu');
    btn?.addEventListener('click', (e) => {
        e.stopPropagation();
        menu.classList.toggle('show');
        updateSwaggerUrls();
    });
    document.addEventListener('click', () => menu?.classList.remove('show'));
}

function updateSwaggerUrls() {
    document.getElementById('linkSwagAccount').href = `${state.endpoints.account}/swagger-ui/index.html`;
    document.getElementById('linkSwagDriver').href = `${state.endpoints.driver}/swagger-ui/index.html`;
    document.getElementById('linkSwagRide').href = `${state.endpoints.ride}/swagger-ui/index.html`;
    document.getElementById('linkSwagFare').href = `${state.endpoints.fare}/swagger-ui/index.html`;
}

// ==========================================================================
// Network Inspector Logging & API Fetch Helper
// ==========================================================================
async function apiCall(service, path, method = 'GET', body = null, customHeaders = {}) {
    const baseUrl = state.endpoints[service] || 'http://localhost:8080';
    const url = `${baseUrl}${path}`;

    const headers = {
        'Content-Type': 'application/json',
        ...customHeaders
    };

    if (state.token && !headers['Authorization']) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }

    const options = {
        method,
        headers
    };

    if (body && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
        options.body = JSON.stringify(body);
    }

    const startTime = performance.now();
    try {
        const response = await fetch(url, options);
        const duration = Math.round(performance.now() - startTime);

        let data = null;
        const text = await response.text();
        try {
            data = text ? JSON.parse(text) : {};
        } catch {
            data = { rawResponse: text };
        }

        logNetwork(method, url, response.status, duration, body, data);

        if (!response.ok) {
            const errorMsg = data.message || data.error || `HTTP error ${response.status}`;
            throw new Error(errorMsg);
        }

        return data;
    } catch (err) {
        if (!err.message.includes('HTTP error')) {
            logNetwork(method, url, 'ERR', 0, body, { error: 'Network Error', message: err.message });
        }
        throw err;
    }
}

function logNetwork(method, url, status, duration, requestBody, responseBody) {
    const consoleBox = document.getElementById('networkConsole');
    if (!consoleBox) return;

    if (consoleBox.querySelector('.console-placeholder')) {
        consoleBox.innerHTML = '';
    }

    const entry = document.createElement('div');
    entry.className = 'log-entry';

    const statusClass = typeof status === 'number' ? `log-status-${status}` : 'log-status-500';

    entry.innerHTML = `
        <div class="log-meta">
            <span class="log-method method-${method}">${method}</span>
            <span class="${statusClass}"><strong>${status}</strong></span>
            <span style="color:var(--text-muted); font-size:0.7rem;">${duration}ms</span>
            <span style="color:var(--text-muted); font-size:0.7rem; margin-left:auto;">${new Date().toLocaleTimeString()}</span>
        </div>
        <div style="font-size:0.72rem; color:var(--text-primary); margin-bottom:4px; word-break:break-all;">${url}</div>
        ${requestBody ? `<div style="font-size:0.68rem; color:var(--text-muted); margin-bottom:2px;">Request Body:</div><div class="log-body" style="margin-bottom:6px;">${escapeHtml(JSON.stringify(requestBody, null, 2))}</div>` : ''}
        <div style="font-size:0.68rem; color:var(--text-muted); margin-bottom:2px;">Response Body:</div>
        <div class="log-body">${escapeHtml(JSON.stringify(responseBody, null, 2))}</div>
    `;

    consoleBox.prepend(entry);
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

// ==========================================================================
// Service Health Poller
// ==========================================================================
async function checkAllServiceHealth() {
    const checks = [
        { id: 'chipAccount', url: `${state.endpoints.account}/actuator/health` },
        { id: 'chipDriver', url: `${state.endpoints.driver}/actuator/health` },
        { id: 'chipRide', url: `${state.endpoints.ride}/actuator/health` },
        { id: 'chipFare', url: `${state.endpoints.fare}/actuator/health` }
    ];

    checks.forEach(async (chk) => {
        const chip = document.getElementById(chk.id);
        if (!chip) return;
        try {
            const res = await fetch(chk.url, { method: 'GET', signal: AbortSignal.timeout(2500) });
            if (res.ok) {
                chip.classList.add('online');
                chip.classList.remove('offline');
            } else {
                chip.classList.add('offline');
                chip.classList.remove('online');
            }
        } catch {
            chip.classList.add('offline');
            chip.classList.remove('online');
        }
    });
}

// ==========================================================================
// Network Endpoints Modal Config (Multi-PC Setup)
// ==========================================================================
function initEndpointsConfig() {
    const modal = document.getElementById('configModal');
    const btnOpen = document.getElementById('btnOpenConfig');
    const btnClose = document.getElementById('btnCloseConfig');
    const form = document.getElementById('formConfigEndpoints');
    const btnReset = document.getElementById('btnResetEndpoints');
    const btnTest = document.getElementById('btnTestEndpoints');

    btnOpen?.addEventListener('click', () => {
        document.getElementById('cfgAccountUrl').value = state.endpoints.account;
        document.getElementById('cfgDriverUrl').value = state.endpoints.driver;
        document.getElementById('cfgRideUrl').value = state.endpoints.ride;
        document.getElementById('cfgFareUrl').value = state.endpoints.fare;
        modal.classList.remove('hidden');
    });

    btnClose?.addEventListener('click', () => modal.classList.add('hidden'));

    form?.addEventListener('submit', (e) => {
        e.preventDefault();
        state.endpoints.account = document.getElementById('cfgAccountUrl').value.trim();
        state.endpoints.driver = document.getElementById('cfgDriverUrl').value.trim();
        state.endpoints.ride = document.getElementById('cfgRideUrl').value.trim();
        state.endpoints.fare = document.getElementById('cfgFareUrl').value.trim();

        localStorage.setItem('ridelink_endpoints', JSON.stringify(state.endpoints));
        showToast('Service endpoints updated successfully!', 'success');
        modal.classList.add('hidden');
        checkAllServiceHealth();
    });

    btnReset?.addEventListener('click', () => {
        state.endpoints = {
            account: 'http://localhost:8081',
            driver: 'http://localhost:8082',
            ride: 'http://localhost:8083',
            fare: 'http://localhost:8084'
        };
        localStorage.setItem('ridelink_endpoints', JSON.stringify(state.endpoints));
        document.getElementById('cfgAccountUrl').value = state.endpoints.account;
        document.getElementById('cfgDriverUrl').value = state.endpoints.driver;
        document.getElementById('cfgRideUrl').value = state.endpoints.ride;
        document.getElementById('cfgFareUrl').value = state.endpoints.fare;
        showToast('Reset to localhost defaults', 'success');
        checkAllServiceHealth();
    });

    btnTest?.addEventListener('click', async () => {
        showToast('Testing all 4 services...', 'info');
        await checkAllServiceHealth();
        showToast('Health checks complete. View top status chips.', 'success');
    });
}

// ==========================================================================
// Authentication & Profiles (Service 1 · Port 8081)
// ==========================================================================
function setupEventListeners() {
    // Login Form
    document.getElementById('formLogin')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const email = document.getElementById('loginEmail').value.trim();
        const password = document.getElementById('loginPassword').value;

        try {
            const data = await apiCall('account', '/api/auth/login', 'POST', { email, password });
            state.token = data.accessToken || data.token;
            state.account = {
                id: data.accountId || data.id,
                name: email.split('@')[0],
                email: email,
                role: data.role || 'PASSENGER',
                status: 'ACTIVE'
            };

            localStorage.setItem('ridelink_jwt', state.token);
            localStorage.setItem('ridelink_user', JSON.stringify(state.account));

            updateAuthDisplay();
            showToast(`Welcome! Logged in as ${state.account.role}`, 'success');

            // Auto switch to relevant tab
            if (state.account.role === 'DRIVER') {
                document.querySelector('[data-tab="tab-drivers"]')?.click();
            } else {
                document.querySelector('[data-tab="tab-rides"]')?.click();
            }
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Register Form
    document.getElementById('formRegister')?.addEventListener('submit', async (e) => {
        e.preventDefault();
        const role = document.querySelector('input[name="regRole"]:checked')?.value || 'PASSENGER';
        const name = document.getElementById('regName').value.trim();
        const email = document.getElementById('regEmail').value.trim();
        const phone = document.getElementById('regPhone').value.trim();
        const password = document.getElementById('regPassword').value;

        const endpoint = role === 'DRIVER' ? '/api/auth/register/driver' : '/api/auth/register/passenger';

        try {
            const data = await apiCall('account', endpoint, 'POST', {
                name, email, phone, password
            });

            showToast(`Registration successful as ${role}! Please log in.`, 'success');
            document.getElementById('subtab-login-btn')?.click();
            document.getElementById('loginEmail').value = email;
            document.getElementById('loginPassword').value = password;
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Logout
    document.getElementById('btnLogout')?.addEventListener('click', () => {
        state.token = null;
        state.account = null;
        localStorage.removeItem('ridelink_jwt');
        localStorage.removeItem('ridelink_user');
        updateAuthDisplay();
        showToast('Logged out successfully', 'info');
        document.querySelector('[data-tab="tab-auth"]')?.click();
    });

    // Copy JWT Token
    document.getElementById('btnCopyJwt')?.addEventListener('click', () => {
        if (!state.token) {
            showToast('No active token to copy', 'error');
            return;
        }
        navigator.clipboard.writeText(state.token);
        showToast('JWT Token copied to clipboard!', 'success');
    });

    // Clear Logs
    document.getElementById('btnClearLogs')?.addEventListener('click', () => {
        document.getElementById('networkConsole').innerHTML = '<div class="console-placeholder">Waiting for your next API call...</div>';
    });

    // Ride Booking actions
    document.getElementById('btnCalculateEstimate')?.addEventListener('click', calculateFareEstimate);
    document.getElementById('btnRequestRide')?.addEventListener('click', requestRide);
    document.getElementById('btnRefreshActiveRide')?.addEventListener('click', refreshActiveRide);
    document.getElementById('btnRetryDriverAssign')?.addEventListener('click', retryDriverAssign);
    document.getElementById('btnCancelRide')?.addEventListener('click', cancelActiveRide);
    document.getElementById('btnLoadMyRides')?.addEventListener('click', loadPassengerRides);

    // Driver Hub actions
    document.getElementById('btnSetAvailable')?.addEventListener('click', () => updateDriverAvailability('AVAILABLE'));
    document.getElementById('btnSetOffline')?.addEventListener('click', () => updateDriverAvailability('OFFLINE'));
    document.getElementById('formDriverProfile')?.addEventListener('submit', saveDriverProfile);
    document.getElementById('btnRefreshDriverProfile')?.addEventListener('click', loadDriverProfile);
    document.getElementById('formRegisterVehicle')?.addEventListener('submit', registerVehicle);
    document.getElementById('btnLoadDriverRides')?.addEventListener('click', loadDriverRides);

    // Fare & Payment actions
    document.getElementById('formProcessPayment')?.addEventListener('submit', processPayment);
    document.getElementById('btnLoadPaymentHistory')?.addEventListener('click', loadPaymentHistory);

    // Admin Actions
    document.getElementById('formAdminStatus')?.addEventListener('submit', updateAccountStatusAdmin);
    document.getElementById('btnAdminSearchDrivers')?.addEventListener('click', adminSearchEligibleDrivers);
}

function updateAuthDisplay() {
    const tokenDisplay = document.getElementById('jwtTokenDisplay');
    if (state.token) {
        tokenDisplay.textContent = state.token;
        tokenDisplay.title = 'Click Copy button above';
        document.getElementById('unauthenticatedWarning')?.classList.add('hidden');
        document.getElementById('profileDetailsCard')?.classList.remove('hidden');
    } else {
        tokenDisplay.textContent = 'No active token. Please log in.';
        document.getElementById('unauthenticatedWarning')?.classList.remove('hidden');
        document.getElementById('profileDetailsCard')?.classList.add('hidden');
    }
}

// Quick Login Helper
window.setQuickLogin = function(email, password) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = password;
};

// Preset Route Helper
window.setPresetRoute = function(fromName, fromLat, fromLng, toName, toLat, toLng) {
    document.getElementById('ridePickupName').value = fromName;
    document.getElementById('ridePickupLat').value = fromLat;
    document.getElementById('ridePickupLng').value = fromLng;
    document.getElementById('rideDropoffName').value = toName;
    document.getElementById('rideDropoffLat').value = toLat;
    document.getElementById('rideDropoffLng').value = toLng;
    calculateFareEstimate();
};

// ==========================================================================
// Ride Management Service (Service 3 · Port 8083) & Estimates (Port 8084)
// ==========================================================================
function getCoordinatesDistance(lat1, lon1, lat2, lon2) {
    // Haversine formula approximation in KM
    const R = 6371;
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
              Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
              Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return Math.max(1.0, Math.round(R * c * 10) / 10);
}

async function calculateFareEstimate() {
    const pLat = parseFloat(document.getElementById('ridePickupLat').value);
    const pLng = parseFloat(document.getElementById('ridePickupLng').value);
    const dLat = parseFloat(document.getElementById('rideDropoffLat').value);
    const dLng = parseFloat(document.getElementById('rideDropoffLng').value);
    const vehicleType = document.getElementById('rideVehicleType').value;

    const distanceKm = getCoordinatesDistance(pLat, pLng, dLat, dLng);
    const durationMinutes = Math.max(5, Math.round(distanceKm * 2.5));

    try {
        const est = await apiCall('fare', '/api/fares/estimate', 'POST', {
            distanceKm,
            durationMinutes,
            vehicleType,
            pickupTime: new Date().toISOString()
        });

        const card = document.getElementById('estimateResultCard');
        card.classList.remove('hidden');
        document.getElementById('estAmountDisplay').textContent = `LKR ${est.estimatedFare ? est.estimatedFare.toFixed(2) : est.baseFare.toFixed(2)}`;
        document.getElementById('estDetailsDisplay').textContent = `Distance: ${distanceKm} km | Est. Duration: ${durationMinutes} mins | Vehicle: ${vehicleType}`;
        showToast(`Fare Estimate: LKR ${est.estimatedFare?.toFixed(2)}`, 'success');
    } catch (err) {
        showToast(`Estimate Error: ${err.message}`, 'error');
    }
}

async function requestRide() {
    if (!state.token) {
        showToast('Please sign in as a Passenger first!', 'error');
        document.querySelector('[data-tab="tab-auth"]')?.click();
        return;
    }

    const pickupName = document.getElementById('ridePickupName').value.trim();
    const destinationName = document.getElementById('rideDropoffName').value.trim();
    const pickupLatitude = parseFloat(document.getElementById('ridePickupLat').value);
    const pickupLongitude = parseFloat(document.getElementById('ridePickupLng').value);
    const destinationLatitude = parseFloat(document.getElementById('rideDropoffLat').value);
    const destinationLongitude = parseFloat(document.getElementById('rideDropoffLng').value);
    const requestedVehicleType = document.getElementById('rideVehicleType').value;

    const estimatedDistanceKm = getCoordinatesDistance(pickupLatitude, pickupLongitude, destinationLatitude, destinationLongitude);
    const estimatedDurationMinutes = Math.max(5, Math.round(estimatedDistanceKm * 2.5));

    try {
        const ride = await apiCall('ride', '/api/rides', 'POST', {
            pickupName,
            pickupLatitude,
            pickupLongitude,
            destinationName,
            destinationLatitude,
            destinationLongitude,
            serviceArea: 'Colombo',
            requestedVehicleType,
            estimatedDistanceKm,
            estimatedDurationMinutes
        });

        state.activeRideId = ride.id || ride.rideId;
        localStorage.setItem('ridelink_active_ride_id', state.activeRideId);

        renderActiveRide(ride);
        showToast(`Ride created! Status: ${ride.status}`, 'success');
        loadPassengerRides();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function refreshActiveRide() {
    if (!state.activeRideId) return;

    try {
        const ride = await apiCall('ride', `/api/rides/${state.activeRideId}`, 'GET');
        renderActiveRide(ride);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderActiveRide(ride) {
    document.getElementById('noActiveRidePlaceholder').classList.add('hidden');
    document.getElementById('activeRideDetails').classList.remove('hidden');

    const rideId = ride.id || ride.rideId;
    document.getElementById('activeRideId').textContent = rideId;
    document.getElementById('activeRideStatusBadge').textContent = ride.status;
    document.getElementById('activeRideFareDisplay').textContent = ride.estimatedFareAmount ? `LKR ${parseFloat(ride.estimatedFareAmount).toFixed(2)}` : (ride.estimatedFare ? `LKR ${ride.estimatedFare}` : 'Calculating...');
    document.getElementById('activeRideDriverDisplay').textContent = ride.driverAccountId ? `Driver Account: ${ride.driverAccountId.substring(0, 8)}...` : (ride.driverProfileId ? `Driver Profile: ${ride.driverProfileId.substring(0, 8)}...` : 'Searching / Unassigned');

    // Update status stepper
    const steps = ['REQUESTED', 'ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED'];
    const currentStatus = ride.status;
    const currentIdx = steps.indexOf(currentStatus);

    steps.forEach((st, idx) => {
        const stepEl = document.getElementById(`step-${st}`);
        if (!stepEl) return;
        stepEl.classList.remove('active', 'completed');

        if (idx < currentIdx) {
            stepEl.classList.add('completed');
        } else if (idx === currentIdx) {
            stepEl.classList.add('active');
        }
    });

    // Auto-fill fare in payment tab if completed
    if (ride.status === 'COMPLETED' && (ride.finalFareId || ride.fareId)) {
        document.getElementById('payFareId').value = ride.finalFareId || ride.fareId;
        if (ride.finalFareAmount || ride.finalFare) {
            document.getElementById('payAmount').value = ride.finalFareAmount || ride.finalFare;
        }
    }
}

async function retryDriverAssign() {
    if (!state.activeRideId) return;
    try {
        const ride = await apiCall('ride', `/api/rides/${state.activeRideId}/assign`, 'POST');
        renderActiveRide(ride);
        showToast(`Driver matching result: ${ride.status}`, 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function cancelActiveRide() {
    if (!state.activeRideId) return;
    try {
        const ride = await apiCall('ride', `/api/rides/${state.activeRideId}/cancel`, 'POST', {
            reason: 'Cancelled by Passenger via UI'
        });
        renderActiveRide(ride);
        showToast('Ride cancelled.', 'info');
        loadPassengerRides();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadPassengerRides() {
    if (!state.token) return;
    const tbody = document.querySelector('#tablePassengerRides tbody');
    try {
        const rides = await apiCall('ride', '/api/rides/my', 'GET');
        if (!rides || rides.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No ride history found.</td></tr>';
            return;
        }

        tbody.innerHTML = rides.map(r => `
            <tr>
                <td class="copyable" onclick="navigator.clipboard.writeText('${r.id || r.rideId}')">${(r.id || r.rideId).substring(0, 8)}...</td>
                <td>${escapeHtml(r.pickupName || r.pickupLocation)} ➔ ${escapeHtml(r.destinationName || r.dropoffLocation)}</td>
                <td>${r.requestedVehicleType || r.vehicleType}</td>
                <td><span class="badge ${r.status === 'COMPLETED' ? 'badge-success' : (r.status === 'CANCELLED' ? 'badge-danger' : 'badge-warning')}">${r.status}</span></td>
                <td>
                    <button class="btn btn-xs btn-outline" onclick="selectActiveRide('${r.id || r.rideId}')">Track</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center" style="color:var(--color-danger);">${err.message}</td></tr>`;
    }
}

window.selectActiveRide = function(rideId) {
    state.activeRideId = rideId;
    localStorage.setItem('ridelink_active_ride_id', rideId);
    refreshActiveRide();
    document.getElementById('activeRideCard').scrollIntoView({ behavior: 'smooth' });
};

// ==========================================================================
// Driver & Vehicle Operations (Service 2 · Port 8082 + 8083 Driver Actions)
// ==========================================================================
async function loadDriverProfile() {
    if (!state.token) return;
    try {
        const profile = await apiCall('driver', '/api/drivers/me', 'GET');
        document.getElementById('driverAvailabilityBadge').textContent = profile.availabilityStatus || 'OFFLINE';
        document.getElementById('driverLicenseNo').value = profile.licenseNumber || '';
        document.getElementById('driverLicenseExp').value = profile.licenseExpiryDate || '2030-12-31';
        document.getElementById('driverServiceArea').value = profile.serviceArea || 'Colombo';
        document.getElementById('driverLat').value = profile.currentLatitude || 6.9340;
        document.getElementById('driverLng').value = profile.currentLongitude || 79.8450;
        showToast('Driver profile loaded', 'info');
    } catch (err) {
        // If not created yet, encourage saving profile
        document.getElementById('driverAvailabilityBadge').textContent = 'NOT CREATED';
    }
}

async function saveDriverProfile(e) {
    e.preventDefault();
    if (!state.token) {
        showToast('Please log in as a DRIVER first', 'error');
        return;
    }

    const licenseNumber = document.getElementById('driverLicenseNo').value.trim();
    const licenseExpiryDate = document.getElementById('driverLicenseExp').value;
    const serviceArea = document.getElementById('driverServiceArea').value.trim();
    const currentLatitude = parseFloat(document.getElementById('driverLat').value);
    const currentLongitude = parseFloat(document.getElementById('driverLng').value);

    try {
        // Try creating profile or updating location
        try {
            await apiCall('driver', '/api/drivers/profile', 'POST', {
                licenseNumber,
                licenseExpiryDate,
                serviceArea
            });
        } catch {
            // Already created, update location
        }

        await apiCall('driver', '/api/drivers/me/location', 'PATCH', {
            serviceArea,
            currentLatitude,
            currentLongitude
        });

        showToast('Driver profile & GPS location saved!', 'success');
        loadDriverProfile();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function updateDriverAvailability(status) {
    if (!state.token) {
        showToast('Please log in as a DRIVER first', 'error');
        return;
    }

    try {
        const res = await apiCall('driver', '/api/drivers/me/availability', 'PATCH', { availabilityStatus: status });
        document.getElementById('driverAvailabilityBadge').textContent = res.availabilityStatus || status;
        showToast(`Availability set to: ${status}`, 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function registerVehicle(e) {
    e.preventDefault();
    if (!state.token) {
        showToast('Please log in as a DRIVER first', 'error');
        return;
    }

    const registrationNumber = document.getElementById('vehRegNo').value.trim();
    const vehicleType = document.getElementById('vehType').value;
    const make = document.getElementById('vehMake').value.trim();
    const model = document.getElementById('vehModel').value.trim();
    const passengerCapacity = parseInt(document.getElementById('vehCapacity').value) || 4;

    try {
        const veh = await apiCall('driver', '/api/drivers/me/vehicles', 'POST', {
            registrationNumber,
            make,
            model,
            manufactureYear: 2022,
            colour: 'Silver',
            vehicleType,
            passengerCapacity
        });

        // Activate vehicle
        const vehId = veh.id || veh.vehicleId;
        if (vehId) {
            await apiCall('driver', `/api/drivers/me/vehicles/${vehId}/active?active=true`, 'PATCH');
        }

        showToast(`Vehicle ${make} ${model} registered & activated!`, 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadDriverRides() {
    if (!state.token) return;
    const tbody = document.querySelector('#tableDriverRides tbody');
    try {
        const rides = await apiCall('ride', '/api/rides/driver/my', 'GET');
        if (!rides || rides.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">No assigned rides currently.</td></tr>';
            return;
        }

        tbody.innerHTML = rides.map(r => `
            <tr>
                <td class="copyable">${r.rideId.substring(0, 8)}...</td>
                <td>${escapeHtml(r.pickupLocation)} ➔ ${escapeHtml(r.dropoffLocation)}</td>
                <td><span class="badge badge-warning">${r.status}</span></td>
                <td>
                    ${r.status === 'ASSIGNED' ? `<button class="btn btn-xs btn-primary" onclick="driverAcceptRide('${r.rideId}')">Accept</button>` : ''}
                    ${r.status === 'ACCEPTED' ? `<button class="btn btn-xs btn-accent" onclick="driverStartTrip('${r.rideId}')">Start Trip</button>` : ''}
                    ${r.status === 'IN_PROGRESS' ? `<button class="btn btn-xs btn-success" onclick="driverCompleteTrip('${r.rideId}')">Complete Trip</button>` : ''}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center" style="color:var(--color-danger);">${err.message}</td></tr>`;
    }
}

window.driverAcceptRide = async function(rideId) {
    try {
        await apiCall('ride', `/api/rides/${rideId}/accept`, 'POST');
        showToast('Ride accepted! Proceed to pickup location.', 'success');
        loadDriverRides();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.driverStartTrip = async function(rideId) {
    try {
        await apiCall('ride', `/api/rides/${rideId}/start`, 'POST');
        showToast('Trip started! Status: IN_PROGRESS', 'success');
        loadDriverRides();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

window.driverCompleteTrip = async function(rideId) {
    const actualDistanceKm = prompt('Enter Actual Distance Completed (KM):', '5.2');
    if (!actualDistanceKm) return;
    const actualDurationMinutes = prompt('Enter Actual Duration (Minutes):', '14');
    if (!actualDurationMinutes) return;

    try {
        const res = await apiCall('ride', `/api/rides/${rideId}/complete`, 'POST', {
            actualDistanceKm: parseFloat(actualDistanceKm),
            actualDurationMinutes: parseInt(actualDurationMinutes)
        });

        showToast('Trip completed and final fare generated!', 'success');
        loadDriverRides();
    } catch (err) {
        showToast(err.message, 'error');
    }
};

// ==========================================================================
// Fare & Payment Service (Service 4 · Port 8084)
// ==========================================================================
window.generateNewIdempotencyKey = function() {
    const uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        const r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
        return v.toString(16);
    });
    const input = document.getElementById('payIdempotencyKey');
    if (input) input.value = uuid;
    return uuid;
};

async function processPayment(e) {
    e.preventDefault();
    if (!state.token) {
        showToast('Please sign in to make payments', 'error');
        return;
    }

    const fareId = document.getElementById('payFareId').value.trim();
    const amount = parseFloat(document.getElementById('payAmount').value);
    const paymentMethod = document.getElementById('payMethod').value;
    const idempotencyKey = document.getElementById('payIdempotencyKey').value || generateNewIdempotencyKey();

    try {
        const payment = await apiCall('fare', '/api/payments', 'POST', {
            fareId,
            amount,
            paymentMethod,
            idempotencyKey
        }, {
            'Idempotency-Key': idempotencyKey
        });

        showToast('Payment processed successfully!', 'success');
        renderReceipt(payment);
        loadPaymentHistory();
        generateNewIdempotencyKey();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderReceipt(payment) {
    const card = document.getElementById('digitalReceiptCard');
    card.classList.remove('hidden');

    document.getElementById('rcptTxId').textContent = payment.transactionReference || payment.id;
    document.getElementById('rcptPaymentId').textContent = payment.paymentId || payment.id;
    document.getElementById('rcptFareId').textContent = payment.fareId;
    document.getElementById('rcptMethod').textContent = payment.paymentMethod;
    document.getElementById('rcptTime').textContent = new Date().toLocaleString();
    document.getElementById('rcptAmount').textContent = `LKR ${payment.amount ? payment.amount.toFixed(2) : '0.00'}`;

    card.scrollIntoView({ behavior: 'smooth' });
}

async function loadPaymentHistory() {
    if (!state.token) return;
    const tbody = document.querySelector('#tablePayments tbody');
    try {
        const payments = await apiCall('fare', '/api/payments/my', 'GET');
        if (!payments || payments.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No payment transactions found.</td></tr>';
            return;
        }

        tbody.innerHTML = payments.map(p => `
            <tr>
                <td class="copyable">${(p.paymentId || p.id).substring(0, 8)}...</td>
                <td><strong>LKR ${p.amount.toFixed(2)}</strong></td>
                <td>${p.paymentMethod}</td>
                <td><span class="badge badge-success">${p.status}</span></td>
                <td>${new Date(p.createdAt || Date.now()).toLocaleTimeString()}</td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center" style="color:var(--color-danger);">${err.message}</td></tr>`;
    }
}

// ==========================================================================
// Admin & Profiles
// ==========================================================================
async function loadUserProfile() {
    if (!state.token) return;
    try {
        const me = await apiCall('account', '/api/accounts/me', 'GET');
        document.getElementById('profileNameDisplay').textContent = me.name;
        document.getElementById('profileEmailDisplay').textContent = me.email;
        document.getElementById('badgeRole').textContent = me.role;
        document.getElementById('badgeStatus').textContent = me.status;
        document.getElementById('profileAccountId').textContent = me.id;
        document.getElementById('profilePhoneDisplay').textContent = me.phone || '-';
        document.getElementById('profileCreatedDisplay').textContent = me.createdAt ? new Date(me.createdAt).toLocaleDateString() : '-';
        document.getElementById('profileUpdatedDisplay').textContent = me.updatedAt ? new Date(me.updatedAt).toLocaleDateString() : '-';
        document.getElementById('profileAvatar').textContent = me.name ? me.name.charAt(0).toUpperCase() : 'U';
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function updateAccountStatusAdmin(e) {
    e.preventDefault();
    const accountId = document.getElementById('adminTargetAccountId').value.trim();
    const status = document.getElementById('adminTargetStatus').value;
    const reason = document.getElementById('adminStatusReason').value.trim();

    try {
        await apiCall('account', `/api/accounts/${accountId}/status`, 'PATCH', { status, reason });
        showToast(`Account status updated to: ${status}`, 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function adminSearchEligibleDrivers() {
    const vehicleType = document.getElementById('adminSearchVehType').value;
    const radius = document.getElementById('adminSearchRadius').value;
    const resDiv = document.getElementById('adminDriversResult');

    try {
        const drivers = await apiCall('driver', `/api/drivers/eligible?vehicleType=${vehicleType}&latitude=6.9344&longitude=79.8428&radiusKm=${radius}`, 'GET');
        if (!drivers || drivers.length === 0) {
            resDiv.innerHTML = '<div class="alert-box warning-alert">No eligible drivers found in this radius.</div>';
            return;
        }

        resDiv.innerHTML = drivers.map(d => `
            <div class="card-box margin-top" style="padding:10px;">
                <div><strong>Driver:</strong> ${d.driverId}</div>
                <div><strong>Vehicle:</strong> ${d.vehicleType} (${d.vehicleRegistrationNumber || 'N/A'})</div>
                <div><strong>Status:</strong> <span class="badge badge-success">${d.availabilityStatus}</span></div>
            </div>
        `).join('');
    } catch (err) {
        resDiv.innerHTML = `<div class="alert-box warning-alert">${err.message}</div>`;
    }
}

// Toast Helper
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;

    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}
