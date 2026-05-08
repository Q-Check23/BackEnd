package team23.q_check.notice.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.club.domain.model.ClubMember;
import team23.q_check.club.domain.repository.ClubMemberRepository;
import team23.q_check.club.domain.service.ClubAuthorizationService;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.notice.domain.model.Notice;
import team23.q_check.notice.domain.repository.NoticeRepository;
import team23.q_check.notice.dto.CreateNoticeRequestDto;
import team23.q_check.notice.dto.NoticeResponseDto;

import java.util.List;

@Service
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final ClubAuthorizationService clubAuthorizationService;
    private final ClubMemberRepository clubMemberRepository;

    public NoticeService(
            NoticeRepository noticeRepository,
            ClubAuthorizationService clubAuthorizationService,
            ClubMemberRepository clubMemberRepository
    ) {
        this.noticeRepository = noticeRepository;
        this.clubAuthorizationService = clubAuthorizationService;
        this.clubMemberRepository = clubMemberRepository;
    }

    @Transactional(readOnly = true)
    public List<NoticeResponseDto> getClubNotices(Long currentUserId, Long clubId) {
        clubAuthorizationService.requireMembership(clubId, currentUserId);
        return noticeRepository.findAllByClub_IdOrderByCreatedAtDesc(clubId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Transactional
    public NoticeResponseDto createNotice(Long currentUserId, Long clubId, CreateNoticeRequestDto request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "title is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "content is required");
        }

        ClubMember authorMembership = clubAuthorizationService.requireAdminOrOwner(clubId, currentUserId);

        Notice notice = new Notice(
                authorMembership.getClub(),
                authorMembership.getUser(),
                request.title().trim(),
                request.content().trim()
        );
        return toResponseDto(noticeRepository.save(notice));
    }

    private NoticeResponseDto toResponseDto(Notice notice) {
        ClubMember authorMembership = clubMemberRepository
                .findByClub_IdAndUser_Id(notice.getClub().getId(), notice.getAuthor().getId())
                .orElse(null);
        return new NoticeResponseDto(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getAuthor().getUsername(),
                authorMembership == null ? null : authorMembership.getRole(),
                notice.getCreatedAt().toString()
        );
    }
}
