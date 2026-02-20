# College Bus Tracker

A real-time college bus tracking application that helps students and administrators monitor bus locations and routes. This repo contains the code for the College Bus Tracker project — see sections below for setup, configuration, and contribution guidelines.

## WORK FLOW
Student in frontend subscribes to a url, where the location of Bus is broadcasted. 
For live location, developed a native app for Driver, for driver side simplicity made login using phone number after successful login driver selects the bus for the shift. Then location turns on starts sharing location to database. Driver side for persistent location sharing websockets has been implemented.

In server side the response from driver's app is stored in server memory(RAM) for fastlookups.

Repository: [jethin7911/college_bus_tracker](https://github.com/jethin7911/college_bus_tracker)

## Features
- Real-time bus location tracking (GPS)
- Route visualization on a map
- Student-facing interface for live bus status
- Admin dashboard for route and schedule management


## Tech stack
Replace or update this list with what your project actually uses:
- Backend: springboot
- Frontend: html,css, js. 
- Mobile: Kotlin
- Database: mysql
- Real-time: WebSockets 
- Maps: Leaflet.js
- Deployment: Railway 



Live demo: https://collegebustracker-production.up.railway.app/

## Video demo of project


https://github.com/user-attachments/assets/5e8fb1e7-cc6a-4176-a06e-f992c3957a9e



https://github.com/user-attachments/assets/b7df470b-77f9-4e39-840d-1b00d8a59cf1




## Environment / Configuration variables
Provide a full list of env vars required by the repo and what they mean:
- DATABASE_URL — 
- PORT — 



## Contributing
Contributions are welcome!
1. Fork the repository
2. Create a feature branch:
3. Commit your changes: 
4. Push to the branch: 
5. Open a Pull Request with a clear description of changes

Please follow the coding style and include tests for new features.


## Contact
For questions or help, open an issue in this repo or contact the maintainer:
- GitHub: [jethin7911](https://github.com/jethin7911)
