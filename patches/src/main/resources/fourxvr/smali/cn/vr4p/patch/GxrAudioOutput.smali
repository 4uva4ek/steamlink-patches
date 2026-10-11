.class public final Lcn/vr4p/patch/GxrAudioOutput;
.super Ljava/lang/Object;
.source "GxrAudioOutput.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcn/vr4p/patch/GxrAudioOutput$Output;
    }
.end annotation


# static fields
.field private static final TAG:Ljava/lang/String; = "4XVRAudio"

.field private static available:Z

.field private static extensions:Ljava/lang/Object;

.field private static final ids:Ljava/util/concurrent/atomic/AtomicLong;

.field private static final outputs:Ljava/util/concurrent/ConcurrentHashMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/concurrent/ConcurrentHashMap<",
            "Ljava/lang/Long;",
            "Lcn/vr4p/patch/GxrAudioOutput$Output;",
            ">;"
        }
    .end annotation
.end field

.field private static trackExtensions:Ljava/lang/Object;


# direct methods
.method static constructor <clinit>()V
    .locals 8

    .line 16
    const-string v0, "4XVRAudio"

    new-instance v1, Ljava/util/concurrent/ConcurrentHashMap;

    invoke-direct {v1}, Ljava/util/concurrent/ConcurrentHashMap;-><init>()V

    sput-object v1, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    .line 17
    new-instance v1, Ljava/util/concurrent/atomic/AtomicLong;

    const-wide/16 v2, -0x1

    invoke-direct {v1, v2, v3}, Ljava/util/concurrent/atomic/AtomicLong;-><init>(J)V

    sput-object v1, Lcn/vr4p/patch/GxrAudioOutput;->ids:Ljava/util/concurrent/atomic/AtomicLong;

    .line 22
    const/4 v1, 0x0

    :try_start_0
    const-string v2, "4xvr_audio_bridge"

    invoke-static {v2}, Ljava/lang/System;->loadLibrary(Ljava/lang/String;)V

    .line 23
    const-string v2, "xrvst2"

    sget-object v3, Landroid/os/Build;->DEVICE:Ljava/lang/String;

    invoke-virtual {v2, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v2

    if-eqz v2, :cond_1

    .line 24
    const-string v2, "com.android.extensions.xr.XrExtensions"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    .line 25
    new-array v3, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v3

    new-array v4, v1, [Ljava/lang/Object;

    invoke-virtual {v3, v4}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    sput-object v3, Lcn/vr4p/patch/GxrAudioOutput;->extensions:Ljava/lang/Object;

    .line 26
    const-string v3, "getOpenXrWorldReferenceSpaceType"

    new-array v4, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    sget-object v3, Lcn/vr4p/patch/GxrAudioOutput;->extensions:Ljava/lang/Object;

    new-array v4, v1, [Ljava/lang/Object;

    invoke-virtual {v2, v3, v4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Ljava/lang/Integer;

    invoke-virtual {v2}, Ljava/lang/Integer;->intValue()I

    move-result v2

    const v3, 0x3ba1ea38

    const/4 v4, 0x1

    if-ne v2, v3, :cond_0

    move v2, v4

    goto :goto_0

    :cond_0
    move v2, v1

    :goto_0
    sput-boolean v2, Lcn/vr4p/patch/GxrAudioOutput;->available:Z

    .line 27
    const-string v2, "com.android.extensions.xr.node.Node"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    new-array v3, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    .line 28
    const-string v2, "com.android.extensions.xr.node.NodeTransaction"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    new-array v3, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    .line 29
    const-string v2, "com.android.extensions.xr.media.PointSourceParams"

    invoke-static {v2}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v2

    .line 30
    const-string v3, "com.android.extensions.xr.media.PointSourceParams$Builder"

    invoke-static {v3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v3

    new-array v5, v1, [Ljava/lang/Class;

    invoke-virtual {v3, v5}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    .line 31
    const-string v3, "com.android.extensions.xr.media.AudioTrackExtensions"

    invoke-static {v3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v3

    .line 32
    new-array v5, v1, [Ljava/lang/Class;

    invoke-virtual {v3, v5}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v5

    new-array v6, v1, [Ljava/lang/Object;

    invoke-virtual {v5, v6}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v5

    sput-object v5, Lcn/vr4p/patch/GxrAudioOutput;->trackExtensions:Ljava/lang/Object;

    .line 33
    const-string v5, "setPointSourceParams"

    const/4 v6, 0x2

    new-array v6, v6, [Ljava/lang/Class;

    const-class v7, Landroid/media/AudioTrack$Builder;

    aput-object v7, v6, v1

    aput-object v2, v6, v4

    invoke-virtual {v3, v5, v6}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    .line 34
    const-string v2, "getSpatialSourceType"

    new-array v4, v4, [Ljava/lang/Class;

    const-class v5, Landroid/media/AudioTrack;

    aput-object v5, v4, v1

    invoke-virtual {v3, v2, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    .line 36
    :cond_1
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "XR audio output available="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    sget-boolean v3, Lcn/vr4p/patch/GxrAudioOutput;->available:Z

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Z)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 37
    goto :goto_1

    :catchall_0
    move-exception v2

    sput-boolean v1, Lcn/vr4p/patch/GxrAudioOutput;->available:Z

    const-string v1, "Platform output unavailable; original audio retained"

    invoke-static {v0, v1, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 38
    :goto_1
    return-void
.end method

.method public constructor <init>()V
    .locals 0

    .line 14
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static CreateAAudio(IIJIZII)J
    .locals 12

    .line 50
    invoke-static {p1, p2, p3}, Lcn/vr4p/patch/GxrAudioOutput;->channelMask(IJ)I

    move-result v6

    .line 51
    sget-boolean v0, Lcn/vr4p/patch/GxrAudioOutput;->available:Z

    if-eqz v0, :cond_0

    if-eqz p5, :cond_0

    const/4 v0, 0x2

    move/from16 v11, p6

    if-eq v11, v0, :cond_1

    if-nez p7, :cond_1

    if-eqz v6, :cond_1

    .line 53
    :try_start_0
    new-instance v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    move-object v1, v0

    move v2, p0

    move v3, p1

    move-wide v4, p2

    move/from16 v7, p4

    move/from16 v8, p5

    move/from16 v9, p6

    move/from16 v10, p7

    invoke-direct/range {v1 .. v10}, Lcn/vr4p/patch/GxrAudioOutput$Output;-><init>(IIJIIZII)V

    .line 54
    sget-object v1, Lcn/vr4p/patch/GxrAudioOutput;->ids:Ljava/util/concurrent/atomic/AtomicLong;

    invoke-virtual {v1}, Ljava/util/concurrent/atomic/AtomicLong;->getAndDecrement()J

    move-result-wide v1

    sget-object v3, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {v1, v2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v4

    invoke-virtual {v3, v4, v0}, Ljava/util/concurrent/ConcurrentHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 55
    return-wide v1

    .line 56
    :catchall_0
    move-exception v0

    const-string v1, "4XVRAudio"

    const-string v2, "XR output not selected; original audio retained"

    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    goto :goto_0

    .line 51
    :cond_0
    move/from16 v11, p6

    .line 58
    :cond_1
    :goto_0
    invoke-static/range {p0 .. p7}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->CreateAAudio(IIJIZII)J

    move-result-wide v0

    return-wide v0
.end method

.method public static GetSampleCountInBuf(J)I
    .locals 2

    .line 84
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->GetSampleCountInBuf(J)I

    move-result p0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_0

    :cond_1
    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->queuedFrames()I

    move-result p0

    :goto_0
    return p0
.end method

.method public static IsPlayingAAudio(J)Z
    .locals 2

    .line 82
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->IsPlayingAAudio(J)Z

    move-result p0

    if-eqz p0, :cond_0

    const/4 p0, 0x1

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_0

    :cond_1
    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->playing()Z

    move-result p0

    :goto_0
    return p0
.end method

.method public static NeedWaitAAudio(J)Z
    .locals 2

    .line 83
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->NeedWaitAAudio(J)Z

    move-result p0

    if-eqz p0, :cond_0

    const/4 p0, 0x1

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_0

    :cond_1
    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->needWait()Z

    move-result p0

    :goto_0
    return p0
.end method

.method public static PauseAAudio(J)V
    .locals 2

    .line 80
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->pause()V

    goto :goto_0

    :cond_0
    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->PauseAAudio(J)V

    :cond_1
    :goto_0
    return-void
.end method

.method public static PlayAAudio(J)V
    .locals 2

    .line 79
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-eqz v0, :cond_0

    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->play()V

    goto :goto_0

    :cond_0
    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->PlayAAudio(J)V

    :cond_1
    :goto_0
    return-void
.end method

.method public static ReleaseAAudio(J)V
    .locals 2

    .line 71
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->remove(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    .line 72
    if-nez v0, :cond_1

    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->ReleaseAAudio(J)V

    :cond_0
    return-void

    .line 73
    :cond_1
    invoke-virtual {v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->close()V

    .line 74
    return-void
.end method

.method public static ResetAAudio(JI)V
    .locals 2

    .line 81
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-eqz v0, :cond_0

    invoke-virtual {v0, p2}, Lcn/vr4p/patch/GxrAudioOutput$Output;->resetAmbisonic(I)V

    goto :goto_0

    :cond_0
    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0, p1, p2}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->ResetAAudio(JI)V

    :cond_1
    :goto_0
    return-void
.end method

.method public static SetListenPoseAAudio(JFFFFFF)V
    .locals 8

    .line 90
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    move-object v1, v0

    check-cast v1, Lcn/vr4p/patch/GxrAudioOutput$Output;

    .line 91
    if-eqz v1, :cond_0

    move v2, p2

    move v3, p3

    move v4, p4

    move v5, p5

    move v6, p6

    move v7, p7

    invoke-virtual/range {v1 .. v7}, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener(FFFFFF)V

    goto :goto_0

    .line 92
    :cond_0
    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static/range {p0 .. p7}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->SetListenPoseAAudio(JFFFFFF)V

    .line 93
    :cond_1
    :goto_0
    return-void
.end method

.method public static UpdateVolumeBoost(JIZI)V
    .locals 2

    .line 86
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    if-eqz v0, :cond_0

    invoke-virtual {v0, p2, p3, p4}, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateSettings(IZI)V

    goto :goto_0

    :cond_0
    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0, p1, p2, p3, p4}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->UpdateVolumeBoost(JIZI)V

    .line 87
    :cond_1
    :goto_0
    return-void
.end method

.method public static WriteAAudio(JLjava/nio/ByteBuffer;IZ)V
    .locals 2

    .line 76
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p0, p1}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object v1

    invoke-virtual {v0, v1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    check-cast v0, Lcn/vr4p/patch/GxrAudioOutput$Output;

    .line 77
    if-nez v0, :cond_0

    invoke-static {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput;->isTaggedHandle(J)Z

    move-result v0

    if-nez v0, :cond_1

    invoke-static {p0, p1, p2, p3, p4}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->WriteAAudio(JLjava/nio/ByteBuffer;IZ)V

    goto :goto_0

    :cond_0
    invoke-virtual {v0, p2, p3, p4}, Lcn/vr4p/patch/GxrAudioOutput$Output;->write(Ljava/nio/ByteBuffer;IZ)V

    .line 78
    :cond_1
    :goto_0
    return-void
.end method

.method static synthetic access$000([F)Z
    .locals 0

    .line 14
    invoke-static {p0}, Lcn/vr4p/patch/GxrAudioOutput;->moviePose([F)Z

    move-result p0

    return p0
.end method

.method static synthetic access$100()Ljava/lang/Object;
    .locals 1

    .line 14
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->extensions:Ljava/lang/Object;

    return-object v0
.end method

.method static synthetic access$200()Ljava/lang/Object;
    .locals 1

    .line 14
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->trackExtensions:Ljava/lang/Object;

    return-object v0
.end method

.method public static bind(Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;J)V
    .locals 2

    .line 62
    sget-object v0, Lcn/vr4p/patch/GxrAudioOutput;->outputs:Ljava/util/concurrent/ConcurrentHashMap;

    invoke-static {p1, p2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object p1

    invoke-virtual {v0, p1}, Ljava/util/concurrent/ConcurrentHashMap;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p1

    check-cast p1, Lcn/vr4p/patch/GxrAudioOutput$Output;

    .line 63
    if-eqz p1, :cond_0

    .line 64
    iget-wide v0, p0, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->m_lAudioDelayTime:J

    iput-wide v0, p1, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousDelay:J

    .line 65
    iput-object p0, p1, Lcn/vr4p/patch/GxrAudioOutput$Output;->owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

    .line 67
    const-wide/16 p1, 0x0

    iput-wide p1, p0, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->m_lAudioDelayTime:J

    .line 69
    :cond_0
    return-void
.end method

.method static channelMask(IJ)I
    .locals 6

    .line 44
    const/4 v0, 0x6

    const/4 v1, 0x0

    if-eq p0, v0, :cond_0

    const/16 v0, 0x8

    if-eq p0, v0, :cond_0

    const/16 v0, 0xc

    if-eq p0, v0, :cond_0

    return v1

    .line 45
    :cond_0
    invoke-static {p1, p2}, Ljava/lang/Long;->bitCount(J)I

    move-result v0

    if-ne v0, p0, :cond_2

    const-wide/32 v2, -0x40000

    and-long/2addr v2, p1

    const-wide/16 v4, 0x0

    cmp-long p0, v2, v4

    if-eqz p0, :cond_1

    goto :goto_0

    .line 47
    :cond_1
    const/4 p0, 0x2

    shl-long p0, p1, p0

    long-to-int p0, p0

    return p0

    .line 45
    :cond_2
    :goto_0
    return v1
.end method

.method public static initialize()V
    .locals 0

    .line 39
    return-void
.end method

.method private static isTaggedHandle(J)Z
    .locals 2

    .line 42
    const-wide/16 v0, 0x0

    cmp-long v0, p0, v0

    if-gez v0, :cond_0

    const-wide v0, -0x100000000L

    cmp-long p0, p0, v0

    if-lez p0, :cond_0

    const/4 p0, 0x1

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    :goto_0
    return p0
.end method

.method private static native moviePose([F)Z
.end method
