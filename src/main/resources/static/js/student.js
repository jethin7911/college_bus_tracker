/* ================================
   Configuration
================================ */
const API_BASE = `${location.origin}/api`;
const WS_URL = `${location.origin}/ws`;

let map;
let busMarker = null;
let selectedBusId = null;
let stompClient = null;
let isConnected = false;
let busSubscription = null;
let busSelect;

/* ================================
   Initialize Map
================================ */
function initMap() {
    map = L.map("map").setView([11.3182, 75.9376], 15);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "© OpenStreetMap contributors"
    }).addTo(map);
}

/* ================================
   Load Buses
================================ */
async function loadBuses() {
    const response = await fetch(`${API_BASE}/buses`);
    const buses = await response.json();

    busSelect.innerHTML = `<option value="">Select Bus</option>`;
    buses.forEach(bus => {
        const option = document.createElement("option");
        option.value = bus.id;
        option.textContent = bus.busName;
        busSelect.appendChild(option);
    });
}

function subscribeToSelectedBus(busId) {
    if (!stompClient || !isConnected) return;

    if (busSubscription) {
        busSubscription.unsubscribe();
    }

    busSubscription = stompClient.subscribe(
        `/topic/bus/${busId}`,
        msg => handleLocationUpdate(JSON.parse(msg.body))
    );
}

/* ================================
   WebSocket
================================ */
function connectWebSocket() {
    const socket = new SockJS(WS_URL);
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        isConnected = true;
        if (selectedBusId) subscribeToSelectedBus(selectedBusId);
    });
}

/* ================================
   Handle Updates
================================ */
function handleLocationUpdate(data) {
    if (!selectedBusId || data.busId !== Number(selectedBusId)) return;
    updateBusMarker(data.latitude, data.longitude);
}

function updateBusMarker(lat, lng) {
    const pos = [lat, lng];
    if (!busMarker) {
        busMarker = L.marker(pos).addTo(map);
        map.setView(pos, 16);
    } else {
        busMarker.setLatLng(pos);
    }
}

/* ================================
   Initial Location
================================ */
async function fetchInitialLocation() {
    const res = await fetch(`${API_BASE}/bus/${selectedBusId}/location`);
    if (!res.ok) return;
    const data = await res.json();
    if (data.latitude && data.longitude) {
        updateBusMarker(data.latitude, data.longitude);
    }
}

/* ================================
   Init
================================ */
document.addEventListener("DOMContentLoaded", () => {
    busSelect = document.getElementById("busSelect");

    initMap();
    loadBuses();
    connectWebSocket();

    // EVENT LISTENER MUST BE HERE
    busSelect.addEventListener("change", async () => {
        selectedBusId = busSelect.value;

        if (!selectedBusId) return;

        subscribeToSelectedBus(selectedBusId);
        await fetchInitialLocation();
    });
});