import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy } from '@angular/core';
import { Theme, ThemeService } from 'app/core/theme/theme.service';
import { Subscription } from 'rxjs';
import { EmojiUtils } from 'app/shared/metis/emoji/emoji.utils';

@Component({
    selector: 'jhi-emoji',
    templateUrl: './emoji.component.html',
    styleUrls: ['./emoji.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmojiComponent implements OnDestroy {
    utils = EmojiUtils;

    @Input() emoji: string;

    dark = false;
    themeSubscription: Subscription;

    constructor(private themeService: ThemeService, private cdr: ChangeDetectorRef) {
        this.themeSubscription = themeService.getCurrentThemeObservable().subscribe((theme) => {
            this.dark = theme === Theme.DARK;
            this.cdr.detectChanges();
        });
    }

    ngOnDestroy(): void {
        this.themeSubscription.unsubscribe();
    }
}
