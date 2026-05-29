package com.undoschool.courseservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        assertThat(response.startAtUtc().toString()).isEqualTo("2026-06-06T12:30:00Z");
        assertThat(response.endAtUtc().toString()).isEqualTo("2026-06-06T13:30:00Z");
        assertThat(response.displayTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(offering.getStatus().name()).isEqualTo("PUBLISHED");
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
}
