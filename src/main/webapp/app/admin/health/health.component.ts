import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { faEye, faSync } from '@fortawesome/free-solid-svg-icons';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { Health, HealthDetails, HealthStatus } from 'app/admin/health/health.model';
import { HealthModalComponent } from './health-modal.component';
import { HealthService } from './health.service';

@Component({
    selector: 'jhi-health',
    templateUrl: './health.component.html',
})
export class HealthComponent implements OnInit {
    health?: Health;

    // Icons
    faSync = faSync;
    faEye = faEye;

    constructor(private modalService: NgbModal, private healthService: HealthService) {}

    ngOnInit() {
        this.refresh();
    }

    getBadgeClass(statusState: HealthStatus) {
        if (statusState === 'UP') {
            return 'bg-success';
        }
        return 'bg-danger';
    }

    refresh(): void {
        this.healthService.checkHealth().subscribe({
            next: (health) => (this.health = health),
            error: (error: HttpErrorResponse) => {
                if (error.status === 503) {
                    this.health = error.error;
                }
            },
        });
    }

    showHealth(health: { key: string; value: HealthDetails }): void {
        const modalRef = this.modalService.open(HealthModalComponent);
        modalRef.componentInstance.health = health;
    }
}
