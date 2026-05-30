package com.undoschool.bookingservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
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
    void getsAvailableOfferingsInParentTimezone() {
        OfferingResponse offering = offering(UUID.randomUUID(), "PUBLISHED", List.of(session(
                UUID.randomUUID(),
                "2026-06-13 05:30 PM",
                "2026-06-13 07:30 PM"
        )));
        when(courseServiceClient.getAvailableOfferings("Asia/Kolkata")).thenReturn(List.of(offering));

        List<OfferingResponse> responses = bookingService.getAvailableOfferings("Asia/Kolkata");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).sessions()).hasSize(1);
        verify(courseServiceClient).getAvailableOfferings("Asia/Kolkata");
    }

    @Test
    void rejectsAvailableOfferingsWithInvalidTimezone() {
        assertThatThrownBy(() -> bookingService.getAvailableOfferings("Invalid/Zone"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");

        verifyNoInteractions(courseServiceClient);
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
    void locksParentBeforeCheckingDuplicateBooking() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        OfferingResponse offering = offering(offeringId, "PUBLISHED", List.of(session(offeringId)));
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId)).thenReturn(List.of());
        when(courseServiceClient.getOffering(offeringId, "UTC")).thenReturn(offering);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        bookingService.bookOffering(new BookOfferingRequest(parentId, offeringId, "UTC"));

        InOrder inOrder = inOrder(lockRepository, bookingRepository);
        inOrder.verify(lockRepository).ensureExists(parentId);
        inOrder.verify(lockRepository).findLocked(parentId);
        inOrder.verify(bookingRepository).findByParentIdAndOfferingId(parentId, offeringId);
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
        verifyNoInteractions(courseServiceClient);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsWhenParentCannotBeLocked() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lock parent");

        verify(lockRepository).ensureExists(parentId);
        verifyNoInteractions(courseServiceClient);
        verify(bookingRepository, never()).save(any(Booking.class));
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
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsOfferingWithNoSessions() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(courseServiceClient.getOffering(offeringId, "UTC"))
                .thenReturn(offering(offeringId, "PUBLISHED", List.of()));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no sessions");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsOfferingWithNullSessions() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(courseServiceClient.getOffering(offeringId, "UTC"))
                .thenReturn(offering(offeringId, "PUBLISHED", null));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("no sessions");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsOfferingWithInvalidSessionTimeFormat() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(courseServiceClient.getOffering(offeringId, "UTC"))
                .thenReturn(offering(offeringId, "PUBLISHED", List.of(session(
                        offeringId,
                        "2026-06-06T12:30:00Z",
                        "2026-06-06 01:30 PM"
                ))));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("time format");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void rejectsOfferingWithInvalidSessionTimeRange() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, offeringId)).thenReturn(Optional.empty());
        when(courseServiceClient.getOffering(offeringId, "UTC"))
                .thenReturn(offering(offeringId, "PUBLISHED", List.of(session(
                        offeringId,
                        "2026-06-06 01:30 PM",
                        "2026-06-06 12:30 PM"
                ))));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                offeringId,
                "UTC"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("invalid session time range");

        verify(bookingRepository, never()).save(any(Booking.class));
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

    @Test
    void allowsAdjacentExistingBookingWhenTimesTouchButDoNotOverlap() {
        UUID parentId = UUID.randomUUID();
        UUID existingOfferingId = UUID.randomUUID();
        UUID requestedOfferingId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, requestedOfferingId)).thenReturn(Optional.empty());
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId))
                .thenReturn(List.of(new Booking(parentId, existingOfferingId, "Course", "Batch")));
        when(courseServiceClient.getOffering(requestedOfferingId, "UTC"))
                .thenReturn(offering(requestedOfferingId, "PUBLISHED", List.of(session(
                        requestedOfferingId,
                        "2026-06-06 01:30 PM",
                        "2026-06-06 02:30 PM"
                ))));
        when(courseServiceClient.getOffering(existingOfferingId, "UTC"))
                .thenReturn(offering(existingOfferingId, "PUBLISHED", List.of(session(
                        existingOfferingId,
                        "2026-06-06 12:30 PM",
                        "2026-06-06 01:30 PM"
                ))));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                requestedOfferingId,
                "UTC"
        ));

        assertThat(response.offeringId()).isEqualTo(requestedOfferingId);
        verify(bookingRepository).flush();
    }

    @Test
    void rejectsOverlapAgainstAnySessionInMultiSessionOffering() {
        UUID parentId = UUID.randomUUID();
        UUID existingOfferingId = UUID.randomUUID();
        UUID requestedOfferingId = UUID.randomUUID();
        when(lockRepository.findLocked(parentId)).thenReturn(Optional.of(anyLock(parentId)));
        when(bookingRepository.findByParentIdAndOfferingId(parentId, requestedOfferingId)).thenReturn(Optional.empty());
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId))
                .thenReturn(List.of(new Booking(parentId, existingOfferingId, "Course", "Batch")));
        when(courseServiceClient.getOffering(requestedOfferingId, "UTC"))
                .thenReturn(offering(requestedOfferingId, "PUBLISHED", List.of(
                        session(requestedOfferingId, "2026-06-07 11:00 AM", "2026-06-07 12:00 PM"),
                        session(requestedOfferingId, "2026-06-14 05:30 PM", "2026-06-14 06:30 PM")
                )));
        when(courseServiceClient.getOffering(existingOfferingId, "UTC"))
                .thenReturn(offering(existingOfferingId, "PUBLISHED", List.of(
                        session(existingOfferingId, "2026-06-07 09:00 AM", "2026-06-07 10:00 AM"),
                        session(existingOfferingId, "2026-06-14 05:00 PM", "2026-06-14 06:00 PM")
                )));

        assertThatThrownBy(() -> bookingService.bookOffering(new BookOfferingRequest(
                parentId,
                requestedOfferingId,
                "UTC"
        ))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlaps");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void getsBookingsWithOfferingDetailsInRequestedTimezone() {
        UUID parentId = UUID.randomUUID();
        UUID offeringId = UUID.randomUUID();
        Booking booking = new Booking(parentId, offeringId, "Minecraft Coding", "Saturday Batch");
        OfferingResponse offering = offering(offeringId, "PUBLISHED", List.of(session(offeringId)));
        when(bookingRepository.findByParentIdOrderByBookedAtDesc(parentId)).thenReturn(List.of(booking));
        when(courseServiceClient.getOffering(offeringId, "Asia/Kolkata")).thenReturn(offering);

        List<BookingResponse> responses = bookingService.getBookings(parentId, "Asia/Kolkata");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).parentId()).isEqualTo(parentId);
        assertThat(responses.get(0).offering()).isEqualTo(offering);
        verify(courseServiceClient).getOffering(offeringId, "Asia/Kolkata");
    }

    @Test
    void rejectsGetBookingsWithInvalidTimezone() {
        assertThatThrownBy(() -> bookingService.getBookings(UUID.randomUUID(), "Bad/Zone"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");

        verifyNoInteractions(bookingRepository, courseServiceClient);
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
        return session(offeringId, "2026-06-06 12:30 PM", "2026-06-06 01:30 PM");
    }

    private SessionResponse session(UUID offeringId, String startAtUtc, String endAtUtc) {
        return new SessionResponse(
                UUID.randomUUID(),
                offeringId,
                UUID.randomUUID(),
                startAtUtc,
                endAtUtc,
                startAtUtc,
                endAtUtc,
                "UTC"
        );
    }

    private ParentBookingLock anyLock(UUID parentId) {
        return new ParentBookingLock(parentId);
    }
}
