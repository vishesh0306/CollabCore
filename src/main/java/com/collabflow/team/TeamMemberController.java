package com.collabflow.team;

import java.util.UUID;

import com.collabflow.shared.CurrentUser;
import com.collabflow.team.dto.AddMemberRequest;
import com.collabflow.team.dto.ChangeRoleRequest;
import com.collabflow.team.dto.MemberResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/members")
@RequiredArgsConstructor
public class TeamMemberController {

    private final TeamMemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MemberResponse addMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                    @Valid @RequestBody AddMemberRequest request) {
        return memberService.addMember(CurrentUser.id(jwt), teamId, request);
    }

    @PutMapping("/{userId}")
    public MemberResponse changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                                     @PathVariable UUID userId, @Valid @RequestBody ChangeRoleRequest request) {
        return memberService.changeRole(CurrentUser.id(jwt), teamId, userId, request);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID teamId,
                             @PathVariable UUID userId) {
        memberService.removeMember(CurrentUser.id(jwt), teamId, userId);
    }
}
