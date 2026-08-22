package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record PassiveFact(String name, String kind, List<String> effects, List<String> tags) {
}
