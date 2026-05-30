package com.undoschool.courseservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.undoschool.courseservice.dto.AddSessionRequest;
import com.undoschool.courseservice.dto.CreateOfferingRequest;
import com.undoschool.courseservice.dto.OfferingResponse;
import com.undoschool.courseservice.dto.SessionResponse;
import com.undoschool.courseservice.entity.Course;
import com.undoschool.courseservice.entity.CourseSession;
import com.undoschool.courseservice.entity.Offering;
import com.undoschool.courseservice.entity.OfferingStatus;
import com.undoschool.courseservice.exception.BadRequestException;
import com.undoschool.courseservice.repository.CourseRepository;
import com.undoschool.courseservice.repository.CourseSessionRepository;
import com.undoschool.courseservice.repository.OfferingRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OfferingServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private OfferingRepository offeringRepository;

    @Mock
    private CourseSessionRepository sessionRepository;

    private OfferingService offeringService;

    @BeforeEach
    void setUp() {
        TimeZoneService timeZoneService = new TimeZoneService();
        offeringService = new OfferingService(
                courseRepository,
                offeringRepository,
                sessionRepository,
                timeZoneService
        );
    }

    @Test
    void createsOfferingAndReusesExistingCourse() {
        UUID teacherId = UUID.randomUUID();
        Course course = new Course("Python Coding");
        when(courseRepository.findByNameIgnoreCase("Python Coding")).thenReturn(Optional.of(course));
        when(offeringRepository.save(any(Offering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfferingResponse response = offeringService.createOffering(new CreateOfferingRequest(
                teacherId,
                "Python Coding",
                "Saturday Batch",
                "Asia/Kolkata"
        ));

        assertThat(response.teacherId()).isEqualTo(teacherId);
        assertThat(response.courseName()).isEqualTo("Python Coding");
        assertThat(response.offeringName()).isEqualTo("Saturday Batch");
        assertThat(response.teacherTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void createsOfferingAndCreatesCourseWhenCourseDoesNotExist() {
        UUID teacherId = UUID.randomUUID();
        when(courseRepository.findByNameIgnoreCase("Python Coding")).thenReturn(Optional.empty());
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(offeringRepository.save(any(Offering.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OfferingResponse response = offeringService.createOffering(new CreateOfferingRequest(
                teacherId,
                "  Python Coding  ",
                "  Weekday Batch  ",
                "Asia/Kolkata"
        ));

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        ArgumentCaptor<Offering> offeringCaptor = ArgumentCaptor.forClass(Offering.class);
        verify(courseRepository).save(courseCaptor.capture());
        verify(offeringRepository).save(offeringCaptor.capture());
        assertThat(courseCaptor.getValue().getName()).isEqualTo("Python Coding");
        assertThat(offeringCaptor.getValue().getName()).isEqualTo("Weekday Batch");
        assertThat(response.courseName()).isEqualTo("Python Coding");
        assertThat(response.offeringName()).isEqualTo("Weekday Batch");
    }

    @Test
    void rejectsCreateOfferingWithInvalidTimezone() {
        assertThatThrownBy(() -> offeringService.createOffering(new CreateOfferingRequest(
                UUID.randomUUID(),
                "Python Coding",
                "Saturday Batch",
                "Not/A_Timezone"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");

        verifyNoInteractions(courseRepository, offeringRepository, sessionRepository);
    }

    @Test
    void addsSessionUsingOfferingTimezone() {
        UUID offeringId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Minecraft Coding"), teacherId, "Saturday Batch", "Asia/Kolkata");
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(sessionRepository.save(any(CourseSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = offeringService.addSession(offeringId, new AddSessionRequest(
                LocalDateTime.of(2026, 6, 6, 18, 0),
                LocalDateTime.of(2026, 6, 6, 19, 0),
                null
        ));

        ArgumentCaptor<CourseSession> captor = ArgumentCaptor.forClass(CourseSession.class);
        verify(sessionRepository).save(captor.capture());
        assertThat(captor.getValue().getTeacherId()).isEqualTo(teacherId);
        assertThat(response.startAtUtc()).isEqualTo(Instant.parse("2026-06-06T12:30:00Z"));
        assertThat(response.endAtUtc()).isEqualTo(Instant.parse("2026-06-06T13:30:00Z"));
        assertThat(response.localStart().toString()).isEqualTo("2026-06-06T18:00+05:30");
        assertThat(response.localEnd().toString()).isEqualTo("2026-06-06T19:00+05:30");
        assertThat(response.displayTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(offering.getStatus().name()).isEqualTo("PUBLISHED");
        verify(offeringRepository).save(offering);
    }

    @Test
    void addsSessionUsingRequestTimezoneOverride() {
        UUID offeringId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Public Speaking"), teacherId, "Summer Camp", "America/New_York");
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(sessionRepository.save(any(CourseSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionResponse response = offeringService.addSession(offeringId, new AddSessionRequest(
                LocalDateTime.of(2026, 6, 13, 23, 0),
                LocalDateTime.of(2026, 6, 14, 1, 0),
                "Asia/Kolkata"
        ));

        assertThat(response.startAtUtc()).isEqualTo(Instant.parse("2026-06-13T17:30:00Z"));
        assertThat(response.endAtUtc()).isEqualTo(Instant.parse("2026-06-13T19:30:00Z"));
        assertThat(response.localStart().toString()).isEqualTo("2026-06-13T23:00+05:30");
        assertThat(response.localEnd().toString()).isEqualTo("2026-06-14T01:00+05:30");
        assertThat(response.displayTimezone()).isEqualTo("Asia/Kolkata");
    }

    @Test
    void rejectsSessionWhenEndIsNotAfterStart() {
        UUID offeringId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Art"), UUID.randomUUID(), "Evening Batch", "UTC");
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> offeringService.addSession(offeringId, new AddSessionRequest(
                LocalDateTime.of(2026, 6, 6, 18, 0),
                LocalDateTime.of(2026, 6, 6, 18, 0),
                null
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("end time");
        verify(sessionRepository, never()).save(any(CourseSession.class));
    }

    @Test
    void rejectsSessionWhenTimezoneOverrideIsInvalid() {
        UUID offeringId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Art"), UUID.randomUUID(), "Evening Batch", "UTC");
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> offeringService.addSession(offeringId, new AddSessionRequest(
                LocalDateTime.of(2026, 6, 6, 18, 0),
                LocalDateTime.of(2026, 6, 6, 19, 0),
                "Bad/Zone"
        ))).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");

        verify(sessionRepository, never()).save(any(CourseSession.class));
    }

    @Test
    void rejectsSessionForMissingOffering() {
        UUID offeringId = UUID.randomUUID();
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.addSession(offeringId, new AddSessionRequest(
                LocalDateTime.of(2026, 6, 6, 18, 0),
                LocalDateTime.of(2026, 6, 6, 19, 0),
                null
        ))).hasMessageContaining("Offering not found");

        verifyNoInteractions(sessionRepository);
    }

    @Test
    void getsTeacherOfferingsWithAllSessionsEvenWhenSessionsAreNotUpcoming() {
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Python Coding"), teacherId, "Draft Batch", "Asia/Kolkata");
        CourseSession pastSession = new CourseSession(
                offering,
                teacherId,
                Instant.parse("2026-05-20T13:30:00Z"),
                Instant.parse("2026-05-20T14:30:00Z"),
                "Asia/Kolkata"
        );
        when(offeringRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId)).thenReturn(List.of(offering));
        when(sessionRepository.findByOfferingIdOrderByStartAt(offering.getId())).thenReturn(List.of(pastSession));

        List<OfferingResponse> responses = offeringService.getTeacherOfferings(teacherId, "Asia/Kolkata");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).offeringName()).isEqualTo("Draft Batch");
        assertThat(responses.get(0).sessions()).hasSize(1);
        assertThat(responses.get(0).sessions().get(0).localStart().toString()).isEqualTo("2026-05-20T19:00+05:30");
    }

    @Test
    void getsAvailableOfferingsWithAllSessionsEvenWhenSessionsAreNotUpcoming() {
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Python Coding"), teacherId, "Published Batch", "Asia/Kolkata");
        offering.publish();
        CourseSession pastSession = new CourseSession(
                offering,
                teacherId,
                Instant.parse("2026-05-20T13:30:00Z"),
                Instant.parse("2026-05-20T14:30:00Z"),
                "Asia/Kolkata"
        );
        when(offeringRepository.findAvailable(OfferingStatus.PUBLISHED)).thenReturn(List.of(offering));
        when(sessionRepository.findByOfferingIdOrderByStartAt(offering.getId())).thenReturn(List.of(pastSession));

        List<OfferingResponse> responses = offeringService.getAvailableOfferings("Asia/Kolkata");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).offeringName()).isEqualTo("Published Batch");
        assertThat(responses.get(0).sessions()).hasSize(1);
    }

    @Test
    void getAvailableOfferingsUsesOfferingTimezoneWhenNoDisplayTimezoneIsProvided() {
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Public Speaking"), teacherId, "Summer Camp", "America/New_York");
        offering.publish();
        CourseSession session = new CourseSession(
                offering,
                teacherId,
                Instant.parse("2026-06-13T17:30:00Z"),
                Instant.parse("2026-06-13T19:30:00Z"),
                "America/New_York"
        );
        when(offeringRepository.findAvailable(OfferingStatus.PUBLISHED)).thenReturn(List.of(offering));
        when(sessionRepository.findByOfferingIdOrderByStartAt(offering.getId())).thenReturn(List.of(session));

        OfferingResponse response = offeringService.getAvailableOfferings(null).get(0);

        assertThat(response.sessions().get(0).localStart().toString()).isEqualTo("2026-06-13T13:30-04:00");
        assertThat(response.sessions().get(0).displayTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void rejectsAvailableOfferingsWithInvalidDisplayTimezone() {
        assertThatThrownBy(() -> offeringService.getAvailableOfferings("Invalid/Zone"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid timezone");

        verify(offeringRepository, never()).findAvailable(any());
    }

    @Test
    void getsOfferingByIdWithAllSessionsInRequestedTimezone() {
        UUID offeringId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Offering offering = new Offering(new Course("Minecraft Coding"), teacherId, "Saturday Batch", "Asia/Kolkata");
        CourseSession first = new CourseSession(
                offering,
                teacherId,
                Instant.parse("2026-06-06T12:30:00Z"),
                Instant.parse("2026-06-06T13:30:00Z"),
                "Asia/Kolkata"
        );
        CourseSession second = new CourseSession(
                offering,
                teacherId,
                Instant.parse("2026-06-13T12:30:00Z"),
                Instant.parse("2026-06-13T13:30:00Z"),
                "Asia/Kolkata"
        );
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(sessionRepository.findByOfferingIdOrderByStartAt(offeringId)).thenReturn(List.of(first, second));

        OfferingResponse response = offeringService.getOffering(offeringId, "America/New_York");

        assertThat(response.sessions()).hasSize(2);
        assertThat(response.sessions().get(0).startAtUtc()).isEqualTo(Instant.parse("2026-06-06T12:30:00Z"));
        assertThat(response.sessions().get(0).localStart().toString()).isEqualTo("2026-06-06T08:30-04:00");
        assertThat(response.sessions().get(0).displayTimezone()).isEqualTo("America/New_York");
    }

    @Test
    void rejectsGetOfferingWhenOfferingDoesNotExist() {
        UUID offeringId = UUID.randomUUID();
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> offeringService.getOffering(offeringId, "UTC"))
                .hasMessageContaining("Offering not found");

        verifyNoInteractions(sessionRepository);
    }
}

