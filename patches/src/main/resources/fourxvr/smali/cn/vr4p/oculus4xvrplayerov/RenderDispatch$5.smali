.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;
.super Ljava/lang/Object;
.source "RenderDispatch.java"

# interfaces
.implements Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->requestScene(Lcn/vr4p/oculus4xvrplayerov/V4Theatre;I)Z
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic val$target:I

.field final synthetic val$theatre:Lcn/vr4p/oculus4xvrplayerov/V4Theatre;


# direct methods
.method constructor <init>(ILcn/vr4p/oculus4xvrplayerov/V4Theatre;)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    iput p1, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$target:I

    iput-object p2, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$theatre:Lcn/vr4p/oculus4xvrplayerov/V4Theatre;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public apply()Z
    .locals 2

    iget v0, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$target:I

    const/4 v1, 0x1

    if-nez v0, :cond_0

    invoke-static {}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->SetPassthrough()V

    return v1

    :cond_0
    iget v0, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$target:I

    invoke-static {v0}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->RecoveryTheatres(I)Z

    move-result v0

    if-eqz v0, :cond_1

    return v1

    :cond_1
    invoke-static {}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$Native;->leavePassthrough()Z

    move-result v0

    if-eqz v0, :cond_2

    const/4 v0, 0x0

    return v0

    :cond_2
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "passthrough handoff failed"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0
.end method

.method public load()V
    .locals 2

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$theatre:Lcn/vr4p/oculus4xvrplayerov/V4Theatre;

    iget v1, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;->val$target:I

    invoke-virtual {v0, v1}, Lcn/vr4p/oculus4xvrplayerov/V4Theatre;->lambda$SetCurSubTheatreID$0$V4Theatre(I)V

    return-void
.end method
