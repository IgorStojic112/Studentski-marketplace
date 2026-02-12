package ba.sum.fsre.studentskimarketplace.data.model;

public class Ad {
    private String id;
    private String user_id;
    private String title;
    private String description;
    private String faculty;
    private Double price;
    private String created_at;
    private String imageUrl;

    public Ad() {}

    public String getId() { return id; }
    public String getUser_id() { return user_id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFaculty() { return faculty; }
    public Double getPrice() { return price; }

    public String getCreated_at() { return created_at; }
    public String getImageUrl() { return imageUrl; }

    public void setId(String id) { this.id = id; }
    public void setUser_id(String user_id) { this.user_id = user_id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public void setPrice(Double price) { this.price = price; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}