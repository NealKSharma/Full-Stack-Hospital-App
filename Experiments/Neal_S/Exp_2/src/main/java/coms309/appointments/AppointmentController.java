package coms309.appointments;

import coms309.appointments.Appointment;
import org.springframework.web.bind.annotation.*;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Controller to create appointments
 *
 * @author Neal Kaushik Sharma
 */

@RestController
public class AppointmentController {

    HashMap<String, Appointment> appointmentList = new  HashMap<>();

    // Lists all appointments
    @GetMapping("/appointments")
    public  HashMap<String, Appointment> getAllAppointments() {
        return appointmentList;
    }

    // CREATE a new appointment
    @PostMapping("/appointments")
    public String createAppointment(@RequestBody Appointment appointment) {
        System.out.println(appointment);
        appointmentList.put(appointment.getFirstName(), appointment);
        return "Appointment created for " + appointment.getFirstName()
                + " on " + appointment.getDate() + " at " + appointment.getTime();
    }

    // READ a specific appointment by firstName
    @GetMapping("/appointments/{firstName}")
    public Appointment getAppointment(@PathVariable String firstName) {
        return appointmentList.get(firstName);
    }

    // LIST appointments on a specific date
    @GetMapping("/appointments/date")
    public List<Appointment> getAppointmentsByDate(@RequestParam("date") String date) {
        List<Appointment> res = new ArrayList<>();
        for (Appointment appointment : appointmentList.values()) {
            if (appointment.getDate().equals(date)) {
                res.add(appointment);
            }
        }
        return res;
    }

    // UPDATE appointment by firstName (PathVariable)
    @PutMapping("/appointments/{firstName}")
    public Appointment updateAppointment(@PathVariable String firstName, @RequestBody Appointment appointment) {
        appointmentList.replace(firstName, appointment);
        return appointmentList.get(firstName);
    }

    // UPDATE appointment by firstName (RequestParam)
    @PutMapping(
            value="/appointments",
            params = { "firstName" }
    )
    public Appointment updateAppointment2(@RequestParam("firstName") String firstName, @RequestBody Appointment appointment) {
        appointmentList.replace(firstName, appointment);
        return appointmentList.get(firstName);
    }

    // DELETE appointment by firstName
    @DeleteMapping("/appointments/{firstName}")
    public HashMap<String, Appointment> deleteAppointment(@PathVariable String firstName) {
        appointmentList.remove(firstName);
        return appointmentList;
    }
}