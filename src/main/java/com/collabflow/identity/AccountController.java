package com.collabflow.identity;

import com.collabflow.identity.dto.ChangeEmailRequest;
import com.collabflow.identity.dto.ChangePasswordRequest;
import com.collabflow.identity.dto.UserResponse;
import com.collabflow.shared.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints about the logged-in user. Spring passes in the already-verified token as {@code jwt}. */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public UserResponse getMe(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(accountService.getUser(CurrentUser.id(jwt)));
    }

    @PutMapping("/email")
    public UserResponse changeEmail(@AuthenticationPrincipal Jwt jwt,
                                    @Valid @RequestBody ChangeEmailRequest request) {
        return UserResponse.from(accountService.changeEmail(CurrentUser.id(jwt), request));
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(CurrentUser.id(jwt), request);
    }
}
