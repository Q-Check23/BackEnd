package team23.q_check.identity.service;

import org.springframework.stereotype.Service;
import team23.q_check.identity.dto.UserResponseDto;

@Service
public class UserService {

    public UserResponseDto getSampleUser() {
        return new UserResponseDto(7L, "123456789012345678", "kimjyun");
    }
}
