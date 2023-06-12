import { IrisStateStore } from 'app/iris/state-store.service';
import {
    ActionType,
    ActiveConversationMessageLoadedAction,
    ConversationErrorOccurredAction,
    HistoryMessageLoadedAction,
    MessageStoreState,
    SessionReceivedAction,
    StudentMessageSentAction,
} from 'app/iris/state-store.model';
import { skip, take } from 'rxjs';
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { mockClientMessage, mockServerMessage, mockState } from '../../helpers/sample/iris-sample-data';
import { IrisErrorMessageKey, errorMessages } from 'app/entities/iris/iris-errors.model';

describe('IrisStateStore', () => {
    let stateStore: IrisStateStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [IrisStateStore],
        });
        stateStore = TestBed.inject(IrisStateStore);
        stateStore.dispatch(new SessionReceivedAction(0, []));
    });

    it('should dispatch and handle HistoryMessageLoadedAction', async () => {
        const action: HistoryMessageLoadedAction = new HistoryMessageLoadedAction(mockServerMessage);

        const obs = stateStore.getState();

        const promise = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action);

        const state = (await promise) as MessageStoreState;

        expect(state).toEqual({
            ...mockState,
            messages: [action.message],
        });
    });

    it('should dispatch and handle ActiveConversationMessageLoadedAction', async () => {
        const action: ActiveConversationMessageLoadedAction = new ActiveConversationMessageLoadedAction(mockServerMessage);

        const obs = stateStore.getState();

        const promise = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action);

        const state = (await promise) as MessageStoreState;

        expect(state).toEqual({
            ...mockState,
            messages: [action.message],
        });
    });

    it('should dispatch and handle StudentMessageSentAction', async () => {
        const action: StudentMessageSentAction = {
            type: ActionType.STUDENT_MESSAGE_SENT,
            message: mockClientMessage,
        };

        const obs = stateStore.getState();

        const promise = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action);

        const state = (await promise) as MessageStoreState;

        expect(state).toEqual({
            ...mockState,
            isLoading: true,
            messages: [action.message],
        });
    });

    it('should dispatch and handle 3 messages', async () => {
        const action1: StudentMessageSentAction = {
            type: ActionType.STUDENT_MESSAGE_SENT,
            message: mockClientMessage,
        };

        const action2: ActiveConversationMessageLoadedAction = {
            type: ActionType.ACTIVE_CONVERSATION_MESSAGE_LOADED,
            message: mockServerMessage,
        };

        const action3: ActiveConversationMessageLoadedAction = {
            type: ActionType.ACTIVE_CONVERSATION_MESSAGE_LOADED,
            message: mockServerMessage,
        };

        const obs = stateStore.getState();

        const promise1 = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action1);

        const state1 = (await promise1) as MessageStoreState;

        expect(state1).toEqual({
            ...mockState,
            isLoading: true,
            messages: [action1.message],
        });

        const promise2 = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action2);

        const state2 = (await promise2) as MessageStoreState;

        expect(state2).toEqual({
            ...mockState,
            messages: [action1.message, action2.message],
        });

        // the observable should only be aware of the previously emitted value
        const promise3 = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action3);

        const state3 = (await promise3) as MessageStoreState;

        expect(state3).toEqual({
            ...mockState,
            messages: [action1.message, action2.message, action3.message],
        });
    });

    it('should dispatch error occurrences', async () => {
        const obs = stateStore.getState();

        const promise = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(new ConversationErrorOccurredAction(errorMessages[IrisErrorMessageKey.HISTORY_LOAD_FAILED]));

        const state = (await promise) as MessageStoreState;

        expect(state).toStrictEqual({
            ...mockState,
            error: errorMessages[IrisErrorMessageKey.HISTORY_LOAD_FAILED],
        });
    });

    it('should proceed with then clause after dispatchAndThen is executed', async () => {
        const action: StudentMessageSentAction = {
            type: ActionType.STUDENT_MESSAGE_SENT,
            message: mockClientMessage,
        };

        await stateStore.dispatchAndThen(action).then(async () => {
            const promise = stateStore.getState().pipe(take(1)).toPromise();
            const state = (await promise) as MessageStoreState;
            expect(state).toEqual({
                ...mockState,
                isLoading: true,
                messages: [action.message],
            });
        });
    });
});

describe('IrisStateStore with an empty session state', () => {
    let stateStore: IrisStateStore;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [IrisStateStore],
        });
        stateStore = TestBed.inject(IrisStateStore);
    });

    it('should not dispatch new message actions with an empty session id', async () => {
        const action: ActiveConversationMessageLoadedAction = {
            type: ActionType.ACTIVE_CONVERSATION_MESSAGE_LOADED,
            message: mockServerMessage,
        };

        const obs = stateStore.getState();

        const promise = obs.pipe(skip(1), take(1)).toPromise();

        stateStore.dispatch(action);

        const state = (await promise) as MessageStoreState;

        expect(state).toStrictEqual({
            ...mockState,
            error: errorMessages[IrisErrorMessageKey.INVALID_SESSION_STATE],
            sessionId: null,
        });
    });
});
