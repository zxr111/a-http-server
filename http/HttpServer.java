package http;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HttpServer {
    private static final HashMap<String, Processor>  routeMap = new HashMap<>();

    public void use(String url, Processor processor) {
        routeMap.put(url, processor);
    }

    public void listen(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket client = serverSocket.accept();
                new Thread(() -> {
                    try {
                        Request request = new Request(client.getInputStream());
                        Response response = new Response(client.getOutputStream());
                        if (routeMap.containsKey(request.getUrl())) {
                            routeMap.get(request.getUrl()).callback(request, response);
                        } else {
                            response.setStatus(404).send(request.getUrl() + " not fond");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        HttpServer httpSever = new HttpServer();
        httpSever.use("/test", (request, response) -> {
            response.setStatus(200)
                    .setHeader("Content-Type", "text/html")
                    .send("<h1> test </h1>");
        });
        httpSever.use("/", (request, response) -> {
            response.setStatus(200)
                    .setHeader("Content-Type", "text/html")
                    .send("<h1> root </h1>");
        });
        httpSever.listen(5678);
    }
}

class Request {
    private String method;
    private String url;
    private String params;

    public Request(InputStream inputStream) {
        try {
            String reqCxt = new BufferedReader(new InputStreamReader(inputStream)).readLine();
            String[] reqParts = reqCxt.split(" ");
            if (reqParts.length == 3 && reqParts[2].equals("HTTP/1.1")) {
                this.method = reqParts[0];
                String fullUrl = reqParts[1];
                if (fullUrl.contains("?")) {
                    this.url = fullUrl.substring(0, fullUrl.indexOf("?"));
                    this.params = fullUrl.substring(fullUrl.indexOf("?") + 1);
                } else {
                    this.url = fullUrl;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
}

class Response {
    private String status;
    private Processor processor;
    private final HashMap<String, String> headers = new HashMap<>();
    private OutputStream outputStream;

    public Response(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void send(String data) {
        try(OutputStream out = outputStream) {
            StringBuilder sb = new StringBuilder();
            sb.append("HTTP/1.1 ").append(this.status).append("\n");

            for (String key: headers.keySet()) {
                sb.append(key).append(":").append(headers.get(key)).append("\n");
            }

            sb.append("\n").append(data);

            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Response setHeader(String key, String val) {
        this.headers.put(key, val);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public Response setStatus(String status) {
        this.status = status;
        return this;
    }

    public Response setStatus(int status) {
        this.status = Integer.toString(status);
        return this;
    }

    public Processor getProcessor() {
        return processor;
    }

    public void setProcessor(Processor processor) {
        this.processor = processor;
    }

}