package com.parkyc.poelens.mechanics.service;

import com.parkyc.poelens.mechanics.domain.entity.MechanicEntity;

import java.util.Optional;

public interface MechanicService {

    String gameVersion();

    Optional<MechanicEntity> find(String name);
}
