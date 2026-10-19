package ru.terentjev.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.terentjev.service.SettingsService;
import ru.terentjev.service.model.KeyValue;
import ru.terentjev.service.model.Settings;
import ru.terentjev.service.model.SettingsScope;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SettingsControllerTest.TestConfig.class)
class SettingsControllerTest {

    static class TestConfig {
        @Bean
        SimpleMeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private static final UUID DEVICE = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    @Test
    void getSettingsReturnsEffectiveSettings() throws Exception {
        when(settingsService.read(DEVICE, SettingsScope.FB2))
                .thenReturn(new Settings("FB2", List.of(
                        new KeyValue("font", "Georgia"),
                        new KeyValue("size", "28")
                )));

        mockMvc.perform(get("/api/v1/settings")
                        .param("deviceUuid", DEVICE.toString())
                        .param("scope", "FB2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("FB2"))
                .andExpect(jsonPath("$.settings[0].key").value("font"))
                .andExpect(jsonPath("$.settings[0].value").value("Georgia"));
    }

    @Test
    void putSettingsSavesWithAuthenticatedUser() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .with(authenticatedUser("alice"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("scope", "PDF")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"key":"font","value":"Courier"}]
                                """))
                .andExpect(status().isOk());

        verify(settingsService).save(
                eq(DEVICE),
                eq(SettingsScope.PDF),
                org.mockito.ArgumentMatchers.argThat(list ->
                        list.size() == 1
                                && list.get(0).getKey().equals("font")
                                && list.get(0).getValue().equals("Courier")
                ),
                eq("alice")
        );
    }

    @Test
    void getDefinitionsReturnsAvailableSettingsForScope() throws Exception {
        when(settingsService.definitions(SettingsScope.FB2)).thenReturn(List.of("font", "size", "color"));
        when(settingsService.definitions(SettingsScope.PDF)).thenReturn(List.of("color", "background"));

        mockMvc.perform(get("/api/v1/settings/definitions").param("scope", "FB2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("font"))
                .andExpect(jsonPath("$[1]").value("size"))
                .andExpect(jsonPath("$[2]").value("color"));

        mockMvc.perform(get("/api/v1/settings/definitions").param("scope", "PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("color"))
                .andExpect(jsonPath("$[1]").value("background"));
    }

    @Test
    void getDefinitionsReturns400WhenScopeInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/settings/definitions").param("scope", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(settingsService, never()).definitions(any());
    }

    @Test
    void getSettingsReturns400WhenScopeInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/settings")
                        .param("deviceUuid", DEVICE.toString())
                        .param("scope", "UNKNOWN"))
                .andExpect(status().isBadRequest());

        verify(settingsService, never()).read(any(), any());
    }

    @Test
    void getSettingsReturns400WhenDeviceUuidMissing() throws Exception {
        mockMvc.perform(get("/api/v1/settings")
                        .param("scope", "FB2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putSettingsReturns400WhenBodyEmpty() throws Exception {
        mockMvc.perform(put("/api/v1/settings")
                        .with(authenticatedUser("bob"))
                        .param("deviceUuid", DEVICE.toString())
                        .param("scope", "GLOBAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isOk());

        verify(settingsService).save(
                eq(DEVICE),
                eq(SettingsScope.GLOBAL),
                eq(List.of()),
                eq("bob")
        );
    }

    private static RequestPostProcessor authenticatedUser(String username) {
        return request -> {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            username, null, java.util.Collections.emptyList()
                    ));
            return request;
        };
    }

}
