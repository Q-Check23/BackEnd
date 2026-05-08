package team23.q_check.event.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.repository.ClubRepository;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.form.FieldType;
import team23.q_check.event.domain.model.form.FormField;
import team23.q_check.event.dto.CreateEventRequestDto;
import team23.q_check.event.dto.EventDetailResponseDto;
import team23.q_check.event.dto.EventListItemDto;
import team23.q_check.event.dto.EventPageResponseDto;
import team23.q_check.event.dto.FormFieldRequestDto;
import team23.q_check.event.dto.FormFieldResponseDto;
import team23.q_check.event.dto.UpdateEventRequestDto;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.domain.repository.AttendanceLogRepository;
import team23.q_check.event.domain.repository.EventPhotoRepository;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.FormAnswerRepository;
import team23.q_check.event.domain.repository.FormFieldRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final FormFieldRepository formFieldRepository;
    private final RegistrationRepository registrationRepository;
    private final FormAnswerRepository formAnswerRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final EventPhotoRepository eventPhotoRepository;
    private final ClubRepository clubRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final ObjectMapper objectMapper;

    public EventService(
            EventRepository eventRepository,
            FormFieldRepository formFieldRepository,
            RegistrationRepository registrationRepository,
            FormAnswerRepository formAnswerRepository,
            AttendanceLogRepository attendanceLogRepository,
            EventPhotoRepository eventPhotoRepository,
            ClubRepository clubRepository,
            ClubAuthorizationService clubAuthorizationService,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.formFieldRepository = formFieldRepository;
        this.registrationRepository = registrationRepository;
        this.formAnswerRepository = formAnswerRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.eventPhotoRepository = eventPhotoRepository;
        this.clubRepository = clubRepository;
        this.clubAuthorizationService = clubAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EventDetailResponseDto createEvent(Long currentUserId, CreateEventRequestDto request) {
        validateCreateRequest(request);
        ClubMember membership = clubAuthorizationService.requireAdminOrOwner(request.clubId(), currentUserId);
        Club club = membership.getClub();

        LocalDateTime startTime = parseDateTime(request.startTime(), "startTime");
        LocalDateTime endTime = request.endTime() == null
                ? startTime
                : parseDateTime(request.endTime(), "endTime");
        if (endTime.isBefore(startTime)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "endTime must be on or after startTime");
        }

        BigDecimal registerFee = request.registerFee() == null ? BigDecimal.ZERO : request.registerFee();
        if (registerFee.signum() < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "registerFee must be zero or positive");
        }

        Event event = new Event(
                club,
                request.title().trim(),
                request.description(),
                startTime,
                endTime,
                request.location(),
                request.discordChannelId(),
                true,
                registerFee
        );
        Event savedEvent = eventRepository.save(event);

        List<FormField> fields = createFormFields(savedEvent, request.formFields());
        return toDetailDto(savedEvent, fields);
    }

    @Transactional(readOnly = true)
    public EventPageResponseDto getEvents(int page, int size, Long clubId) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<Event> eventPage = (clubId == null)
                ? eventRepository.findAll(pageRequest)
                : eventRepository.findByClub_Id(clubId, pageRequest);
        List<EventListItemDto> items = eventPage.getContent().stream()
                .map(event -> new EventListItemDto(
                        event.getId(),
                        event.getTitle(),
                        event.getStartTime().toString(),
                        event.getEndTime() == null ? null : event.getEndTime().toString(),
                        event.getLocation(),
                        event.getRegisterFee(),
                        event.getIsActive()
                ))
                .toList();

        return new EventPageResponseDto(
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalPages(),
                eventPage.getTotalElements(),
                items
        );
    }

    @Transactional(readOnly = true)
    public EventDetailResponseDto getEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        List<FormField> fields = formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(eventId);
        return toDetailDto(event, fields);
    }

    /**
     * 행사를 hard delete 한다. 활성 등록자(REGISTERED 또는 CHECKED_IN)가 있으면 거부.
     * 비활성화로 충분한 경우엔 PUT /api/events/{id} 의 isActive=false 를 사용할 것.
     */
    @Transactional
    public void deleteEvent(Long currentUserId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        boolean hasActiveRegistrations = registrationRepository.existsByEvent_IdAndStatusIn(
                eventId,
                List.of(RegistrationStatus.REGISTERED, RegistrationStatus.CHECKED_IN)
        );
        if (hasActiveRegistrations) {
            throw new AppException(ErrorCode.CONFLICT,
                    "Cannot delete an event with active registrations. Set isActive=false instead.");
        }

        attendanceLogRepository.deleteAllByEvent_Id(eventId);
        formAnswerRepository.deleteAllByRegistration_Event_Id(eventId);
        registrationRepository.deleteAllByEvent_Id(eventId);
        formFieldRepository.deleteAllByEvent_Id(eventId);
        eventPhotoRepository.deleteAllByEvent_Id(eventId);
        eventRepository.delete(event);
    }

    @Transactional
    public EventDetailResponseDto updateEvent(Long currentUserId, Long eventId, UpdateEventRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        LocalDateTime startTime = request.startTime() == null ? null : parseDateTime(request.startTime(), "startTime");
        LocalDateTime endTime = request.endTime() == null ? null : parseDateTime(request.endTime(), "endTime");
        LocalDateTime effectiveStart = startTime != null ? startTime : event.getStartTime();
        LocalDateTime effectiveEnd = endTime != null ? endTime : event.getEndTime();
        if (effectiveEnd != null && effectiveStart != null && effectiveEnd.isBefore(effectiveStart)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "endTime must be on or after startTime");
        }
        if (request.registerFee() != null && request.registerFee().signum() < 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "registerFee must be zero or positive");
        }

        event.update(
                request.title(),
                request.description(),
                startTime,
                endTime,
                request.location(),
                request.discordChannelId(),
                request.registerFee(),
                request.isActive()
        );

        List<FormField> fields = formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(eventId);
        return toDetailDto(event, fields);
    }

    private List<FormField> createFormFields(Event event, List<FormFieldRequestDto> fieldRequests) {
        if (fieldRequests == null || fieldRequests.isEmpty()) {
            return Collections.emptyList();
        }

        for (int i = 0; i < fieldRequests.size(); i++) {
            FormFieldRequestDto field = fieldRequests.get(i);
            validateFormField(field);
            FieldType fieldType = parseFieldType(field.type());
            String optionsJson = toOptionsJson(fieldType, field.options());
            FormField formField = new FormField(
                    event,
                    (long) i,
                    field.label().trim(),
                    fieldType,
                    optionsJson,
                    Boolean.TRUE.equals(field.required())
            );
            formFieldRepository.save(formField);
        }
        return formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(event.getId());
    }

    private EventDetailResponseDto toDetailDto(Event event, List<FormField> fields) {
        List<FormFieldResponseDto> fieldDtos = fields.stream()
                .map(field -> new FormFieldResponseDto(
                        field.getId(),
                        field.getFieldType().name(),
                        field.getLabel(),
                        field.getIsRequired(),
                        parseOptions(field.getOptions())
                ))
                .toList();

        return new EventDetailResponseDto(
                event.getId(),
                event.getClub().getId(),
                event.getTitle(),
                event.getDescription(),
                event.getStartTime().toString(),
                event.getEndTime() == null ? null : event.getEndTime().toString(),
                event.getLocation(),
                event.getDiscordChannelId(),
                event.getRegisterFee(),
                event.getIsActive(),
                fieldDtos
        );
    }

    private void validateCreateRequest(CreateEventRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        if (request.clubId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "clubId is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "title is required");
        }
        if (request.startTime() == null || request.startTime().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "startTime is required");
        }
        if (!clubRepository.existsById(request.clubId())) {
            throw new AppException(ErrorCode.NOT_FOUND, "Club not found: " + request.clubId());
        }
    }

    private void validateFormField(FormFieldRequestDto field) {
        if (field == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "formField item must not be null");
        }
        if (field.type() == null || field.type().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "formField.type is required");
        }
        if (field.label() == null || field.label().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "formField.label is required");
        }
    }

    private FieldType parseFieldType(String rawType) {
        try {
            return FieldType.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid field type: " + rawType);
        }
    }

    private LocalDateTime parseDateTime(String rawDateTime, String fieldName) {
        try {
            return LocalDateTime.parse(rawDateTime);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REQUEST, fieldName + " must be ISO-8601 datetime");
        }
    }

    private String toOptionsJson(FieldType fieldType, List<String> options) {
        if (fieldType != FieldType.SELECT) {
            return null;
        }
        if (options == null || options.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "SELECT field requires options");
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to serialize field options");
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to parse field options");
        }
    }
}
