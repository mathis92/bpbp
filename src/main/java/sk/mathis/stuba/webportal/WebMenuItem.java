package sk.mathis.stuba.webportal;

public class WebMenuItem {

    private final String title;
    private final String address;
    private final String icon;

    public WebMenuItem(String title, String addres, String icon) {
        this.title = title;
        this.address = addres;
        this.icon = icon;
    }

    public WebMenuItem(String title, String address) {
        this(title, address, null);
    }

    public String getTitle() {
        return title;
    }

    public String getAddress() {
        return address;
    }

    public String getIcon() {
        return icon;
    }
}
