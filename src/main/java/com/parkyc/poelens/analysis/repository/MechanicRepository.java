package com.parkyc.poelens.analysis.repository;

import com.parkyc.poelens.analysis.entity.MechanicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MechanicRepository extends JpaRepository<MechanicEntity, Long> {

    Optional<MechanicEntity> findByNameIgnoreCaseAndGameVersion(String name, String gameVersion);
}
