package backend.notifications;

class NotificationMessage {
    public String type;
    public String title;
    public String content;

    public NotificationMessage() {
    }

    public NotificationMessage(String type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
    }
}
