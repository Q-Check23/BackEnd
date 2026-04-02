package team23.q_check.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.common.error.AppException;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.service.UserService;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.domain.repository.UserRepository;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(userRepository);
    }

    @Test
    void getMyUser_returnsUserInfo() throws Exception {
        User user = new User("dev-1", "qcheck_user");
        user.updateRealName("김지윤");
        setId(user, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyUserResponseDto result = userService.getMyUser(1L);

        assertEquals(1L, result.id());
        assertEquals("qcheck_user", result.username());
        assertEquals("김지윤", result.realName());
    }

    @Test
    void updateMyUser_updatesRealName() throws Exception {
        User user = new User("dev-1", "qcheck_user");
        setId(user, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        MyUserResponseDto result = userService.updateMyUser(1L, new UpdateMyUserRequestDto("홍길동"));

        assertEquals("홍길동", result.realName());
        assertEquals("홍길동", user.getRealName());
    }

    @Test
    void getMyUser_whenNotFound_throwsAppException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> userService.getMyUser(99L));
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
