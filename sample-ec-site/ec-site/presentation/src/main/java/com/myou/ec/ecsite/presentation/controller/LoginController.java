package com.myou.ec.ecsite.presentation.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginForm(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            String errorMessage = switch (error) {
                case "bad_credentials" -> "ユーザーIDまたはパスワードが正しくありません。";
                case "locked" -> "このアカウントはロックされています。管理者に連絡してください。";
                case "disabled" -> "このアカウントは現在無効です。";
                default -> "不明なエラーが発生しました。時間をおいて再度お試しください。";
            };
            model.addAttribute("errorMessage", errorMessage);
            return "loginFailure";
        }
        return "login";
    }
}
