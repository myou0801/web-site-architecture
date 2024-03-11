package com.myou.ec.ecsite.presentation.controller.user;

import com.myou.ec.ecsite.application.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String show(){
        return "user/index";
    }

    @PostMapping("register")
    public String registerUser(UserForm userForm) {
        userService.registerUser(userForm.toUser());
        return "redirect:/users/list";
    }

    @GetMapping("list")
    public String list(Model model){
        var users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "user/list";
    }

}
