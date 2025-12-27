package coms309.appointments;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Provides the Definition/Structure for the appointment row
 *
 * @author Neal Kaushik Sharma
 */

@Getter
@Setter
@NoArgsConstructor
public class Appointment {

    private String firstName;
    private String lastName;
    private String phone;
    private String details;
    private String date;
    private String time;

    public Appointment(String firstName, String lastName, String phone, String date, String time, String details){
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.details = details;
        this.date = date;
        this.time = time;
    }

    public String getFirstName() { return this.firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return this.lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return this.phone;
    }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDetails() {
        return this.details;
    }
    public void setDetails(String details) {
        this.details = details;
    }

    public String getDate() { return this.date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return this.time; }
    public void setTime(String time) { this.time = time; }

    @Override
    public String toString() {
        return firstName + " "
                + lastName + " "
                + phone + " "
                + details + " "
                + date + " "
                + time;
    }
}
