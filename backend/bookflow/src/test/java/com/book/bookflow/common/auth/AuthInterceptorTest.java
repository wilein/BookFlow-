package com.book.bookflow.common.auth;

import com.book.bookflow.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuthInterceptorTest {

    @Test
    void devLoginRequiresExplicitSwitch() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(UserService.class));
        ReflectionTestUtils.setField(interceptor, "devLoginEnabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/auth/dev-login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertTrue(response.getContentAsString().contains("\"code\":\"401\""));
    }

    @Test
    void devLoginIsPublicWhenExplicitlyEnabled() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(UserService.class));
        ReflectionTestUtils.setField(interceptor, "devLoginEnabled", true);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/user/auth/dev-login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void publicBookBrowseDoesNotRequireToken() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(UserService.class));
        ReflectionTestUtils.setField(interceptor, "devLoginEnabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/book/detail");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
    }

    @Test
    void studentCardUploadPathIsNeverPublic() throws Exception {
        AuthInterceptor interceptor = new AuthInterceptor(mock(UserService.class));
        ReflectionTestUtils.setField(interceptor, "devLoginEnabled", false);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/uploads/profile/student-card/a.jpg");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));
        assertTrue(response.getContentAsString().contains("\"code\":\"401\""));
    }
}
