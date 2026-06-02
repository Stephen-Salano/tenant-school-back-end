package io.psyllabus.platform.student.dto.request;

import io.psyllabus.platform.student.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentUpdateRequest (
        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName,

        @Past
        LocalDate dateOfBirth,

        Gender gender,

        String medicalConditions,

        String photoUrl
){
}
