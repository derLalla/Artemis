import { AfterViewInit, Component, EventEmitter, Output } from '@angular/core';
import { Interactable } from '@interactjs/core/Interactable';
import interact from 'interactjs';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { WindowRef } from 'app/core/websocket/window.service';

@Component({
    selector: 'jhi-code-editor-instructions',
    styleUrls: ['./code-editor-instructions.scss'],
    templateUrl: './code-editor-instructions.component.html',
})
export class CodeEditorInstructionsComponent implements AfterViewInit {
    @Output()
    onToggleCollapse = new EventEmitter<{ event: any; horizontal: boolean; interactable: Interactable; resizableMinWidth?: number; resizableMinHeight?: number }>();

    /** Resizable constants **/
    initialInstructionsWidth: number;
    minInstructionsWidth: number;
    interactResizable: Interactable;

    constructor(private $window: WindowRef, public artemisMarkdown: ArtemisMarkdownService) {}

    /**
     * After the view was initialized, we create an interact.js resizable object,
     * designate the edges which can be used to resize the target element and set min and max values.
     * The 'resizemove' callback function processes the event values and sets new width and height values for the element.
     */
    ngAfterViewInit(): void {
        this.initialInstructionsWidth = this.$window.nativeWindow.screen.width - 300 / 2;
        this.minInstructionsWidth = this.$window.nativeWindow.screen.width / 4 - 50;
        this.interactResizable = interact('.resizable-instructions');
    }

    /**
     * Calls the parent (editorComponent) toggleCollapse method
     * @param $event - any event
     */
    toggleEditorCollapse($event: any) {
        this.onToggleCollapse.emit({ event: $event, horizontal: true, interactable: this.interactResizable, resizableMinWidth: this.minInstructionsWidth });
    }
}
