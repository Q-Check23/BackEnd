package team23.q_check.event.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.EventPhoto;
import team23.q_check.event.domain.repository.EventPhotoRepository;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.dto.EventPhotoResponseDto;
import team23.q_check.event.dto.UploadEventPhotoRequestDto;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.util.List;

@Service
public class EventPhotoService {

    private static final int MAX_URL_LENGTH = 255;

    private final EventPhotoRepository eventPhotoRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public EventPhotoService(
            EventPhotoRepository eventPhotoRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.eventPhotoRepository = eventPhotoRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    /** 클럽 멤버가 행사 사진 URL을 등록한다. */
    @Transactional
    public EventPhotoResponseDto uploadPhoto(Long currentUserId, Long eventId, UploadEventPhotoRequestDto request) {
        String photoUrl = validatePhotoUrl(request);

        Event event = getEvent(eventId);
        clubAuthorizationService.requireMembership(event.getClub().getId(), currentUserId);

        User uploader = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        EventPhoto saved = eventPhotoRepository.save(new EventPhoto(event, uploader, photoUrl));
        return toDto(saved);
    }

    /** 클럽 멤버에게 행사 사진을 최신순으로 보여준다. */
    @Transactional(readOnly = true)
    public List<EventPhotoResponseDto> getEventPhotos(Long currentUserId, Long eventId) {
        Event event = getEvent(eventId);
        clubAuthorizationService.requireMembership(event.getClub().getId(), currentUserId);

        return eventPhotoRepository.findAllByEvent_IdOrderByCreatedAtDesc(eventId).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 사진을 삭제한다. 본인이 올린 사진이거나 클럽 ADMIN 이상이어야 한다.
     * 일반 MEMBER가 다른 사람의 사진을 지우는 것은 거부.
     */
    @Transactional
    public void deletePhoto(Long currentUserId, Long eventId, Long photoId) {
        EventPhoto photo = eventPhotoRepository.findByIdAndEvent_Id(photoId, eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Photo not found: " + photoId));

        ClubMember membership = clubAuthorizationService.requireMembership(
                photo.getEvent().getClub().getId(), currentUserId);

        boolean isUploader = photo.getUploader().getId().equals(currentUserId);
        boolean isAdminOrOwner = membership.getRole() == ClubRole.ADMIN
                || membership.getRole() == ClubRole.OWNER;
        if (!isUploader && !isAdminOrOwner) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Only the uploader or club ADMIN/OWNER can delete this photo");
        }

        eventPhotoRepository.delete(photo);
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
    }

    private String validatePhotoUrl(UploadEventPhotoRequestDto request) {
        if (request == null || request.photoUrl() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "photoUrl is required");
        }
        String trimmed = request.photoUrl().trim();
        if (trimmed.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "photoUrl is required");
        }
        if (trimmed.length() > MAX_URL_LENGTH) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "photoUrl must be at most " + MAX_URL_LENGTH + " characters");
        }
        return trimmed;
    }

    private EventPhotoResponseDto toDto(EventPhoto photo) {
        return new EventPhotoResponseDto(
                photo.getId(),
                photo.getPhotoUrl(),
                photo.getUploader().getId(),
                photo.getUploader().getUsername(),
                photo.getCreatedAt().toString()
        );
    }
}
