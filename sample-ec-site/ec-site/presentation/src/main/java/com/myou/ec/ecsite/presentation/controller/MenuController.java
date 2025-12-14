package com.myou.ec.ecsite.presentation.controller;

import com.myou.ec.ecsite.presentation.auth.security.userdetails.AuthAccountDetails;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/menu")
@Observed
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);


    @GetMapping
    public String home(Model model, @AuthenticationPrincipal AuthAccountDetails userDetails) {
        logger.info("Displaying menu for user: {}", userDetails.getUsername());
        // userDetails is automatically available in the model if using Spring Security with Thymeleaf
        return "menu";
    }

}
