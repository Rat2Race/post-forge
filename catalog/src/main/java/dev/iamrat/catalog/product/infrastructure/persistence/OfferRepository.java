package dev.iamrat.catalog.product.infrastructure.persistence;

import dev.iamrat.catalog.product.domain.Offer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {
    Optional<Offer> findBySourceAndExternalProductId(String source, String externalProductId);
}
