package com.servicehomes.api.identity.web;

import com.servicehomes.api.identity.application.ProfileService;
import com.servicehomes.api.identity.application.dto.HostProfileDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/hosts")
@RequiredArgsConstructor
public class HostController {

    private final ProfileService profileService;

    @GetMapping("/{hostId}")
    public ResponseEntity<HostProfileDto> getHostProfile(@PathVariable UUID hostId) {
        return ResponseEntity.ok(profileService.getHostProfile(hostId));
    }
}
