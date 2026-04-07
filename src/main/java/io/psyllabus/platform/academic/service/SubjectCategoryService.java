package io.psyllabus.platform.academic.service;

import io.psyllabus.platform.academic.dto.request.SubjectCategoryCreateRequest;
import io.psyllabus.platform.academic.dto.response.SubjectCategoryResponse;
import java.util.List;

/**
 * Manages subject categories (e.g. Core, Elective, Practical).
 */
public interface SubjectCategoryService {
    /**
     * Create a new subject category.
     * @param request category details
     * @return the created category
     */
    SubjectCategoryResponse createCategory(SubjectCategoryCreateRequest request);

    /**
     * List all categories for the current tenant.
     * @return list of categories
     */
    List<SubjectCategoryResponse> listCategories();
}
