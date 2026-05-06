package team23.q_check.identity.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.dto.UserSearchResultDto;
import team23.q_check.identity.domain.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public MyUserResponseDto getMyUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));
        return toDto(user);
    }

    @Transactional
    public MyUserResponseDto updateMyUser(Long currentUserId, UpdateMyUserRequestDto request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "User not found: " + currentUserId));
        user.updateRealName(request.realName());
        return toDto(user);
    }

    /**
     * 닉네임(부분일치) 또는 이메일(정확매치)로 사용자를 검색한다.
     * 둘 중 하나는 비어있지 않아야 하며, 응답에 이메일은 포함하지 않는다.
     */
    @Transactional(readOnly = true)
    public List<UserSearchResultDto> searchUsers(String nickname, String email) {
        boolean hasNickname = nickname != null && !nickname.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (!hasNickname && !hasEmail) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "nickname or email is required");
        }

        if (hasEmail) {
            return userRepository.findByEmail(email.trim())
                    .map(user -> List.of(toSearchDto(user)))
                    .orElse(List.of());
        }

        return userRepository
                .findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc(nickname.trim())
                .stream()
                .map(this::toSearchDto)
                .toList();
    }

    private MyUserResponseDto toDto(User user) {
        return new MyUserResponseDto(user.getId(), user.getUsername(), user.getRealName());
    }

    private UserSearchResultDto toSearchDto(User user) {
        return new UserSearchResultDto(user.getId(), user.getUsername(), user.getRealName());
    }
}
