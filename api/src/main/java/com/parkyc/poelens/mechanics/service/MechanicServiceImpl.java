package com.parkyc.poelens.mechanics.service;

import com.parkyc.poelens.mechanics.domain.entity.MechanicEntity;
import com.parkyc.poelens.mechanics.repository.MechanicRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MechanicServiceImpl implements MechanicService {

    private final MechanicRepository mechanicRepository;
    public MechanicServiceImpl(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    @Override
    public Optional<MechanicEntity> find(String gameVersion, String name) {
        return mechanicRepository.findByNameIgnoreCaseAndGameVersion(name, gameVersion);
    }
}
