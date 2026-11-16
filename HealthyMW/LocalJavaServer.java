package backend;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;

public class LocalJavaServer {
    private static Connection connection;

    public static void main(String[] args) {
        try {
            Class.forName("org.sqlite.JDBC");
            connect();
            createTables();
            preloadBaseData();

            HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);

            server.createContext("/api/astronautas", new AstronautsHandler());
            server.createContext("/api/guardar", new SaveHandler());
            server.createContext("/api/indicadores", new IndicatorsHandler());
            server.createContext("/api/historial", new HistoryHandler());

            server.createContext("/api/crud/generos", new GenresCrudHandler());
            server.createContext("/api/crud/puestos", new PositionsCrudHandler());
            server.createContext("/api/crud/astronautas", new AstronautsCrudHandler());

            server.createContext("/", new FilesHandler());

            server.setExecutor(null);
            server.start();

            System.out.println("==================================================");
            System.out.println(" Java server active at http://localhost:8081 !");
            System.out.println(" Page: http://localhost:8081/  (index.html in " + WEB_FOLDER + ")");
            System.out.println("==================================================");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CRUD handler for Genres (Create, Read, Update, Delete for administrators)
    static class GenresCrudHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                String method = exchange.getRequestMethod();
                if (method.equalsIgnoreCase("GET")) {
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM GENEROS");
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"descripcion\":\"%s\"}",
                            rs.getInt("idGENEROS"), rs.getString("DESCRIPCION")));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString());
                } else if (method.equalsIgnoreCase("POST")) {
                    byte[] bytes = exchange.getRequestBody().readAllBytes();
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    String idStr = extractJsonValue(json, "id");
                    String description = extractJsonValue(json, "descripcion");

                    if (idStr != null && !idStr.isEmpty() && Integer.parseInt(idStr) > 0) {
                        PreparedStatement pstmt = connection.prepareStatement("UPDATE GENEROS SET DESCRIPCION = ? WHERE idGENEROS = ?");
                        pstmt.setString(1, description);
                        pstmt.setInt(2, Integer.parseInt(idStr));
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Genre updated successfully!");
                    } else {
                        PreparedStatement pstmt = connection.prepareStatement("INSERT INTO GENEROS (DESCRIPCION) VALUES (?)");
                        pstmt.setString(1, description);
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Genre saved successfully!");
                    }
                } else if (method.equalsIgnoreCase("DELETE")) {
                    String query = exchange.getRequestURI().getQuery();
                    int id = 0;
                    if (query != null && query.contains("id=")) {
                        id = Integer.parseInt(query.split("id=")[1].split("&")[0]);
                    }
                    try {
                        PreparedStatement pstmt = connection.prepareStatement("DELETE FROM GENEROS WHERE idGENEROS = ?");
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Genre deleted successfully!");
                    } catch (SQLException ex) {
                        sendResponse(exchange, 400, "Cannot delete: assigned to one or more astronauts.");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error in Genres: " + e.getMessage());
            }
        }
    }

    static class PositionsCrudHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                String method = exchange.getRequestMethod();
                if (method.equalsIgnoreCase("GET")) {
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM PUESTOS");
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"descripcion\":\"%s\"}",
                            rs.getInt("idPUESTOS"), rs.getString("DESCRIPCION")));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString());
                } else if (method.equalsIgnoreCase("POST")) {
                    byte[] bytes = exchange.getRequestBody().readAllBytes();
                    String json = new String(bytes, StandardCharsets.UTF_8);
                    String idStr = extractJsonValue(json, "id");
                    String description = extractJsonValue(json, "descripcion");

                    if (idStr != null && !idStr.isEmpty() && Integer.parseInt(idStr) > 0) {
                        PreparedStatement pstmt = connection.prepareStatement("UPDATE PUESTOS SET DESCRIPCION = ? WHERE idPUESTOS = ?");
                        pstmt.setString(1, description);
                        pstmt.setInt(2, Integer.parseInt(idStr));
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Position updated successfully!");
                    } else {
                        PreparedStatement pstmt = connection.prepareStatement("INSERT INTO PUESTOS (DESCRIPCION) VALUES (?)");
                        pstmt.setString(1, description);
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Position saved successfully!");
                    }
                } else if (method.equalsIgnoreCase("DELETE")) {
                    String query = exchange.getRequestURI().getQuery();
                    int id = 0;
                    if (query != null && query.contains("id=")) {
                        id = Integer.parseInt(query.split("id=")[1].split("&")[0]);
                    }
                    try {
                        PreparedStatement pstmt = connection.prepareStatement("DELETE FROM PUESTOS WHERE idPUESTOS = ?");
                        pstmt.setInt(1, id);
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Position deleted successfully!");
                    } catch (SQLException ex) {
                        sendResponse(exchange, 400, "Cannot delete: assigned to one or more astronauts.");
                    }
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error in Positions: " + e.getMessage());
            }
        }
    }

    static class AstronautsCrudHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            try {
                String method = exchange.getRequestMethod();
                if (method.equalsIgnoreCase("GET")) {
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(
                        "SELECT a.*, g.DESCRIPCION as gen_desc, p.DESCRIPCION as pues_desc " +
                        "FROM ASTRONAUTAS a " +
                        "JOIN GENEROS g ON a.idGENEROS = g.idGENEROS " +
                        "JOIN PUESTOS p ON a.idPUESTOS = p.idPUESTOS"
                    );
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"idGeneros\":%d,\"idPuestos\":%d,\"nombre\":\"%s\",\"paterno\":\"%s\",\"materno\":\"%s\",\"edad\":%d,\"generoDesc\":\"%s\",\"puestoDesc\":\"%s\"}",
                            rs.getInt("idASTRONAUTAS"),
                            rs.getInt("idGENEROS"),
                            rs.getInt("idPUESTOS"),
                            rs.getString("NOMBRE"),
                            rs.getString("PATERNO"),
                            rs.getString("MATERNO"),
                            rs.getInt("EDAD"),
                            rs.getString("gen_desc"),
                            rs.getString("pues_desc")
                        ));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString());
                } else if (method.equalsIgnoreCase("POST")) {
                    byte[] bytes = exchange.getRequestBody().readAllBytes();
                    String json = new String(bytes, StandardCharsets.UTF_8);

                    String idStr = extractJsonValue(json, "id");
                    String name = extractJsonValue(json, "nombre");
                    String paternalLastName = extractJsonValue(json, "paterno");
                    String maternalLastName = extractJsonValue(json, "materno");
                    int age = Integer.parseInt(extractJsonValue(json, "edad"));
                    int genderId = Integer.parseInt(extractJsonValue(json, "idGeneros"));
                    int positionId = Integer.parseInt(extractJsonValue(json, "idPuestos"));

                    if (idStr != null && !idStr.isEmpty() && Integer.parseInt(idStr) > 0) {
                        PreparedStatement pstmt = connection.prepareStatement(
                            "UPDATE ASTRONAUTAS SET idGENEROS = ?, idPUESTOS = ?, NOMBRE = ?, PATERNO = ?, MATERNO = ?, EDAD = ? WHERE idASTRONAUTAS = ?"
                        );
                        pstmt.setInt(1, genderId);
                        pstmt.setInt(2, positionId);
                        pstmt.setString(3, name);
                        pstmt.setString(4, paternalLastName);
                        pstmt.setString(5, maternalLastName);
                        pstmt.setInt(6, age);
                        pstmt.setInt(7, Integer.parseInt(idStr));
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Astronaut updated successfully!");
                    } else {
                        PreparedStatement pstmt = connection.prepareStatement(
                            "INSERT INTO ASTRONAUTAS (idGENEROS, idPUESTOS, NOMBRE, PATERNO, MATERNO, EDAD) VALUES (?, ?, ?, ?, ?, ?)"
                        );
                        pstmt.setInt(1, genderId);
                        pstmt.setInt(2, positionId);
                        pstmt.setString(3, name);
                        pstmt.setString(4, paternalLastName);
                        pstmt.setString(5, maternalLastName);
                        pstmt.setInt(6, age);
                        pstmt.executeUpdate();
                        sendResponse(exchange, 200, "Astronaut registered successfully!");
                    }
                } else if (method.equalsIgnoreCase("DELETE")) {
                    String query = exchange.getRequestURI().getQuery();
                    int id = 0;
                    if (query != null && query.contains("id=")) {
                        id = Integer.parseInt(query.split("id=")[1].split("&")[0]);
                    }
                    
                    PreparedStatement pstmtReg = connection.prepareStatement("DELETE FROM REGISTROS WHERE idASTRONAUTAS = ?");
                    pstmtReg.setInt(1, id);
                    pstmtReg.executeUpdate();

                    PreparedStatement pstmt = connection.prepareStatement("DELETE FROM ASTRONAUTAS WHERE idASTRONAUTAS = ?");
                    pstmt.setInt(1, id);
                    pstmt.executeUpdate();
                    sendResponse(exchange, 200, "Astronaut deleted successfully!");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Error in Astronauts: " + e.getMessage());
            }
        }
    }

    static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    int astronautId = 0;
                    if (query != null && query.contains("idAstronauta=")) {
                        String[] parts = query.split("idAstronauta=");
                        if (parts.length > 1) {
                            astronautId = Integer.parseInt(parts[1].split("&")[0]);
                        }
                    }

                    String sql = "SELECT * FROM REGISTROS";
                    if (astronautId > 0) {
                        sql += " WHERE idASTRONAUTAS = ?";
                    }
                    sql += " ORDER BY idREGISTROS DESC";

                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    if (astronautId > 0) pstmt.setInt(1, astronautId);
                    ResultSet rs = pstmt.executeQuery();

                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format(
                            "{\"fecha\":\"%s\",\"hora\":\"%s\",\"fc\":%d,\"presSistolica\":%d,\"presDiastolica\":%d,\"azucar\":%d,\"temperatura\":%s,\"sueno\":%d,\"oxigeno\":%d,\"recomendaciones\":\"\"}",
                            rs.getString("FECHA") != null ? rs.getString("FECHA") : "",
                            rs.getString("HORA") != null ? rs.getString("HORA") : "",
                            rs.getInt("FRECUENCIAC"),
                            rs.getInt("PRESIONSIS"),
                            rs.getInt("PRESIONDIA"),
                            rs.getInt("AZUCARSANGRE"),
                            String.valueOf(rs.getDouble("TEMPCORP")),
                            rs.getInt("SUEÑO"),
                            rs.getInt("SATURACOX")
                        ));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "[]");
                }
            }
        }
    }

    private static void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:SaludAstronauta.db");
            System.out.println("Local database 'SaludAstronauta.db' connected.");
        } catch (SQLException e) {
            System.out.println("Error connecting to DB: " + e.getMessage());
        }
    }

    private static void createTables() {
        String[] sqlTables = {
            "CREATE TABLE IF NOT EXISTS PUESTOS (" +
            "  idPUESTOS INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  DESCRIPCION TEXT NOT NULL" +
            ")",
            "CREATE TABLE IF NOT EXISTS GENEROS (" +
            "  idGENEROS INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  DESCRIPCION TEXT NOT NULL" +
            ")",
            "CREATE TABLE IF NOT EXISTS ASTRONAUTAS (" +
            "  idASTRONAUTAS INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idGENEROS INTEGER NOT NULL," +
            "  idPUESTOS INTEGER NOT NULL," +
            "  NOMBRE TEXT NOT NULL," +
            "  PATERNO TEXT NOT NULL," +
            "  MATERNO TEXT NOT NULL," +
            "  EDAD INTEGER NOT NULL," +
            "  FOREIGN KEY(idGENEROS) REFERENCES GENEROS(idGENEROS)," +
            "  FOREIGN KEY(idPUESTOS) REFERENCES PUESTOS(idPUESTOS)" +
            ")",
            "CREATE TABLE IF NOT EXISTS REGISTROS (" +
            "  idREGISTROS INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idASTRONAUTAS INTEGER NOT NULL," +
            "  FRECUENCIAC INTEGER NOT NULL," +
            "  PRESIONSIS INTEGER NOT NULL," +
            "  PRESIONDIA INTEGER NOT NULL," +
            "  AZUCARSANGRE INTEGER NOT NULL," +
            "  TEMPCORP REAL NOT NULL," +
            "  SUEÑO INTEGER NOT NULL," +
            "  SATURACOX INTEGER NOT NULL," +
            "  FECHA TEXT," +
            "  HORA TEXT," +
            "  FOREIGN KEY(idASTRONAUTAS) REFERENCES ASTRONAUTAS(idASTRONAUTAS)" +
            ")",
            "CREATE TABLE IF NOT EXISTS RECOMENDACIONES (" +
            "  idRECOMENDACIONES INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  idDIAGNOSTICOS INTEGER NOT NULL," +
            "  DESCRIPCION TEXT NOT NULL" +
            ")"
        };

        try (Statement stmt = connection.createStatement()) {
            for (String sql : sqlTables) {
                stmt.executeUpdate(sql);
            }
            if (!columnExists("REGISTROS", "FECHA")) stmt.executeUpdate("ALTER TABLE REGISTROS ADD COLUMN FECHA TEXT");
            if (!columnExists("REGISTROS", "HORA"))  stmt.executeUpdate("ALTER TABLE REGISTROS ADD COLUMN HORA TEXT");
            if (!columnExists("ASTRONAUTAS", "idPUESTOS")) stmt.executeUpdate("ALTER TABLE ASTRONAUTAS ADD COLUMN idPUESTOS INTEGER DEFAULT 2");
            System.out.println("Tables successfully verified/created in SQLite!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static boolean columnExists(String tableName, String columnName) {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Insert initial data internally into the database from Java
    private static void preloadBaseData() {
        try {
            Statement stmt = connection.createStatement();
            ResultSet rsPositions = stmt.executeQuery("SELECT COUNT(*) FROM PUESTOS");
            if (rsPositions.next() && rsPositions.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO PUESTOS (idPUESTOS, DESCRIPCION) VALUES (1, 'Administrator'), (2, 'Astronaut')");
            }

            ResultSet rsGenres = stmt.executeQuery("SELECT COUNT(*) FROM GENEROS");
            if (rsGenres.next() && rsGenres.getInt(1) == 0) {
                stmt.executeUpdate("INSERT INTO GENEROS (DESCRIPCION) VALUES ('Male'), ('Female')");
            }

            ResultSet rsAstronauts = stmt.executeQuery("SELECT COUNT(*) FROM ASTRONAUTAS");
            if (rsAstronauts.next() && rsAstronauts.getInt(1) == 0) {
                // Insert commander (tables administrator) with ID=1
                stmt.executeUpdate("INSERT INTO ASTRONAUTAS (idGENEROS, idPUESTOS, NOMBRE, PATERNO, MATERNO, EDAD) VALUES (1, 1, 'Commander', 'Admin', 'Base', 40)");
                // Insert mission astronaut with ID=2 
                stmt.executeUpdate("INSERT INTO ASTRONAUTAS (idGENEROS, idPUESTOS, NOMBRE, PATERNO, MATERNO, EDAD) VALUES (1, 2, 'Carlos', 'López', 'Martínez', 29)");
                System.out.println("Default test positions and astronauts inserted.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class AstronautsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    String sql = "SELECT idASTRONAUTAS, idPUESTOS, NOMBRE, PATERNO, MATERNO FROM ASTRONAUTAS";
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append(String.format("{\"id\":%d,\"idPuesto\":%d,\"nombre\":\"%s %s %s\"}",
                            rs.getInt("idASTRONAUTAS"),
                            rs.getInt("idPUESTOS"),
                            rs.getString("NOMBRE"),
                            rs.getString("PATERNO"),
                            rs.getString("MATERNO")
                        ));
                        first = false;
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString());
                } catch (Exception e) {
                    sendResponse(exchange, 500, "[]");
                }
            }
        }
    }

    static class SaveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                try {
                    byte[] bytes = exchange.getRequestBody().readAllBytes();
                    String receivedJson = new String(bytes, StandardCharsets.UTF_8);

                    int astronautId = Integer.parseInt(extractJsonValue(receivedJson, "idAstronauta"));
                    int heartRate = Integer.parseInt(extractJsonValue(receivedJson, "fc"));
                    int sysPressure = Integer.parseInt(extractJsonValue(receivedJson, "presSistolica"));
                    int diaPressure = Integer.parseInt(extractJsonValue(receivedJson, "presDiastolica"));
                    int sugar = Integer.parseInt(extractJsonValue(receivedJson, "azucar"));
                    double temperature = Double.parseDouble(extractJsonValue(receivedJson, "temperatura"));
                    int sleep = Integer.parseInt(extractJsonValue(receivedJson, "sueno"));
                    int oxygen = Integer.parseInt(extractJsonValue(receivedJson, "oxigeno"));

                    boolean hasMoodColumn = columnExists("REGISTROS", "idESTADOANIMO");
                    String sql = "INSERT INTO REGISTROS (idASTRONAUTAS, " + (hasMoodColumn ? "idESTADOANIMO, " : "") +
                                 "FRECUENCIAC, PRESIONSIS, PRESIONDIA, AZUCARSANGRE, TEMPCORP, SUEÑO, SATURACOX, FECHA, HORA) " +
                                 "VALUES (?, " + (hasMoodColumn ? "1, " : "") +
                                 "?, ?, ?, ?, ?, ?, ?, date('now','localtime'), time('now','localtime'))";

                    PreparedStatement pstmt = connection.prepareStatement(sql);
                    pstmt.setInt(1, astronautId);
                    pstmt.setInt(2, heartRate);
                    pstmt.setInt(3, sysPressure);
                    pstmt.setInt(4, diaPressure);
                    pstmt.setInt(5, sugar);
                    pstmt.setDouble(6, temperature);
                    pstmt.setInt(7, sleep);
                    pstmt.setInt(8, oxygen);
                    pstmt.executeUpdate();

                    sendResponse(exchange, 200, "Vital signs successfully registered for Astronaut ID: " + astronautId + "!");

                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(exchange, 500, "Error saving to database: " + e.getMessage());
                }
            }
        }
    }

    static class IndicatorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                try {
                    String sql = "SELECT r.*, a.NOMBRE, a.PATERNO FROM REGISTROS r JOIN ASTRONAUTAS a ON r.idASTRONAUTAS = a.idASTRONAUTAS ORDER BY r.idREGISTROS DESC LIMIT 1";
                    Statement stmt = connection.createStatement();
                    ResultSet rs = stmt.executeQuery(sql);

                    String jsonResponse = "{}";
                    if (rs.next()) {
                        jsonResponse = String.format(
                            "{\"astronauta\":\"%s %s\",\"fc\":%d,\"presSis\":%d,\"presDia\":%d,\"azucar\":%d,\"temperatura\":%s,\"sueno\":%d,\"oxigeno\":%d}",
                            rs.getString("NOMBRE"),
                            rs.getString("PATERNO"),
                            rs.getInt("FRECUENCIAC"),
                            rs.getInt("PRESIONSIS"),
                            rs.getInt("PRESIONDIA"),
                            rs.getInt("AZUCARSANGRE"),
                            String.valueOf(rs.getDouble("TEMPCORP")),
                            rs.getInt("SUEÑO"),
                            rs.getInt("SATURACOX")
                        );
                    }
                    sendResponse(exchange, 200, jsonResponse);
                } catch (Exception e) {
                    sendResponse(exchange, 500, "{\"error\":\"Error reading database\"}");
                }
            }
        }
    }

    // Place index.html, References.pdf, and images in the project root folder
    private static final Path WEB_FOLDER = Paths.get("").toAbsolutePath().normalize();

    static class FilesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String route = exchange.getRequestURI().getPath();
            if (route == null || route.equals("/")) route = "/index.html";

            Path file = WEB_FOLDER.resolve(route.substring(1)).normalize();
            String contentType = getContentType(file.getFileName() == null ? "" : file.getFileName().toString());

            if (!file.startsWith(WEB_FOLDER) || contentType == null || !Files.isRegularFile(file)) {
                byte[] msg = ("Not found: " + route).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(404, msg.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(msg); }
                return;
            }

            byte[] content = Files.readAllBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(content); }
        }

        private static String getContentType(String fileName) {
            String n = fileName.toLowerCase();
            if (n.endsWith(".html") || n.endsWith(".htm")) return "text/html; charset=utf-8";
            if (n.endsWith(".css"))  return "text/css; charset=utf-8";
            if (n.endsWith(".js"))   return "application/javascript; charset=utf-8";
            if (n.endsWith(".gif"))  return "image/gif";
            if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
            if (n.endsWith(".png"))  return "image/png";
            if (n.endsWith(".svg"))  return "image/svg+xml";
            if (n.endsWith(".ico"))  return "image/x-icon";
            if (n.endsWith(".pdf"))  return "application/pdf";
            return null;
        }
    }

    private static void addCorsHeaders(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        String t = response.trim();
        boolean isJson = t.startsWith("{") || t.startsWith("[");
        exchange.getResponseHeaders().set("Content-Type", (isJson ? "application/json" : "text/plain") + "; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static String extractJsonValue(String json, String field) {
        try {
            String search = "\"" + field + "\":";
            int start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim().replace("\"", "");
        } catch (Exception e) {
            return "";
        }
    }
}