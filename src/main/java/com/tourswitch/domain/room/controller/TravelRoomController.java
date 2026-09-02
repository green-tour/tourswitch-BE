package com.tourswitch.domain.room.controller;

import com.tourswitch.domain.room.request.CreateTravelRoomRequest;
import com.tourswitch.domain.room.response.CreateTravelRoomResponse;
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

    @PostMapping
    public GlobalRes<CreateTravelRoomResponse> createRoom(@RequestParam Long memberId,
                                                           @Valid @RequestBody CreateTravelRoomRequest request) {
        return GlobalRes.success(travelRoomService.createRoom(memberId, request));
    }
}
