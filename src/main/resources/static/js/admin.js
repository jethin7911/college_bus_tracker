/* ================================
   CONFIG
================================ */

//const API_BASE = "fluently-unhectored-cedric.ngrok-free.dev/api/admin"
const API_BASE = `${location.origin}/api`;
let loggedInAdminId = null;

/* ================================
   ELEMENTS
================================ */
const loginSection = document.getElementById("loginSection");
const dashboardSection = document.getElementById("dashboardSection");

const loginBtn = document.getElementById("loginBtn");
const logoutBtn = document.getElementById("logoutBtn");

const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");

/* ================================
   LOGIN
================================ */
loginBtn.addEventListener("click", async () => {
    const username = usernameInput.value.trim();
    const password = passwordInput.value.trim();

    if (!username || !password) {
        alert("Enter username and password");
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });

        if (!res.ok) throw new Error("Invalid login");

        const data = await res.json();
        loggedInAdminId = data.adminId;

        loginSection.style.display = "none";
        dashboardSection.classList.remove("hidden");

        await loadBuses();
        await loadDrivers();

    } catch (err) {
        alert("Login failed");
        console.error(err);
    }
});

/* ================================
   LOGOUT
================================ */
logoutBtn.addEventListener("click", () => {
    location.reload();
});

/* ================================
   ADD BUS
================================ */
document.getElementById("addBusBtn").addEventListener("click", async () => {
    const busNumber = document.getElementById("busNumber").value.trim();
    const routeName = document.getElementById("routeName").value.trim();

    if (!busNumber) {
        alert("Bus number required");
        return;
    }

    // FIXED: Changed to match backend BusDTO field name
    // Backend expects: busName (not busNumber and routeName separately)
    const busName = routeName ? `${busNumber} - ${routeName}` : busNumber;

    await fetch(`${API_BASE}/admin/buses`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ busName })  // FIXED: Changed from busNumber, routeName
    });

    alert("Bus added");
    loadBuses();

    // Clear input fields
    document.getElementById("busNumber").value = "";
    document.getElementById("routeName").value = "";
});

/* ================================
   ADD DRIVER (CAPTAIN)
================================ */
document.getElementById("addDriverBtn").addEventListener("click", async () => {
    const name = document.getElementById("driverName").value.trim();
    const phoneNumber = document.getElementById("driverPhone").value.trim();

    if (!name || !phoneNumber) {
        alert("All fields required");
        return;
    }

    await fetch(`${API_BASE}/admin/drivers`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name, phoneNumber })
    });

    alert("Captain added");
    loadDrivers();

    // Clear input fields
    document.getElementById("driverName").value = "";
    document.getElementById("driverPhone").value = "";
});

/* ================================
   LOAD BUSES
================================ */
async function loadBuses() {
    const res = await fetch(`${API_BASE}/admin/buses`);
    const buses = await res.json();

    const busSelect = document.getElementById("busSelect");
    busSelect.innerHTML = "";

    buses.forEach(bus => {
        const opt = document.createElement("option");
        opt.value = bus.id;
        opt.textContent = bus.busName;  // FIXED: Changed from bus.busNumber to bus.busName
        busSelect.appendChild(opt);
    });
}

/* ================================
   LOAD DRIVERS
================================ */
async function loadDrivers() {
    const res = await fetch(`${API_BASE}/admin/drivers`);
    const drivers = await res.json();

    const driverSelect = document.getElementById("driverSelect");
    driverSelect.innerHTML = "";

    drivers.forEach(driver => {
        const opt = document.createElement("option");
        opt.value = driver.id;
        opt.textContent = driver.name;
        driverSelect.appendChild(opt);
    });
}