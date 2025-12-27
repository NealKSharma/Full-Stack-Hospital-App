package coms309.people;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Provides the Definition/Structure for the report row
 *
 * @author Devank Uppal
 */

@Getter // Lombok Shortcut for generating getter methods (Matches variable names set ie firstName -> getFirstName)
@Setter // Similarly for setters as well
@NoArgsConstructor // Default constructor
public class Report_adv {
    private int id;
    private String type;
    private String result;
    private String date;

    public Report_adv(int id, String type, String result, String date) {
        this.id = id;
        this.type = type;
        this.result = result;
        this.date = date;
    }

    /**
     * Get the report id
     */
    public int getId() { return id; }

    /**
     * Set the report id
     */
    public void setId(int id) { this.id = id; }

    /**
     * Get the report type (
     */
    public String getType() { return type; }

    /**
     * Set the report type
     */
    public void setType(String type) { this.type = type;}

    /**
     * Get the result of the report
     */
    public String getResult() { return result; }

    /**
     * Set the result of the report
     */
    public void setResult(String result) { this.result = result;}

    /**
     * Get the date of the report
     */
    public String getDate() { return date; }

    /**
     * Set the date of the report
     */
    public void setDate(String date) { this.date = date;}
}

