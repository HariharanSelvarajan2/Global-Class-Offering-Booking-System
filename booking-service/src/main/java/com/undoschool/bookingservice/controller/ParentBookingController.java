package com.undoschool.bookingservice.controller;

import com.undoschool.bookingservice.dto.BookOfferingRequest;
import com.undoschool.bookingservice.dto.BookingResponse;
import com.undoschool.bookingservice.dto.OfferingResponse;
import com.undoschool.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parent")
public class ParentBookingController {

    private final BookingService bookingService;

    public ParentBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/offerings")
    public List<OfferingResponse> getAvailableOfferings(@RequestParam("timezone") String timezone) {
        return bookingService.getAvailableOfferings(timezone);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse bookOffering(@Valid @RequestBody BookOfferingRequest request) {
        return bookingService.bookOffering(request);
    }

    @GetMapping("/bookings")
    public List<BookingResponse> getBookings(
            @RequestParam("parentId") UUID parentId,
            @RequestParam("timezone") String timezone
    ) {
        return bookingService.getBookings(parentId, timezone);
    }
}
