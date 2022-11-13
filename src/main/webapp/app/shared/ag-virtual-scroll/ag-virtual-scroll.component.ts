/**
 * Component based on Open Source Project ag-virtual-scroll
 * https://github.com/ericferreira1992/ag-virtual-scroll
 *
 */

import { AfterContentChecked, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, Renderer2, SimpleChanges, ViewChild } from '@angular/core';
import { AgVsRenderEvent } from 'app/shared/ag-virtual-scroll/ag-vs-render-event.class';

@Component({
    selector: 'ag-virtual-scroll',
    templateUrl: './ag-virtual-scroll.component.html',
    styleUrls: ['ag-virtual-scroll.style.css'],
})
export class AgVirtualScrollComponent implements OnInit, OnChanges, OnDestroy, AfterContentChecked {
    @ViewChild('itemsContainer', { static: true }) private itemsContainerElRef: ElementRef<HTMLElement>;

    @Input('min-row-height') public minRowHeight = 40;
    @Input('height') public height = 'auto';
    @Input('items') public originalItems: any[] = [];

    @Input() forceReload: boolean;
    @Output() forceReloadChange = new EventEmitter<boolean>();

    @Output() private onItemsRender = new EventEmitter<AgVsRenderEvent<any>>();

    public prevOriginalItems: any[] = [];
    public items: any[] = [];

    public currentScroll = 0;
    public contentHeight = 0;
    public paddingTop = 0;

    public startIndex = 0;
    public endIndex = 0;

    private scrollIsUp = false;
    private lastScrollIsUp = false;

    private previousItemsHeight: any[] = [];

    public containerWidth = 0;

    scrollListener: any;

    public get el() {
        return this.elRef && this.elRef.nativeElement;
    }

    public get itemsContainerEl() {
        return this.itemsContainerElRef && this.itemsContainerElRef.nativeElement;
    }

    constructor(private elRef: ElementRef<HTMLElement>, private renderer: Renderer2) {}

    ngOnInit() {
        this.scrollListener = this.renderer.listen(this.el, 'scroll', this.onScroll.bind(this));
        this.el.style.height = this.height;
        this.minRowHeight = Number(this.minRowHeight);
    }

    ngOnChanges(changes: SimpleChanges) {
        setTimeout(() => {
            if ('originalItems' in changes) {
                if (!this.originalItems) {
                    this.originalItems = [];
                }

                if (this.forceReload) {
                    this.previousItemsHeight = new Array(this.originalItems.length).fill(null);

                    if (this.el.scrollTop !== 0) {
                        this.el.scrollTop = 0;
                    } else {
                        this.currentScroll = 0;
                        this.prepareDataItems();
                    }
                } else {
                    if (this.originalItems.length > this.prevOriginalItems.length) {
                        this.previousItemsHeight = this.previousItemsHeight.concat(new Array(this.originalItems.length - this.prevOriginalItems.length).fill(null));
                        this.prepareDataItems();
                    } else {
                        const begin = 0;
                        const end = this.prevOriginalItems.length - 1;

                        for (let i = begin; i <= end; i++) {
                            if (this.originalItems[i] !== this.prevOriginalItems[i]) {
                                this.previousItemsHeight[i] = null;

                                const displayIndex = i - this.startIndex;
                                if (displayIndex >= 0 && displayIndex <= this.endIndex) {
                                    // if item is displayed to the user then it is updated
                                    this.items[displayIndex] = this.originalItems[i];
                                }
                            }
                        }
                    }
                }
                this.prevOriginalItems = this.originalItems;
                this.forceReloadChange.emit((this.forceReload = false));
            }
        });
    }

    ngAfterContentChecked() {
        const currentContainerWidth = this.itemsContainerEl && this.itemsContainerEl.clientWidth;
        if (currentContainerWidth !== this.containerWidth) {
            this.containerWidth = currentContainerWidth;
        }

        this.manipuleRenderedItems();
    }

    private onScroll() {
        const up = this.el.scrollTop < this.currentScroll;
        this.currentScroll = this.el.scrollTop;

        this.prepareDataItems();
        this.lastScrollIsUp = this.scrollIsUp;
        this.scrollIsUp = up;
    }

    private prepareDataItems() {
        this.registerCurrentItemsHeight();
        this.prepareDataVirtualScroll();
    }

    private registerCurrentItemsHeight() {
        const children = this.itemsContainerEl.children;
        for (let i = 0; i < children.length; i++) {
            const child = children[i];
            const realIndex = this.startIndex + i;
            this.previousItemsHeight[realIndex] = child.getBoundingClientRect().height;
        }
    }

    private getDimensions() {
        const dimensions = {
            contentHeight: 0,
            paddingTop: 0,
            itemsThatAreGone: 0,
        };

        dimensions.contentHeight = this.originalItems.reduce((prev, curr, i) => {
            const height = this.previousItemsHeight[i];
            return prev + (height ? height : this.minRowHeight);
        }, 0);

        if (this.currentScroll >= this.minRowHeight) {
            let newPaddingTop = 0;
            let itemsThatAreGone = 0;
            let initialScroll = this.currentScroll;

            for (const h of this.previousItemsHeight) {
                const height = h ? h : this.minRowHeight;
                if (initialScroll >= height) {
                    newPaddingTop += height;
                    initialScroll -= height;
                    itemsThatAreGone++;
                } else {
                    break;
                }
            }

            dimensions.paddingTop = newPaddingTop;
            dimensions.itemsThatAreGone = itemsThatAreGone;
        }

        return dimensions;
    }

    private prepareDataVirtualScroll() {
        const dimensions = this.getDimensions();

        this.contentHeight = dimensions.contentHeight;
        this.paddingTop = dimensions.paddingTop;
        this.startIndex = dimensions.itemsThatAreGone;
        this.endIndex = Math.min(this.startIndex + this.numberItemsCanRender(), this.originalItems.length - 1);

        this.items = this.originalItems.slice(this.startIndex, Math.min(this.endIndex + 1, this.originalItems.length));

        this.onItemsRender.emit(
            new AgVsRenderEvent<any>({
                items: this.items,
                startIndex: this.startIndex,
                endIndex: this.endIndex,
                length: this.items.length,
            }),
        );

        this.manipuleRenderedItems();
    }

    private numberItemsCanRender() {
        return Math.floor(this.el.clientHeight / this.minRowHeight) + 2;
    }

    private manipuleRenderedItems() {
        const children = this.itemsContainerEl.children;
        for (let i = 0; i < children.length; i++) {
            const child = children[i] as HTMLElement;
            if (child.style.display !== 'none') {
                child.style.minHeight = `${this.minRowHeight}px`;
                child.style.height = `${this.minRowHeight}px`;
            }
        }
    }

    ngOnDestroy() {
        // stop listening to scroll events
        this.scrollListener();
    }
}
