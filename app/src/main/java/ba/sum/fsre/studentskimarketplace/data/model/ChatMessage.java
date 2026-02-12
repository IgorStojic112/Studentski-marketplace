package ba.sum.fsre.studentskimarketplace.data.model;

import com.google.gson.annotations.SerializedName;

public class ChatMessage {
    @SerializedName("id")
    public String id;

    @SerializedName("sender_id")
    public String senderId;

    @SerializedName("receiver_id")
    public String receiverId;

    @SerializedName("content")
    public String content;

    @SerializedName("created_at")
    public String createdAt;

    public ChatMessage() {}

    public ChatMessage(String senderId, String receiverId, String content) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }
}