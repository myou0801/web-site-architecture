package com.myou.ec.ecsite.presentation.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;

public class PasswordChangeForm {

    @NotEmpty(message = "現在のパスワードを入力してください。")
    private String currentPassword;

    @NotEmpty(message = "新しいパスワードを入力してください。")
    private String newPassword;

    @NotEmpty(message = "新しいパスワード（確認）を入力してください。")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @AssertTrue(message = "新しいパスワードと確認用パスワードが一致しません。")
    public boolean isValidConfirmPassword(){
        return newPassword.equals(confirmPassword);
    }
}