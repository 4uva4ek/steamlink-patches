.class Lcn/vr4p/oculus4xvrplayerov/RenderDispatch$Native;
.super Ljava/lang/Object;
.source "RenderDispatch.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcn/vr4p/oculus4xvrplayerov/RenderDispatch;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0xa
    name = "Native"
.end annotation


# direct methods
.method static constructor <clinit>()V
    .locals 1

    const-string v0, "4xvr_render_dispatch"

    invoke-static {v0}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V

    return-void
.end method

.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static initialize()V
    .locals 0

    return-void
.end method

.method static native leavePassthrough()Z
.end method
