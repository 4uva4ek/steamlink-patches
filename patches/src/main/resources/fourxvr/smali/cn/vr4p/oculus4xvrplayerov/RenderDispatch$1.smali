.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$1;
.super Ljava/lang/Object;
.source "RenderDispatch.java"

# interfaces
.implements Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# direct methods
.method constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public frame()J
    .locals 2

    invoke-static {}, Lcn/vr4p/oculus4xvrplayerov/Main4XActivity;->GetRenderVideoFrame()J

    move-result-wide v0

    return-wide v0
.end method
