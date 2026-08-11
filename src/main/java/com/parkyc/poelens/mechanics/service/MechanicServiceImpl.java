package com.parkyc.poelens.mechanics.service;

import com.parkyc.poelens.mechanics.domain.entity.MechanicEntity;
import com.parkyc.poelens.mechanics.repository.MechanicRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MechanicServiceImpl implements MechanicService {

    private final MechanicRepository mechanicRepository;
    private final String gameVersion;

    public MechanicServiceImpl(MechanicRepository mechanicRepository,
                               @Value("${poe.catalog.game-version}") String gameVersion) {
        this.mechanicRepository = mechanicRepository;
        this.gameVersion = gameVersion;
    }

    @Override
    public String gameVersion() {
        return gameVersion;
    }

    @Override
    public Optional<MechanicEntity> find(String name) {
        return mechanicRepository.findByNameIgnoreCaseAndGameVersion(name, gameVersion);
    }
}
