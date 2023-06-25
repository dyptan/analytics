package com.dyptan;

import com.dyptan.controller.LoginController;
import com.dyptan.repository.UserRepository;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.CoreMatchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Ignore
@RunWith(SpringRunner.class)
@WebMvcTest(LoginController.class)
public class UITest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ApplicationContext applicationContext;

    @MockBean
    UserRepository userRepository;

    @Ignore
    @Test
    public void getLoginPage() throws Exception {
        this.mvc.perform(get("/login"))
//                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                .characterEncoding("UTF-8")
//                .content("userName=Guest"))
                .andDo(print())
                .andExpect(status().isOk())
//                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string(containsString("html")))
                .andReturn().getResponse();
    }

}