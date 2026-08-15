package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record BuffFact(String name, String kind, String appliesTo, List<String> tags) {
}
