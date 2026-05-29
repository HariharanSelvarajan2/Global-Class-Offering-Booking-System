package com.undoschool.courseservice.controller;

import com.undoschool.courseservice.dto.OfferingResponse;
import com.undoschool.courseservice.service.OfferingService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/offerings")
public class InternalOfferingController {

    private final OfferingService offeringService;

    public InternalOfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @GetMapping
    public List<OfferingResponse> getAvailableOfferings(
            @RequestParam(value = "timezone", required = false) String timezone
    ) {
        return offeringService.getAvailableOfferings(timezone);
    }

    @GetMapping("/{offeringId}")
    public OfferingResponse getOffering(
            @PathVariable("offeringId") UUID offeringId,
            @RequestParam(value = "timezone", required = false) String timezone
    ) {
        return offeringService.getOffering(offeringId, timezone);
    }
}
