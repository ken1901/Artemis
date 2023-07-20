import { MockHttpService } from '../helpers/mocks/service/mock-http.service';
import { LearningPathService } from 'app/course/learning-paths/learning-path.service';
import { ArtemisTestModule } from '../test.module';
import { HttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';

describe('LearningPathService', () => {
    let learningPathService: LearningPathService;
    let httpService: HttpClient;
    let putStub: jest.SpyInstance;
    let getStub: jest.SpyInstance;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [ArtemisTestModule],
            providers: [{ provide: HttpClient, useClass: MockHttpService }],
        })
            .compileComponents()
            .then(() => {
                httpService = TestBed.inject(HttpClient);
                learningPathService = new LearningPathService(httpService);
                putStub = jest.spyOn(httpService, 'put');
                getStub = jest.spyOn(httpService, 'get');
            });
    });

    afterEach(() => {
        jest.restoreAllMocks();
    });

    it('should send a request to the server to enable learning paths for course', () => {
        learningPathService.enableLearningPaths(1).subscribe();
        expect(putStub).toHaveBeenCalledOnce();
        expect(putStub).toHaveBeenCalledWith('api/courses/1/learning-paths/enable', null, { observe: 'response' });
    });

    it('should send a request to the server to get learning path id of the current user in the course', () => {
        learningPathService.getLearningPathId(1).subscribe();
        expect(getStub).toHaveBeenCalledOnce();
        expect(getStub).toHaveBeenCalledWith('api/courses/1/learning-path-id', { observe: 'response' });
    });

    it('should send a request to the server to get ngx representation of learning path', () => {
        learningPathService.getNgxLearningPath(1).subscribe();
        expect(getStub).toHaveBeenCalledOnce();
        expect(getStub).toHaveBeenCalledWith('api/learning-path/1', { observe: 'response' });
    });

    it('should send a request to the server to get recommendation for learning path', () => {
        learningPathService.getRecommendation(1).subscribe();
        expect(getStub).toHaveBeenCalledOnce();
        expect(getStub).toHaveBeenCalledWith('api/learning-path/1/recommendation', { observe: 'response' });
    });
});
