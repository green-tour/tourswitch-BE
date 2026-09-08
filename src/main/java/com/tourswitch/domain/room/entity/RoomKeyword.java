package com.tourswitch.domain.room.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@IdClass(RoomKeywordId.class)
@Table(name = "room_keyword")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomKeyword {
    @Id @Column(name = "travel_room_id")
    private Long travelRoomId;
    @Id @Column(name = "keyword_id")
    private Long keywordId;

    public static RoomKeyword create(Long roomId, Long keywordId) {
        RoomKeyword value = new RoomKeyword();
        value.travelRoomId = roomId;
        value.keywordId = keywordId;
        return value;
    }
}
