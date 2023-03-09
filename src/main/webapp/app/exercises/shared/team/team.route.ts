import { Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core/auth/user-route-access-service';
import { TeamComponent } from 'app/exercises/shared/team/team.component';
import { Authority } from 'app/shared/constants/authority.constants';
import { TeamsComponent } from './teams.component';

export const teamRoute: Routes = [
    {
        path: '',
        component: TeamsComponent,
        data: {
            authorities: [Authority.TA, Authority.EDITOR, Authority.INSTRUCTOR, Authority.ADMIN],
            pageTitle: 'artemisApp.team.home.title',
        },
        canActivate: [UserRouteAccessService],
    },
    {
        path: ':teamId',
        component: TeamComponent,
        data: {
            authorities: [Authority.USER],
            pageTitle: 'artemisApp.team.detail.title',
        },
        canActivate: [UserRouteAccessService],
    },
];
