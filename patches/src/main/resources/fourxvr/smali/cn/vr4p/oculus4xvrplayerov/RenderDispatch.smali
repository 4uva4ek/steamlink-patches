.class public final Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;
.super Ljava/lang/Object;
.source "RenderDispatch.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$Native;
    }
.end annotation


# static fields
.field private static final queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;


# direct methods
.method static constructor <clinit>()V
    .locals 4

    new-instance v0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    new-instance v1, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$1;

    invoke-direct {v1}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$1;-><init>()V

    new-instance v2, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$2;

    invoke-direct {v2}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$2;-><init>()V

    new-instance v3, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$3;

    invoke-direct {v3}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$3;-><init>()V

    invoke-direct {v0, v1, v2, v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;-><init>(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;)V

    sput-object v0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static drain()V
    .locals 1

    sget-object v0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-virtual {v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->drain()V

    return-void
.end method

.method public static initialize()V
    .locals 0

    invoke-static {}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$Native;->initialize()V

    return-void
.end method

.method public static removeWindow(Lcn/vr4p/oculus4xvrplayerov/V4Window;JZ)V
    .locals 2

    sget-object v0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    new-instance v1, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;

    invoke-direct {v1, p0, p1, p2, p3}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$4;-><init>(Lcn/vr4p/oculus4xvrplayerov/V4Window;JZ)V

    invoke-virtual {v0, p1, p2, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->removeWindow(JLjava/lang/Runnable;)V

    return-void
.end method

.method public static requestScene(Lcn/vr4p/oculus4xvrplayerov/V4Theatre;I)Z
    .locals 2

    sget-object v0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    new-instance v1, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;

    invoke-direct {v1, p1, p0}, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$5;-><init>(ILcn/vr4p/oculus4xvrplayerov/V4Theatre;)V

    invoke-virtual {v0, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->scene(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;)V

    const/4 p0, 0x1

    return p0
.end method

.method public static reset()V
    .locals 1

    sget-object v0, Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;->queue:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-virtual {v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->reset()V

    return-void
.end method
