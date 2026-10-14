.class public final Lcn/vr4p/patch/PremiumUnlock;
.super Ljava/lang/Object;
.source "PremiumUnlock.java"

# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static Launch()Z
    .locals 1

    sget-object v0, Lcn/vr4p/oculus4xvrplayerov/V4Window;->s_MainActivity:Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;

    if-eqz v0, :cond_0

    invoke-static {v0}, Lcn/vr4p/patch/PremiumUnlock;->unlock(Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;)V

    :cond_0
    const/4 v0, 0x1

    return v0
.end method

.method public static unlock(Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;)V
    .locals 2

    :try_start_0
    const/4 v0, 0x1

    invoke-virtual {p0, v0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->ChangeCurVIPStateClear(Z)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    goto :goto_0

    :catchall_0
    move-exception v0

    :goto_0
    return-void
.end method

.method private static find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;
    .locals 3

    .line 167
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v0

    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;

    move-result-object v1

    invoke-virtual {v1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v1

    const-string v2, "id"

    invoke-virtual {v0, p1, v2, v1}, Landroid/content/res/Resources;->getIdentifier(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)I

    move-result p1

    .line 168
    if-nez p1, :cond_0

    const/4 p0, 0x0

    goto :goto_0

    :cond_0
    invoke-virtual {p0, p1}, Landroid/view/View;->findViewById(I)Landroid/view/View;

    move-result-object p0

    :goto_0
    return-object p0
.end method

.method public static tidyBuyDialog(Lcn/vr4p/oculus4xvrplayerov/V4Window;)V
    .locals 7

    .line 142
    :try_start_0
    invoke-virtual {p0}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->GetViewGroup()Landroid/view/ViewGroup;

    move-result-object p0

    .line 143
    if-nez p0, :cond_0

    return-void

    .line 144
    :cond_0
    const-string v0, "Dialog_BuyVIP_BuyBtn"

    invoke-static {p0, v0}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object v0

    .line 145
    const-string v1, "Dialog_BuyVIP_Synchronize"

    invoke-static {p0, v1}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object v1

    .line 146
    const-string v2, "Dialog_BuyVIP_CloseBtn"

    invoke-static {p0, v2}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object p0

    .line 147
    if-eqz v0, :cond_4

    if-eqz v1, :cond_4

    if-nez p0, :cond_1

    goto :goto_1

    .line 148
    :cond_1
    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v2

    instance-of v2, v2, Landroid/view/ViewGroup;

    if-eqz v2, :cond_3

    invoke-virtual {v1}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v2

    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v3

    if-ne v2, v3, :cond_3

    .line 149
    invoke-virtual {p0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v2

    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v3

    if-eq v2, v3, :cond_2

    goto :goto_0

    .line 150
    :cond_2
    invoke-virtual {v0}, Landroid/view/View;->getParent()Landroid/view/ViewParent;

    move-result-object v2

    check-cast v2, Landroid/view/ViewGroup;

    .line 151
    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v3

    check-cast v3, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 152
    invoke-virtual {v1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v4

    check-cast v4, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 153
    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v5

    check-cast v5, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 154
    iget v6, v4, Landroid/view/ViewGroup$MarginLayoutParams;->bottomMargin:I

    .line 155
    iget v3, v3, Landroid/view/ViewGroup$MarginLayoutParams;->topMargin:I

    iput v3, v4, Landroid/view/ViewGroup$MarginLayoutParams;->topMargin:I

    .line 156
    const/4 v3, 0x0

    iput v3, v4, Landroid/view/ViewGroup$MarginLayoutParams;->bottomMargin:I

    .line 157
    iput v6, v5, Landroid/view/ViewGroup$MarginLayoutParams;->bottomMargin:I

    .line 158
    invoke-virtual {p0, v5}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 159
    invoke-virtual {v2, v1}, Landroid/view/ViewGroup;->removeView(Landroid/view/View;)V

    .line 160
    invoke-virtual {v2, v0}, Landroid/view/ViewGroup;->indexOfChild(Landroid/view/View;)I

    move-result p0

    add-int/lit8 p0, p0, 0x1

    invoke-virtual {v2, v1, p0, v4}, Landroid/view/ViewGroup;->addView(Landroid/view/View;ILandroid/view/ViewGroup$LayoutParams;)V

    .line 161
    const/16 p0, 0x8

    invoke-virtual {v0, p0}, Landroid/view/View;->setVisibility(I)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 163
    goto :goto_2

    .line 149
    :cond_3
    :goto_0
    return-void

    .line 147
    :cond_4
    :goto_1
    return-void

    .line 162
    :catchall_0
    move-exception p0

    .line 164
    :goto_2
    return-void
.end method

.method public static tidyBuyView(Landroid/view/View;)V
    .locals 7

    .line 118
    :try_start_0
    const-string v0, "dialog_buyvip_buy_btn"

    invoke-static {p0, v0}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object v0

    .line 119
    const-string v1, "dialog_buyvip_sync_data_btn"

    invoke-static {p0, v1}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object v1

    .line 120
    const-string v2, "dialog_buyvip_sync_data_btn_text"

    invoke-static {p0, v2}, Lcn/vr4p/patch/PremiumUnlock;->find(Landroid/view/View;Ljava/lang/String;)Landroid/view/View;

    move-result-object p0

    .line 121
    if-eqz v0, :cond_4

    if-nez v1, :cond_0

    goto :goto_1

    .line 122
    :cond_0
    invoke-virtual {v0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v2

    check-cast v2, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 123
    invoke-virtual {v1}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v3

    check-cast v3, Landroid/view/ViewGroup$MarginLayoutParams;

    .line 124
    iget v4, v2, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    if-lez v4, :cond_3

    iget v4, v3, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    if-gtz v4, :cond_1

    goto :goto_0

    .line 125
    :cond_1
    iget v4, v3, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    .line 126
    iget v5, v3, Landroid/view/ViewGroup$MarginLayoutParams;->leftMargin:I

    iget v6, v3, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    add-int/2addr v5, v6

    .line 127
    iget v6, v2, Landroid/view/ViewGroup$MarginLayoutParams;->leftMargin:I

    iput v6, v3, Landroid/view/ViewGroup$MarginLayoutParams;->leftMargin:I

    .line 128
    iget v2, v2, Landroid/view/ViewGroup$MarginLayoutParams;->leftMargin:I

    sub-int/2addr v5, v2

    iput v5, v3, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    .line 129
    invoke-virtual {v1, v3}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 130
    if-eqz p0, :cond_2

    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v1

    iget v1, v1, Landroid/view/ViewGroup$LayoutParams;->width:I

    if-lez v1, :cond_2

    .line 131
    invoke-virtual {p0}, Landroid/view/View;->getLayoutParams()Landroid/view/ViewGroup$LayoutParams;

    move-result-object v1

    .line 132
    iget v2, v1, Landroid/view/ViewGroup$LayoutParams;->width:I

    iget v3, v3, Landroid/view/ViewGroup$MarginLayoutParams;->width:I

    sub-int/2addr v3, v4

    add-int/2addr v2, v3

    iput v2, v1, Landroid/view/ViewGroup$LayoutParams;->width:I

    .line 133
    invoke-virtual {p0, v1}, Landroid/view/View;->setLayoutParams(Landroid/view/ViewGroup$LayoutParams;)V

    .line 135
    :cond_2
    const/16 p0, 0x8

    invoke-virtual {v0, p0}, Landroid/view/View;->setVisibility(I)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 137
    goto :goto_2

    .line 124
    :cond_3
    :goto_0
    return-void

    .line 121
    :cond_4
    :goto_1
    return-void

    .line 136
    :catchall_0
    move-exception p0

    .line 138
    :goto_2
    return-void
.end method
