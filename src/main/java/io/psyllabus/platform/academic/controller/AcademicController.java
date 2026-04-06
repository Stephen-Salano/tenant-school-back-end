package io.psyllabus.platform.academic.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/academic")
@RequiredArgsConstructor
@Tag(name = "Academic Structure", description = "Manage academic years and terms")
public class AcademicController {

}
