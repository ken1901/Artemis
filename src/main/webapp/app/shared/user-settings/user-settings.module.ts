import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { AccountInformationComponent } from 'app/shared/user-settings/account-information/account-information.component';
import { NotificationSettingsComponent } from 'app/shared/user-settings/notification-settings/notification-settings.component';
import { UserSettingsContainerComponent } from 'app/shared/user-settings/user-settings-container/user-settings-container.component';
import { userSettingsState } from 'app/shared/user-settings/user-settings.route';

@NgModule({
    imports: [RouterModule.forChild(userSettingsState), ArtemisSharedModule],
    declarations: [UserSettingsContainerComponent, AccountInformationComponent, NotificationSettingsComponent],
})
export class UserSettingsModule {}
