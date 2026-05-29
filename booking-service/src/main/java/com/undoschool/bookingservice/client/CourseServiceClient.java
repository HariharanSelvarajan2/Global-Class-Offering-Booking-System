package com.undoschool.bookingservice.client;

import com.undoschool.bookingservice.dto.OfferingResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "course-service", url = "${services.course-service.url}")
public interface CourseServiceClient {

    @GetMapping("/internal/v1/offerings")
    List<OfferingResponse> getAvailableOfferings(@RequestParam(value = "timezone", required = false) String timezone);

    @GetMapping("/internal/v1/offerings/{offeringId}")
    OfferingResponse getOffering(
            @PathVariable("offeringId") UUID offeringId,
            @RequestParam(value = "timezone", required = false) String timezone
    );
}
