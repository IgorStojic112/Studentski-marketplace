package ba.sum.fsre.studentskimarketplace.data.model;

public class Conversation {

    private String id;
    private String user1_id;
    private String user2_id;
    private String created_at;

    public Conversation(){}

    public String getId() { return id;}
    public String getUser1id(){return user1_id;}
    public String getUser2id(){return user2_id;}
    public String getCreated_at(){return created_at;}

    public void setId(String Id){ this.id = id; }
    public void setUser1_id (String user1_id){ this.user1_id = user1_id;}
    public void setUser_id2 (String user2_id){ this.user2_id = user2_id;}
    public void setCreated_at (String created_at) {this.created_at = created_at;}
}
