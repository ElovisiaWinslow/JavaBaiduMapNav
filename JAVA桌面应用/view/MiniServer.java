package view;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MiniServer {
    // 设置端口，避免和 Live Server (5500) 冲突
    private static final int PORT = 8899;
    // 你的 HTML 项目实际所在的文件夹路径 (注意：这里不要包含 index.html，只要文件夹)
    private static final String WEB_ROOT = "";
    
    private static HttpServer server;

    public static void startAndOpen() {
        try {
            if (server == null) {
                // 创建服务器
                server = HttpServer.create(new InetSocketAddress(PORT), 0);
                server.createContext("/", new StaticFileHandler());
                server.setExecutor(null); 
                server.start();
                System.out.println("本地地图服务器已启动: http://localhost:" + PORT);
            }
            
            // 唤起浏览器打开页面
            Desktop.getDesktop().browse(new URI("http://localhost:" + PORT + "/index.html"));
            
        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(null, "无法启动地图服务: " + e.getMessage());
        }
    }

    // 处理文件请求的处理器
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String uriPath = t.getRequestURI().getPath();
            // 默认访问 index.html
            if (uriPath.equals("/")) uriPath = "/index.html";
            
            // 处理中文路径和空格
            uriPath = URLDecoder.decode(uriPath, "UTF-8");

            Path filePath = Paths.get(WEB_ROOT, uriPath);
            File file = filePath.toFile();

            if (file.exists() && !file.isDirectory()) {
                // 简单的 MIME 类型判断
                String mime = "text/html";
                if(uriPath.endsWith(".js")) mime = "application/javascript";
                else if(uriPath.endsWith(".css")) mime = "text/css";
                else if(uriPath.endsWith(".png")) mime = "image/png";
                
                t.getResponseHeaders().set("Content-Type", mime + "; charset=utf-8");
                t.sendResponseHeaders(200, file.length());
                
                try (OutputStream os = t.getResponseBody()) {
                    Files.copy(filePath, os);
                }
            } else {
                // 文件不存在 404
                String response = "404 Not Found (Check path: " + filePath + ")";
                t.sendResponseHeaders(404, response.length());
                try (OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        }
    }
}