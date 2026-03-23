package io.psyllabus.platform.school.enums;

/**
 * Drives auto-generation of academic terms in AcademicTermServiceImpl.
 * CBC → 3 terms (Jan–Apr, May–Aug, Sep–Dec)
 * IGCSE → 2 terms (Sep–Dec, Jan–Jun)
 */
public enum CurriculumType {
   /**
    * Kenya Competency-Based Curriculum.
    * Default for Kenyan schools. Generates 3 terms.
    * Triggers seeding of the 7 CBC Learning Areas when
    * {@code SchoolProfile.useSystemLearningAreas = true}.
    */
   CBC,

   /**
    * International General Certificate of Secondary Education.
    * Generates 2 terms. No CBC Learning Area seeding.
    */
   IGCSE
}
