package coms309;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello and welcome to COMS 309";
    }
    
    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    // Additions by Neal Kaushik Sharma

    @GetMapping("/about/{course}/{name}")
    public String courseWelcome(@PathVariable String course, @PathVariable String name) {
        return "Welcome " + name + " to the course " + course;
    }

    @GetMapping ("/location")
    public String location() {
        String location = "Ames, Iowa"; // Can be changed or made dynamic if able to get user's ip
        return "Your location is: " + location;
    }

    @GetMapping ("/location/{location}")
    public String  location(@PathVariable String location) {
        return "Your location is: " + location;
    }

    @GetMapping("/date")
    public String date() {
        return "The current date is: " + java.time.LocalDate.now();
    }
}
