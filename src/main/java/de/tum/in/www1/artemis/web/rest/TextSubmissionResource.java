package de.tum.in.www1.artemis.web.rest;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import de.tum.in.www1.artemis.domain.*;
import de.tum.in.www1.artemis.service.*;
import de.tum.in.www1.artemis.web.rest.errors.AccessForbiddenException;
import de.tum.in.www1.artemis.web.rest.errors.BadRequestAlertException;
import io.github.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing TextSubmission.
 */
@RestController
@RequestMapping("/api")
public class TextSubmissionResource extends GenericSubmissionResource<TextSubmission, TextExercise> {

    private static final String ENTITY_NAME = "textSubmission";

    private final Logger log = LoggerFactory.getLogger(TextSubmissionResource.class);

    private final TextExerciseService textExerciseService;

    private final TextSubmissionService textSubmissionService;

    public TextSubmissionResource(ExerciseService exerciseService, TextExerciseService textExerciseService, CourseService courseService, ParticipationService participationService,
            TextSubmissionService textSubmissionService, UserService userService, AuthorizationCheckService authCheckService) {
        super(courseService, authCheckService, userService, exerciseService, participationService);
        this.textExerciseService = textExerciseService;
        this.textSubmissionService = textSubmissionService;
    }

    /**
     * POST /exercises/{exerciseId}/text-submissions : Create a new textSubmission. This is called when a student saves his/her answer
     *
     * @param exerciseId     the id of the exercise for which to init a participation
     * @param principal      the current user principal
     * @param textSubmission the textSubmission to create
     * @return the ResponseEntity with status 200 (OK) and the Result as its body, or with status 4xx if the request is invalid
     */
    @PostMapping("/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<TextSubmission> createTextSubmission(@PathVariable Long exerciseId, Principal principal, @RequestBody TextSubmission textSubmission) {
        log.debug("REST request to save TextSubmission : {}", textSubmission);
        if (textSubmission.getId() != null) {
            throw new BadRequestAlertException("A new textSubmission cannot already have an ID", ENTITY_NAME, "idexists");
        }

        return handleTextSubmission(exerciseId, principal, textSubmission);
    }

    /**
     * PUT /exercises/{exerciseId}/text-submissions : Updates an existing textSubmission. This function is called by the text editor for saving and submitting text submissions. The
     * submit specific handling occurs in the TextSubmissionService.save() function.
     *
     * @param exerciseId     the id of the exercise for which to init a participation
     * @param principal      the current user principal
     * @param textSubmission the textSubmission to update
     * @return the ResponseEntity with status 200 (OK) and with body the updated textSubmission, or with status 400 (Bad Request) if the textSubmission is not valid, or with status
     *         500 (Internal Server Error) if the textSubmission couldn't be updated
     */
    @PutMapping("/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('USER', 'TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<TextSubmission> updateTextSubmission(@PathVariable Long exerciseId, Principal principal, @RequestBody TextSubmission textSubmission) {
        log.debug("REST request to update TextSubmission : {}", textSubmission);
        if (textSubmission.getId() == null) {
            return createTextSubmission(exerciseId, principal, textSubmission);
        }

        return handleTextSubmission(exerciseId, principal, textSubmission);
    }

    @NotNull
    private ResponseEntity<TextSubmission> handleTextSubmission(@PathVariable Long exerciseId, Principal principal, @RequestBody TextSubmission textSubmission) {
        TextExercise textExercise = textExerciseService.findOne(exerciseId);
        ResponseEntity<TextSubmission> responseFailure = this.checkExerciseValidityForStudent(textExercise);
        if (responseFailure != null) {
            return responseFailure;
        }

        textSubmission = textSubmissionService.handleTextSubmission(textSubmission, textExercise, principal);

        this.textSubmissionService.hideDetails(textSubmission);
        return ResponseEntity.ok(textSubmission);
    }

    /**
     * GET /text-submissions : get all the textSubmissions for an exercise. It is possible to filter, to receive only the one that have been already submitted, or only the one
     * assessed by the tutor who is doing the call
     *
     * @param exerciseId exerciseID  for which all submissions should be returned
     * @param submittedOnly mark if only submitted Submissions should be returned
     * @param assessedByTutor mark if only assessed Submissions should be returned
     * @return the ResponseEntity with status 200 (OK) and the list of textSubmissions in body
     */
    @GetMapping(value = "/exercises/{exerciseId}/text-submissions")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    public ResponseEntity<List<TextSubmission>> getAllTextSubmissions(@PathVariable Long exerciseId, @RequestParam(defaultValue = "false") boolean submittedOnly,
            @RequestParam(defaultValue = "false") boolean assessedByTutor) {
        log.debug("REST request to get all TextSubmissions");
        Exercise exercise = exerciseService.findOne(exerciseId);

        if (!authCheckService.isAtLeastTeachingAssistantForExercise(exercise)) {
            throw new AccessForbiddenException("You are not allowed to access this resource");
        }

        List<TextSubmission> textSubmissions;
        if (assessedByTutor) {
            User user = userService.getUserWithGroupsAndAuthorities();
            textSubmissions = textSubmissionService.getAllTextSubmissionsByTutorForExercise(exerciseId, user.getId());
        }
        else {
            textSubmissions = textSubmissionService.getSubmissions(exerciseId, submittedOnly, TextSubmission.class);
        }

        // tutors should not see information about the student of a submission
        if (!authCheckService.isAtLeastInstructorForExercise(exercise)) {
            textSubmissions.forEach(textSubmission -> {
                if (textSubmission.getParticipation() != null && textSubmission.getParticipation() instanceof StudentParticipation) {
                    ((StudentParticipation) textSubmission.getParticipation()).filterSensitiveInformation();
                }
            });
        }

        return ResponseEntity.ok().body(textSubmissions);
    }

    /**
     * GET /text-submission-without-assessment : get one textSubmission without assessment.
     *
     * @param exerciseId exerciseID  for which a submission should be returned
     * @return the ResponseEntity with status 200 (OK) and the list of textSubmissions in body
     */
    @GetMapping(value = "/exercises/{exerciseId}/text-submission-without-assessment")
    @PreAuthorize("hasAnyRole('TA', 'INSTRUCTOR', 'ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<TextSubmission> getTextSubmissionWithoutAssessment(@PathVariable Long exerciseId) {
        log.debug("REST request to get a text submission without assessment");
        Exercise exercise = exerciseService.findOne(exerciseId);

        var exerciseValid = this.checkExerciseValidityForTutor(exercise, TextExercise.class);
        if (exerciseValid != null) {
            return exerciseValid;
        }

        // Check if the limit of simultaneously locked submissions has been reached
        textSubmissionService.checkSubmissionLockLimit(exercise.getCourse().getId());

        Optional<TextSubmission> textSubmissionWithoutAssessment = this.textSubmissionService.getTextSubmissionWithoutManualResult((TextExercise) exercise);

        // tutors should not see information about the student of a submission
        if (textSubmissionWithoutAssessment.isPresent() && textSubmissionWithoutAssessment.get().getParticipation() != null
                && textSubmissionWithoutAssessment.get().getParticipation() instanceof StudentParticipation) {
            ((StudentParticipation) textSubmissionWithoutAssessment.get().getParticipation()).filterSensitiveInformation();
        }

        return ResponseUtil.wrapOrNotFound(textSubmissionWithoutAssessment);
    }
}
