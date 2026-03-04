package team23.q_check.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.repository.ClubRepository;
import team23.q_check.club.service.ClubAuthorizationService;
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
import team23.q_check.event.repository.EventRepository;
import team23.q_check.event.repository.FormFieldRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final FormFieldRepository formFieldRepository;
    private final ClubRepository clubRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final ObjectMapper objectMapper;

    public EventService(
            EventRepository eventRepository,
            FormFieldRepository formFieldRepository,
            ClubRepository clubRepository,
            ClubAuthorizationService clubAuthorizationService,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.formFieldRepository = formFieldRepository;
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
        Event event = new Event(club, request.title().trim(), startTime, startTime, null, true);
        Event savedEvent = eventRepository.save(event);

        List<FormField> fields = createFormFields(savedEvent, request.formFields());
        return toDetailDto(savedEvent, fields);
    }

    @Transactional(readOnly = true)
    public EventPageResponseDto getEvents(int page, int size) {
        Page<Event> eventPage = eventRepository.findAll(PageRequest.of(page, size));
        List<EventListItemDto> items = eventPage.getContent().stream()
                .map(event -> new EventListItemDto(
                        event.getId(),
                        event.getTitle(),
                        event.getStartTime().toString(),
                        event.getLocation(),
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

    @Transactional
    public EventDetailResponseDto updateEvent(Long currentUserId, Long eventId, UpdateEventRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        LocalDateTime startTime = request.startTime() == null ? null : parseDateTime(request.startTime(), "startTime");
        event.update(request.title(), startTime, request.location(), request.isActive());

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
                event.getStartTime().toString(),
                event.getLocation(),
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
