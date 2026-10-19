.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$3;
.super Ljava/lang/Object;
.source "RenderDispatch.java"

# interfaces
.implements Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;


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
.method public report(Ljava/lang/Throwable;)V
    .locals 2

    const-string v0, "4XVRRenderDispatch"

    const-string v1, "dispatch failed"

    invoke-static {v0, v1, p1}, Landroid/util/Log;->e(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    return-void
.end method
