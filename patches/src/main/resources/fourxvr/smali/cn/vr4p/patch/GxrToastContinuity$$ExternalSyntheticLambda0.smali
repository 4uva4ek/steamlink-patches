.class public final synthetic Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;
.super Ljava/lang/Object;
.source "D8$$SyntheticClass"

# interfaces
.implements Ljava/lang/Runnable;


# instance fields
.field public final synthetic f$0:Lcn/vr4p/patch/GxrToastContinuity;

.field public final synthetic f$1:I

.field public final synthetic f$2:Landroid/widget/Toast;


# direct methods
.method public synthetic constructor <init>(Lcn/vr4p/patch/GxrToastContinuity;ILandroid/widget/Toast;)V
    .locals 0

    .line 0
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$0:Lcn/vr4p/patch/GxrToastContinuity;

    iput p2, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$1:I

    iput-object p3, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$2:Landroid/widget/Toast;

    return-void
.end method


# virtual methods
.method public final run()V
    .locals 3

    .line 0
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$0:Lcn/vr4p/patch/GxrToastContinuity;

    iget v1, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$1:I

    iget-object v2, p0, Lcn/vr4p/patch/GxrToastContinuity$$ExternalSyntheticLambda0;->f$2:Landroid/widget/Toast;

    invoke-virtual {v0, v1, v2}, Lcn/vr4p/patch/GxrToastContinuity;->lambda$show$0$cn-vr4p-patch-GxrToastContinuity(ILandroid/widget/Toast;)V

    return-void
.end method
