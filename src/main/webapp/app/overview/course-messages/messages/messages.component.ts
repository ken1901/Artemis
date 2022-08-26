import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnDestroy, Output, QueryList, ViewChild, ViewChildren } from '@angular/core';
import { faCircleNotch, faEnvelope } from '@fortawesome/free-solid-svg-icons';
import { MetisService } from 'app/shared/metis/metis.service';
import { PostOverviewDirective } from 'app/shared/metis/post-overview.directive';
import { Conversation } from 'app/entities/metis/conversation/conversation.model';
import { Subscription } from 'rxjs';
import { Post } from 'app/entities/metis/post.model';

@Component({
    selector: 'jhi-messages',
    templateUrl: './messages.component.html',
    styleUrls: ['./messages.component.scss'],
    providers: [MetisService],
})
export class MessagesComponent extends PostOverviewDirective implements AfterViewInit, OnDestroy {
    @Output() openThread = new EventEmitter<Post>();

    @ViewChildren('postingThread') messages: QueryList<any>;
    @ViewChild('container') content: ElementRef;
    private previousScrollDistanceFromTop: number;
    // as set for the css class '.posting-infinite-scroll-container'
    protected messagesContainerHeight = 350;
    isCourseMessagesPage = true;
    inlineInputEnabled = true;

    private scrollBottomSubscription: Subscription;

    // Icons
    faEnvelope = faEnvelope;
    faCircleNotch = faCircleNotch;

    /**
     * receives and updates the selected conversation, fetching its posts
     * @param activeConversation selectedConversation
     */
    @Input() set activeConversation(activeConversation: Conversation) {
        if (activeConversation) {
            this.conversation = activeConversation;
            if (this.course) {
                this.onSelectContext();
                this.createEmptyPost();
            }
        }
    }

    processReceivedPosts(posts: Post[]): void {
        this.posts = posts.slice().reverse();
        this.previousScrollDistanceFromTop = this.content.nativeElement.scrollHeight - this.content.nativeElement.scrollTop;
    }

    /**
     * subscribes to changes in the message container, in order display the bottom of the list at the messages page
     */
    ngAfterViewInit() {
        this.scrollBottomSubscription = this.messages.changes.subscribe(this.handleScrollOnNewMessage);
    }

    ngOnDestroy(): void {
        this.scrollBottomSubscription?.unsubscribe();
    }

    /**
     * scrolls to the bottom of the message container if in messages page and posts exist for the selected conversation and
     * only the first page is fetched or user is scrolled to the last post of the conversation
     */
    handleScrollOnNewMessage = () => {
        if (
            this.posts.length > 0 &&
            ((this.content.nativeElement.scrollTop === 0 && this.page === 1) || Math.ceil(this.previousScrollDistanceFromTop) === this.messagesContainerHeight)
        ) {
            this.scrollToBottomOfMessages();
        }
    };

    scrollToBottomOfMessages() {
        this.content.nativeElement.scrollTop = this.content.nativeElement.scrollHeight;
    }

    fetchLecturesAndExercisesOfCourse(): void {}
}