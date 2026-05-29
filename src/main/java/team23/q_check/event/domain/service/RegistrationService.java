package team23.q_check.event.domain.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.domain.model.form.FieldType;
import team23.q_check.event.domain.model.form.FormAnswer;
import team23.q_check.event.domain.model.form.FormField;
import team23.q_check.event.dto.AdminRegistrationItemDto;
import team23.q_check.event.dto.CreateRegistrationRequestDto;
import team23.q_check.event.dto.CreateRegistrationResponseDto;
import team23.q_check.event.dto.MyRegistrationResponseDto;
import team23.q_check.event.dto.RegistrationAnswerRequestDto;
import team23.q_check.event.dto.RegistrationAnswerResponseDto;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.event.domain.model.AttendanceLog;
import team23.q_check.event.domain.repository.AttendanceLogRepository;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.FormAnswerRepository;
import team23.q_check.event.domain.repository.FormFieldRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class RegistrationService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final FormFieldRepository formFieldRepository;
    private final FormAnswerRepository formAnswerRepository;
    private final UserRepository userRepository;
    private final AttendanceLogRepository attendanceLogRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final ObjectMapper objectMapper;

    public RegistrationService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            FormFieldRepository formFieldRepository,
            FormAnswerRepository formAnswerRepository,
            UserRepository userRepository,
            AttendanceLogRepository attendanceLogRepository,
            ClubAuthorizationService clubAuthorizationService,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.formFieldRepository = formFieldRepository;
        this.formAnswerRepository = formAnswerRepository;
        this.userRepository = userRepository;
        this.attendanceLogRepository = attendanceLogRepository;
        this.clubAuthorizationService = clubAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateRegistrationResponseDto createRegistration(
            Long currentUserId,
            Long eventId,
            CreateRegistrationRequestDto request
    ) {
        Event event = getEvent(eventId);
        if (!Boolean.TRUE.equals(event.getIsActive())) {
            throw new AppException(ErrorCode.CONFLICT, "Event is not active");
        }
        if (registrationRepository.existsByEvent_IdAndUser_Id(eventId, currentUserId)) {
            throw new AppException(ErrorCode.CONFLICT, "Already registered for this event");
        }

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        Map<Long, String> answerMap = toAnswerMap(request);
        List<FormField> fields = formFieldRepository.findAllByEvent_IdOrderBySortOrderAsc(eventId);
        validateAnswers(fields, answerMap);

        Registration registration = registrationRepository.save(
                new Registration(event, user, UUID.randomUUID().toString(), RegistrationStatus.REGISTERED)
        );

        for (FormField field : fields) {
            if (answerMap.containsKey(field.getId())) {
                formAnswerRepository.save(new FormAnswer(registration, field, answerMap.get(field.getId())));
            }
        }

        return new CreateRegistrationResponseDto(registration.getId(), registration.getQrToken());
    }

    /**
     * 본인의 참가 신청을 취소한다. 행사 시작 전, REGISTERED 상태일 때만 가능.
     * form_answers는 히스토리 보존을 위해 그대로 둔다.
     */
    @Transactional
    public void cancelMyRegistration(Long currentUserId, Long eventId) {
        Registration registration = registrationRepository.findByEvent_IdAndUser_Id(eventId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration not found"));

        if (registration.getStatus() == RegistrationStatus.CHECKED_IN) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot cancel a checked-in registration");
        }
        if (registration.getStatus() == RegistrationStatus.CANCELED) {
            throw new AppException(ErrorCode.CONFLICT, "Registration is already canceled");
        }

        LocalDateTime startTime = registration.getEvent().getStartTime();
        if (startTime != null && !LocalDateTime.now().isBefore(startTime)) {
            throw new AppException(ErrorCode.CONFLICT, "Cannot cancel after event has started");
        }

        registration.updateStatus(RegistrationStatus.CANCELED);
    }

    @Transactional(readOnly = true)
    public MyRegistrationResponseDto getMyRegistration(Long currentUserId, Long eventId) {
        Registration registration = registrationRepository.findByEvent_IdAndUser_Id(eventId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Registration not found"));
        List<RegistrationAnswerResponseDto> answers = getAnswers(registration.getId());
        return new MyRegistrationResponseDto(
                registration.getId(),
                registration.getQrToken(),
                registration.getStatus().name(),
                registration.getRegisteredAt().toString(),
                answers
        );
    }

    @Transactional(readOnly = true)
    public List<AdminRegistrationItemDto> getEventRegistrations(Long currentUserId, Long eventId) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        List<Registration> registrations = registrationRepository.findAllByEvent_Id(eventId);
        List<FormAnswer> answers = formAnswerRepository.findAllByRegistration_Event_Id(eventId);
        Map<Long, List<RegistrationAnswerResponseDto>> answersByRegistrationId = new HashMap<>();

        for (FormAnswer answer : answers) {
            answersByRegistrationId.computeIfAbsent(answer.getRegistration().getId(), key -> new ArrayList<>())
                    .add(new RegistrationAnswerResponseDto(
                            answer.getField().getId(),
                            answer.getField().getLabel(),
                            answer.getAnswerValue()
                    ));
        }

        Map<Long, LocalDateTime> checkInTimeByRegistrationId = new HashMap<>();
        for (AttendanceLog log : attendanceLogRepository.findAllByEvent_Id(eventId)) {
            checkInTimeByRegistrationId.put(log.getRegistration().getId(), log.getCheckInTime());
        }

        return registrations.stream()
                .map(registration -> new AdminRegistrationItemDto(
                        registration.getId(),
                        registration.getUser().getId(),
                        registration.getUser().getUsername(),
                        registration.getUser().getRealName(),
                        registration.getStatus().name(),
                        registration.getQrToken(),
                        checkInTimeByRegistrationId.get(registration.getId()),
                        answersByRegistrationId.getOrDefault(registration.getId(), List.of())
                ))
                .toList();
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
    }

    private Map<Long, String> toAnswerMap(CreateRegistrationRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        Map<Long, String> result = new HashMap<>();
        if (request.answers() == null) {
            return result;
        }
        for (RegistrationAnswerRequestDto answer : request.answers()) {
            if (answer == null || answer.fieldId() == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "answers.fieldId is required");
            }
            result.put(answer.fieldId(), answer.value());
        }
        return result;
    }

    private void validateAnswers(List<FormField> fields, Map<Long, String> answerMap) {
        Set<Long> fieldIds = new HashSet<>();
        for (FormField field : fields) {
            fieldIds.add(field.getId());
            if (Boolean.TRUE.equals(field.getIsRequired())) {
                String value = answerMap.get(field.getId());
                if (value == null || value.isBlank()) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Required field is missing: " + field.getId());
                }
            }
            if (field.getFieldType() == FieldType.SELECT && answerMap.containsKey(field.getId())) {
                validateSelectValue(field, answerMap.get(field.getId()));
            }
        }

        for (Long answerFieldId : answerMap.keySet()) {
            if (!fieldIds.contains(answerFieldId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "Invalid fieldId: " + answerFieldId);
            }
        }
    }

    private void validateSelectValue(FormField field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        List<String> options = parseOptions(field.getOptions());
        if (!options.contains(value)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Value is out of range for SELECT field: " + field.getId());
        }
    }

    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to parse form field options");
        }
    }

    private List<RegistrationAnswerResponseDto> getAnswers(Long registrationId) {
        return formAnswerRepository.findAllByRegistration_Id(registrationId).stream()
                .map(answer -> new RegistrationAnswerResponseDto(
                        answer.getField().getId(),
                        answer.getField().getLabel(),
                        answer.getAnswerValue()
                ))
                .toList();
    }
}
