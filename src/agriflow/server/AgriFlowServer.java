package agriflow.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny HTTP server: serves web/index.html and the JSON API. All algorithm work happens in agriflow.algorithms. */
public class AgriFlowServer {
    private final HttpServer server;
    private final ApiStrings strings;
    private final ApiGraph graph;

    public AgriFlowServer(int port) throws IOException {
        Data data = new Data();
        strings = new ApiStrings(data);
        graph = new ApiGraph(data);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    public void start() { server.start(); }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        try {
            if (path.equals("/") || path.equals("/index.html")) { send(ex, 200, "text/html; charset=UTF-8", loadIndex()); return; }
            if (!path.startsWith("/api/")) { send(ex, 404, "application/json", "{\"error\":\"not found\"}"); return; }
            Q q = new Q(ex.getRequestURI().getRawQuery());
            String body = route(path, q);
            if (body == null) send(ex, 404, "application/json", "{\"error\":\"unknown endpoint " + Json.esc(path) + "\"}");
            else send(ex, 200, "application/json", body);
        } catch (Q.Bad e) {
            send(ex, 400, "application/json", Json.obj().put("error", e.getMessage()).toString());
        } catch (Exception e) {
            e.printStackTrace();
            send(ex, 500, "application/json", Json.obj().put("error", "server error: " + e).toString());
        }
    }

    private String route(String path, Q q) {
        switch (path) {
            // existing AgriFlow endpoints
            case "/api/health": return strings.health();
            case "/api/search": return strings.search(q);
            case "/api/edit-distance": return strings.editDistance(q);
            case "/api/suffix": return strings.suffix(q);
            // Module 1
            case "/api/engine": return strings.engine();
            case "/api/similar": return strings.similar(q);
            // Module 2
            case "/api/strlab": return strings.strlab(q);
            case "/api/race": return strings.race(q);
            case "/api/multi": return strings.multi(q);
            case "/api/repeats": return strings.repeats();
            // Module 3
            case "/api/edit": return strings.edit(q);
            case "/api/align": return strings.align(q);
            case "/api/matrix": return strings.matrix(q);
            case "/api/obst": return strings.obst(q);
            case "/api/route": return strings.route(q);
            case "/api/hamilton": return strings.hamilton(q);
            case "/api/setcover": return strings.setcover(q);
            case "/api/tree": return strings.tree(q);
            case "/api/sos": return strings.sos(q);
            case "/api/greedy": return strings.greedy(q);
            // Module 4
            case "/api/flow": return graph.flow(q);
            case "/api/matching": return graph.matching(q);
            case "/api/mcmf": return graph.mcmf(q);
            case "/api/projects": return graph.projects(q);
            // Module 5
            case "/api/vcover": return graph.vcover(q);
            case "/api/knapsack": return graph.knapsack(q);
            case "/api/subsetsum": return graph.subsetsum(q);
            // Module 6
            case "/api/prime": return graph.prime(q);
            case "/api/qsort": return graph.qsort(q);
            case "/api/hash": return graph.hash(q);
            case "/api/reservoir": return graph.reservoir(q);
            case "/api/parallel": return graph.parallel(q);
            default: return null;
        }
    }

    private byte[] loadIndex() throws IOException {
        try (InputStream in = AgriFlowServer.class.getResourceAsStream("/web/index.html")) {
            if (in != null) return in.readAllBytes();
        }
        return Files.readAllBytes(Path.of("web", "index.html"));
    }

    private static void send(HttpExchange ex, int code, String type, String body) throws IOException {
        send(ex, code, type, body.getBytes(StandardCharsets.UTF_8));
    }

    private static void send(HttpExchange ex, int code, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
}
