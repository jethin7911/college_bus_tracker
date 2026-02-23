/* ================================
   CONFIG-MOBILE TESTING VERSION
================================ */


//const API_BASE = `https://${SERVER_IP}:${SERVER_PORT}/api/driver`;
//const WS_URL = `https://${SERVER_IP}:${SERVER_PORT}/ws`;

//url for ngrok
// REST API base (http / https only)
const API_BASE = `${location.origin}/api/driver`;

// WebSocket base (ws / wss automatically)
// SockJS endpoint MUST be http/https
const WS_URL = `${location.origin}/ws`;


let driverId = null;
let assignmentId = null;
let selectedBusId = null;
let stompClient = null;
let locationInterval = null;

/* ================================
   ELEMENTS
================================ */
const loginSection = document.getElementById("loginSection");
const busSection = document.getElementById("busSection");
const statusSection = document.getElementById("statusSection");

const phoneInput = document.getElementById("phone");
const busSelect = document.getElementById("busSelect");
const statusText = document.getElementById("statusText");

/* ================================
   LOGIN
================================ */
document.getElementById("loginBtn").addEventListener("click", async () => {
    const phoneNumber = phoneInput.value.trim();

    if (!phoneNumber) {
        alert("Enter phone number");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ phoneNumber })
        });

        const data = await res.json();

        console.log("=== LOGIN RESPONSE ===");
        console.log("Full response:", data);
        console.log("Driver ID:", data.driverId);
        console.log("=====================");

        // Check if login was successful
        if (!data.driverId) {
            alert(data.message || "Login failed. Contact admin to add your number.");
            console.error("Login failed:", data);
            return;
        }

        driverId = data.driverId;

        console.log("✓ Driver logged in successfully. ID:", driverId);

        loginSection.style.display = "none";
        busSection.classList.remove("hidden");

        loadAvailableBuses();

    } catch (err) {
        console.error("Login error:", err);
        alert("Invalid phone number or server error");
    }
});

/* ================================
   LOAD AVAILABLE BUSES
================================ */
async function loadAvailableBuses() {

    const res = await fetch(`${API_BASE}/buses/available`);
    const buses = await res.json();

    busSelect.innerHTML = `<option value="">Select Bus</option>`;

    buses.forEach(bus => {
        const opt = document.createElement("option");
        opt.value = bus.id;
        opt.textContent = bus.busName;
        busSelect.appendChild(opt);
    });
}

/* ================================
   START TRACKING
================================ */
document.getElementById("startBtn").addEventListener("click", async () => {
    selectedBusId = busSelect.value;

    if (!selectedBusId) {
        alert("Select a bus");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/start-tracking`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                driverId: parseInt(driverId),
                busId: parseInt(selectedBusId)
            })
        });

        const data = await res.json();

        console.log("=== START TRACKING RESPONSE ===");
        console.log("Success:", data.success);
        console.log("Message:", data.message);
        console.log("Assignment ID:", data.assignmentId);
        console.log("================================");

        // CRITICAL: Check if request was successful
        if (!data.success) {
            alert(data.message || "Failed to start tracking");
            console.error("Backend rejected tracking:", data.message);
            return; // Stop execution
        }

        // CRITICAL: Validate assignment ID exists
        if (!data.assignmentId) {
            alert("Error: No assignment ID received from server");
            console.error("Assignment ID is null/undefined");
            return;
        }

        assignmentId = data.assignmentId;

        console.log("=== TRACKING STARTED SUCCESSFULLY ===");
        console.log("Assignment ID:", assignmentId);
        console.log("Bus ID:", selectedBusId);
        console.log("Driver ID:", driverId);
        console.log("=====================================");

        busSection.classList.add("hidden");
        statusSection.classList.remove("hidden");

        // Connect WebSocket first, then start sending location after connection
        connectWebSocket();

    } catch (err) {
        console.error("Start tracking error:", err);
        alert("Failed to start tracking: " + err.message);
    }
});

/* ================================
   STOP TRACKING
================================ */
document.getElementById("stopBtn").addEventListener("click", async () => {
    try {
        await fetch(`${API_BASE}/stop-tracking?assignmentId=${assignmentId}`, {
            method: "POST"
        });

        clearInterval(locationInterval);

        if (stompClient && stompClient.connected) {
            stompClient.disconnect();
        }

        console.log("Tracking stopped");
        alert("Tracking stopped");
        location.reload();

    } catch (err) {
        console.error("Stop tracking error:", err);
    }
});

/* ================================
   WEBSOCKET
================================ */
function connectWebSocket() {
    console.log("Connecting to WebSocket...");

    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);

    // Disable debug output (optional)
    stompClient.debug = null;

    stompClient.connect(
        {},
        // On successful connection
        () => {
            console.log("✓ WebSocket connected successfully");
            statusText.innerText = "Connected. Sending live location...";

            // Start sending location AFTER WebSocket is connected
            startSendingLocation();
        },
        // On connection error
        (error) => {
            console.error("✗ WebSocket connection error:", error);
            statusText.innerText = "Connection failed. Retrying...";

            // Retry connection after 3 seconds
            setTimeout(connectWebSocket, 3000);
        }
    );
}

/* ================================
   SEND LOCATION
================================ */
function startSendingLocation() {
    console.log("=== STARTING LOCATION UPDATES ===");
    console.log("Assignment ID for location updates:", assignmentId);

    if (!assignmentId) {
        console.error("ERROR: Assignment ID is null! Cannot send location.");
        alert("Error: Assignment ID is missing. Please restart tracking.");
        return;
    }

    // Send location immediately first time
    sendCurrentLocation();

    // Then send every 3 seconds
    locationInterval = setInterval(() => {
        sendCurrentLocation();
    }, 3000);
}

/* ================================
   SEND CURRENT LOCATION
================================ */
function sendCurrentLocation() {
    if (!stompClient || !stompClient.connected) {
        console.warn("WebSocket not connected. Skipping location update.");
        return;
    }

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const payload = {
                assignmentId: assignmentId,
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
            };

            console.log("📍 Sending location:", payload);

            try {
                stompClient.send("/app/location", {}, JSON.stringify(payload));
                console.log("✓ Location sent successfully");
            } catch (error) {
                console.error("✗ Error sending location:", error);
            }
        },
        (error) => {
            console.error("Geolocation error:", error);

            switch(error.code) {
                case error.PERMISSION_DENIED:
                    statusText.innerText = "Location permission denied";
                    break;
                case error.POSITION_UNAVAILABLE:
                    statusText.innerText = "Location unavailable";
                    break;
                case error.TIMEOUT:
                    statusText.innerText = "Location request timeout";
                    break;
            }
        },
        {
            enableHighAccuracy: true,
            timeout: 5000,
            maximumAge: 0
        }
    );
}