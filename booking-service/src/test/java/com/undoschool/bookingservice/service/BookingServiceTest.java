package com.undoschool.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.undoschool.bookingservice.client.CourseServiceClient;
import com.undoschool.bookingservice.dto.BookOfferingRequest;
import com.undoschool.bookingservice.dto.BookingResponse;
import com.undoschool.bookingservice.dto.OfferingResponse;
import com.undoschool.bookingservice.dto.SessionResponse;
import com.undoschool.bookingservice.entity.Booking;
import com.undoschool.bookingservice.entity.ParentBookingLock;
import com.undoschool.bookingservice.exception.BadRequestException;
import com.undoschool.bookingservice.exception.ConflictException;
import com.undoschool.bookingservice.repository.BookingRepository;
import com.undoschool.bookingservice.repository.ParentBookingLockRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private CourseServiceClient courseServiceClient;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ParentBookingLockRepository lockRepository;

    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        bookingService = new BookingService(
                courseServiceClient,
                bookingRepository,
                lockRepository,
                new TimeZoneValidator()
        );
    }

    @Test
    void booksCompleteOfferingWhenThereIsNoOverlap() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        OfferingResponse offering = offering(offeringId, "PUBLISHED", List.of(session(offeringId)));
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId)).thenReturn(List.of());
        when(courseServiceClient.getOffering(offeringId, "America/New_York")).thenReturn(offering);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "America/New_York"
        ));

        assertThat(response.parentId()).isEqualTo(parentId);
        assertThat(response.offeringId()).isEqualTo(offeringId);
        assertThat(response.offering().sessions()).hasSize(1);
        verify(lockRepository).ensureExists(parentId);
        verify(lockRepository).findLocked(parentId);
        verify(bookingRepository).flush();
    }

    @Test
    void rejectsDuplicateBookingForSameParentAndOffering() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId))
                .thenReturn(Optional.of(new Booking(parentId, offeringId, "Course", "Batch")));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");
    }

    @Test
    void rejectsUnpublishedOffering() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(courseServiceClient.getOffering(offeringId, "UTC")).thenReturn(offering(offeringId, "DRAFT", List.of(session(offeringId))));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("published");
    }

    @Test
    void rejectsOverlappingExistingBooking() {
        UUID parentId = UUID.randomUUID();
        UUID existingOfferingId = UUID.randomUUID();
        UUID requestedOfferingId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, requestedOfferingId)).thenReturn(Optional.empty());
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId))
                .thenReturn(List.of(new Booking(parentId, existingOfferingId, "Course", "Batch")));
        when(courseServiceClient.getOffering(requestedOfferingId, "UTC"))
                .thenReturn(offering(requestedOfferingId, "PUBLISHED", List.of(session(requestedOfferingId))));
        when(courseServiceClient.getOffering(existingOfferingId, "UTC"))
                .thenReturn(offering(existingOfferingId, "PUBLISHED", List.of(session(existingOfferingId))));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                requestedOfferingId,
                "UTC"
        ))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");
    }

    private OfferingResponse offering(UUID offeringId, String status, List<SessionResponse> sessions) {
        return new OfferingResponse(
                offeringId,
                UUID.randomUUID(),
                "Minecraft Coding",
                "Saturday Batch",
                "Asia/Kolkata",
                status,
                sessions
        );
    }

    private SessionResponse session(UUID offeringId) {
        return new SessionResponse(
                UUID.randomUUID(),
                offeringId,
                UUID.randomUUID(),
                Instant.parse("2026-06-06T12:30:00Z"),
                Instant.parse("2026-06-06T13:30:00Z"),
                null,
                null,
                "UTC"
        );
    }

    private ParentBookingLock anyLock(UUID parentId) {
        return new ParentBookingLock(parentId);
    }
}
