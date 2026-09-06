package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record Mechanic(String title, String explanation, List<MechanicDetail> details) {
    public Mechanic(String title, String explanation) {
        this(title, explanation, List.of());
    }
}
