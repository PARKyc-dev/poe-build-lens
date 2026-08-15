package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record PassiveFact(String name, List<String> effects, List<String> tags) {
}
