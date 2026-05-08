package team23.q_check.identity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import team23.q_check.common.error.AppException;
import team23.q_check.common.error.ErrorCode;
import team23.q_check.identity.domain.model.User;
import team23.q_check.identity.domain.service.UserService;
import team23.q_check.identity.dto.MyUserResponseDto;
import team23.q_check.identity.dto.UpdateMyUserRequestDto;
import team23.q_check.identity.dto.UserSearchResultDto;
import team23.q_check.identity.domain.repository.UserRepository;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserService(
                userRepository,
                mock(team23.q_check.event.domain.repository.RegistrationRepository.class)
        );
    }

    @Test
    void getMyUser_returnsUserInfo() throws Exception {
        User user = new User("dev-1", null, "qcheck_user", null);
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
        User user = new User("dev-1", null, "qcheck_user", null);
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

    @Test
    void searchUsers_byNickname_returnsMatchingList() throws Exception {
        User u1 = new User("dev-1", null, "kimjyun", null);
        u1.updateRealName("김지윤");
        setId(u1, 1L);
        User u2 = new User("dev-2", null, "kimminseo", null);
        setId(u2, 2L);
        when(userRepository.findTop20ByUsernameContainingIgnoreCaseOrderByUsernameAsc("kim"))
                .thenReturn(List.of(u1, u2));

        List<UserSearchResultDto> result = userService.searchUsers("kim", null);

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).userId());
        assertEquals("kimjyun", result.get(0).username());
        assertEquals("김지윤", result.get(0).realName());
        assertEquals(2L, result.get(1).userId());
    }

    @Test
    void searchUsers_byEmail_returnsSingleResult() throws Exception {
        User u1 = new User("dev-1", null, "kimjyun", null);
        setId(u1, 1L);
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(u1));

        List<UserSearchResultDto> result = userService.searchUsers(null, "kim@example.com");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
    }

    @Test
    void searchUsers_byEmail_whenNotFound_returnsEmptyList() {
        when(userRepository.findByEmail("nope@example.com")).thenReturn(Optional.empty());

        List<UserSearchResultDto> result = userService.searchUsers(null, "nope@example.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void searchUsers_whenBothEmpty_throwsInvalidRequest() {
        AppException exception = assertThrows(
                AppException.class,
                () -> userService.searchUsers("  ", null)
        );
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    void searchUsers_emailTakesPriorityOverNickname() throws Exception {
        User u1 = new User("dev-1", null, "kimjyun", null);
        setId(u1, 1L);
        when(userRepository.findByEmail("kim@example.com")).thenReturn(Optional.of(u1));

        List<UserSearchResultDto> result = userService.searchUsers("kim", "kim@example.com");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
    }

    private void setId(User user, Long id) throws Exception {
        Field field = User.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
    }
}
