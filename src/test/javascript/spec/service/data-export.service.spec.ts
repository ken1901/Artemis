import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { DataExportService } from 'app/core/legal/data-export/data-export.service';
import { DataExport } from 'app/entities/data-export.model';
import { User } from 'app/core/user/user.model';
import dayjs from 'dayjs';
import { MockTranslateService } from '../helpers/mocks/service/mock-translate.service';
import { TranslateService } from '@ngx-translate/core';
import { MockSyncStorage } from '../helpers/mocks/service/mock-sync-storage.service';
import { SessionStorageService } from 'ngx-webstorage';
import { MockAccountService } from '../helpers/mocks/service/mock-account.service';
import { AccountService } from 'app/core/auth/account.service';

describe('DataExportService', () => {
    let service: DataExportService;
    let httpMock: HttpTestingController;
    let accountService: AccountService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [HttpClientTestingModule],
            providers: [
                { provide: TranslateService, useClass: MockTranslateService },

                { provide: SessionStorageService, useClass: MockSyncStorage },
                { provide: AccountService, useClass: MockAccountService },
            ],
        });
        service = TestBed.inject(DataExportService);
        httpMock = TestBed.inject(HttpTestingController);
        accountService = TestBed.inject(AccountService);
    });

    it('should make PUT request to request data export', fakeAsync(() => {
        const dataExport = new DataExport();
        dataExport.user = new User();
        dataExport.user.id = 1;
        dataExport.id = 1;
        dataExport.requestDate = dayjs();
        dataExport.creationDate = dayjs();
        accountService.userIdentity = dataExport.user;
        let req;
        service.requestDataExport().subscribe((resp) => expect(resp).toEqual(dataExport));
        req = httpMock.expectOne({ method: 'PUT', url: `api/1/data-export` });
        req.flush(dataExport);
        tick();
    }));

    it('should make GET request to download data export', fakeAsync(() => {
        const user = new User();
        user.id = 1;
        accountService.userIdentity = user;
        service.downloadDataExport(1).subscribe();
        const req = httpMock.expectOne({ method: 'GET', url: `api/1/data-export/1` });
        req.flush(new Blob());
        tick();
    }));
});