package de.tum.in.www1.artemis.web.rest;

import java.time.Instant;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.tum.in.www1.artemis.versioning.IgnoreGlobalMapping;

@RestController
public class TimeResource {

    /**
     * {@code GET /time}:
     * @return the current server time as Instant
     */
    @IgnoreGlobalMapping
    @GetMapping("/time")
    public ResponseEntity<Instant> time() {
        return ResponseEntity.ok(Instant.now());
    }
}
