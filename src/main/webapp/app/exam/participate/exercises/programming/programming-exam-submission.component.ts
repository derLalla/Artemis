import { Component, Input, ViewChild, OnInit } from '@angular/core';
import { ExamSubmissionComponent } from 'app/exam/participate/exercises/exam-submission.component';
import { ProgrammingExerciseStudentParticipation } from 'app/entities/participation/programming-exercise-student-participation.model';
import { ButtonSize, ButtonType } from 'app/shared/components/button.component';
import { ProgrammingExercise } from 'app/entities/programming-exercise.model';
import { CommitState, DomainType, EditorState } from 'app/exercises/programming/shared/code-editor/model/code-editor.model';
import { Submission } from 'app/entities/submission.model';
import { Exercise } from 'app/entities/exercise.model';
import { StudentParticipation } from 'app/entities/participation/student-participation.model';
import { DomainService } from 'app/exercises/programming/shared/code-editor/service/code-editor-domain.service';
import * as moment from 'moment';
import { CodeEditorContainerComponent } from 'app/exercises/programming/shared/code-editor/container/code-editor-container.component';

@Component({
    selector: 'jhi-programming-submission-exam',
    templateUrl: './programming-exam-submission.component.html',
    providers: [{ provide: ExamSubmissionComponent, useExisting: ProgrammingExamSubmissionComponent }],
    styleUrls: ['./programming-exam-submission.component.scss'],
})
export class ProgrammingExamSubmissionComponent extends ExamSubmissionComponent implements OnInit {
    @ViewChild(CodeEditorContainerComponent, { static: false }) codeEditorContainer: CodeEditorContainerComponent;

    // IMPORTANT: this reference must be activeExercise.studentParticipation[0] otherwise the parent component will not be able to react to change
    @Input()
    studentParticipation: ProgrammingExerciseStudentParticipation;
    @Input()
    exercise: ProgrammingExercise;
    @Input()
    courseId: number;

    repositoryIsLocked = false;
    showEditorInstructions = true;

    constructor(private domainService: DomainService) {
        super();
    }

    getSubmission(): Submission | null {
        if (this.studentParticipation && this.studentParticipation.submissions && this.studentParticipation.submissions.length > 0) {
            return this.studentParticipation.submissions[0];
        }
        return null;
    }

    getExercise(): Exercise {
        return this.exercise;
    }

    isSaving: boolean;
    readonly ButtonType = ButtonType;
    readonly ButtonSize = ButtonSize;

    /**
     * On init set up the route param subscription.
     * Will load the participation according to participation Id with the latest result and result details.
     */
    ngOnInit(): void {
        // We lock the repository when the buildAndTestAfterDueDate is set and the due date has passed.
        const dueDateHasPassed = !this.exercise.dueDate || moment(this.exercise.dueDate).isBefore(moment());
        this.repositoryIsLocked = !!this.exercise.buildAndTestStudentSubmissionsAfterDueDate && !!this.exercise.dueDate && dueDateHasPassed;

        const participation = { ...this.studentParticipation, exercise: this.exercise } as StudentParticipation;
        this.domainService.setDomain([DomainType.PARTICIPATION, participation]);
    }

    reload(): void {
        // this.ngOnInit();
        // if (this.instructions) {
        //     this.instructions.refreshInstructions();
        // }
    }

    /**
     * Update {@link Submission#isSynced} & {@link Submission#submitted} based on the CommitState.
     * The submission is only synced, if all changes are committed (CommitState.CLEAN).
     *
     * @param commitState current CommitState from CodeEditorActionsComponent
     */
    onCommitStateChange(commitState: CommitState): void {
        if (this.studentParticipation.submissions && this.studentParticipation.submissions.length > 0) {
            if (commitState === CommitState.CLEAN) {
                this.studentParticipation.submissions[0].submitted = true;
                this.studentParticipation.submissions[0].isSynced = true;
            }
        }
    }

    onFileChanged() {
        if (this.studentParticipation.submissions && this.studentParticipation.submissions.length > 0) {
            this.studentParticipation.submissions[0].isSynced = false;
        }
    }

    hasUnsavedChanges(): boolean {
        if (this.exercise.allowOfflineIde && !this.exercise.allowOnlineEditor) {
            return false;
        }
        return this.codeEditorContainer.editorState === EditorState.UNSAVED_CHANGES;
    }

    onActivate(): void {
        this.reload();
    }

    updateSubmissionFromView(): void {
        // Note: we just save here and do not commit, because this can lead to problems!
        this.codeEditorContainer.actions.onSave();
    }
}
