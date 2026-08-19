package com.parkyc.poelens.build.application;

import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;

import java.util.List;

public interface NarrativeRefiner {
    List<List<Mechanic>> refine(BuildFacts facts, List<Mechanic> offence, List<Mechanic> defence);
}
