package com.myou.ec.ecsite.presentation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import io.micrometer.observation.annotation.Observed;

@Controller
@RequestMapping("menu")
@Observed
public class MenuController {

    private static Logger logger = LoggerFactory.getLogger(MenuController.class);

    @GetMapping("/")
    @ResponseBody
    public String home() {
        return "success";
    }

}
