/* ================================
   Configuration
================================ */
//const API_BASE_URL = "https://192.168.137.1:8080";
//const WS_URL = "https://192.168.137.1:8080/ws";

// REST API base (http / https only)
const API_BASE = `${location.origin}/api`;

// WebSocket base (ws / wss automatically)
const WS_URL = `${location.origin}/ws`;

let map;
let busMarker = null;
let selectedBusId = null;
let stompClient = null;
let isConnected = false;

/* ================================
   Initialize Map
================================ */
function initMap() {
    map = L.map("map").setView([11.3182, 75.9376], 15); // Default: Kozhikode
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "© OpenStreetMap contributors"
    }).addTo(map);
}

/* ================================
   Load Buses for Dropdown
================================ */
async function loadBuses() {
    try {
        const response = await fetch(`${API_BASE}/buses`);
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
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);

    // Disable debug logging (optional)
    stompClient.debug = null;

    stompClient.connect({},
        // On successful connection
        () => {
            isConnected = true;
            updateConnectionStatus("Real-time updates active ✓");
            console.log("WebSocket connected successfully");

            // Subscribe to location updates
            stompClient.subscribe("/topic/location-updates", (message) => {
                const locationData = JSON.parse(message.body);
                handleLocationUpdate(locationData);
            });
        },
        // On connection error
        (error) => {
            isConnected = false;
            updateConnectionStatus("Connection lost - Retrying...");
            console.error("WebSocket error:", error);

            // Retry connection after 5 seconds
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
        return; // Not the bus we're tracking
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