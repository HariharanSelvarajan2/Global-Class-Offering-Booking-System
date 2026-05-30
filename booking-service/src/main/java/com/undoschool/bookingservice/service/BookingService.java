package com.undoschool.bookingservice.service;

import com.undoschool.bookingservice.client.CourseServiceClient;
import com.undoschool.bookingservice.dto.BookOfferingRequest;
import com.undoschool.bookingservice.dto.BookingResponse;
import com.undoschool.bookingservice.dto.OfferingResponse;
import com.undoschool.bookingservice.dto.SessionResponse;
import com.undoschool.bookingservice.entity.Booking;
import com.undoschool.bookingservice.exception.BadRequestException;
import com.undoschool.bookingservice.exception.ConflictException;
import com.undoschool.bookingservice.repository.BookingRepository;
import com.undoschool.bookingservice.repository.ParentBookingLockRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final CourseServiceClient courseServiceClient;
    private final BookingRepository bookingRepository;
    private final ParentBookingLockRepository lockRepository;
    private final TimeZoneValidator timeZoneValidator;

    @Autowired
    public BookingService(
            CourseServiceClient courseServiceClient,
            BookingRepository bookingRepository,
            ParentBookingLockRepository lockRepository,
            TimeZoneValidator timeZoneValidator
    ) {
        this.courseServiceClient = courseServiceClient;
        this.bookingRepository = bookingRepository;
        this.lockRepository = lockRepository;
        this.timeZoneValidator = timeZoneValidator;
    }

    public List<OfferingResponse> getAvailableOfferings(String timezone) {
        timeZoneValidator.validate(timezone);
        return courseServiceClient.getAvailableOfferings(timezone);
    }

    @Transactional
    public BookingResponse bookOffering(BookOfferingRequest request) {
        timeZoneValidator.validate(request.timezone());
        lockParent(request.parentId());

        rejectDuplicateBooking(request);
        OfferingResponse offering = courseServiceClient.getOffering(request.offeringId(), request.timezone());
        validateBookable(offering);
        rejectOverlappingBooking(request.parentId(), offering.sessions(), request.timezone());

        Booking booking = saveBooking(request.parentId(), offering);
        return new BookingResponse(
                booking.getId(),
                booking.getParentId(),
                booking.getOfferingId(),
                booking.getCourseName(),
                booking.getOfferingName(),
                booking.getStatus().name(),
                booking.getBookedAt(),
                offering
        );
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getBookings(UUID parentId, String timezone) {
        timeZoneValidator.validate(timezone);
        return bookingRepository.findByParentIdOrderByBookedAtDesc(parentId).stream()
                .map(booking -> new BookingResponse(
                        booking.getId(),
                        booking.getParentId(),
                        booking.getOfferingId(),
                        booking.getCourseName(),
                        booking.getOfferingName(),
                        booking.getStatus().name(),
                        booking.getBookedAt(),
                        courseServiceClient.getOffering(booking.getOfferingId(), timezone)
                ))
                .toList();
    }

    private void rejectDuplicateBooking(BookOfferingRequest request) {
        bookingRepository.findByParentIdAndOfferingId(request.parentId(), request.offeringId())
                .ifPresent(existing -> {
                    throw new ConflictException("Parent has already booked this offering.");
                });
    }

    private void lockParent(UUID parentId) {
        lockRepository.ensureExists(parentId);
        lockRepository.findLocked(parentId)
                .orElseThrow(() -> new BadRequestException("Could not lock parent booking stream."));
    }

    private void rejectOverlappingBooking(UUID parentId, List<SessionResponse> requestedSessions, String timezone) {
        List<SessionResponse> requested = sessionsOrEmpty(requestedSessions).stream().map(this::validated).toList();

        boolean overlaps = bookingRepository.findByParentIdOrderByBookedAtDesc(parentId).stream()
                .map(booking -> courseServiceClient.getOffering(booking.getOfferingId(), timezone))
                .flatMap(offering -> sessionsOrEmpty(offering.sessions()).stream())
                .map(this::validated)
                .anyMatch(existing -> requested.stream().anyMatch(session -> overlaps(session, existing)));

        if (overlaps) {
            throw new ConflictException("Booking overlaps with an existing booked session.");
        }
    }

    private void validateBookable(OfferingResponse offering) {
        if (!"PUBLISHED".equals(offering.status())) {
            throw new BadRequestException("Only published offerings can be booked.");
        }
        if (offering.sessions() == null || offering.sessions().isEmpty()) {
            throw new BadRequestException("Offering has no sessions to book.");
        }
    }

    private List<SessionResponse> sessionsOrEmpty(List<SessionResponse> sessions) {
        return sessions == null ? List.of() : sessions;
    }

    private Booking saveBooking(UUID parentId, OfferingResponse offering) {
        Booking booking = bookingRepository.save(new Booking(
                parentId,
                offering.id(),
                offering.courseName(),
                offering.offeringName()
        ));
        bookingRepository.flush();
        return booking;
    }

    private SessionResponse validated(SessionResponse session) {
        validateSession(session);
        return session;
    }

    private void validateSession(SessionResponse session) {
        Instant start = session.startAtUtc();
        Instant end = session.endAtUtc();
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BadRequestException("Offering contains an invalid session time range.");
        }
    }

    private boolean overlaps(SessionResponse first, SessionResponse second) {
        Instant firstStart = first.startAtUtc();
        Instant firstEnd = first.endAtUtc();
        Instant secondStart = second.startAtUtc();
        Instant secondEnd = second.endAtUtc();
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }
}
