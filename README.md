# Digital Modulation Simulator (Java backend required)

## Files
- `index.html` — frontend page
- `style.css` — page styling
- `script.js` — sends requests to Java and displays returned samples
- `DigitalModulationServer.java` — Java HTTP server and ASK/FSK calculation backend
- `README.md` — these instructions

## Run in VS Code on Windows (JDK 11)
1. Open this folder in VS Code.
2. Use **Terminal → New Terminal**.
3. Run: `javac DigitalModulationServer.java`
4. Run: `java DigitalModulationServer`
5. Open `http://localhost:8080`.

The terminal is needed to start the backend in this basic five-file version. After the backend starts, you can use the website in your browser.

## Backend requirement
The frontend requests `/api/simulate` from the Java server. Java validates the input, computes the ASK/FSK samples, and returns them. The browser only draws the returned samples; it does not generate fallback simulation data. If the Java backend is not running, simulation requests fail and the page displays a backend error.

Opening `index.html` directly from disk is not supported; access it through the Java server at `http://localhost:8080`.

## Note
This is a teaching/demo simulator for ASK and FSK waveform generation. It is not a physical radio transmitter.
