package com.undoschool.courseservice.controller;

import com.undoschool.courseservice.dto.AddSessionRequest;
import com.undoschool.courseservice.dto.CreateOfferingRequest;
import com.undoschool.courseservice.dto.OfferingResponse;
import com.undoschool.courseservice.dto.SessionResponse;
import com.undoschool.courseservice.service.OfferingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teacher")
public class TeacherOfferingController {

    private final OfferingService offeringService;

    public TeacherOfferingController(OfferingService offeringService) {
        this.offeringService = offeringService;
    }

    @PostMapping("/offerings")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferingResponse createOffering(@Valid @RequestBody CreateOfferingRequest request) {
        return offeringService.createOffering(request);
    }

    @PostMapping("/offerings/{offeringId}/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse addSession(
            @PathVariable("offeringId") UUID offeringId,
            @Valid @RequestBody AddSessionRequest request
    ) {
        return offeringService.addSession(offeringId, request);
    }

    @GetMapping("/{teacherId}/offerings")
    public List<OfferingResponse> getTeacherOfferings(
            @PathVariable("teacherId") UUID teacherId,
            @RequestParam(value = "timezone", required = false) String timezone
    ) {
        return offeringService.getTeacherOfferings(teacherId, timezone);
    }
}
