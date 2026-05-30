package com.undoschool.courseservice.service;

import com.undoschool.courseservice.dto.AddSessionRequest;
import com.undoschool.courseservice.dto.CreateOfferingRequest;
import com.undoschool.courseservice.dto.OfferingResponse;
import com.undoschool.courseservice.dto.SessionResponse;
import com.undoschool.courseservice.exception.BadRequestException;
import com.undoschool.courseservice.exception.NotFoundException;
import com.undoschool.courseservice.entity.Course;
import com.undoschool.courseservice.entity.CourseSession;
import com.undoschool.courseservice.entity.Offering;
import com.undoschool.courseservice.entity.OfferingStatus;
import com.undoschool.courseservice.repository.CourseRepository;
import com.undoschool.courseservice.repository.CourseSessionRepository;
import com.undoschool.courseservice.repository.OfferingRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferingService {

    private final CourseRepository courseRepository;
    private final OfferingRepository offeringRepository;
    private final CourseSessionRepository sessionRepository;
    private final TimeZoneService timeZoneService;

    public OfferingService(
            CourseRepository courseRepository,
            OfferingRepository offeringRepository,
            CourseSessionRepository sessionRepository,
            TimeZoneService timeZoneService
    ) {
        this.courseRepository = courseRepository;
        this.offeringRepository = offeringRepository;
        this.sessionRepository = sessionRepository;
        this.timeZoneService = timeZoneService;
    }

    @Transactional
    public OfferingResponse createOffering(CreateOfferingRequest request) {
        timeZoneService.parse(request.teacherTimezone());
        Course course = courseRepository.findByNameIgnoreCase(request.courseName().trim())
                .orElseGet(() -> courseRepository.save(new Course(request.courseName().trim())));
        Offering offering = offeringRepository.save(new Offering(
                course,
                request.teacherId(),
                request.offeringName().trim(),
                request.teacherTimezone()
        ));
        return toResponse(offering, List.of(), request.teacherTimezone());
    }

    @Transactional
    public SessionResponse addSession(UUID offeringId, AddSessionRequest request) {
        Offering offering = findOffering(offeringId);
        String timezone = resolveTimezone(request.timezone(), offering);
        Instant start = timeZoneService.toInstant(request.localStart(), timezone);
        Instant end = timeZoneService.toInstant(request.localEnd(), timezone);

        if (!end.isAfter(start)) {
            throw new BadRequestException("Session end time must be after start time.");
        }

        CourseSession session = sessionRepository.save(
                new CourseSession(offering, offering.getTeacherId(), start, end, timezone)
        );
        offering.publish();
        offeringRepository.save(offering);
        return toResponse(session, timezone);
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getTeacherOfferings(UUID teacherId, String timezone) {
        return offeringRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId).stream()
                .map(offering -> toResponse(
                        offering,
                        sessionRepository.findByOfferingIdOrderByStartAt(offering.getId()),
                        resolveTimezone(timezone, offering)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String timezone) {
        if (timezone != null && !timezone.isBlank()) {
            timeZoneService.parse(timezone);
        }
        return toResponses(offeringRepository.findAvailable(OfferingStatus.PUBLISHED), timezone);
    }

    @Transactional(readOnly = true)
    public OfferingResponse getOffering(UUID offeringId, String timezone) {
        Offering offering = findOffering(offeringId);
        List<CourseSession> sessions = sessionRepository.findByOfferingIdOrderByStartAt(offeringId);
        return toResponse(offering, sessions, resolveTimezone(timezone, offering));
    }

    private List<OfferingResponse> toResponses(List<Offering> offerings, String timezone) {
        return offerings.stream()
                .map(offering -> toResponse(
                        offering,
                        sessionRepository.findByOfferingIdOrderByStartAt(offering.getId()),
                        resolveTimezone(timezone, offering)
                ))
                .toList();
    }

    private String resolveTimezone(String timezone, Offering offering) {
        if (timezone == null || timezone.isBlank()) {
            return offering.getTeacherTimezone();
        }
        timeZoneService.parse(timezone);
        return timezone;
    }

    private OfferingResponse toResponse(Offering offering, List<CourseSession> sessions, String timezone) {
        return new OfferingResponse(
                offering.getId(),
                offering.getTeacherId(),
                offering.getCourse().getName(),
                offering.getName(),
                offering.getTeacherTimezone(),
                offering.getStatus().name(),
                sessions.stream().map(session -> toResponse(session, timezone)).toList()
        );
    }

    private SessionResponse toResponse(CourseSession session, String timezone) {
        return new SessionResponse(
                session.getId(),
                session.getOffering().getId(),
                session.getTeacherId(),
                timeZoneService.formatUtc(session.getStartAt()),
                timeZoneService.formatUtc(session.getEndAt()),
                timeZoneService.formatLocal(session.getStartAt(), timezone),
                timeZoneService.formatLocal(session.getEndAt(), timezone),
                timezone
        );
    }

    private Offering findOffering(UUID offeringId) {
        return offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering not found: " + offeringId));
    }
}
