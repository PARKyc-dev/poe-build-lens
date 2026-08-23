package com.parkyc.poelens.build.domain.analysis;

import com.parkyc.poelens.build.domain.dto.ItemFact;
import com.parkyc.poelens.build.domain.dto.JewelFact;
import com.parkyc.poelens.build.domain.dto.Mechanic;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EquipmentMechanicAnalyzer {

    public List<Mechanic> analyse(List<ItemFact> items, List<JewelFact> jewels) {
        List<Mechanic> analysis = new ArrayList<>();
        for (ItemFact item : safe(items)) {
            if (item.name() != null && !item.name().isBlank()) {
                analysis.add(new Mechanic("장비: " + item.slot() + " · " + item.name(), "옵션: " + String.join(" · ", safe(item.modifiers()))));
            }
        }
        for (JewelFact jewel : safe(jewels)) {
            if (jewel.name() != null && !jewel.name().isBlank()) {
                String kind = "cluster".equals(jewel.kind()) ? "군 주얼" : "주얼";
                analysis.add(new Mechanic(kind + ": " + jewel.name(), "옵션: " + String.join(" · ", safe(jewel.modifiers()))));
            }
        }
        return analysis;
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
