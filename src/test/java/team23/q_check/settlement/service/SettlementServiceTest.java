package team23.q_check.settlement.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import team23.q_check.club.domain.model.Club;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.model.RegistrationStatus;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.settlement.domain.model.Settlement;
import team23.q_check.settlement.domain.model.SettlementItem;
import team23.q_check.settlement.domain.model.SettlementItemStatus;
import team23.q_check.settlement.domain.repository.SettlementItemRepository;
import team23.q_check.settlement.domain.repository.SettlementRepository;
import team23.q_check.settlement.domain.service.SettlementService;
import team23.q_check.settlement.dto.CreateSettlementRequestDto;
import team23.q_check.settlement.dto.SettlementGroupRequestDto;
import team23.q_check.settlement.dto.SettlementItemResponseDto;
import team23.q_check.settlement.dto.SettlementResponseDto;
import team23.q_check.settlement.dto.SettlementSummaryDto;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementServiceTest {

    private SettlementRepository settlementRepository;
    private SettlementItemRepository settlementItemRepository;
    private EventRepository eventRepository;
    private RegistrationRepository registrationRepository;
    private UserRepository userRepository;
    private ClubAuthorizationService clubAuthorizationService;
    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        settlementRepository = mock(SettlementRepository.class);
        settlementItemRepository = mock(SettlementItemRepository.class);
        eventRepository = mock(EventRepository.class);
        registrationRepository = mock(RegistrationRepository.class);
        userRepository = mock(UserRepository.class);
        clubAuthorizationService = mock(ClubAuthorizationService.class);
        settlementService = new SettlementService(
                settlementRepository,
                settlementItemRepository,
                eventRepository,
                registrationRepository,
                userRepository,
                clubAuthorizationService
        );
    }

    @Test
    void createSettlement_withTwoGroups_savesItemsForEachUser() throws Exception {
        Event event = newEvent(1L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        User u1 = newUser(1L, "alice");
        User u2 = newUser(2L, "bob");
        User u3 = newUser(3L, "carol");
        when(registrationRepository.findAllByEvent_Id(100L)).thenReturn(List.of(
                newRegistration(event, u1),
                newRegistration(event, u2),
                newRegistration(event, u3)
        ));
        when(userRepository.findById(10L)).thenReturn(Optional.of(newUser(10L, "creator")));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u2));
        when(userRepository.findById(3L)).thenReturn(Optional.of(u3));

        Settlement saved = newSettlement(500L, event, new BigDecimal("60000"));
        when(settlementRepository.save(any())).thenReturn(saved);
        when(settlementItemRepository.findAllBySettlement_Id(500L)).thenReturn(List.of(
                newItem(801L, saved, u1, new BigDecimal("10000"), SettlementItemStatus.UNPAID),
                newItem(802L, saved, u2, new BigDecimal("10000"), SettlementItemStatus.UNPAID),
                newItem(803L, saved, u3, new BigDecimal("20000"), SettlementItemStatus.UNPAID)
        ));

        CreateSettlementRequestDto request = new CreateSettlementRequestDto(
                100L, "OT 회식비", new BigDecimal("60000"), null,
                List.of(
                        new SettlementGroupRequestDto(List.of(1L, 2L), new BigDecimal("10000")),
                        new SettlementGroupRequestDto(List.of(3L), new BigDecimal("20000"))
                )
        );

        SettlementResponseDto result = settlementService.createSettlement(10L, request);

        ArgumentCaptor<SettlementItem> captor = ArgumentCaptor.forClass(SettlementItem.class);
        verify(settlementItemRepository, times(3)).save(captor.capture());
        List<SettlementItem> savedItems = captor.getAllValues();
        assertEquals(new BigDecimal("10000"), savedItems.get(0).getAmount());
        assertEquals(new BigDecimal("10000"), savedItems.get(1).getAmount());
        assertEquals(new BigDecimal("20000"), savedItems.get(2).getAmount());

        assertEquals(new BigDecimal("40000"), result.allocatedAmount());
        assertEquals(new BigDecimal("20000"), result.unallocatedAmount());
        assertEquals(3, result.items().size());
    }

    @Test
    void createSettlement_whenSameUserInTwoGroups_throwsBadRequest() throws Exception {
        Event event = newEvent(1L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        CreateSettlementRequestDto request = new CreateSettlementRequestDto(
                100L, "회식비", new BigDecimal("30000"), null,
                List.of(
                        new SettlementGroupRequestDto(List.of(1L, 2L), new BigDecimal("10000")),
                        new SettlementGroupRequestDto(List.of(2L, 3L), new BigDecimal("20000"))
                )
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.createSettlement(10L, request)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Duplicate userId"));
    }

    @Test
    void createSettlement_whenUserNotParticipant_throwsBadRequest() throws Exception {
        Event event = newEvent(1L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        User u1 = newUser(1L, "alice");
        when(registrationRepository.findAllByEvent_Id(100L)).thenReturn(List.of(
                newRegistration(event, u1)
        ));

        CreateSettlementRequestDto request = new CreateSettlementRequestDto(
                100L, "회식비", new BigDecimal("30000"), null,
                List.of(new SettlementGroupRequestDto(List.of(1L, 99L), new BigDecimal("15000")))
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.createSettlement(10L, request)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("99"));
    }

    @Test
    void createSettlement_whenNotAdmin_throwsForbidden() throws Exception {
        Event event = newEvent(1L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));
        when(clubAuthorizationService.requireAdminOrOwner(1L, 10L))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN, "forbidden"));

        CreateSettlementRequestDto request = new CreateSettlementRequestDto(
                100L, "회식비", new BigDecimal("30000"), null,
                List.of(new SettlementGroupRequestDto(List.of(1L), new BigDecimal("15000")))
        );

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.createSettlement(10L, request)
        );
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
    }

    @Test
    void getEventSettlements_returnsCompletedCount() throws Exception {
        Event event = newEvent(1L);
        when(eventRepository.findById(100L)).thenReturn(Optional.of(event));

        Settlement s = newSettlement(500L, event, new BigDecimal("30000"));
        when(settlementRepository.findAllByEvent_IdOrderByCreatedAtDesc(100L)).thenReturn(List.of(s));
        when(settlementItemRepository.findAllBySettlement_Id(500L)).thenReturn(List.of(
                newItem(801L, s, newUser(1L, "a"), new BigDecimal("10000"), SettlementItemStatus.COMPLETED),
                newItem(802L, s, newUser(2L, "b"), new BigDecimal("10000"), SettlementItemStatus.PENDING),
                newItem(803L, s, newUser(3L, "c"), new BigDecimal("10000"), SettlementItemStatus.UNPAID)
        ));

        List<SettlementSummaryDto> result = settlementService.getEventSettlements(10L, 100L);

        assertEquals(1, result.size());
        assertEquals(3L, result.get(0).itemCount());
        assertEquals(1L, result.get(0).completedCount());
    }

    @Test
    void markAsPending_whenItemBelongsToOtherUser_throwsNotFound() {
        when(settlementItemRepository.findByIdAndUser_Id(801L, 1L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.markAsPending(1L, 801L)
        );
        assertEquals(ErrorCode.NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void markAsPending_whenAlreadyPending_throwsConflict() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.PENDING);
        when(settlementItemRepository.findByIdAndUser_Id(801L, 1L)).thenReturn(Optional.of(item));

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.markAsPending(1L, 801L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void markAsPending_transitionsFromUnpaid() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.UNPAID);
        when(settlementItemRepository.findByIdAndUser_Id(801L, 1L)).thenReturn(Optional.of(item));

        SettlementItemResponseDto result = settlementService.markAsPending(1L, 801L);

        assertEquals("PENDING", result.status());
        assertEquals(SettlementItemStatus.PENDING, item.getStatus());
    }

    @Test
    void confirmAsCompleted_whenNotPending_throwsConflict() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.UNPAID);
        when(settlementItemRepository.findById(801L)).thenReturn(Optional.of(item));

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.confirmAsCompleted(10L, 801L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    @Test
    void confirmAsCompleted_transitionsPendingToCompleted() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.PENDING);
        when(settlementItemRepository.findById(801L)).thenReturn(Optional.of(item));

        SettlementItemResponseDto result = settlementService.confirmAsCompleted(10L, 801L);

        assertEquals("COMPLETED", result.status());
        assertEquals(SettlementItemStatus.COMPLETED, item.getStatus());
    }

    @Test
    void recordReminder_incrementsCount() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.UNPAID);
        when(settlementItemRepository.findById(801L)).thenReturn(Optional.of(item));

        SettlementItemResponseDto result = settlementService.recordReminder(10L, 801L);

        assertEquals(1L, result.remindCount());
        assertEquals(1L, item.getRemindCount());
    }

    @Test
    void recordReminder_whenCompleted_throwsConflict() throws Exception {
        Event event = newEvent(1L);
        Settlement s = newSettlement(500L, event, new BigDecimal("10000"));
        SettlementItem item = newItem(801L, s, newUser(1L, "a"),
                new BigDecimal("10000"), SettlementItemStatus.COMPLETED);
        when(settlementItemRepository.findById(801L)).thenReturn(Optional.of(item));

        AppException exception = assertThrows(
                AppException.class,
                () -> settlementService.recordReminder(10L, 801L)
        );
        assertEquals(ErrorCode.CONFLICT, exception.getErrorCode());
    }

    private Event newEvent(Long clubId) throws Exception {
        Club club = new Club("UMC", "desc", "guild-1", null);
        setId(club, clubId);
        Event event = new Event(club, "OT",
                LocalDateTime.parse("2026-03-10T19:00:00"),
                LocalDateTime.parse("2026-03-10T19:00:00"),
                null, true);
        setId(event, 100L);
        return event;
    }

    private User newUser(Long id, String username) throws Exception {
        User user = new User("dev-" + id, null, username, null);
        setId(user, id);
        return user;
    }

    private Registration newRegistration(Event event, User user) throws Exception {
        Registration registration = new Registration(event, user, "qr-" + user.getId(),
                RegistrationStatus.REGISTERED);
        setId(registration, user.getId() + 1000);
        return registration;
    }

    private Settlement newSettlement(Long id, Event event, BigDecimal totalAmount) throws Exception {
        User creator = newUser(10L, "creator");
        Settlement settlement = new Settlement(event, creator, "정산", totalAmount, null);
        setId(settlement, id);
        return settlement;
    }

    private SettlementItem newItem(Long id, Settlement settlement, User user,
                                   BigDecimal amount, SettlementItemStatus status) throws Exception {
        SettlementItem item = new SettlementItem(settlement, user, amount);
        setId(item, id);
        Field statusField = SettlementItem.class.getDeclaredField("status");
        statusField.setAccessible(true);
        statusField.set(item, status);
        return item;
    }

    private void setId(Object target, Long id) throws Exception {
        Field field = target.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(target, id);
    }
}
