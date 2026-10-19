.class public final Lcn/vr4p/patch/SubtitlePositionControls;
.super Ljava/lang/Object;
.source "SubtitlePositionControls.java"


# static fields
.field private static final KEY:Ljava/lang/String; = "offset_percent"

.field private static final PREFS:Ljava/lang/String; = "4xvr_subtitle_position"

.field private static final ROW_TAG:Ljava/lang/String; = "4xvr.subtitle.position.row"

.field private static final TAG:Ljava/lang/String; = "4XVRSubtitlePosition"


# instance fields
.field private final minus:Landroid/widget/TextView;

.field private percent:I

.field private final plus:Landroid/widget/TextView;

.field private final preferences:Landroid/content/SharedPreferences;

.field private final value:Landroid/widget/TextView;


# direct methods
.method static constructor <clinit>()V
    .locals 1

    .line 21
    const-string v0, "4xvr_subtitle_position"

    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V

    return-void
.end method

.method private constructor <init>(Landroid/content/Context;Landroid/widget/TextView;Landroid/widget/TextView;Landroid/widget/TextView;)V
    .locals 2

    .line 30
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 31
    const-string v0, "4xvr_subtitle_position"

    const/4 v1, 0x0

    invoke-virtual {p1, v0, v1}, Landroid/content/Context;->getSharedPreferences(Ljava/lang/String;I)Landroid/content/SharedPreferences;

    move-result-object p1

    iput-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->preferences:Landroid/content/SharedPreferences;

    .line 32
    iput-object p2, p0, Lcn/vr4p/patch/SubtitlePositionControls;->value:Landroid/widget/TextView;

    .line 33
    iput-object p3, p0, Lcn/vr4p/patch/SubtitlePositionControls;->minus:Landroid/widget/TextView;

    .line 34
    iput-object p4, p0, Lcn/vr4p/patch/SubtitlePositionControls;->plus:Landroid/widget/TextView;

    .line 36
    :try_start_0
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->preferences:Landroid/content/SharedPreferences;

    const-string p2, "offset_percent"

    invoke-interface {p1, p2, v1}, Landroid/content/SharedPreferences;->getInt(Ljava/lang/String;I)I

    move-result p1
    :try_end_0
    .catch Ljava/lang/ClassCastException; {:try_start_0 .. :try_end_0} :catch_0

    .line 37
    goto :goto_0

    :catch_0
    move-exception p1

    move p1, v1

    .line 38
    :goto_0
    invoke-direct {p0, p1, v1}, Lcn/vr4p/patch/SubtitlePositionControls;->update(IZ)V

    .line 39
    return-void
.end method

