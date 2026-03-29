package com.test.basic;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = {"/", "/home"})
    public String mainPage() {
        return "redirect:/jikimi/";
    }

    @GetMapping("/jikimi")
    public String jikimiRoot() {
        return "redirect:/jikimi/";
    }

    @GetMapping("/jikimi/")
    public String index() {
        return "forward:/jikimi/index.html";
    }

    // Safety redirect for direct root-path access without basePath.
    @GetMapping({"/admin", "/admin/"})
    public String adminPage() {
        return "redirect:/jikimi/admin";
    }
}
