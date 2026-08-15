package com.parkyc.poelens.mechanics.service;

import com.parkyc.poelens.mechanics.domain.entity.MechanicEntity;

import java.util.Optional;

public interface MechanicService {

    Optional<MechanicEntity> find(String gameVersion, String name);
}
