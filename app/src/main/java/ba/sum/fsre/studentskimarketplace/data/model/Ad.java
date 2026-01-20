package ba.sum.fsre.studentskimarketplace.data.model;

public class Ad {
    private String id;
    private String user_id;
    private String title;
    private String description;
    private String faculty;
    private int price;
    private String created_at;

    public Ad() {}

    public Ad(String id, String title, String faculty, int price){
        this.id = id;
        this.title = title;
        this.faculty = faculty;
        this.price = price;
    }

    public String getId() { return id; }
    public String getUser_id() { return user_id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getFaculty() { return faculty; }
    public int getPrice() { return price; }
    public String getCreated_at() { return created_at; }
}
