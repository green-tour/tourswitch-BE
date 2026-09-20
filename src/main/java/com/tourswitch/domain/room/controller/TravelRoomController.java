package com.tourswitch.domain.room.controller;

import com.tourswitch.global.security.principal.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.tourswitch.domain.room.request.CreateTravelRoomRequest;
import com.tourswitch.domain.room.response.ActiveRoomResponse;
import com.tourswitch.domain.room.response.CreateTravelRoomResponse;
import com.tourswitch.domain.room.service.ActiveRoomService;
import com.tourswitch.domain.room.service.TravelRoomService;
import com.tourswitch.global.response.GlobalRes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class TravelRoomController {
    private final TravelRoomService travelRoomService;
    private final ActiveRoomService activeRoomService;

    @GetMapping("/active")
    public GlobalRes<ActiveRoomResponse> getActiveRoom(@AuthenticationPrincipal UserPrincipal principal) {
        return GlobalRes.success(activeRoomService.getActiveRoom(principal.memberId()));
    }

    @PostMapping
    public GlobalRes<CreateTravelRoomResponse> createRoom(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody CreateTravelRoomRequest request) {
        return GlobalRes.success(travelRoomService.createRoom(principal.memberId(), request));
    }
}
