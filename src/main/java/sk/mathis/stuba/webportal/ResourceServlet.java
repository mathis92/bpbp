package sk.mathis.stuba.webportal;

import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ResourceServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String requestUrl = request.getRequestURI();
        requestUrl = requestUrl.replaceAll("/resource", "");
        if (requestUrl.trim().isEmpty()) {
            requestUrl = "/index.html";
        }
        String requestFileName = "/webportal" + requestUrl;
        InputStream is = getClass().getResourceAsStream(requestFileName);
        if (is == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("404 Not Found");
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            String fileNameLc = requestFileName.toLowerCase();
            if (fileNameLc.endsWith(".js")) {
                response.setContentType("text/javascript");
            } else if (fileNameLc.endsWith(".css")) {
                response.setContentType("text/css");
            } else if (fileNameLc.endsWith(".png")) {
                response.setContentType("image/png");
            } else if (fileNameLc.endsWith(".svg")) {
                response.setContentType("image/svg");
            } else if (fileNameLc.endsWith(".eot")) {
                response.setContentType("font/opentype");
            } else if (fileNameLc.endsWith(".ttf")) {
                response.setContentType("application/x-font-ttf");
            } else if (fileNameLc.endsWith(".woff")) {
                response.setContentType("application/x-font-woff");
            } else if (fileNameLc.endsWith(".html")) {
                response.setContentType("text/html");
            } else {
                throw new ServletException("Unknown file type");
            }
            byte[] buffer = new byte[1024];
            while (is.available() > 0) {
                int rd = is.read(buffer, 0, buffer.length);
                response.getOutputStream().write(buffer, 0, rd);
            }
        }
    }
}
