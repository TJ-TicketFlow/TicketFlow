package com.ticketflow.service;

import com.ticketflow.entity.Wishlist;
import com.ticketflow.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용으로 설정하여 효율성 증대
public class WishlistService {

    private final WishlistRepository wishlistRepository;

    // 1. 유저 ID로 찜한 공연 목록 조회
    public List<Wishlist> findWishlistByUserId(String userId) {
        // Repository에서 유저 아이디로 리스트를 가져옵니다.
        return wishlistRepository.findByUser_UserId(userId);
    }

    // 2. 유저가 찜한 공연의 개수 조회 (마이페이지 benefits용)
    public int countByUserId(String userId) {
        // long 타입을 int로 변환
        return (int) wishlistRepository.countByUser_UserId(userId);
    }
}