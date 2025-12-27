package coms309;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

@RestController
class WelcomeController {

    private HashSet<String> users = new HashSet<>();

    @GetMapping("/")
    public String welcome() {
        return "Hello and welcome to COMS 309";
    }

    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        if (users.contains(name)) {
            return "Hello and welcome to COMS 309: " + name;
        }
        return "Name not found: " + name;
    }

    @PostMapping("/")
    public String postWelcome(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (users.add(name)) {
            return "Added: " + name;
        }
        return name + " already exists";
    }

    @PutMapping("/update/{oldName}")
    public String updateWelcome(@PathVariable String oldName, @RequestBody Map<String, String> body) {
        String newName = body.get("name");
        if (!users.remove(oldName)) {
            return "Name not found: " + oldName;
        }
        users.add(newName);
        return "Updated " + oldName + " to: " + newName;
    }

    @DeleteMapping("/delete/{name}")
    public String deleteWelcome(@PathVariable String name) {
        if (users.remove(name)) {
            return "Deleted: " + name;
        }
        return "Name not found: " + name;
    }
}
