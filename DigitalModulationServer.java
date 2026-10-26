import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class DigitalModulationServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int SAMPLES_PER_BIT = 80;
    private static final double CARRIER_CYCLES_PER_BIT = 4.0;

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", String.valueOf(DEFAULT_PORT)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api/simulate", DigitalModulationServer::simulate);
        server.createContext("/", DigitalModulationServer::serveStatic);
        server.setExecutor(null);
        server.start();
        System.out.println("Digital Modulation Simulator backend is running on port " + port);
        System.out.println("Open http://localhost:" + port + " in your browser.");
    }

    private static void simulate(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            sendJson(ex, 405, "{\"error\":\"Use POST for simulation.\"}");
            return;
        }
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String bits = jsonString(body, "bits");
        String mode = jsonString(body, "mode");
        if (bits == null || !bits.matches("[01]{1,64}")) {
            sendJson(ex, 400, "{\"error\":\"Bits must contain 1 to 64 binary digits.\"}");
            return;
        }
        if (!"ASK".equals(mode) && !"FSK".equals(mode)) {
            sendJson(ex, 400, "{\"error\":\"Mode must be ASK or FSK.\"}");
            return;
        }

        double[] samples = new double[bits.length() * SAMPLES_PER_BIT];
        for (int i = 0; i < samples.length; i++) {
            int bitIndex = i / SAMPLES_PER_BIT;
            int withinBit = i % SAMPLES_PER_BIT;
            double phase = 2.0 * Math.PI * CARRIER_CYCLES_PER_BIT * withinBit / SAMPLES_PER_BIT;
            int bit = bits.charAt(bitIndex) - '0';
            if ("ASK".equals(mode)) {
                double amplitude = bit == 1 ? 1.0 : 0.20;
                samples[i] = amplitude * Math.sin(phase);
            } else {
                double cycles = bit == 1 ? 6.0 : 2.0;
                samples[i] = Math.sin(2.0 * Math.PI * cycles * withinBit / SAMPLES_PER_BIT);
            }
        }

        StringBuilder out = new StringBuilder();
        out.append("{\"backend\":\"Java\",\"mode\":\"").append(mode)
           .append("\",\"bits\":\"").append(bits).append("\",\"bitCount\":").append(bits.length())
           .append(",\"samples\":[");
        for (int i = 0; i < samples.length; i++) {
            if (i > 0) out.append(',');
            out.append(String.format(Locale.US, "%.5f", samples[i]));
        }
        out.append("]}");
        sendJson(ex, 200, out.toString());
    }

    private static String jsonString(String json, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        return m.find() ? m.group(1) : null;
    }

    private static void serveStatic(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            sendText(ex, 405, "Method not allowed", "text/plain; charset=utf-8");
            return;
        }
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..") || path.contains("\\\\")) {
            sendText(ex, 400, "Bad request", "text/plain; charset=utf-8");
            return;
        }
        Path file = Paths.get("." + path).normalize();
        if (!Files.isRegularFile(file)) {
            sendText(ex, 404, "File not found. Start the Java server from the project folder.", "text/plain; charset=utf-8");
            return;
        }
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String type = name.endsWith(".html") ? "text/html; charset=utf-8" :
                      name.endsWith(".css") ? "text/css; charset=utf-8" :
                      name.endsWith(".js") ? "application/javascript; charset=utf-8" : "application/octet-stream";
        byte[] bytes = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        sendText(ex, status, body, "application/json; charset=utf-8");
    }

    private static void sendText(HttpExchange ex, int status, String body, String type) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }
}
