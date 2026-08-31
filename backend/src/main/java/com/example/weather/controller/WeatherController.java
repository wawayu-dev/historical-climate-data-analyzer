package com.example.weather.controller;

import com.example.weather.dto.ApiResponse;
import com.example.weather.dto.MetadataDTO;
import com.example.weather.dto.UploadResultDTO;
import com.example.weather.model.TemperatureAnomaly;
import com.example.weather.service.WeatherAnalysisService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/weather")
public class WeatherController {
    private final WeatherAnalysisService analysisService;

    public WeatherController(WeatherAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadResultDTO> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success("上传成功", analysisService.upload(file));
    }

    @GetMapping("/metadata")
    public ApiResponse<MetadataDTO> metadata() {
        return ApiResponse.success(analysisService.metadata());
    }

    @GetMapping("/results")
    public ApiResponse<List<TemperatureAnomaly>> results(
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer month
    ) {
        return ApiResponse.success(analysisService.results(regions, startYear, endYear, month));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) Integer startYear,
            @RequestParam(required = false) Integer endYear,
            @RequestParam(required = false) Integer month
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("temperature-anomaly.csv").build());
        return ResponseEntity.ok().headers(headers)
                .body(analysisService.exportCsv(regions, startYear, endYear, month));
    }
}
