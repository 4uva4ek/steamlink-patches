.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;
.super Ljava/lang/Object;
.source "RenderDispatch.java"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->removeWindow(Lcn/vr4p/oculus4xvrplayerov/V4Window;JZ)V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic val$fade:Z

.field final synthetic val$id:J

.field final synthetic val$window:Lcn/vr4p/oculus4xvrplayerov/V4Window;


# direct methods
.method constructor <init>(Lcn/vr4p/oculus4xvrplayerov/V4Window;JZ)V
    .locals 0
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    iput-object p1, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$window:Lcn/vr4p/oculus4xvrplayerov/V4Window;

    iput-wide p2, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$id:J

    iput-boolean p4, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$fade:Z

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public run()V
    .locals 4

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$window:Lcn/vr4p/oculus4xvrplayerov/V4Window;

    invoke-virtual {v0}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->IsVisible()Z

    move-result v0

    if-nez v0, :cond_0

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$window:Lcn/vr4p/oculus4xvrplayerov/V4Window;

    iget-wide v1, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$id:J

    iget-boolean v3, p0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;->val$fade:Z

    invoke-virtual {v0, v1, v2, v3}, Lcn/vr4p/oculus4xvrplayerov/V4Window;->RemoveBufferFromCPP(JZ)V

    :cond_0
    return-void
.end method
