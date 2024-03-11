package com.myou.ec.ecsite.presentation.controller;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("menu")
@Observed
public class MenuController {

    private static final Logger logger = LoggerFactory.getLogger(MenuController.class);

    @GetMapping("/")
    @ResponseBody
    public String home() {
        logger.info("menu");
        return "success";
    }

}
