package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record OffenceFact(String name, String role, String delivery, List<String> tags) {
}
