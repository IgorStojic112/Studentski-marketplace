package ba.sum.fsre.studentskimarketplace.data.model;

public class Favorite {
    private String id;
    private String user_id;
    private String ad_id;
    private String created_at;

    public Favorite() {}

    public String getId() { return id; }
    public String getUser_id() { return user_id; }
    public String getAd_id() { return ad_id; }
    public String getCreated_at() { return created_at; }

    public void setId(String id) { this.id = id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setAd_id(String ad_id) { this.ad_id = ad_id; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
}

