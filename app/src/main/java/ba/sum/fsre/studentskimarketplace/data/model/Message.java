package ba.sum.fsre.studentskimarketplace.data.model;

public class Message {

    private String id;
    private String conversation_id;
    private String sender_id;
    private String content;
    private String created_at;

    public Message(){}

    public String getId() { return id;}
    public String getConversation_id() { return conversation_id;}
    public String getSender_id() { return sender_id;}
    public String getContent() { return content;}
    public String getCreated_at() { return created_at;}

    public void setId(String Id){ this.id = id; }
    public void setConversation_id(String conversation_id) {this.conversation_id = conversation_id;}
    public void setSender_id(String sender_id){ this.sender_id = sender_id;}
    public void setContent(String content){this.content = content;}
    public void setCreated_at(String created_at){this.created_at = created_at;}



}
