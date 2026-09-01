package io.sj.saju.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditPackageRepository extends JpaRepository<CreditPackage, UUID> {
    List<CreditPackage> findByActiveTrueOrderBySortOrderAsc();
}
