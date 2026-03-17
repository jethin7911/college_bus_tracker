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
let previousPosition = null;

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
    updateFooterTime(data.timestamp);
}
function createBusIcon(rotation = 0) {
    return L.divIcon({
        className: "bus-icon-wrapper",
        html: `
          <img 
            src="/Images/BusIcon.png"
            class="bus-icon"
            style="transform: rotate(${rotation}deg);"
          />
        `,
        iconSize: [42, 42],
        iconAnchor: [21, 21]
    });
}
function updateBusMarker(lat, lng) {
    const currentPos = [lat, lng];

    let rotation = 0;

    if (previousPosition) {
        rotation = calculateBearing(
            previousPosition[0],
            previousPosition[1],
            lat,
            lng
        );
    }

    if (!busMarker) {
        busMarker = L.marker(currentPos, {
            icon: createBusIcon(rotation)
        }).addTo(map);

        map.setView(currentPos, 16);
    } else {
        busMarker.setLatLng(currentPos);
        busMarker.setIcon(createBusIcon(rotation));
    }

    previousPosition = currentPos;
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
        updateFooterTime(data.timestamp);
    }
}

function updateFooterTime(timestamp) {
    const footerEl = document.getElementById("lastUpdate");
    if (!footerEl) return;

    const time = timestamp
        ? new Date(timestamp)
        : new Date();

    footerEl.textContent = time.toLocaleTimeString();
}
function calculateBearing(lat1, lng1, lat2, lng2) {
    const toRad = deg => deg * Math.PI / 180;
    const toDeg = rad => rad * 180 / Math.PI;

    const dLng = toRad(lng2 - lng1);

    const y = Math.sin(dLng) * Math.cos(toRad(lat2));
    const x =
        Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
        Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLng);

    let bearing = toDeg(Math.atan2(y, x));
    return (bearing + 360) % 360; // normalize
}
/* ================================
   Init
================================ */
document.addEventListener("DOMContentLoaded", () => {
    busSelect = document.getElementById("busSelect");

    initMap();
    loadBuses();
    connectWebSocket();
    const menuBtn = document.querySelector(".menu");
    const dropdown = document.querySelector(".dropdown");

    menuBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        const isOpen = dropdown.style.display === "block";
        dropdown.style.display = isOpen ? "none" : "block";
    });

    document.addEventListener("click", () => {
        dropdown.style.display = "none";
    });
    // EVENT LISTENER MUST BE HERE
    busSelect.addEventListener("change", async () => {
        selectedBusId = busSelect.value;

        if (!selectedBusId) return;

        subscribeToSelectedBus(selectedBusId);
        await fetchInitialLocation();
    });
});
