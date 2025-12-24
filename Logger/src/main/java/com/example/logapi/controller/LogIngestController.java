package com.example.logapi.controller;

import com.example.logapi.model.LogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/ingest")
@Validated
public class LogIngestController {

    private static final Logger log = LoggerFactory.getLogger(LogIngestController.class);
    private final JdbcTemplate jdbc;

    public LogIngestController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostMapping("/syslog")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void ingest(@Valid @RequestBody LogEvent e) {
        log.info("Received log event: host={}, program={}, severity={}, facility={}", 
                 e.host(), e.program(), e.severity(), e.facility());
        try {
            int result = jdbc.update("""
                    INSERT INTO system_logs
                    (received_at, hostname, program, severity, facility, message)
                    VALUES (now(), ?, ?, ?, ?, ?)
                    """,
                    e.host(), e.program(), e.severity(), e.facility(), e.message());
            log.info("Log event inserted successfully, rows affected: {}", result);
        } catch (Exception ex) {
            log.error("Failed to insert log event", ex);
            throw ex;
        }
    }
}
