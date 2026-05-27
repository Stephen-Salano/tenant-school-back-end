package io.psyllabus.platform.academic.controller;

import io.psyllabus.platform.academic.dto.response.TeacherAssignmentResponse;
import io.psyllabus.platform.academic.enums.AssignmentStatus;
import io.psyllabus.platform.academic.service.TeacherAssignmentService;
import io.psyllabus.platform.auth.entities.User;
import io.psyllabus.platform.common.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TeacherAssignmentControllerTest {

    private final TeacherAssignmentService teacherAssignmentService = mock(TeacherAssignmentService.class);
    private final TeacherAssignmentController controller = new TeacherAssignmentController(teacherAssignmentService);

    @Test
    void getMyAssignments_ShouldResolveUserIdAndReturnAssignments() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.builder().id(userId).username("teacher").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(user, null);

        TeacherAssignmentResponse response = new TeacherAssignmentResponse(
                UUID.randomUUID(), UUID.randomUUID(), userId, "John Doe",
                UUID.randomUUID(), "Grade 1", UUID.randomUUID(), "2026", 5L,
                UUID.randomUUID(), "Math", UUID.randomUUID(), "Term 1",
                false, 5, AssignmentStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now()
        );

        when(teacherAssignmentService.getAssignmentsByUser(userId)).thenReturn(List.of(response));

        // Act
        ResponseEntity<ApiResponse<List<TeacherAssignmentResponse>>> entity = controller.getMyAssignments(authentication);

        // Assert
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(1, entity.getBody().data().size());
        assertEquals("John Doe", entity.getBody().data().get(0).staffName());
        assertEquals("2026", entity.getBody().data().get(0).academicYearName());
        assertEquals(5L, entity.getBody().data().get(0).enrolledCount());
        verify(teacherAssignmentService).getAssignmentsByUser(userId);
    }
}
