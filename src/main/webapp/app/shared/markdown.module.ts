import { NgModule } from '@angular/core';
import { MarkdownEditorComponent } from 'app/shared/markdown-editor/markdown-editor.component';
import { ArtemisMarkdownEditorModule } from 'app/shared/markdown-editor/markdown-editor.module';
import { ArtemisMarkdownService } from 'app/shared/markdown.service';
import { HtmlForMarkdownPipe } from 'app/shared/pipes/html-for-markdown.pipe';

@NgModule({
    imports: [ArtemisMarkdownEditorModule],
    declarations: [HtmlForMarkdownPipe],
    providers: [ArtemisMarkdownService],
    exports: [MarkdownEditorComponent, HtmlForMarkdownPipe],
})
export class ArtemisMarkdownModule {}
