package nexlink.server.models;

/**
 *
 * @author WARDAN
 */
public class Message {

    // fields matching DB columns
    private int id;
    private String sender;
    private String receiver;
    private String message;

    // constructor for creating new message (no id — DB auto generates it)
    public Message(String sender, String receiver, String message) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    // constructor for fetching existing message from DB (has id)
    public Message(int id, String sender, String receiver, String message) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    // getters
    public int getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    // setter — only id needs setter, rest never change
    public void setId(int id) {
        this.id = id;
    }

}
