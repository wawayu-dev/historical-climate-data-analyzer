package com.example.weather.controller;

import com.example.weather.store.WeatherDataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WeatherControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WeatherDataStore dataStore;

    @BeforeEach
    void resetStore() {
        dataStore.replace(com.example.weather.store.WeatherSnapshot.empty());
    }

    @Test
    void uploadsFiltersAndExportsData() throws Exception {
        String csv = "日期,地区,气温\n"
                + "2020-01-01,北京,0\n2021-01-01,北京,10\n"
                + "2020-01-01,上海,20\n2021-01-01,上海,30\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "weather.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/weather/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.resultCount").value(4));

        mockMvc.perform(get("/api/weather/results")
                        .param("regions", "北京")
                        .param("startYear", "2021")
                        .param("endYear", "2021")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].region").value("北京"))
                .andExpect(jsonPath("$.data[0].anomaly").value(5.0));

        mockMvc.perform(get("/api/weather/export").param("regions", "上海"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"temperature-anomaly.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("上海,20.00,25.00,-5.00")));
    }

    @Test
    void rejectsInvalidMonth() throws Exception {
        mockMvc.perform(get("/api/weather/results").param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
