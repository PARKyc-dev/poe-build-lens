package com.parkyc.poelens.analysis.model;

import java.util.List;

public record PobBuild(String gameVersion, int level, String characterClass, List<String> gems, List<String> items) {
}
