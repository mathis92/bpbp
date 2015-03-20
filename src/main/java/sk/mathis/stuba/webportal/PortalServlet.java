
package sk.mathis.stuba.webportal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author Martin Banas
 */
public class PortalServlet extends HttpServlet {
    
    private final List<WebMenuItem> wmiList = new ArrayList<>();
    private String currentPage;

    public PortalServlet() {
        this.wmiList.add(new WebMenuItem("Switch status", "/status.html", "qrcode"));
        this.wmiList.add(new WebMenuItem("MAC Address table", "/macaddrtable.html", "th-large"));
        this.wmiList.add(new WebMenuItem("Statistics", "/statistics.html", "stats"));
        this.wmiList.add(new WebMenuItem("Filtering", "/filtering.html", "filter"));
        this.wmiList.add(new WebMenuItem("Port security", "/portsecurity.html", "lock"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.currentPage = request.getRequestURI();
        String html = this.getHeaderTemplate();
        System.out.println(currentPage);
        
        
        
        
        html += this.getFooterTemplate();
        response.setStatus(HttpServletResponse.SC_OK);
        ByteArrayInputStream htmlBais = new ByteArrayInputStream(html.getBytes("UTF-8"));
        byte[] buffer = new byte[1024];
        while (htmlBais.available() > 0) {
            int rd = htmlBais.read(buffer, 0, buffer.length);
            response.getOutputStream().write(buffer, 0, rd);
        }
    }        
    
    private String getNotFoundPage() {
        String html = "<div class=\"jumbotron\" style=\"margin-top: 15px;\">\n"
                + "  <h1>Page not found!</h1>\n"
                + "</div>";
        return html;
    }

    private String getHeaderTemplate() {
        String html = "<!DOCTYPE html><html><head><title>Software Multilayer Switch</title><meta charset=\"utf-8\">";
        html += this.getLinkedJavascript("/resource/js/jquery.js");
        html += this.getLinkedCss("/resource/css/bootstrap.css");
        html += this.getLinkedCss("/resource/css/bootstrap-theme.css");
        html += this.getLinkedJavascript("/resource/js/bootstrap.js");
        html += "</head><body>";
        html += "<div class=\"container\" style=\"width:800px\">";
        html += "<ul class=\"nav nav-tabs\" role=\"tablist\">";
        for (WebMenuItem wmi : this.wmiList) {
            String activeClass = (this.currentPage.equals(wmi.getAddress())) ? " class=\"active\"" : "";
            String iconHtml = (wmi.getIcon() == null) ? "" : "<span class=\"glyphicon glyphicon-" + wmi.getIcon() + "\"></span> ";
            html += "<li" + activeClass + "><a href=\"" + wmi.getAddress() + "\">" + iconHtml + wmi.getTitle() + "</a></li>";
        }
        html += "</ul>";
        return html;
    }

    private String getFooterTemplate() {
        String html = "</div></body></html>";
        return html;
    }

    private String getLinkedJavascript(String address) {
        return "<script type=\"text/javascript\" src=\"" + address + "\"></script>";
    }

    private String getLinkedCss(String address) {
        return "<link rel=\"stylesheet\" href=\"" + address + "\" />";
    }
}
