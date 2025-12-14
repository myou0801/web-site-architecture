package com.myou.ec.ecsite.presentation.controller;

import com.myou.ec.ecsite.application.auth.sharedservice.PasswordChangeSharedService;
import com.myou.ec.ecsite.domain.auth.exception.AuthDomainException;
import com.myou.ec.ecsite.domain.auth.exception.PasswordPolicyViolationException;
import com.myou.ec.ecsite.presentation.auth.security.userdetails.AuthAccountDetails;
import com.myou.ec.ecsite.presentation.form.PasswordChangeForm;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/password")
public class PasswordChangeController {

    private final PasswordChangeSharedService passwordChangeSharedService;

    public PasswordChangeController(PasswordChangeSharedService passwordChangeSharedService) {
        this.passwordChangeSharedService = passwordChangeSharedService;
    }

    // パスワード変更フォームの初期化
    @ModelAttribute("passwordChangeForm")
    public PasswordChangeForm setupForm() {
        return new PasswordChangeForm();
    }

    @GetMapping("/change")
    public String showPasswordChangeForm() {
        return "account/passwordChange";
    }

    @PostMapping("/change")
    public String changePassword(@Validated @ModelAttribute("passwordChangeForm") PasswordChangeForm form,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model,
                                 @AuthenticationPrincipal AuthAccountDetails authAccountDetails) {

        if (bindingResult.hasErrors()) {
            return "account/passwordChange";
        }
        
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "passwordChangeForm.confirmPassword.unmatch", "新しいパスワードと確認用パスワードが一致しません。");
            return "account/passwordChange";
        }

        try {
            passwordChangeSharedService.changePassword(
                    authAccountDetails.authAccountId(),
                    form.getCurrentPassword(),
                    form.getNewPassword());

            redirectAttributes.addFlashAttribute("successMessage", "パスワードを変更しました。");
            return "redirect:/account/password/change/complete"; 

        } catch (PasswordPolicyViolationException e) {
            // パスワードポリシー違反の例外をハンドリング
            // TODO: e.getPasswordViolations() から詳細なエラーを取得し、個別のフィールドにバインドする
            bindingResult.reject("error.password.policy", "パスワードはポリシーの要件を満たしていません。");
            return "account/passwordChange";
        } catch (AuthDomainException e) {
            // その他の認証ドメイン例外（例：現在のパスワードが違うなど）
            model.addAttribute("errorMessage", e.getMessage());
            return "account/passwordChange";
        } catch (Exception e) {
            // 予期せぬ例外
            model.addAttribute("errorMessage", "予期せぬエラーが発生しました。");
            return "account/passwordChange";
        }
    }
    
    @GetMapping("/change/complete")
    public String showChangeComplete() {
        return "account/passwordChangeComplete";
    }
}
