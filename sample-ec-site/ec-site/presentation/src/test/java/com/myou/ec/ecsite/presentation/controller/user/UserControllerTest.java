package com.myou.ec.ecsite.presentation.controller.user;

import com.myou.ec.ecsite.application.service.user.UserService;
import com.myou.ec.ecsite.domain.model.user.MailAddress;
import com.myou.ec.ecsite.domain.model.user.User;
import com.myou.ec.ecsite.domain.model.user.UserName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;


@WebMvcTest(controllers = UserController.class)
@AutoConfigureObservability
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UserService userService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void show() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/users/"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("user/index"));
    }

    @Test
    void registerUser() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .param("userName", "aaa")
                        .param("mailAddress", "bbb"))
                .andExpect(MockMvcResultMatchers.status().isFound())
                .andExpect(MockMvcResultMatchers.redirectedUrl("/users/list"));
    }

    @Test
    void list() throws Exception {
        BDDMockito.given(userService.findAllUsers())
                .willReturn(List.of(new User(1L, new UserName("user1"), new MailAddress("test@mail.com"))));
        mvc.perform(MockMvcRequestBuilders.get("/users/list"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.view().name("user/list"));
    }
}
