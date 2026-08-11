package com.parkyc.poelens.analysis.catalog;

import com.parkyc.poelens.analysis.entity.MechanicEntity;
import com.parkyc.poelens.analysis.repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MechanicsCatalog {

    private final MechanicRepository mechanicRepository;
    private final String gameVersion;

    public MechanicsCatalog(MechanicRepository mechanicRepository,
                            @Value("${poe.catalog.game-version}") String gameVersion) {
        this.mechanicRepository = mechanicRepository;
        this.gameVersion = gameVersion;
    }

    public String gameVersion() {
        return gameVersion;
    }

    public Optional<MechanicEntity> find(String name) {
        return mechanicRepository.findByNameIgnoreCaseAndGameVersion(name, gameVersion);
    }
}
