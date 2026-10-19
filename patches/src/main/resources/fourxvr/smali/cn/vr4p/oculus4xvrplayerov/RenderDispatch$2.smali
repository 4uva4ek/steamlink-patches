.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$2;
.super Ljava/lang/Object;
.source "RenderDispatch.java"

# interfaces
.implements Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;


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
.method public start(Ljava/lang/Runnable;)V
    .locals 2

    new-instance v0, Ljava/lang/Thread;

    const-string v1, "4XVR-TheatreLoad"

    invoke-direct {v0, p1, v1}, Ljava/lang/Thread;-><init>(Ljava/lang/Runnable;Ljava/lang/String;)V

    invoke-virtual {v0}, Ljava/lang/Thread;->start()V

    return-void
.end method
