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
}
