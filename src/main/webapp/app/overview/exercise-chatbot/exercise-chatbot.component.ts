import { Component, OnDestroy } from '@angular/core';
import { faChevronDown, faCommentDots } from '@fortawesome/free-solid-svg-icons';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { ExerciseChatWidgetComponent } from 'app/overview/exercise-chatbot/exercise-chatwidget/exercise-chat-widget.component';
import { Overlay } from '@angular/cdk/overlay';
import { shakeAnimation } from 'angular-animations';

@Component({
    selector: 'jhi-exercise-chatbot',
    templateUrl: './exercise-chatbot.component.html',
    styleUrls: ['./exercise-chatbot.component.scss'],
    animations: [shakeAnimation({ anchor: 'shake', direction: '=>', duration: 700 })],
})
export class ExerciseChatbotComponent implements OnDestroy {
    public chatAccepted = false;
    public buttonDisabled = false;
    dialogRef: MatDialogRef<ExerciseChatWidgetComponent> | null = null;
    chatOpen = false;
    runAnimation = false;

    // Icons
    faCommentDots = faCommentDots;
    faChevronDown = faChevronDown;

    constructor(public dialog: MatDialog, private overlay: Overlay) {}

    ngOnDestroy() {
        if (this.dialogRef) {
            this.dialogRef.close();
        }
    }

    handleButtonClick() {
        if (this.chatOpen && this.dialogRef) {
            this.dialogRef!.close();
            //this.runAnimation = false;
            this.chatOpen = false;
        } else {
            this.openChat();
            //this.runAnimation = true;
        }
    }

    openChat() {
        if (!this.buttonDisabled) {
            this.chatOpen = true;
            this.dialogRef = this.dialog.open(ExerciseChatWidgetComponent, {
                hasBackdrop: false,
                scrollStrategy: this.overlay.scrollStrategies.noop(),
                position: { bottom: '0px', right: '0px' },
            });
            this.dialogRef.afterClosed().subscribe(() => {
                this.buttonDisabled = false;
                this.chatOpen = false;
            });
        }
        this.buttonDisabled = true;
    }
}
