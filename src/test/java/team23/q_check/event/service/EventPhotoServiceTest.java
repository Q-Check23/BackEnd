package team23.q_check.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.model.ClubRole;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.EventPhoto;
import team23.q_check.event.domain.repository.EventPhotoRepository;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.service.EventPhotoService;
import team23.q_check.event.dto.EventPhotoResponseDto;
import team23.q_check.event.dto.UploadEventPhotoRequestDto;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventPhotoServiceTest {

    private EventPhotoRepository eventPhotoRepository;
    private EventRepository eventRepository;
    private UserRepository userRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private EventPhotoService eventPhotoService;

    @BeforeEach
    void setUp() {
        eventPhotoRepository = mock(EventPhotoRepository.class);
        eventRepository = mock(EventRepository.class);
        userRepository = mock(UserRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        eventPhotoService = new EventPhotoService(
                eventPhotoRepository, eventRepository, userRepository, clubAuthorizationService
        );
    }

    @Test
    void uploadPhoto_savesAndReturnsDto() throws Exception {
        Event event = newEvent();
        User uploader = newUser(7L, "kimjyun");
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(clubAuthorizationService.requireMembership(1L, 7L))
                .thenReturn(new ClubMember(event.getClub(), uploader, ClubRole.MEMBER));
        when(userRepository.findById(7L)).thenReturn(Optional.of(uploader));
        when(eventPhotoRepository.save(any(EventPhoto.class))).thenAnswer(invocation -> {
            EventPhoto photo = invocation.getArgument(0);
            setId(photo, 501L);
            return photo;
        });

        EventPhotoResponseDto result = eventPhotoService.uploadPhoto(
                7L, 100L, new UploadEventPhotoRequestDto("https://cdn.example.com/a.jpg"));

        assertEquals(501L, result.photoId());
        assertEquals("https://cdn.example.com/a.jpg", result.photoUrl());
        assertEquals(7L, result.uploaderUserId());
        assertEquals("kimjyun", result.uploaderUsername());

        ArgumentCaptor<EventPhoto> captor = ArgumentCaptor.forClass(EventPhoto.class);
        verify(eventPhotoRepository).save(captor.capture());
        assertEquals("https://cdn.example.com/a.jpg", captor.getValue().getPhotoUrl());
    }

    @Test
    void uploadPhoto_blankUrl_throwsBadRequest() throws Exception {
        AppException exception = assertThrows(
                AppException.class,
                () -> eventPhotoService.uploadPhoto(7L, 100L,
                        new UploadEventPhotoRequestDto("   "))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void uploadPhoto_urlTooLong_throwsBadRequest() {
        String url = "https://" + "a".repeat(260);
        AppException exception = assertThrows(
                AppException.class,
                () -> eventPhotoService.uploadPhoto(7L, 100L, new UploadEventPhotoRequestDto(url))
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void uploadPhoto_nonMember_throwsForbidden() throws Exception {
        Event event = newEvent();
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(clubAuthorizationService.requireMembership(1L, 99L))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN, "Not a member of this club"));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventPhotoService.uploadPhoto(99L, 100L,
                        new UploadEventPhotoRequestDto("https://cdn.example.com/a.jpg"))
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(eventPhotoRepository, never()).save(any());
    }

    @Test
    void getEventPhotos_returnsListInLatestFirstOrder() throws Exception {
        Event event = newEvent();
        User uploader = newUser(7L, "kimjyun");
        EventPhoto p1 = new EventPhoto(event, uploader, "https://cdn.example.com/a.jpg");
        EventPhoto p2 = new EventPhoto(event, uploader, "https://cdn.example.com/b.jpg");
        setId(p1, 501L);
        setId(p2, 502L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(clubAuthorizationService.requireMembership(1L, 7L))
                .thenReturn(new ClubMember(event.getClub(), uploader, ClubRole.MEMBER));
        when(eventPhotoRepository.findAllByEvent_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of(p2, p1));

        List<EventPhotoResponseDto> result = eventPhotoService.getEventPhotos(7L, 100L);

        assertEquals(2, result.size());
        assertEquals(502L, result.get(0).photoId());
        assertEquals(501L, result.get(1).photoId());
    }

    @Test
    void deletePhoto_byUploader_succeeds() throws Exception {
        Event event = newEvent();
        User uploader = newUser(7L, "kimjyun");
        EventPhoto photo = new EventPhoto(event, uploader, "https://cdn.example.com/a.jpg");
        setId(photo, 501L);
        when(eventPhotoRepository.findByIdAndEvent_Id(501L, 100L)).thenReturn(Optional.of(photo));
        when(clubAuthorizationService.requireMembership(1L, 7L))
                .thenReturn(new ClubMember(event.getClub(), uploader, ClubRole.MEMBER));

        eventPhotoService.deletePhoto(7L, 100L, 501L);

        verify(eventPhotoRepository).delete(photo);
    }

    @Test
    void deletePhoto_byAdminWhoIsNotUploader_succeeds() throws Exception {
        Event event = newEvent();
        User uploader = newUser(7L, "kimjyun");
        User admin = newUser(1L, "admin");
        EventPhoto photo = new EventPhoto(event, uploader, "https://cdn.example.com/a.jpg");
        setId(photo, 501L);
        when(eventPhotoRepository.findByIdAndEvent_Id(501L, 100L)).thenReturn(Optional.of(photo));
        when(clubAuthorizationService.requireMembership(1L, 1L))
                .thenReturn(new ClubMember(event.getClub(), admin, ClubRole.ADMIN));

        eventPhotoService.deletePhoto(1L, 100L, 501L);

        verify(eventPhotoRepository).delete(photo);
    }

    @Test
    void deletePhoto_byOtherMember_throwsForbidden() throws Exception {
        Event event = newEvent();
        User uploader = newUser(7L, "kimjyun");
        User other = newUser(8L, "other");
        EventPhoto photo = new EventPhoto(event, uploader, "https://cdn.example.com/a.jpg");
        setId(photo, 501L);
        when(eventPhotoRepository.findByIdAndEvent_Id(501L, 100L)).thenReturn(Optional.of(photo));
        when(clubAuthorizationService.requireMembership(1L, 8L))
                .thenReturn(new ClubMember(event.getClub(), other, ClubRole.MEMBER));

        AppException exception = assertThrows(
                AppException.class,
                () -> eventPhotoService.deletePhoto(8L, 100L, 501L)
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(eventPhotoRepository, never()).delete(any());
    }

    @Test
    void deletePhoto_whenNotFound_throwsNotFound() {
        when(eventPhotoRepository.findByIdAndEvent_Id(999L, 100L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> eventPhotoService.deletePhoto(1L, 100L, 999L)
        );
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    private Event newEvent() throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, 1L);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T22:00:00"),
                null, true);
        setId(event, 100L);
        return event;
    }

    private User newUser(Long id, String username) throws Exception {
        User user = new User("dev-" + id, null, username, null);
        setId(user, id);
        return user;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
