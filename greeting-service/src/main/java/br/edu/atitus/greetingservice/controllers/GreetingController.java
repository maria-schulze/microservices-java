package br.edu.atitus.greetingservice.controllers;

import br.edu.atitus.greetingservice.configs.GreetingConfig;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/greeting")
public class GreetingController {

    private final GreetingConfig config;

    public GreetingController(GreetingConfig config) {
        this.config = config;
    }

    @GetMapping({"", "/"})
    public String getGreeting(
            @RequestParam(required = false) String name) {
        if (name == null || name.isEmpty()) {
            name = config.getDefaultName();
        }
        return String.format("%s, %s!!!", config.getGreeting(), name);
    }

    @GetMapping("/{name}")
    public String getGreetingPath(@PathVariable String name) {
        return String.format("%s, %s!!!", config.getGreeting(), name);
    }

    @PostMapping({"", "/"})
    public String postGreeting(@RequestBody Map<String, String> body) {
        String name = body.getOrDefault("name", config.getDefaultName());
        return String.format("%s, %s!!!", config.getGreeting(), name);
    }
}
