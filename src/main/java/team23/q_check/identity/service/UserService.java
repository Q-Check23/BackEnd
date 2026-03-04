package team23.q_check.identity.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.dto.UserResponseDto;
import team23.q_check.identity.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDto getSampleUser() {
        return new UserResponseDto(7L, "123456789012345678", "kimjyun");
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

    private MyUserResponseDto toDto(User user) {
        return new MyUserResponseDto(user.getId(), user.getUsername(), user.getRealName());
    }
}
