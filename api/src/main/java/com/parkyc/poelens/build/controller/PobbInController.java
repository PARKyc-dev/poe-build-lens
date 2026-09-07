package com.parkyc.poelens.build.controller;

import com.parkyc.poelens.build.service.PobbInClient;
import com.parkyc.poelens.common.code.ResponseCode;
import com.parkyc.poelens.common.dto.CommonDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pobb-in")
public class PobbInController {
    private final PobbInClient pobbInClient;

    public PobbInController(PobbInClient pobbInClient) {
        this.pobbInClient = pobbInClient;
    }

    @GetMapping("/{id}")
    public CommonDTO.Response<String> rawBuildCode(@PathVariable String id) {
        return new CommonDTO.Response<>(ResponseCode.OK.name(), ResponseCode.OK.message(), pobbInClient.fetch(id));
    }
}
