package com.neogaming.seller.repository;

import com.neogaming.seller.domain.FollowedSeller;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowedSellerRepository extends JpaRepository<FollowedSeller, UUID> {

    boolean existsByUserIdAndSellerId(UUID userId, UUID sellerId);

    Optional<FollowedSeller> findByUserIdAndSellerId(UUID userId, UUID sellerId);

    List<FollowedSeller> findByUserIdOrderByCreatedAtDesc(UUID userId);

    long countByUserId(UUID userId);
}
