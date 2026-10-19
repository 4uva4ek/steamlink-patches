.class final Lcn/vr4p/patch/GxrAudioOutput$Output;
.super Ljava/lang/Object;
.source "GxrAudioOutput.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcn/vr4p/patch/GxrAudioOutput;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "Output"
.end annotation


# instance fields
.field ambisonic:I

.field final channels:I

.field closed:Z

.field end:I

.field epochNano:J

.field final frameBytes:I

.field gain:F

.field headEpoch:J

.field lastHead:J

.field lastPose:J

.field final layout:J

.field level:I

.field final listener:[F

.field final node:Ljava/lang/Object;

.field final nodeClass:Ljava/lang/Class;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/Class<",
            "*>;"
        }
    .end annotation
.end field

.field original:J

.field owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

.field final pending:Ljava/nio/ByteBuffer;

.field final pose:[F

.field previousDelay:J

.field final previousPose:[F

.field final rate:I

.field reverb:I

.field final scaled:Ljava/nio/ByteBuffer;

.field spatialClosed:Z

.field final stamp:Landroid/media/AudioTimestamp;

.field start:I

.field final track:Landroid/media/AudioTrack;

.field final updateClass:Ljava/lang/Class;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/lang/Class<",
            "*>;"
        }
    .end annotation
.end field

.field vr:Z

.field writtenFrames:J


# direct methods
.method constructor <init>(IIJIIZII)V
    .locals 4
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 110
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 97
    const/4 v0, 0x7

    new-array v1, v0, [F

    iput-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    new-array v0, v0, [F

    iput-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousPose:[F

    .line 99
    new-instance v0, Landroid/media/AudioTimestamp;

    invoke-direct {v0}, Landroid/media/AudioTimestamp;-><init>()V

    iput-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    .line 104
    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v0

    iput-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->epochNano:J

    .line 107
    const/4 v0, 0x6

    new-array v0, v0, [F

    fill-array-data v0, :array_0

    iput-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    .line 111
    iput p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->rate:I

    iput p2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->channels:I

    mul-int/lit8 v0, p2, 0x4

    iput v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    .line 112
    iput-wide p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->layout:J

    iput p6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->level:I

    iput-boolean p7, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->vr:Z

    iput p8, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->reverb:I

    iput p9, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->ambisonic:I

    .line 113
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide p3

    const-wide/16 p7, 0xc8

    add-long/2addr p3, p7

    .line 114
    iget-object p7, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    invoke-static {p7}, Lcn/vr4p/patch/GxrAudioOutput;->access$000([F)Z

    move-result p7

    .line 115
    :goto_0
    if-nez p7, :cond_0

    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide p8

    cmp-long p8, p8, p3

    if-gez p8, :cond_0

    const-wide/16 p7, 0x5

    invoke-static {p7, p8}, Landroid/os/SystemClock;->sleep(J)V

    iget-object p7, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    invoke-static {p7}, Lcn/vr4p/patch/GxrAudioOutput;->access$000([F)Z

    move-result p7

    goto :goto_0

    .line 116
    :cond_0
    if-eqz p7, :cond_4

    .line 117
    const p3, 0x1d4c00

    invoke-static {p3}, Ljava/nio/ByteBuffer;->allocateDirect(I)Ljava/nio/ByteBuffer;

    move-result-object p3

    invoke-static {}, Ljava/nio/ByteOrder;->nativeOrder()Ljava/nio/ByteOrder;

    move-result-object p4

    invoke-virtual {p3, p4}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    move-result-object p3

    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    .line 118
    const/high16 p3, 0x10000

    invoke-static {p3}, Ljava/nio/ByteBuffer;->allocateDirect(I)Ljava/nio/ByteBuffer;

    move-result-object p3

    invoke-static {}, Ljava/nio/ByteOrder;->nativeOrder()Ljava/nio/ByteOrder;

    move-result-object p4

    invoke-virtual {p3, p4}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    move-result-object p3

    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    .line 119
    const-string p3, "com.android.extensions.xr.node.Node"

    invoke-static {p3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object p3

    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    .line 120
    const-string p3, "com.android.extensions.xr.node.NodeTransaction"

    invoke-static {p3}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object p3

    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    .line 121
    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$100()Ljava/lang/Object;

    move-result-object p3

    invoke-virtual {p3}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p3

    const-string p4, "createNode"

    const/4 p7, 0x0

    new-array p8, p7, [Ljava/lang/Class;

    invoke-virtual {p3, p4, p8}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p3

    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$100()Ljava/lang/Object;

    move-result-object p4

    new-array p8, p7, [Ljava/lang/Object;

    invoke-virtual {p3, p4, p8}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p3

    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    .line 122
    nop

    .line 124
    const/4 p3, 0x0

    :try_start_0
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->applyPose()V

    .line 125
    const-string p4, "com.android.extensions.xr.media.PointSourceParams"

    invoke-static {p4}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object p4

    .line 126
    const-string p8, "com.android.extensions.xr.media.PointSourceParams$Builder"

    invoke-static {p8}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object p8

    .line 127
    new-array p9, p7, [Ljava/lang/Class;

    invoke-virtual {p8, p9}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object p9

    new-array v0, p7, [Ljava/lang/Object;

    invoke-virtual {p9, v0}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p9

    .line 128
    const-string v0, "setNode"

    const/4 v1, 0x1

    new-array v2, v1, [Ljava/lang/Class;

    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    aput-object v3, v2, p7

    invoke-virtual {p8, v0, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    new-array v2, v1, [Ljava/lang/Object;

    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    aput-object v3, v2, p7

    invoke-virtual {v0, p9, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 129
    const-string v0, "build"

    new-array v2, p7, [Ljava/lang/Class;

    invoke-virtual {p8, v0, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p8

    new-array v0, p7, [Ljava/lang/Object;

    invoke-virtual {p8, p9, v0}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p8

    .line 130
    new-instance p9, Landroid/media/AudioFormat$Builder;

    invoke-direct {p9}, Landroid/media/AudioFormat$Builder;-><init>()V

    invoke-virtual {p9, p1}, Landroid/media/AudioFormat$Builder;->setSampleRate(I)Landroid/media/AudioFormat$Builder;

    move-result-object p9

    const/4 v0, 0x4

    invoke-virtual {p9, v0}, Landroid/media/AudioFormat$Builder;->setEncoding(I)Landroid/media/AudioFormat$Builder;

    move-result-object p9

    invoke-virtual {p9, p5}, Landroid/media/AudioFormat$Builder;->setChannelMask(I)Landroid/media/AudioFormat$Builder;

    move-result-object p9

    invoke-virtual {p9}, Landroid/media/AudioFormat$Builder;->build()Landroid/media/AudioFormat;

    move-result-object p9

    .line 131
    invoke-static {p1, p5, v0}, Landroid/media/AudioTrack;->getMinBufferSize(III)I

    move-result v0

    .line 132
    if-lez v0, :cond_2

    .line 133
    new-instance p5, Landroid/media/AudioTrack$Builder;

    invoke-direct {p5}, Landroid/media/AudioTrack$Builder;-><init>()V

    invoke-virtual {p5, p9}, Landroid/media/AudioTrack$Builder;->setAudioFormat(Landroid/media/AudioFormat;)Landroid/media/AudioTrack$Builder;

    move-result-object p5

    new-instance p9, Landroid/media/AudioAttributes$Builder;

    invoke-direct {p9}, Landroid/media/AudioAttributes$Builder;-><init>()V

    .line 134
    invoke-virtual {p9, v1}, Landroid/media/AudioAttributes$Builder;->setUsage(I)Landroid/media/AudioAttributes$Builder;

    move-result-object p9

    .line 135
    const/4 v2, 0x3

    invoke-virtual {p9, v2}, Landroid/media/AudioAttributes$Builder;->setContentType(I)Landroid/media/AudioAttributes$Builder;

    move-result-object p9

    invoke-virtual {p9, p7}, Landroid/media/AudioAttributes$Builder;->setIsContentSpatialized(Z)Landroid/media/AudioAttributes$Builder;

    move-result-object p9

    invoke-virtual {p9}, Landroid/media/AudioAttributes$Builder;->build()Landroid/media/AudioAttributes;

    move-result-object p9

    .line 134
    invoke-virtual {p5, p9}, Landroid/media/AudioTrack$Builder;->setAudioAttributes(Landroid/media/AudioAttributes;)Landroid/media/AudioTrack$Builder;

    move-result-object p5

    .line 136
    invoke-virtual {p5, v1}, Landroid/media/AudioTrack$Builder;->setTransferMode(I)Landroid/media/AudioTrack$Builder;

    move-result-object p5

    iget p9, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    mul-int/2addr p9, p1

    div-int/2addr p9, v2

    invoke-static {v0, p9}, Ljava/lang/Math;->max(II)I

    move-result p9

    invoke-virtual {p5, p9}, Landroid/media/AudioTrack$Builder;->setBufferSizeInBytes(I)Landroid/media/AudioTrack$Builder;

    move-result-object p5

    .line 137
    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$200()Ljava/lang/Object;

    move-result-object p9

    invoke-virtual {p9}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p9

    const-string v0, "setPointSourceParams"

    const/4 v2, 0x2

    new-array v2, v2, [Ljava/lang/Class;

    const-class v3, Landroid/media/AudioTrack$Builder;

    aput-object v3, v2, p7

    aput-object p4, v2, v1

    invoke-virtual {p9, v0, v2}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p4

    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$200()Ljava/lang/Object;

    move-result-object p9

    filled-new-array {p5, p8}, [Ljava/lang/Object;

    move-result-object p8

    invoke-virtual {p4, p9, p8}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 138
    invoke-virtual {p5}, Landroid/media/AudioTrack$Builder;->build()Landroid/media/AudioTrack;

    move-result-object p3

    .line 139
    invoke-virtual {p3}, Landroid/media/AudioTrack;->getState()I

    move-result p4

    if-ne p4, v1, :cond_1

    invoke-virtual {p3}, Landroid/media/AudioTrack;->getChannelCount()I

    move-result p4

    if-ne p4, p2, :cond_1

    .line 140
    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$200()Ljava/lang/Object;

    move-result-object p2

    invoke-virtual {p2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object p2

    const-string p4, "getSpatialSourceType"

    new-array p5, v1, [Ljava/lang/Class;

    const-class p8, Landroid/media/AudioTrack;

    aput-object p8, p5, p7

    invoke-virtual {p2, p4, p5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p2

    invoke-static {}, Lcn/vr4p/patch/GxrAudioOutput;->access$200()Ljava/lang/Object;

    move-result-object p4

    filled-new-array {p3}, [Ljava/lang/Object;

    move-result-object p5

    invoke-virtual {p2, p4, p5}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object p2

    check-cast p2, Ljava/lang/Integer;

    invoke-virtual {p2}, Ljava/lang/Integer;->intValue()I

    move-result p2

    if-ne p2, v1, :cond_1

    .line 142
    invoke-virtual {p3}, Landroid/media/AudioTrack;->getBufferSizeInFrames()I

    move-result p2

    div-int/lit8 p1, p1, 0x32

    invoke-static {p2, p1}, Ljava/lang/Math;->min(II)I

    move-result p1

    invoke-virtual {p3, p1}, Landroid/media/AudioTrack;->setStartThresholdInFrames(I)I

    .line 143
    invoke-virtual {p3}, Landroid/media/AudioTrack;->play()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 144
    nop

    .line 145
    iput-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {p0, p6}, Lcn/vr4p/patch/GxrAudioOutput$Output;->update(I)V

    .line 146
    return-void

    .line 141
    :cond_1
    :try_start_1
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "spatial track rejected"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    .line 132
    :cond_2
    new-instance p1, Ljava/lang/IllegalStateException;

    new-instance p2, Ljava/lang/StringBuilder;

    invoke-direct {p2}, Ljava/lang/StringBuilder;-><init>()V

    const-string p4, "unsupported format "

    invoke-virtual {p2, p4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2, p5}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p2

    invoke-virtual {p2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p2

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 144
    :catchall_0
    move-exception p1

    if-eqz p3, :cond_3

    invoke-virtual {p3}, Landroid/media/AudioTrack;->release()V

    :cond_3
    iget-object p2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    const-string p3, "dispose"

    new-array p4, p7, [Ljava/lang/Class;

    invoke-virtual {p2, p3, p4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object p2

    iget-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    new-array p4, p7, [Ljava/lang/Object;

    invoke-virtual {p2, p3, p4}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    throw p1

    .line 116
    :cond_4
    new-instance p1, Ljava/lang/IllegalStateException;

    const-string p2, "movie pose unavailable"

    invoke-direct {p1, p2}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw p1

    :array_0
    .array-data 4
        0x0
        0x0
        -0x40800000    # -1.0f
        0x0
        0x3f800000    # 1.0f
        0x0
    .end array-data
.end method

.method private applyPose()V
    .locals 11
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Exception;
        }
    .end annotation

    .line 148
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    const/4 v1, 0x0

    new-array v2, v1, [Ljava/lang/Class;

    invoke-virtual {v0, v2}, Ljava/lang/Class;->getConstructor([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;

    move-result-object v0

    new-array v2, v1, [Ljava/lang/Object;

    invoke-virtual {v0, v2}, Ljava/lang/reflect/Constructor;->newInstance([Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v0

    .line 150
    :try_start_0
    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    const-string v3, "setName"

    const/4 v4, 0x2

    new-array v5, v4, [Ljava/lang/Class;

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    aput-object v6, v5, v1

    const-class v6, Ljava/lang/String;

    const/4 v7, 0x1

    aput-object v6, v5, v7

    invoke-virtual {v2, v3, v5}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v3, v4, [Ljava/lang/Object;

    iget-object v5, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    aput-object v5, v3, v1

    const-string v5, "4XVR movie audio"

    aput-object v5, v3, v7

    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 151
    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    const-string v3, "setPosition"

    const/4 v5, 0x4

    new-array v6, v5, [Ljava/lang/Class;

    iget-object v8, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    aput-object v8, v6, v1

    sget-object v8, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v8, v6, v7

    sget-object v8, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v8, v6, v4

    sget-object v8, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    const/4 v9, 0x3

    aput-object v8, v6, v9

    invoke-virtual {v2, v3, v6}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v3, v5, [Ljava/lang/Object;

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    aput-object v6, v3, v1

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v6, v6, v1

    invoke-static {v6}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v6

    aput-object v6, v3, v7

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v6, v6, v7

    invoke-static {v6}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v6

    aput-object v6, v3, v4

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v6, v6, v4

    invoke-static {v6}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v6

    aput-object v6, v3, v9

    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 152
    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    const-string v3, "setOrientation"

    const/4 v6, 0x5

    new-array v8, v6, [Ljava/lang/Class;

    iget-object v10, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    aput-object v10, v8, v1

    sget-object v10, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v10, v8, v7

    sget-object v10, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v10, v8, v4

    sget-object v10, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v10, v8, v9

    sget-object v10, Ljava/lang/Float;->TYPE:Ljava/lang/Class;

    aput-object v10, v8, v5

    invoke-virtual {v2, v3, v8}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v3, v6, [Ljava/lang/Object;

    iget-object v8, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    aput-object v8, v3, v1

    iget-object v8, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v8, v8, v9

    invoke-static {v8}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v8

    aput-object v8, v3, v7

    iget-object v7, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v7, v7, v5

    invoke-static {v7}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v7

    aput-object v7, v3, v4

    iget-object v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v4, v4, v6

    invoke-static {v4}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v4

    aput-object v4, v3, v9

    iget-object v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    const/4 v6, 0x6

    aget v4, v4, v6

    invoke-static {v4}, Ljava/lang/Float;->valueOf(F)Ljava/lang/Float;

    move-result-object v4

    aput-object v4, v3, v5

    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 153
    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->updateClass:Ljava/lang/Class;

    const-string v3, "apply"

    new-array v4, v1, [Ljava/lang/Class;

    invoke-virtual {v2, v3, v4}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v2

    new-array v3, v1, [Ljava/lang/Object;

    invoke-virtual {v2, v0, v3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    .line 154
    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousPose:[F

    const/4 v4, 0x7

    invoke-static {v2, v1, v3, v1, v4}, Ljava/lang/System;->arraycopy(Ljava/lang/Object;ILjava/lang/Object;II)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 155
    check-cast v0, Ljava/io/Closeable;

    invoke-interface {v0}, Ljava/io/Closeable;->close()V

    .line 156
    return-void

    .line 155
    :catchall_0
    move-exception v1

    check-cast v0, Ljava/io/Closeable;

    invoke-interface {v0}, Ljava/io/Closeable;->close()V

    throw v1
.end method

.method private consumedFrames()J
    .locals 6

    .line 237
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->getPlaybackHeadPosition()I

    move-result v0

    int-to-long v0, v0

    const-wide v2, 0xffffffffL

    and-long/2addr v0, v2

    .line 238
    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastHead:J

    cmp-long v2, v0, v2

    if-gez v2, :cond_0

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastHead:J

    const-wide v4, 0xf0000000L

    cmp-long v2, v2, v4

    if-lez v2, :cond_0

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->headEpoch:J

    const-wide v4, 0x100000000L

    add-long/2addr v2, v4

    iput-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->headEpoch:J

    .line 239
    :cond_0
    iput-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastHead:J

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->headEpoch:J

    add-long/2addr v2, v0

    return-wide v2
.end method

.method private drain()V
    .locals 7

    .line 199
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-eqz v0, :cond_0

    return-void

    .line 200
    :cond_0
    :goto_0
    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    const/4 v2, 0x0

    if-le v0, v1, :cond_6

    .line 201
    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    sub-int/2addr v0, v1

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->capacity()I

    move-result v1

    iget v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    div-int/2addr v1, v3

    iget v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    mul-int/2addr v1, v3

    invoke-static {v0, v1}, Ljava/lang/Math;->min(II)I

    move-result v0

    .line 202
    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    iget v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    add-int/2addr v3, v0

    invoke-virtual {v1, v3}, Ljava/nio/ByteBuffer;->limit(I)Ljava/nio/Buffer;

    move-result-object v1

    iget v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    invoke-virtual {v1, v3}, Ljava/nio/Buffer;->position(I)Ljava/nio/Buffer;

    .line 203
    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    .line 204
    iget v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->gain:F

    const/high16 v4, 0x3f800000    # 1.0f

    cmpl-float v3, v3, v4

    if-eqz v3, :cond_2

    .line 205
    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->clear()Ljava/nio/Buffer;

    move v1, v2

    :goto_1
    if-ge v1, v0, :cond_1

    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    iget-object v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    iget v5, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    add-int/2addr v5, v1

    invoke-virtual {v4, v5}, Ljava/nio/ByteBuffer;->getFloat(I)F

    move-result v4

    iget v5, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->gain:F

    mul-float/2addr v4, v5

    invoke-virtual {v3, v4}, Ljava/nio/ByteBuffer;->putFloat(F)Ljava/nio/ByteBuffer;

    add-int/lit8 v1, v1, 0x4

    goto :goto_1

    .line 206
    :cond_1
    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    invoke-virtual {v1}, Ljava/nio/ByteBuffer;->flip()Ljava/nio/Buffer;

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->scaled:Ljava/nio/ByteBuffer;

    .line 208
    :cond_2
    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    const/4 v4, 0x1

    invoke-virtual {v3, v1, v0, v4}, Landroid/media/AudioTrack;->write(Ljava/nio/ByteBuffer;II)I

    move-result v0

    .line 209
    if-ltz v0, :cond_5

    .line 210
    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    rem-int v1, v0, v1

    if-nez v1, :cond_4

    .line 211
    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    add-int/2addr v1, v0

    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    iget-wide v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    div-int v1, v0, v1

    int-to-long v5, v1

    add-long/2addr v3, v5

    iput-wide v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    .line 212
    if-nez v0, :cond_3

    goto :goto_2

    .line 213
    :cond_3
    goto :goto_0

    .line 210
    :cond_4
    new-instance v0, Ljava/lang/IllegalStateException;

    const-string v1, "partial PCM frame"

    invoke-direct {v0, v1}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v0

    .line 209
    :cond_5
    new-instance v1, Ljava/lang/IllegalStateException;

    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    const-string v3, "AudioTrack.write="

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {v1, v0}, Ljava/lang/IllegalStateException;-><init>(Ljava/lang/String;)V

    throw v1

    .line 214
    :cond_6
    :goto_2
    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    if-ne v0, v1, :cond_7

    iput v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iput v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    .line 215
    :cond_7
    return-void
.end method

.method private fallback(Ljava/lang/String;)V
    .locals 10

    .line 185
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-nez v0, :cond_4

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    goto/16 :goto_0

    .line 186
    :cond_0
    invoke-virtual {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->playing()Z

    move-result v0

    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->releaseSpatial()V

    .line 187
    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->rate:I

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->channels:I

    iget-wide v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->layout:J

    iget v5, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->level:I

    iget-boolean v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->vr:Z

    iget v7, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->reverb:I

    iget v8, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->ambisonic:I

    invoke-static/range {v1 .. v8}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->CreateAAudio(IIJIZII)J

    move-result-wide v1

    iput-wide v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    .line 188
    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

    if-eqz v1, :cond_1

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousDelay:J

    iput-wide v2, v1, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->m_lAudioDelayTime:J

    .line 189
    :cond_1
    if-nez v0, :cond_2

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->PauseAAudio(J)V

    .line 190
    :cond_2
    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x0

    aget v4, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v5, 0x1

    aget v5, v0, v5

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v6, 0x2

    aget v6, v0, v6

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v7, 0x3

    aget v7, v0, v7

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v8, 0x4

    aget v8, v0, v8

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v9, 0x5

    aget v9, v0, v9

    invoke-static/range {v2 .. v9}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->SetListenPoseAAudio(JFFFFFF)V

    .line 191
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v2, "Original output resumed: "

    invoke-virtual {v0, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p1

    invoke-virtual {p1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p1

    const-string v0, "4XVRAudio"

    invoke-static {v0, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 192
    iget p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    if-le p1, v0, :cond_3

    .line 193
    iget-object p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    invoke-virtual {p1, v0}, Ljava/nio/ByteBuffer;->limit(I)Ljava/nio/Buffer;

    move-result-object p1

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    invoke-virtual {p1, v0}, Ljava/nio/Buffer;->position(I)Ljava/nio/Buffer;

    iget-object p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {p1}, Ljava/nio/ByteBuffer;->compact()Ljava/nio/ByteBuffer;

    iget p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    sub-int/2addr p1, v0

    .line 194
    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-static {v2, v3, v0, p1, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->WriteAAudio(JLjava/nio/ByteBuffer;IZ)V

    .line 196
    :cond_3
    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    .line 197
    return-void

    .line 185
    :cond_4
    :goto_0
    return-void
.end method

.method private releaseSpatial()V
    .locals 4

    .line 181
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->spatialClosed:Z

    if-eqz v0, :cond_0

    return-void

    :cond_0
    const/4 v0, 0x1

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->spatialClosed:Z

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->release()V

    .line 182
    :try_start_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->nodeClass:Ljava/lang/Class;

    const-string v1, "dispose"

    const/4 v2, 0x0

    new-array v3, v2, [Ljava/lang/Class;

    invoke-virtual {v0, v1, v3}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v0

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->node:Ljava/lang/Object;

    new-array v2, v2, [Ljava/lang/Object;

    invoke-virtual {v0, v1, v2}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
    :try_end_0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_0} :catch_0

    goto :goto_0

    :catch_0
    move-exception v0

    const-string v1, "4XVRAudio"

    const-string v2, "Audio node close failed"

    invoke-static {v1, v2, v0}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 183
    :goto_0
    return-void
.end method


# virtual methods
.method declared-synchronized close()V
    .locals 4

    monitor-enter p0

    .line 278
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v0, :cond_0

    monitor-exit p0

    return-void

    :cond_0
    const/4 v0, 0x1

    :try_start_1
    iput-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    .line 279
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->ReleaseAAudio(J)V

    goto :goto_0

    :cond_1
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->releaseSpatial()V

    .line 280
    :goto_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

    if-eqz v0, :cond_2

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->owner:Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;

    iget-wide v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousDelay:J

    iput-wide v1, v0, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->m_lAudioDelayTime:J
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 281
    :cond_2
    monitor-exit p0

    return-void

    .line 277
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized listener(FFFFFF)V
    .locals 9

    monitor-enter p0

    .line 177
    :try_start_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x0

    aput p1, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x1

    aput p2, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x2

    aput p3, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x3

    aput p4, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x4

    aput p5, v0, v1

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->listener:[F

    const/4 v1, 0x5

    aput p6, v0, v1

    .line 178
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    iget-wide v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    move v3, p1

    move v4, p2

    move v5, p3

    move v6, p4

    move v7, p5

    move v8, p6

    invoke-static/range {v1 .. v8}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->SetListenPoseAAudio(JFFFFFF)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 179
    :cond_0
    monitor-exit p0

    return-void

    .line 176
    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method declared-synchronized needWait()Z
    .locals 8

    monitor-enter p0

    .line 242
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const/4 v1, 0x0

    if-eqz v0, :cond_0

    monitor-exit p0

    return v1

    .line 243
    :cond_0
    :try_start_1
    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v4, 0x0

    cmp-long v0, v2, v4

    if-eqz v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->NeedWaitAAudio(J)Z

    move-result v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return v0

    .line 244
    :cond_1
    :try_start_2
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->drain()V
    :try_end_2
    .catch Ljava/lang/RuntimeException; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    .line 247
    :try_start_3
    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->consumedFrames()J

    move-result-wide v6

    sub-long/2addr v2, v6

    invoke-static {v4, v5, v2, v3}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v2

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    sub-int/2addr v0, v4

    iget v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    div-int/2addr v0, v4

    int-to-long v4, v0

    add-long/2addr v2, v4

    .line 248
    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->rate:I

    const v4, 0xbb80

    invoke-static {v0, v4}, Ljava/lang/Math;->min(II)I

    move-result v0

    div-int/lit8 v0, v0, 0xa
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    int-to-long v4, v0

    cmp-long v0, v2, v4

    if-lez v0, :cond_2

    const/4 v1, 0x1

    :cond_2
    monitor-exit p0

    return v1

    .line 244
    :catch_0
    move-exception v0

    :try_start_4
    invoke-virtual {v0}, Ljava/lang/RuntimeException;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->NeedWaitAAudio(J)Z

    move-result v0
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    monitor-exit p0

    return v0

    .line 241
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized pause()V
    .locals 4

    monitor-enter p0

    .line 265
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-nez v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->PauseAAudio(J)V

    goto :goto_0

    :cond_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->pause()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_1
    :goto_0
    monitor-exit p0

    return-void

    .line 265
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized play()V
    .locals 4

    monitor-enter p0

    .line 264
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-nez v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->PlayAAudio(J)V

    goto :goto_0

    :cond_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->play()V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    :cond_1
    :goto_0
    monitor-exit p0

    return-void

    .line 264
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized playing()Z
    .locals 4

    monitor-enter p0

    .line 263
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-nez v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->IsPlayingAAudio(J)Z

    move-result v0

    if-eqz v0, :cond_1

    goto :goto_0

    :cond_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->getPlayState()I

    move-result v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    const/4 v1, 0x3

    if-ne v0, v1, :cond_1

    :goto_0
    const/4 v0, 0x1

    goto :goto_1

    :cond_1
    const/4 v0, 0x0

    :goto_1
    monitor-exit p0

    return v0

    .line 263
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized queuedFrames()I
    .locals 8

    monitor-enter p0

    .line 251
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v0, :cond_0

    monitor-exit p0

    const/4 v0, 0x0

    return v0

    .line 252
    :cond_0
    :try_start_1
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->GetSampleCountInBuf(J)I

    move-result v0
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return v0

    .line 253
    :cond_1
    :try_start_2
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->drain()V
    :try_end_2
    .catch Ljava/lang/RuntimeException; {:try_start_2 .. :try_end_2} :catch_0
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    .line 255
    :try_start_3
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    invoke-virtual {v0, v1}, Landroid/media/AudioTrack;->getTimestamp(Landroid/media/AudioTimestamp;)Z

    move-result v0

    if-eqz v0, :cond_3

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    iget-wide v0, v0, Landroid/media/AudioTimestamp;->nanoTime:J

    iget-wide v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->epochNano:J

    cmp-long v0, v0, v4

    if-ltz v0, :cond_3

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v0

    iget-object v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    iget-wide v4, v4, Landroid/media/AudioTimestamp;->nanoTime:J

    sub-long/2addr v0, v4

    const-wide/32 v4, 0x3b9aca00

    cmp-long v0, v0, v4

    if-gez v0, :cond_3

    .line 256
    invoke-virtual {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->playing()Z

    move-result v0

    if-eqz v0, :cond_2

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v0

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    iget-wide v6, v6, Landroid/media/AudioTimestamp;->nanoTime:J

    sub-long/2addr v0, v6

    invoke-static {v2, v3, v0, v1}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v0

    iget v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->rate:I

    int-to-long v6, v6

    mul-long/2addr v0, v6

    div-long/2addr v0, v4

    goto :goto_0

    :cond_2
    move-wide v0, v2

    .line 257
    :goto_0
    iget-wide v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    iget-object v6, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->stamp:Landroid/media/AudioTimestamp;

    iget-wide v6, v6, Landroid/media/AudioTimestamp;->framePosition:J

    add-long/2addr v6, v0

    invoke-static {v4, v5, v6, v7}, Ljava/lang/Math;->min(JJ)J

    move-result-wide v0

    .line 258
    goto :goto_1

    .line 259
    :cond_3
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->consumedFrames()J

    move-result-wide v0

    .line 261
    :goto_1
    iget-wide v4, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    sub-long/2addr v4, v0

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    sub-int/2addr v0, v1

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    div-int/2addr v0, v1

    int-to-long v0, v0

    add-long/2addr v4, v0

    invoke-static {v2, v3, v4, v5}, Ljava/lang/Math;->max(JJ)J

    move-result-wide v0
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    long-to-int v0, v0

    monitor-exit p0

    return v0

    .line 253
    :catch_0
    move-exception v0

    :try_start_4
    invoke-virtual {v0}, Ljava/lang/RuntimeException;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->GetSampleCountInBuf(J)I

    move-result v0
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    monitor-exit p0

    return v0

    .line 250
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized reset()V
    .locals 4

    monitor-enter p0

    .line 267
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v0, :cond_0

    monitor-exit p0

    return-void

    .line 268
    :cond_0
    :try_start_1
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->ambisonic:I

    invoke-static {v0, v1, v2}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->ResetAAudio(JI)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    .line 269
    :cond_1
    :try_start_2
    invoke-virtual {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->playing()Z

    move-result v0

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v1}, Landroid/media/AudioTrack;->pause()V

    iget-object v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v1}, Landroid/media/AudioTrack;->flush()V

    .line 270
    const/4 v1, 0x0

    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    iput-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastHead:J

    iput-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->headEpoch:J

    iput-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->writtenFrames:J

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v1

    iput-wide v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->epochNano:J

    .line 271
    if-eqz v0, :cond_2

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->track:Landroid/media/AudioTrack;

    invoke-virtual {v0}, Landroid/media/AudioTrack;->play()V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    .line 272
    :cond_2
    monitor-exit p0

    return-void

    .line 266
    :catchall_0
    move-exception v0

    monitor-exit p0

    throw v0
.end method

.method declared-synchronized resetAmbisonic(I)V
    .locals 4

    monitor-enter p0

    .line 275
    :try_start_0
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_0

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1, p1}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->ResetAAudio(JI)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 276
    :cond_0
    monitor-exit p0

    return-void

    .line 274
    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method declared-synchronized update(I)V
    .locals 6

    monitor-enter p0

    .line 158
    if-nez p1, :cond_0

    const p1, 0x3f333333    # 0.7f

    goto :goto_0

    :cond_0
    const/4 v0, 0x2

    if-ne p1, v0, :cond_1

    const/high16 p1, 0x40000000    # 2.0f

    goto :goto_0

    :cond_1
    const/4 v0, 0x3

    if-ne p1, v0, :cond_2

    const/high16 p1, 0x40800000    # 4.0f

    goto :goto_0

    :cond_2
    const/high16 p1, 0x3f800000    # 1.0f

    :goto_0
    :try_start_0
    iput p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->gain:F

    .line 159
    invoke-static {}, Landroid/os/SystemClock;->uptimeMillis()J

    move-result-wide v0

    .line 160
    iget-boolean p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z

    if-nez p1, :cond_7

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v4, 0x0

    cmp-long p1, v2, v4

    if-nez p1, :cond_7

    iget-wide v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastPose:J

    sub-long v2, v0, v2

    const-wide/16 v4, 0x32

    cmp-long p1, v2, v4

    if-gez p1, :cond_3

    goto :goto_5

    .line 161
    :cond_3
    iput-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->lastPose:J
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 163
    :try_start_1
    iget-object p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    invoke-static {p1}, Lcn/vr4p/patch/GxrAudioOutput;->access$000([F)Z

    move-result p1

    if-eqz p1, :cond_6

    .line 164
    nop

    .line 165
    const/4 p1, 0x0

    move v0, p1

    move v1, v0

    :goto_1
    const/4 v2, 0x7

    if-ge v0, v2, :cond_5

    iget-object v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pose:[F

    aget v2, v2, v0

    iget-object v3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->previousPose:[F

    aget v3, v3, v0

    sub-float/2addr v2, v3

    invoke-static {v2}, Ljava/lang/Math;->abs(F)F

    move-result v2

    const v3, 0x38d1b717    # 1.0E-4f

    cmpl-float v2, v2, v3

    if-lez v2, :cond_4

    const/4 v2, 0x1

    goto :goto_2

    :cond_4
    move v2, p1

    :goto_2
    or-int/2addr v1, v2

    add-int/lit8 v0, v0, 0x1

    goto :goto_1

    .line 166
    :cond_5
    if-eqz v1, :cond_6

    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->applyPose()V
    :try_end_1
    .catch Ljava/lang/Exception; {:try_start_1 .. :try_end_1} :catch_0
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    .line 168
    :cond_6
    :goto_3
    goto :goto_4

    :catch_0
    move-exception p1

    :try_start_2
    const-string v0, "4XVRAudio"

    const-string v1, "movie audio pose update failed"

    invoke-static {v0, v1, p1}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    goto :goto_3

    .line 169
    :goto_4
    monitor-exit p0

    return-void

    .line 160
    :cond_7
    :goto_5
    monitor-exit p0

    return-void

    .line 157
    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method declared-synchronized updateSettings(IZI)V
    .locals 4

    monitor-enter p0

    .line 171
    :try_start_0
    iput p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->level:I

    iput-boolean p2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->vr:Z

    iput p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->reverb:I

    .line 172
    if-eqz p2, :cond_0

    const/4 v0, 0x2

    if-ne p3, v0, :cond_1

    :cond_0
    const-string v0, "original spatial/reverb option selected"

    invoke-direct {p0, v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V

    .line 173
    :cond_1
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_2

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1, p1, p2, p3}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->UpdateVolumeBoost(JIZI)V

    goto :goto_0

    .line 174
    :cond_2
    invoke-virtual {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput$Output;->update(I)V
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    .line 175
    :goto_0
    monitor-exit p0

    return-void

    .line 170
    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method

.method declared-synchronized write(Ljava/nio/ByteBuffer;IZ)V
    .locals 4

    monitor-enter p0

    .line 217
    :try_start_0
    iget-boolean v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->closed:Z
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    if-eqz v0, :cond_0

    monitor-exit p0

    return-void

    .line 218
    :cond_0
    :try_start_1
    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-eqz v0, :cond_1

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1, p1, p2, p3}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->WriteAAudio(JLjava/nio/ByteBuffer;IZ)V
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    monitor-exit p0

    return-void

    .line 219
    :cond_1
    if-ltz p2, :cond_5

    :try_start_2
    invoke-virtual {p1}, Ljava/nio/ByteBuffer;->capacity()I

    move-result v0

    if-gt p2, v0, :cond_5

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->frameBytes:I

    rem-int v0, p2, v0

    if-nez v0, :cond_5

    .line 220
    if-eqz p3, :cond_2

    invoke-virtual {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->reset()V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_0

    .line 221
    :cond_2
    :try_start_3
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->drain()V
    :try_end_3
    .catch Ljava/lang/RuntimeException; {:try_start_3 .. :try_end_3} :catch_1
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    .line 223
    nop

    .line 224
    :try_start_4
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->capacity()I

    move-result v0

    iget v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    sub-int/2addr v0, v1

    const/4 v1, 0x0

    if-ge v0, p2, :cond_3

    .line 225
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    invoke-virtual {v0, v2}, Ljava/nio/ByteBuffer;->limit(I)Ljava/nio/Buffer;

    move-result-object v0

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    invoke-virtual {v0, v2}, Ljava/nio/Buffer;->position(I)Ljava/nio/Buffer;

    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->compact()Ljava/nio/ByteBuffer;

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    sub-int/2addr v0, v2

    iput v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    iput v1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->start:I

    .line 227
    :cond_3
    iget-object v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {v0}, Ljava/nio/ByteBuffer;->capacity()I

    move-result v0

    iget v2, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    sub-int/2addr v0, v2

    if-ge v0, p2, :cond_4

    .line 228
    const-string v0, "bounded audio queue full"

    invoke-direct {p0, v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1, p1, p2, p3}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->WriteAAudio(JLjava/nio/ByteBuffer;IZ)V
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_0

    monitor-exit p0

    return-void

    .line 230
    :cond_4
    :try_start_5
    invoke-virtual {p1}, Ljava/nio/ByteBuffer;->duplicate()Ljava/nio/ByteBuffer;

    move-result-object p1

    invoke-static {}, Ljava/nio/ByteOrder;->nativeOrder()Ljava/nio/ByteOrder;

    move-result-object p3

    invoke-virtual {p1, p3}, Ljava/nio/ByteBuffer;->order(Ljava/nio/ByteOrder;)Ljava/nio/ByteBuffer;

    move-result-object p1

    invoke-virtual {p1, v1}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    move-result-object p3

    invoke-virtual {p3, p2}, Ljava/nio/Buffer;->limit(I)Ljava/nio/Buffer;

    .line 231
    iget-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->clear()Ljava/nio/Buffer;

    move-result-object p3

    iget v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    invoke-virtual {p3, v0}, Ljava/nio/Buffer;->position(I)Ljava/nio/Buffer;

    .line 232
    iget-object p3, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->pending:Ljava/nio/ByteBuffer;

    invoke-virtual {p3, p1}, Ljava/nio/ByteBuffer;->put(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;

    .line 233
    iget p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I

    add-int/2addr p1, p2

    iput p1, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->end:I
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_0

    .line 234
    :try_start_6
    invoke-direct {p0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->drain()V
    :try_end_6
    .catch Ljava/lang/RuntimeException; {:try_start_6 .. :try_end_6} :catch_0
    .catchall {:try_start_6 .. :try_end_6} :catchall_0

    goto :goto_0

    :catch_0
    move-exception p1

    :try_start_7
    invoke-virtual {p1}, Ljava/lang/RuntimeException;->toString()Ljava/lang/String;

    move-result-object p1

    invoke-direct {p0, p1}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V
    :try_end_7
    .catchall {:try_start_7 .. :try_end_7} :catchall_0

    .line 235
    :goto_0
    monitor-exit p0

    return-void

    .line 221
    :catch_1
    move-exception v0

    .line 222
    :try_start_8
    invoke-virtual {v0}, Ljava/lang/RuntimeException;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-direct {p0, v0}, Lcn/vr4p/patch/GxrAudioOutput$Output;->fallback(Ljava/lang/String;)V

    iget-wide v0, p0, Lcn/vr4p/patch/GxrAudioOutput$Output;->original:J

    invoke-static {v0, v1, p1, p2, p3}, Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer;->WriteAAudio(JLjava/nio/ByteBuffer;IZ)V
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_0

    monitor-exit p0

    return-void

    .line 219
    :cond_5
    :try_start_9
    new-instance p1, Ljava/lang/IllegalArgumentException;

    const-string p2, "PCM block"

    invoke-direct {p1, p2}, Ljava/lang/IllegalArgumentException;-><init>(Ljava/lang/String;)V

    throw p1
    :try_end_9
    .catchall {:try_start_9 .. :try_end_9} :catchall_0

    .line 216
    :catchall_0
    move-exception p1

    monitor-exit p0

    throw p1
.end method
