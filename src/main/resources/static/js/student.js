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

const WS_URL = "https://collegebustracker-production.up.railway.app/ws";  // ✅ CORRECT

let map;
let busMarker = null;
let selectedBusId = null;
let stompClient = null;
let isConnected = false;
let currentSubscription = null;  // Track current bus subscription

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

        console.log(`Loaded ${buses.length} buses`);
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
    //let finalUrl = WS_URL.replace("http", "ws");
    // Only add the bypass if we are currently using ngrok
    if (window.location.hostname.includes("ngrok")) {
        finalUrl += "?ngrok-skip-browser-warning=true";
    }

    console.log("Connecting to WebSocket:", finalUrl);

    var socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);

    stompClient.debug = function(str) {
        console.log('STOMP:', str);
    };

    stompClient.connect({},
        () => {
            isConnected = true;
            console.log("WebSocket connected successfully");
            updateConnectionStatus("Connected - Select a bus to track");

            // If a bus was already selected, subscribe to it
            if (selectedBusId) {
                subscribeToBus(selectedBusId);
            }
        },
        (error) => {
            isConnected = false;
            console.error("WebSocket connection error:", error);
            updateConnectionStatus("Connection lost - Retrying...");
            setTimeout(connectWebSocket, 5000);
        }
    );
}

/* ================================
   Subscribe to Bus-Specific Topic
================================ */
function subscribeToBus(busId) {
    if (!stompClient || !isConnected) {
        console.warn("WebSocket not connected, cannot subscribe to bus");
        return;
    }

    // Unsubscribe from previous bus if any
    if (currentSubscription) {
        console.log("Unsubscribing from previous bus");
        currentSubscription.unsubscribe();
        currentSubscription = null;
    }

    // Subscribe to the bus-specific topic
    const topic = `/topic/bus/${busId}`;
    console.log(`Subscribing to ${topic}`);

    currentSubscription = stompClient.subscribe(topic, (message) => {
        console.log("Received message from", topic, ":", message.body);
        const locationData = JSON.parse(message.body);
        handleLocationUpdate(locationData);
    });

    updateConnectionStatus(`Tracking bus - Waiting for updates...`);
}

/* ================================
   Handle Incoming Location Update
================================ */
function handleLocationUpdate(locationData) {
    // locationData now has: busId, assignmentId, busName, latitude, longitude, timestamp
    console.log("Received location update:", locationData);

    // Double-check this is for the selected bus
    if (!selectedBusId) return;

    if (Number(locationData.busId) !== Number(selectedBusId)) return;


    // Update marker with new location
    if (locationData.latitude != null && locationData.longitude != null) {
        updateBusMarker(locationData.latitude, locationData.longitude);

        const updateTime = new Date().toLocaleTimeString();
        updateConnectionStatus(`✓ Real-time tracking | Last update: ${updateTime}`);

        console.log(`Updated bus ${locationData.busId} position: [${locationData.latitude}, ${locationData.longitude}]`);
    } else {
        console.warn("Received location update with null coordinates");
    }
}

/* ================================
   Handle Bus Selection
================================ */
function onBusSelect() {
    const busSelect = document.getElementById("busSelect");

    busSelect.addEventListener("change", async () => {
        const newBusId = busSelect.value;

        console.log("Bus selection changed to:", newBusId);

        if (!newBusId) {
            // Clear marker if no bus selected
            if (busMarker) {
                map.removeLayer(busMarker);
                busMarker = null;
            }

            // Unsubscribe from current bus
            if (currentSubscription) {
                currentSubscription.unsubscribe();
                currentSubscription = null;
            }

            selectedBusId = null;
            updateConnectionStatus("Connected - Select a bus to track");
            return;
        }

        selectedBusId = newBusId;

        // Subscribe to the new bus's WebSocket topic
        subscribeToBus(selectedBusId);

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
        console.log(`Fetching initial location for bus ${selectedBusId}`);

        const response = await fetch(
            `${API_BASE}/bus/${selectedBusId}/location`
        );

        if (!response.ok) {
            console.warn("No location available yet for bus", selectedBusId);
            updateConnectionStatus("Waiting for bus to start tracking...");
            return;
        }

        const data = await response.json();
        console.log("Initial location data:", data);

        // Backend explicitly tells whether location is active
        if (!data.active || data.latitude == null || data.longitude == null) {
            console.log("Bus is active but location not received yet");
            updateConnectionStatus("Bus is active - Waiting for first location update...");
            return;
        }

        updateBusMarker(data.latitude, data.longitude);

        const timestamp = data.timestamp ? new Date(data.timestamp).toLocaleTimeString() : 'now';
        updateConnectionStatus(`✓ Real-time tracking | Last update: ${timestamp}`);

    } catch (error) {
        console.error("Error fetching initial location:", error);
        updateConnectionStatus("Error loading location - Check console");
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
        console.log("Created new marker at:", position);
    } else {
        // Move existing marker smoothly
        busMarker.setLatLng(position);
        console.log("Updated marker position to:", position);
    }
}

/* ================================
   Update Connection Status
================================ */
function updateConnectionStatus(message) {
    const statusElement = document.getElementById("connectionStatus");
    if (statusElement) {
        statusElement.textContent = message;
    }
    console.log("Status:", message);
}

/* ================================
   Init on Page Load
================================ */
document.addEventListener("DOMContentLoaded", () => {
    console.log("Initializing College Bus Tracker...");
    initMap();
    loadBuses();
    onBusSelect();
    connectWebSocket(); // Start WebSocket connection

    console.log("Initialization complete");
});