.method public static attach(Landroid/view/ViewGroup;)V
    .locals 9

    .line 74
    if-eqz p0, :cond_4

    const-string v0, "4xvr.subtitle.position.row"

    invoke-virtual {p0, v0}, Landroid/view/ViewGroup;->findViewWithTag(Ljava/lang/Object;)Landroid/view/View;

    move-result-object v1

    if-eqz v1, :cond_0

    goto/16 :goto_2

    .line 75
    :cond_0
    invoke-virtual {p0}, Landroid/view/ViewGroup;->getChildCount()I

    move-result v1

    const/4 v2, 0x2

    if-ne v1, v2, :cond_3

    const/4 v1, 0x0

    invoke-virtual {p0, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v3

    instance-of v3, v3, Landroid/widget/LinearLayout;

    if-eqz v3, :cond_3

    .line 76
    const/4 v3, 0x1

    invoke-virtual {p0, v3}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v4

    instance-of v4, v4, Landroid/widget/LinearLayout;

    if-eqz v4, :cond_3

    .line 79
    invoke-virtual {p0, v1}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v4

    check-cast v4, Landroid/widget/LinearLayout;

    .line 80
    move v5, v1

    :goto_0
    if-ge v5, v2, :cond_1

    .line 81
    invoke-virtual {p0, v5}, Landroid/view/ViewGroup;->getChildAt(I)Landroid/view/View;

    move-result-object v6

    .line 82
    invoke-virtual {v6}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v7

    .line 83
    const/16 v8, 0x1cc

    iput v8, v7, Landroid/view/ViewGroup$LayoutParams;->height:I

    .line 84
    invoke-virtual {v6, v7}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 80
    add-int/lit8 v5, v5, 0x1

    goto :goto_0

    .line 86
    :cond_1
    invoke-virtual {p0}, Landroid/view/ViewGroup;->getContext()Landroid/content/Context;

    move-result-object p0

    .line 87
    new-instance v5, Landroid/view/View;

    invoke-direct {v5, p0}, Landroid/view/View;-><init>(Landroid/content/Context;)V

    .line 88
    const v6, 0x22808080

    invoke-virtual {v5, v6}, Landroid/view/View;->setBackgroundColor(I)V

    .line 89
    new-instance v6, Landroid/widget/LinearLayout$LayoutParams;

    const/4 v7, -0x1

    invoke-direct {v6, v7, v3}, Landroid/widget/LinearLayout$LayoutParams;-><init>(II)V

    .line 90
    const/16 v3, 0x14

    invoke-virtual {v6, v3, v1, v3, v1}, Landroid/widget/LinearLayout$LayoutParams;->setMargins(IIII)V

    .line 91
    invoke-virtual {v4, v5, v6}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 92
    new-instance v5, Landroid/widget/LinearLayout;

    invoke-direct {v5, p0}, Landroid/widget/LinearLayout;-><init>(Landroid/content/Context;)V

    .line 93
    invoke-virtual {v5, v0}, Landroid/widget/LinearLayout;->setTag(Ljava/lang/Object;)V

    .line 94
    invoke-virtual {v5, v1}, Landroid/widget/LinearLayout;->setOrientation(I)V

    .line 95
    const/16 v0, 0x10

    invoke-virtual {v5, v0}, Landroid/widget/LinearLayout;->setGravity(I)V

    .line 96
    new-instance v0, Landroid/widget/LinearLayout$LayoutParams;

    const/16 v6, 0x38

    invoke-direct {v0, v7, v6}, Landroid/widget/LinearLayout$LayoutParams;-><init>(II)V

    .line 97
    invoke-virtual {v0, v3, v1, v3, v1}, Landroid/widget/LinearLayout$LayoutParams;->setMargins(IIII)V

    .line 98
    invoke-virtual {v4, v5, v0}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 99
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;

    move-result-object v0

    invoke-virtual {v0}, Landroid/content/res/Configuration;->getLocales()Landroid/os/LocaleList;

    move-result-object v0

    .line 100
    invoke-virtual {v0}, Landroid/os/LocaleList;->size()I

    move-result v3

    new-array v4, v3, [Ljava/util/Locale;

    .line 101
    move v6, v1

    :goto_1
    if-ge v6, v3, :cond_2

    invoke-virtual {v0, v6}, Landroid/os/LocaleList;->get(I)Ljava/util/Locale;

    move-result-object v7

    aput-object v7, v4, v6

    add-int/lit8 v6, v6, 0x1

    goto :goto_1

    .line 102
    :cond_2
    invoke-static {v4}, Lcn/vr4p/patch/SubtitlePositionStrings;->forLocales([Ljava/util/Locale;)Lcn/vr4p/patch/SubtitlePositionStrings;

    move-result-object v0

    .line 103
    iget-object v3, v0, Lcn/vr4p/patch/SubtitlePositionStrings;->title:Ljava/lang/String;

    invoke-static {p0, v3, v1}, Lcn/vr4p/patch/SubtitlePositionControls;->text(Landroid/content/Context;Ljava/lang/String;I)Landroid/widget/TextView;

    move-result-object v3

    .line 104
    invoke-virtual {v3, v2}, Landroid/widget/TextView;->setMaxLines(I)V

    .line 105
    const v2, 0x800013

    invoke-virtual {v3, v2}, Landroid/widget/TextView;->setGravity(I)V

    .line 106
    new-instance v2, Landroid/widget/LinearLayout$LayoutParams;

    const/high16 v4, 0x3f800000    # 1.0f

    const/16 v6, 0x30

    invoke-direct {v2, v1, v6, v4}, Landroid/widget/LinearLayout$LayoutParams;-><init>(IIF)V

    invoke-virtual {v5, v3, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;Landroid/view/ViewGroup$LayoutParams;)V

    .line 107
    const-string v2, "\u2212"

    iget-object v3, v0, Lcn/vr4p/patch/SubtitlePositionStrings;->down:Ljava/lang/String;

    invoke-static {p0, v2, v6, v3}, Lcn/vr4p/patch/SubtitlePositionControls;->button(Landroid/content/Context;Ljava/lang/String;ILjava/lang/String;)Landroid/widget/TextView;

    move-result-object v2

    .line 108
    const-string v3, "0%"

    const/16 v4, 0x48

    invoke-static {p0, v3, v4}, Lcn/vr4p/patch/SubtitlePositionControls;->text(Landroid/content/Context;Ljava/lang/String;I)Landroid/widget/TextView;

    move-result-object v3

    .line 109
    const-string v4, "+"

    iget-object v7, v0, Lcn/vr4p/patch/SubtitlePositionStrings;->up:Ljava/lang/String;

    invoke-static {p0, v4, v6, v7}, Lcn/vr4p/patch/SubtitlePositionControls;->button(Landroid/content/Context;Ljava/lang/String;ILjava/lang/String;)Landroid/widget/TextView;

    move-result-object v4

    .line 110
    iget-object v6, v0, Lcn/vr4p/patch/SubtitlePositionStrings;->reset:Ljava/lang/String;

    const/16 v7, 0x70

    iget-object v0, v0, Lcn/vr4p/patch/SubtitlePositionStrings;->resetDescription:Ljava/lang/String;

    invoke-static {p0, v6, v7, v0}, Lcn/vr4p/patch/SubtitlePositionControls;->button(Landroid/content/Context;Ljava/lang/String;ILjava/lang/String;)Landroid/widget/TextView;

    move-result-object v0

    .line 111
    const/16 v6, 0x8

    invoke-virtual {v0, v6, v1, v6, v1}, Landroid/widget/TextView;->setPadding(IIII)V

    .line 113
    invoke-virtual {v0}, Landroid/widget/TextView;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v1

    check-cast v1, Landroid/widget/LinearLayout$LayoutParams;

    const/16 v6, 0xc

    invoke-virtual {v1, v6}, Landroid/widget/LinearLayout$LayoutParams;->setMarginStart(I)V

    .line 114
    invoke-virtual {v5, v2}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    invoke-virtual {v5, v3}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    invoke-virtual {v5, v4}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    invoke-virtual {v5, v0}, Landroid/widget/LinearLayout;->addView(Landroid/view/View;)V

    .line 115
    new-instance v1, Lcn/vr4p/patch/SubtitlePositionControls;

    invoke-direct {v1, p0, v3, v2, v4}, Lcn/vr4p/patch/SubtitlePositionControls;-><init>(Landroid/content/Context;Landroid/widget/TextView;Landroid/widget/TextView;Landroid/widget/TextView;)V

    .line 116
    new-instance p0, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda0;

    invoke-direct {p0, v1}, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda0;-><init>(Lcn/vr4p/patch/SubtitlePositionControls;)V

    invoke-virtual {v2, p0}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 117
    new-instance p0, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda1;

    invoke-direct {p0, v1}, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda1;-><init>(Lcn/vr4p/patch/SubtitlePositionControls;)V

    invoke-virtual {v4, p0}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 118
    new-instance p0, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda2;

    invoke-direct {p0, v1}, Lcn/vr4p/patch/SubtitlePositionControls$$ExternalSyntheticLambda2;-><init>(Lcn/vr4p/patch/SubtitlePositionControls;)V

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setOnClickListener(Landroid/view/View$OnClickListener;)V

    .line 119
    const-string p0, "4XVRSubtitlePosition"

    const-string v0, "Subtitle position controls attached"

    invoke-static {p0, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 120
    return-void

    .line 77
    :cond_3
    new-instance p0, Ljava/lang/IllegalStateException;

    const-string v0, "Unexpected subtitle panel layout"

    invoke-direct {p0, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p0

    .line 74
    :cond_4
    :goto_2
    return-void
.end method

.method private static button(Landroid/content/Context;Ljava/lang/String;ILjava/lang/String;)Landroid/widget/TextView;
    .locals 1

    .line 64
    invoke-static {p0, p1, p2}, Lcn/vr4p/patch/SubtitlePositionControls;->text(Landroid/content/Context;Ljava/lang/String;I)Landroid/widget/TextView;

    move-result-object p1

    .line 65
    invoke-virtual {p1, p3}, Landroid/widget/TextView;->setContentDescription(Ljava/lang/CharSequence;)V

    .line 66
    invoke-virtual {p0}, Landroid/content/Context;->getResources()Landroid/content/res/Resources;

    move-result-object p2

    .line 67
    invoke-virtual {p0}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object p0

    .line 66
    const-string p3, "radiobutton_sync_subtitle_btn"

    const-string v0, "drawable"

    invoke-virtual {p2, p3, v0, p0}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I

    move-result p0

    .line 68
    if-eqz p0, :cond_0

    invoke-virtual {p1, p0}, Landroid/widget/TextView;->setBackgroundResource(I)V

    .line 69
    :cond_0
    const/4 p0, 0x1

    invoke-virtual {p1, p0}, Landroid/widget/TextView;->setClickable(Z)V

    .line 70
    invoke-virtual {p1, p0}, Landroid/widget/TextView;->setFocusable(Z)V

    .line 71
    return-object p1
.end method

.method static synthetic lambda$attach$0(Lcn/vr4p/patch/SubtitlePositionControls;Landroid/view/View;)V
    .locals 1

    .line 116
    iget p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    const/4 v0, -0x1

    invoke-static {p1, v0}, Lcn/vr4p/patch/PositionValue;->step(II)I

    move-result p1

    const/4 v0, 0x1

    invoke-direct {p0, p1, v0}, Lcn/vr4p/patch/SubtitlePositionControls;->update(IZ)V

    return-void
.end method

.method static synthetic lambda$attach$1(Lcn/vr4p/patch/SubtitlePositionControls;Landroid/view/View;)V
    .locals 1

    .line 117
    iget p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    const/4 v0, 0x1

    invoke-static {p1, v0}, Lcn/vr4p/patch/PositionValue;->step(II)I

    move-result p1

    invoke-direct {p0, p1, v0}, Lcn/vr4p/patch/SubtitlePositionControls;->update(IZ)V

    return-void
.end method

.method static synthetic lambda$attach$2(Lcn/vr4p/patch/SubtitlePositionControls;Landroid/view/View;)V
    .locals 1

    .line 118
    const/4 p1, 0x0

    const/4 v0, 0x1

    invoke-direct {p0, p1, v0}, Lcn/vr4p/patch/SubtitlePositionControls;->update(IZ)V

    return-void
.end method

.method private static native nativeSetPercent(I)I
.end method

.method private static text(Landroid/content/Context;Ljava/lang/String;I)Landroid/widget/TextView;
    .locals 1

    .line 50
    new-instance v0, Landroid/widget/TextView;

    invoke-direct {v0, p0}, Landroid/widget/TextView;-><init>(Landroid/content/Context;)V

    .line 51
    invoke-virtual {v0, p1}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 52
    const/high16 p0, 0x41600000    # 14.0f

    const/4 p1, 0x0

    invoke-virtual {v0, p1, p0}, Landroid/widget/TextView;->setTextSize(IF)V

    .line 53
    const p0, -0x111112

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setTextColor(I)V

    .line 54
    const-string p0, "sans-serif"

    invoke-static {p0, p1}, Landroid/graphics/Typeface;->create(Ljava/lang/String;I)Landroid/graphics/Typeface;

    move-result-object p0

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setTypeface(Landroid/graphics/Typeface;)V

    .line 56
    const/4 p0, 0x1

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setMaxLines(I)V

    .line 57
    invoke-virtual {v0, p1}, Landroid/widget/TextView;->setHorizontallyScrolling(Z)V

    .line 58
    const/16 p0, 0x11

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setGravity(I)V

    .line 59
    invoke-virtual {v0, p1}, Landroid/widget/TextView;->setIncludeFontPadding(Z)V

    .line 60
    new-instance p0, Landroid/widget/LinearLayout$LayoutParams;

    const/16 p1, 0x30

    invoke-direct {p0, p2, p1}, Landroid/widget/LinearLayout$LayoutParams;-><init>(II)V

    invoke-virtual {v0, p0}, Landroid/widget/TextView;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 61
    return-object v0
.end method

.method private update(IZ)V
    .locals 4

    .line 41
    invoke-static {p1}, Lcn/vr4p/patch/PositionValue;->clamp(I)I

    move-result p1

    invoke-static {p1}, Lcn/vr4p/patch/SubtitlePositionControls;->nativeSetPercent(I)I

    move-result p1

    iput p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    .line 42
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->value:Landroid/widget/TextView;

    iget v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    invoke-static {v0}, Lcn/vr4p/patch/PositionValue;->label(I)Ljava/lang/String;

    move-result-object v0

    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setText(Ljava/lang/CharSequence;)V

    .line 43
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->minus:Landroid/widget/TextView;

    iget v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    const/16 v1, -0x64

    const/4 v2, 0x1

    const/4 v3, 0x0

    if-le v0, v1, :cond_0

    move v0, v2

    goto :goto_0

    :cond_0
    move v0, v3

    :goto_0
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setEnabled(Z)V

    .line 44
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->plus:Landroid/widget/TextView;

    iget v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    const/16 v1, 0x64

    if-ge v0, v1, :cond_1

    goto :goto_1

    :cond_1
    move v2, v3

    :goto_1
    invoke-virtual {p1, v2}, Landroid/widget/TextView;->setEnabled(Z)V

    .line 45
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->minus:Landroid/widget/TextView;

    iget-object v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->minus:Landroid/widget/TextView;

    invoke-virtual {v0}, Landroid/widget/TextView;->isEnabled()Z

    move-result v0

    const/high16 v1, 0x3f800000    # 1.0f

    const v2, 0x3eb33333    # 0.35f

    if-eqz v0, :cond_2

    move v0, v1

    goto :goto_2

    :cond_2
    move v0, v2

    :goto_2
    invoke-virtual {p1, v0}, Landroid/widget/TextView;->setAlpha(F)V

    .line 46
    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->plus:Landroid/widget/TextView;

    iget-object v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->plus:Landroid/widget/TextView;

    invoke-virtual {v0}, Landroid/widget/TextView;->isEnabled()Z

    move-result v0

    if-eqz v0, :cond_3

    goto :goto_3

    :cond_3
    move v1, v2

    :goto_3
    invoke-virtual {p1, v1}, Landroid/widget/TextView;->setAlpha(F)V

    .line 47
    if-eqz p2, :cond_4

    iget-object p1, p0, Lcn/vr4p/patch/SubtitlePositionControls;->preferences:Landroid/content/SharedPreferences;

    invoke-interface {p1}, Landroid/content/SharedPreferences;->edit()Landroid/content/SharedPreferences$Editor;

    move-result-object p1

    const-string p2, "offset_percent"

    iget v0, p0, Lcn/vr4p/patch/SubtitlePositionControls;->percent:I

    invoke-interface {p1, p2, v0}, Landroid/content/SharedPreferences$Editor;->putInt(Ljava/lang/String;I)Landroid/content/SharedPreferences$Editor;

    move-result-object p1

    invoke-interface {p1}, Landroid/content/SharedPreferences$Editor;->apply()V

    .line 48
    :cond_4
    return-void
.end method
