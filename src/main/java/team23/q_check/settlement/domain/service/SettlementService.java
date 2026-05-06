package team23.q_check.settlement.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.event.domain.model.Event;
import team23.q_check.event.domain.model.Registration;
import team23.q_check.event.domain.repository.EventRepository;
import team23.q_check.event.domain.repository.RegistrationRepository;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.repository.UserRepository;
import team23.q_check.settlement.domain.model.Settlement;
import team23.q_check.settlement.domain.model.SettlementItem;
import team23.q_check.settlement.domain.repository.SettlementItemRepository;
import team23.q_check.settlement.domain.repository.SettlementRepository;
import team23.q_check.settlement.domain.model.SettlementItemStatus;
import team23.q_check.settlement.dto.CreateSettlementRequestDto;
import team23.q_check.settlement.dto.SettlementGroupRequestDto;
import team23.q_check.settlement.dto.SettlementItemResponseDto;
import team23.q_check.settlement.dto.SettlementResponseDto;
import team23.q_check.settlement.dto.SettlementSummaryDto;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final SettlementItemRepository settlementItemRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final ClubAuthorizationService clubAuthorizationService;

    public SettlementService(
            SettlementRepository settlementRepository,
            SettlementItemRepository settlementItemRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            ClubAuthorizationService clubAuthorizationService
    ) {
        this.settlementRepository = settlementRepository;
        this.settlementItemRepository = settlementItemRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.clubAuthorizationService = clubAuthorizationService;
    }

    @Transactional
    public SettlementResponseDto createSettlement(Long currentUserId, CreateSettlementRequestDto request) {
        validateCreateRequest(request);

        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + request.eventId()));
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        Map<Long, BigDecimal> allocations = expandGroups(request.groups());
        validateParticipants(request.eventId(), allocations.keySet());

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));

        Settlement settlement = settlementRepository.save(new Settlement(
                event,
                creator,
                request.title().trim(),
                request.totalAmount(),
                request.receiptImageUrl()
        ));

        for (Map.Entry<Long, BigDecimal> entry : allocations.entrySet()) {
            User member = userRepository.findById(entry.getKey())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + entry.getKey()));
            settlementItemRepository.save(new SettlementItem(settlement, member, entry.getValue()));
        }

        return toDetailDto(settlement, settlementItemRepository.findAllBySettlement_Id(settlement.getId()));
    }

    @Transactional(readOnly = true)
    public List<SettlementSummaryDto> getEventSettlements(Long currentUserId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Event not found: " + eventId));
        clubAuthorizationService.requireAdminOrOwner(event.getClub().getId(), currentUserId);

        List<Settlement> settlements = settlementRepository.findAllByEvent_IdOrderByCreatedAtDesc(eventId);
        return settlements.stream()
                .map(settlement -> {
                    List<SettlementItem> items = settlementItemRepository.findAllBySettlement_Id(settlement.getId());
                    long completed = items.stream()
                            .filter(item -> item.getStatus() == SettlementItemStatus.COMPLETED)
                            .count();
                    return new SettlementSummaryDto(
                            settlement.getId(),
                            settlement.getEvent().getId(),
                            settlement.getTitle(),
                            settlement.getTotalAmount(),
                            settlement.getCreatedAt().toString(),
                            items.size(),
                            completed
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public SettlementResponseDto getSettlement(Long currentUserId, Long settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Settlement not found: " + settlementId));
        clubAuthorizationService.requireAdminOrOwner(settlement.getEvent().getClub().getId(), currentUserId);

        List<SettlementItem> items = settlementItemRepository.findAllBySettlement_Id(settlementId);
        return toDetailDto(settlement, items);
    }

    /** 본인이 송금했음을 신고: UNPAID → PENDING. */
    @Transactional
    public SettlementItemResponseDto markAsPending(Long currentUserId, Long itemId) {
        SettlementItem item = settlementItemRepository.findByIdAndUser_Id(itemId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Settlement item not found for current user"));
        if (item.getStatus() != SettlementItemStatus.UNPAID) {
            throw new AppException(ErrorCode.CONFLICT, "Only UNPAID items can be marked as PENDING");
        }
        item.markAsPending();
        return toItemDto(item);
    }

    /** 관리자가 송금을 확인: PENDING → COMPLETED. */
    @Transactional
    public SettlementItemResponseDto confirmAsCompleted(Long currentUserId, Long itemId) {
        SettlementItem item = settlementItemRepository.findById(itemId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Settlement item not found: " + itemId));
        clubAuthorizationService.requireAdminOrOwner(
                item.getSettlement().getEvent().getClub().getId(), currentUserId);

        if (item.getStatus() != SettlementItemStatus.PENDING) {
            throw new AppException(ErrorCode.CONFLICT, "Only PENDING items can be confirmed as COMPLETED");
        }
        item.markAsCompleted();
        return toItemDto(item);
    }

    private SettlementItemResponseDto toItemDto(SettlementItem item) {
        return new SettlementItemResponseDto(
                item.getId(),
                item.getUser().getId(),
                item.getUser().getUsername(),
                item.getAmount(),
                item.getStatus().name(),
                item.getUpdatedAt() == null ? null : item.getUpdatedAt().toString(),
                item.getLastRemindedAt() == null ? null : item.getLastRemindedAt().toString(),
                item.getRemindCount()
        );
    }

    private void validateCreateRequest(CreateSettlementRequestDto request) {
        if (request == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Request body is required");
        }
        if (request.eventId() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "eventId is required");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "title is required");
        }
        if (request.totalAmount() == null || request.totalAmount().signum() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "totalAmount must be positive");
        }
        if (request.groups() == null || request.groups().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "groups must not be empty");
        }
    }

    private Map<Long, BigDecimal> expandGroups(List<SettlementGroupRequestDto> groups) {
        Map<Long, BigDecimal> allocations = new LinkedHashMap<>();
        for (SettlementGroupRequestDto group : groups) {
            if (group == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "group must not be null");
            }
            if (group.userIds() == null || group.userIds().isEmpty()) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "group.userIds must not be empty");
            }
            if (group.amount() == null || group.amount().signum() < 0) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "group.amount must be zero or positive");
            }
            for (Long userId : group.userIds()) {
                if (userId == null) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "group.userIds must not contain null");
                }
                if (allocations.containsKey(userId)) {
                    throw new AppException(ErrorCode.INVALID_REQUEST, "Duplicate userId across groups: " + userId);
                }
                allocations.put(userId, group.amount());
            }
        }
        return allocations;
    }

    private void validateParticipants(Long eventId, Set<Long> userIds) {
        Set<Long> participantIds = new HashSet<>();
        for (Registration registration : registrationRepository.findAllByEvent_Id(eventId)) {
            participantIds.add(registration.getUser().getId());
        }
        for (Long userId : userIds) {
            if (!participantIds.contains(userId)) {
                throw new AppException(ErrorCode.INVALID_REQUEST, "User is not a participant of this event: " + userId);
            }
        }
    }

    private SettlementResponseDto toDetailDto(Settlement settlement, List<SettlementItem> items) {
        List<SettlementItemResponseDto> itemDtos = items.stream().map(this::toItemDto).toList();

        BigDecimal allocated = items.stream()
                .map(SettlementItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal unallocated = settlement.getTotalAmount().subtract(allocated);

        return new SettlementResponseDto(
                settlement.getId(),
                settlement.getEvent().getId(),
                settlement.getCreatedBy().getId(),
                settlement.getTitle(),
                settlement.getTotalAmount(),
                settlement.getReceiptImageUrl(),
                settlement.getCreatedAt().toString(),
                allocated,
                unallocated,
                itemDtos
        );
    }
}
