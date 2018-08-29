package app.vyh.services.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vic on 8/28/2018
 **/
@RestController
public class LibraryController {
    @GetMapping
    public Boolean isRunning() {
        return true;
    }
}
