package com.mana.openhand_backend.donations.dataaccesslayer;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select d from Donation d join fetch d.user u order by d.createdAt desc")
    List<Donation> findAllWithUserOrderByCreatedAtDesc();

    @Query("select d from Donation d join fetch d.user u where d.id = :id")
    Optional<Donation> findByIdWithUser(@Param("id") Long id);

}
