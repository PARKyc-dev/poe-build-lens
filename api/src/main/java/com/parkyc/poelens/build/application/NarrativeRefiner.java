package com.parkyc.poelens.build.application;

import com.parkyc.poelens.build.domain.dto.BuildFacts;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import com.parkyc.poelens.build.domain.dto.NarrativeResult;
import com.parkyc.poelens.build.domain.dto.OperationFlow;

import java.util.List;

public interface NarrativeRefiner {
    NarrativeResult refine(BuildFacts facts, String summary, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs);

    default NarrativeResult refine(BuildFacts facts, List<OperationFlow> operationFlows, String summary, List<Mechanic> offence, List<Mechanic> defence, List<Mechanic> buffs) {
        return refine(facts, summary, offence, defence, buffs);
    }
}
