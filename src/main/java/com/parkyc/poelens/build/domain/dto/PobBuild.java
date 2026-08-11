package com.parkyc.poelens.build.domain.dto;

import java.util.List;

public record PobBuild(String gameVersion, int level, String characterClass, List<String> gems, List<String> items) {
}
