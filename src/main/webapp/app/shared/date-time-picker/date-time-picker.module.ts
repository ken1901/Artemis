import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { OWL_DATE_TIME_FORMATS, OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';
import { ArtemisSharedComponentModule } from 'app/shared/components/shared-component.module';
import { ArtemisSharedModule } from 'app/shared/shared.module';
import { FormDateTimePickerComponent } from './date-time-picker.component';

export const MY_NATIVE_FORMATS = {
    fullPickerInput: { year: 'numeric', month: 'short', day: 'numeric', hour: 'numeric', minute: 'numeric', second: 'numeric' },
    datePickerInput: { year: 'numeric', month: 'numeric', day: 'numeric' },
    timePickerInput: { hour: 'numeric', minute: 'numeric' },
    monthYearLabel: { year: 'numeric', month: 'short' },
    dateA11yLabel: { year: 'numeric', month: 'long', day: 'numeric' },
    monthYearA11yLabel: { year: 'numeric', month: 'long' },
};
@NgModule({
    imports: [CommonModule, FormsModule, OwlDateTimeModule, OwlNativeDateTimeModule, ReactiveFormsModule, ArtemisSharedModule, ArtemisSharedComponentModule],
    exports: [FormDateTimePickerComponent],
    declarations: [FormDateTimePickerComponent],
    providers: [{ provide: OWL_DATE_TIME_FORMATS, useValue: MY_NATIVE_FORMATS }],
})
export class FormDateTimePickerModule {}
