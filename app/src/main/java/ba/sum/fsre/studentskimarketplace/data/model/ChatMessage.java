package ba.sum.fsre.studentskimarketplace.data.model;

public class ChatMessage {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private String createdAt;
    public ChatMessage() {}

    public String getId() { return id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }

    public String getContent() { return content; }
    public String getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public void setContent(String content) { this.content = content; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}


