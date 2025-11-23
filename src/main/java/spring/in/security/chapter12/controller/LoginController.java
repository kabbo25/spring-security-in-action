package spring.in.security.chapter12.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "locked", required = false) String locked,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {

        if (locked != null) {
            model.addAttribute("accountLocked", true);
        }

        if (error != null) {
            model.addAttribute("loginError", true);
        }

        if (logout != null) {
            model.addAttribute("logoutSuccess", true);
        }

        return "login";
    }
}
