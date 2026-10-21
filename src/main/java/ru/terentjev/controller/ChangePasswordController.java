package ru.terentjev.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import ru.terentjev.security.SecurityUtil;
import ru.terentjev.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class ChangePasswordController {
    private static final Logger log = LoggerFactory.getLogger(ChangePasswordController.class);
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordController(
            UserService userService,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/change-password")
    public String changePasswordPage() {
        boolean notCompromised = SecurityUtil.user()
                .filter(userService::isCompromised)
                .isEmpty();
        if (notCompromised) {
            return "redirect:/";
        }
        return "change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            HttpServletRequest request
    ) {
        String username = SecurityUtil.userRequired();
        if (!userService.isCompromised(username)) {
            return "redirect:/";
        }
        try {
            if (!newPassword.equals(confirmPassword)) {
                throw new IllegalArgumentException("New passwords do not match");
            }
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
            var user = userService.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("Current password is incorrect");
            }
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                throw new IllegalArgumentException("New password must be different from current password");
            }
            userService.changePassword(username, newPassword);
            model.addAttribute("successMessage", "Password changed successfully. Redirecting...");
            request.setAttribute("passwordChanged", true);
        } catch (Exception e) {
            log.error("Failed to change password", e);
            model.addAttribute("errorMessage", "Failed to change password: " + e.getMessage());
        }
        return "change-password";
    }
}
