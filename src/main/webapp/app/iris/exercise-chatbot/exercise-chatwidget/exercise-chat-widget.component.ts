import { AfterViewInit, Component, ElementRef, Inject, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { faCircle, faExpand, faPaperPlane, faThumbsDown, faThumbsUp, faXmark } from '@fortawesome/free-solid-svg-icons';
import { MAT_DIALOG_DATA, MatDialog } from '@angular/material/dialog';
import { IrisStateStore } from 'app/iris/state-store.service';
import { ConversationErrorOccurredAction, NumNewMessagesResetAction, RateMessageSuccessAction, StudentMessageSentAction } from 'app/iris/state-store.model';
import { IrisHttpMessageService } from 'app/iris/http-message.service';
import { IrisClientMessage, IrisMessage, IrisSender, IrisServerMessage, isServerSentMessage, isStudentSentMessage } from 'app/entities/iris/iris-message.model';
import { IrisMessageContent, IrisMessageContentType } from 'app/entities/iris/iris-content-type.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'jhi-exercise-chat-widget',
    templateUrl: './exercise-chat-widget.component.html',
    styleUrls: ['./exercise-chat-widget.component.scss'],
})
export class ExerciseChatWidgetComponent implements OnInit, OnDestroy, AfterViewInit {
    @ViewChild('chatWidget') chatWidget!: ElementRef;
    @ViewChild('chatBody') chatBody!: ElementRef;
    @ViewChild('unreadMessage', { static: false }) unreadMessage!: ElementRef;

    readonly SENDER_USER = IrisSender.USER;
    readonly SENDER_SERVER = IrisSender.LLM;
    readonly stateStore: IrisStateStore;

    stateSubscription: Subscription;
    messages: IrisMessage[] = [];
    newMessageTextContent = '';
    isLoading: boolean;
    sessionId: number;
    numNewMessages = 0;
    unreadMessageIndex: number;
    error = '';
    dots = 1;

    constructor(private dialog: MatDialog, @Inject(MAT_DIALOG_DATA) public data: any, private httpMessageService: IrisHttpMessageService) {
        this.stateStore = data.stateStore;
    }

    // Icons
    faPaperPlane = faPaperPlane;
    faCircle = faCircle;
    faExpand = faExpand;
    faXmark = faXmark;
    faThumbsUp = faThumbsUp;
    faThumbsDown = faThumbsDown;

    ngOnInit() {
        this.animateDots();
        this.stateSubscription = this.stateStore.getState().subscribe((state) => {
            this.messages = state.messages as IrisMessage[];
            this.isLoading = state.isLoading;
            this.error = state.error;
            this.sessionId = Number(state.sessionId);
            this.numNewMessages = state.numNewMessages;
        });
    }

    ngAfterViewInit() {
        this.unreadMessageIndex = this.messages.length === 0 || this.numNewMessages === 0 ? -1 : this.messages.length - this.numNewMessages;
        if (this.numNewMessages > 0) {
            this.scrollToUnread();
        } else {
            this.scrollToBottom('auto');
        }
    }

    ngOnDestroy() {
        this.stateSubscription.unsubscribe();
    }

    animateDots() {
        setInterval(() => {
            this.dots = this.dots < 3 ? (this.dots += 1) : (this.dots = 1);
        }, 500);
    }

    onSend(): void {
        if (this.newMessageTextContent) {
            const message = this.newUserMessage(this.newMessageTextContent);
            this.stateStore
                .dispatchAndThen(new StudentMessageSentAction(message))
                .then(() => this.httpMessageService.createMessage(<number>this.sessionId, message).toPromise())
                .then(() => this.scrollToBottom('smooth'))
                .catch(() => this.stateStore.dispatch(new ConversationErrorOccurredAction('Something went wrong. Please try again later!')));
            this.newMessageTextContent = '';
        }
        this.scrollToBottom('smooth');
    }

    scrollToBottom(behavior: ScrollBehavior) {
        setTimeout(() => {
            const chatBodyElement: HTMLElement = this.chatBody.nativeElement;
            chatBodyElement.scrollTo({
                top: chatBodyElement.scrollHeight,
                behavior: behavior,
            });
        });
    }

    scrollToUnread() {
        setTimeout(() => {
            const unreadMessageElement: HTMLElement = this.unreadMessage.nativeElement;
            unreadMessageElement.scrollIntoView({ behavior: 'auto' });
        });
    }

    closeChat() {
        this.stateStore.dispatch(new NumNewMessagesResetAction());
        this.dialog.closeAll();
    }

    rateMessage(message_id: number, index: number, helpful: boolean) {
        this.httpMessageService
            .rateMessage(<number>this.sessionId, message_id, helpful)
            .toPromise()
            .then(() => this.stateStore.dispatch(new RateMessageSuccessAction(index, helpful)))
            .catch(() => {
                this.stateStore.dispatch(new ConversationErrorOccurredAction('Something went wrong. Please try again later!'));
                this.scrollToBottom('smooth');
            });
    }

    isStudentSentMessage(message: IrisMessage): message is IrisClientMessage {
        return isStudentSentMessage(message);
    }

    isServerSentMessage(message: IrisMessage): message is IrisServerMessage {
        return isServerSentMessage(message);
    }

    private newUserMessage(message: string): IrisClientMessage {
        const content: IrisMessageContent = {
            type: IrisMessageContentType.TEXT,
            textContent: message,
        };
        return {
            sender: this.SENDER_USER,
            content: [content],
        };
    }
}