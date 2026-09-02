package com.tourswitch.domain.room.repository;

import com.tourswitch.domain.room.entity.RoomKeyword;
import com.tourswitch.domain.room.entity.RoomKeywordId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomKeywordRepository extends JpaRepository<RoomKeyword, RoomKeywordId> {
}
