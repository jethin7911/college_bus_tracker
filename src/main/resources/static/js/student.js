
/* ================================
   DYNAMIC API CONFIGURATION
================================ */

const API_BASE = (window.location.hostname === "localhost" || window.location.hostname === "127.0.0.1")
    ? "https://fluently-unhectored-cedric.ngrok-free.dev" // Use for local development/testing
    : "https://collegebustracker-production.up.railway.app"; // Use for production (Vercel)



// Then use it like:
fetch(`${API_BASE}/buses/locations`)
// WebSocket:
//const WS_URL = new WebSocket(`wss://collegebustracker-production.up.railway.app/ws`);
// Or if using SockJS (which you are, based on WebSocketConfig):
const WS_URL = new WebSocket(`wss://collegebustracker-production.up.railway.app/ws/websocket`);

let map;
let busMarker = null;
let selectedBusId = null;
let stompClient = null;
let isConnected = false;
/* ================================
   Initialize Map
================================ */
function initMap() {
    map = L.map("map").setView([11.3182, 75.9376], 15); // Default: NitC
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "© OpenStreetMap contributors"
    }).addTo(map);
}
/* ================================
   Load Buses for Dropdown
================================ */
async function loadBuses() {
    try {
        const response = await fetch(`${API_BASE}/api/buses`);
        const buses = await response.json();

        const busSelect = document.getElementById("busSelect");
        busSelect.innerHTML = `<option value="">Select Bus</option>`;

        buses.forEach(bus => {
            const option = document.createElement("option");
            option.value = bus.id;
            option.textContent = bus.busName;
            busSelect.appendChild(option);
        });
    } catch (error) {
        console.error("Failed to load buses:", error);
        alert("Unable to load buses. Please try again later.");
    }
}
/* ================================
   WebSocket Connection
================================ */
function connectWebSocket() {
    // 1. Force WSS and add the ngrok bypass as a query parameter
    let finalUrl = WS_URL.replace("http", "ws");
    // Only add the bypass if we are currently using ngrok
    if (window.location.hostname.includes("ngrok")) {
        finalUrl += "?ngrok-skip-browser-warning=true";
    }
    var socket = new WebSocket(finalUrl);
    stompClient = Stomp.over(socket);

    stompClient.debug = null;

    stompClient.connect({},
        () => {
            isConnected = true;
            updateConnectionStatus("Real-time updates active ✓");

            // 3. Ensure this topic matches exactly what your Backend sends to
            stompClient.subscribe("/topic/bus-location", (message) => {
                const locationData = JSON.parse(message.body);
                handleLocationUpdate(locationData);
            });
        },
        (error) => {
            isConnected = false;
            updateConnectionStatus("Connection lost - Retrying...");
            setTimeout(connectWebSocket, 5000);
        }
    );
}

/* ================================
   Handle Incoming Location Update
================================ */
function handleLocationUpdate(locationData) {
    // locationData now has: busId, assignmentId, busName, latitude, longitude, timestamp
    console.log("Received location update:", locationData);

    // Only update if this is for the selected bus
    if (!selectedBusId || locationData.busId !== parseInt(selectedBusId)) {
        return;
    }

    // Update marker with new location
    if (locationData.latitude != null && locationData.longitude != null) {
        updateBusMarker(locationData.latitude, locationData.longitude);
        updateConnectionStatus(`Real-time updates active ✓ | Last update: ${new Date().toLocaleTimeString()}`);
    }
}

/* ================================
   Handle Bus Selection
================================ */
function onBusSelect() {
    const busSelect = document.getElementById("busSelect");

    busSelect.addEventListener("change", async () => {
        selectedBusId = busSelect.value;

        if (!selectedBusId) {
            // Clear marker if no bus selected
            if (busMarker) {
                map.removeLayer(busMarker);
                busMarker = null;
            }
            updateConnectionStatus("Real-time updates active ✓");
            return;
        }

        // Fetch initial location immediately (Hybrid approach)
        await fetchInitialLocation();
    });
}
/* ================================
   Fetch Initial Bus Location (HTTP)
================================ */
async function fetchInitialLocation() {
    if (!selectedBusId) return;

    try {
        const response = await fetch(
            `${API_BASE}/bus/${selectedBusId}/location`
        );

        if (!response.ok) {
            console.warn("No location available yet");
            updateConnectionStatus("Waiting for bus to start...");
            return;
        }

        const data = await response.json();

        // Backend explicitly tells whether location is active
        if (!data.active || data.latitude == null || data.longitude == null) {
            console.log("Bus is active but location not received yet");
            updateConnectionStatus("Waiting for location data...");
            return;
        }

        updateBusMarker(data.latitude, data.longitude);
        updateConnectionStatus("Real-time updates active ✓");

    } catch (error) {
        console.error("Error fetching initial location:", error);
        updateConnectionStatus("Error loading location");
    }
}

/* ================================
   Update / Move Marker
================================ */
function updateBusMarker(lat, lng) {
    const position = [lat, lng];

    if (!busMarker) {
        // Create new marker
        busMarker = L.marker(position, {
            icon: L.icon({
                iconUrl: "https://cdn-icons-png.flaticon.com/512/61/61231.png",
                iconSize: [40, 40],
                iconAnchor: [20, 40],
                shadowUrl: null
            })
        })
            .addTo(map)
            .bindPopup("🚌 Bus Location")
            .openPopup();

        map.setView(position, 16);
    } else {
        // Move existing marker smoothly
        busMarker.setLatLng(position);
    }

    console.log(`Bus marker updated: [${lat}, ${lng}]`);
}
/* ================================
   Update Connection Status
================================ */
function updateConnectionStatus(message) {
    const statusElement = document.getElementById("connectionStatus");
    if (statusElement) {
        statusElement.textContent = message;
    }
}
/* ================================
   Init on Page Load
================================ */
document.addEventListener("DOMContentLoaded", () => {
    initMap();
    loadBuses();
    onBusSelect();
    connectWebSocket(); // Start WebSocket connection
});