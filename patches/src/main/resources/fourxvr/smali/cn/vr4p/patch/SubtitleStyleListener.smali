.class public final Lcn/vr4p/patch/SubtitleStyleListener;
.super Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;
.source "SubtitleStyleListener.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcn/vr4p/patch/SubtitleStyleListener$Cue;
    }
.end annotation


# instance fields
.field private volatile current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

.field private final generation:Ljava/util/concurrent/atomic/AtomicInteger;


# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    .line 26
    invoke-direct {p0, p1}, Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;-><init>(Landroid/content/Context;)V

    .line 14
    new-instance p1, Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-direct {p1}, Ljava/util/concurrent/atomic/AtomicInteger;-><init>()V

    iput-object p1, p0, Lcn/vr4p/patch/SubtitleStyleListener;->generation:Ljava/util/concurrent/atomic/AtomicInteger;

    .line 27
    const-string p1, "4XVRSubtitleStyle"

    const-string v0, "Subtitle style refresh ready"

    invoke-static {p1, v0}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 28
    return-void
.end method

.method private refresh()V
    .locals 6

    .line 54
    iget-object v0, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 55
    if-eqz v0, :cond_4

    iget-boolean v1, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->italic:Z

    sget-boolean v2, Lcn/vr4p/vr4pmovieplayer/V4Config;->m_bUseSubtitleItalic:Z

    if-ne v1, v2, :cond_0

    goto :goto_1

    .line 56
    :cond_0
    iget v1, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->generation:I

    iget-object v2, p0, Lcn/vr4p/patch/SubtitleStyleListener;->generation:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {v2}, Ljava/util/concurrent/atomic/AtomicInteger;->get()I

    move-result v2

    const/4 v3, 0x0

    if-ne v1, v2, :cond_2

    iget-object v1, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->times:[J

    const/4 v2, 0x4

    aget-wide v1, v1, v2

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v4

    cmp-long v1, v1, v4

    if-gtz v1, :cond_1

    goto :goto_0

    .line 60
    :cond_1
    sget-boolean v1, Lcn/vr4p/vr4pmovieplayer/V4Config;->m_bUseSubtitleItalic:Z

    .line 62
    iget-object v2, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->times:[J

    iget-object v4, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->text:Ljava/lang/String;

    invoke-super {p0, v2, v4, v3}, Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;->UpdateSubtitle([JLjava/lang/String;Ljava/nio/ByteBuffer;)V

    .line 63
    new-instance v2, Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    iget-object v3, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->times:[J

    iget-object v4, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->text:Ljava/lang/String;

    iget v0, v0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->generation:I

    invoke-direct {v2, v3, v4, v1, v0}, Lcn/vr4p/patch/SubtitleStyleListener$Cue;-><init>([JLjava/lang/String;ZI)V

    iput-object v2, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 64
    return-void

    .line 57
    :cond_2
    :goto_0
    iget-object v1, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    if-ne v1, v0, :cond_3

    iput-object v3, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 58
    :cond_3
    return-void

    .line 55
    :cond_4
    :goto_1
    return-void
.end method

.method public static refresh(Lcn/vr4p/vr4pmovieplayer/Vr4pMediaPlayer$SubtitleListener;)V
    .locals 1

    .line 51
    instance-of v0, p0, Lcn/vr4p/patch/SubtitleStyleListener;

    if-eqz v0, :cond_0

    check-cast p0, Lcn/vr4p/patch/SubtitleStyleListener;

    invoke-direct {p0}, Lcn/vr4p/patch/SubtitleStyleListener;->refresh()V

    .line 52
    :cond_0
    return-void
.end method


# virtual methods
.method public ClearSubtitleData()V
    .locals 1

    .line 66
    iget-object v0, p0, Lcn/vr4p/patch/SubtitleStyleListener;->generation:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicInteger;->incrementAndGet()I

    const/4 v0, 0x0

    iput-object v0, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 67
    invoke-super {p0}, Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;->ClearSubtitleData()V

    .line 68
    return-void
.end method

.method public UpdateSubtitle([JLjava/lang/String;Ljava/nio/ByteBuffer;)V
    .locals 9

    .line 30
    const/4 v0, 0x3

    aget-wide v0, p1, v0

    const-wide/16 v2, 0x2

    cmp-long v0, v0, v2

    const/4 v1, 0x0

    if-nez v0, :cond_4

    const/4 v0, 0x4

    aget-wide v2, p1, v0

    invoke-static {}, Ljava/lang/System;->nanoTime()J

    move-result-wide v4

    cmp-long v0, v2, v4

    if-gtz v0, :cond_0

    goto :goto_2

    .line 36
    :cond_0
    const/16 v0, 0x9

    aget-wide v2, p1, v0

    const-wide/16 v4, 0x1

    cmp-long v2, v2, v4

    const-wide/16 v3, 0x0

    if-nez v2, :cond_1

    const/16 v2, 0xa

    aget-wide v5, p1, v2

    cmp-long v5, v5, v3

    if-lez v5, :cond_1

    if-eqz p3, :cond_1

    .line 37
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->duplicate()Ljava/nio/ByteBuffer;

    move-result-object p3

    const/4 v5, 0x0

    invoke-virtual {p3, v5}, Ljava/nio/ByteBuffer;->position(I)Ljava/nio/Buffer;

    .line 38
    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->remaining()I

    move-result v5

    if-lez v5, :cond_1

    .line 39
    aget-wide v5, p1, v2

    invoke-virtual {p3}, Ljava/nio/ByteBuffer;->remaining()I

    move-result v2

    int-to-long v7, v2

    invoke-static {v5, v6, v7, v8}, Ljava/lang/Math;->min(JJ)J

    move-result-wide v5

    long-to-int v2, v5

    new-array v2, v2, [B

    .line 40
    invoke-virtual {p3, v2}, Ljava/nio/ByteBuffer;->get([B)Ljava/nio/ByteBuffer;

    .line 41
    :try_start_0
    new-instance p3, Ljava/lang/String;

    sget-object v5, Lcn/vr4p/vr4pmovieplayer/V4Config;->m_strSubtitleCode:Ljava/lang/String;

    invoke-direct {p3, v2, v5}, Ljava/lang/String;-><init>([BLjava/lang/String;)V
    :try_end_0
    .catch Ljava/io/UnsupportedEncodingException; {:try_start_0 .. :try_end_0} :catch_0

    move-object p2, p3

    goto :goto_0

    :catch_0
    move-exception p3

    .line 44
    :cond_1
    :goto_0
    invoke-virtual {p1}, [J->clone()Ljava/lang/Object;

    move-result-object p1

    check-cast p1, [J

    aput-wide v3, p1, v0

    .line 45
    sget-boolean p3, Lcn/vr4p/vr4pmovieplayer/V4Config;->m_bUseSubtitleItalic:Z

    .line 46
    iget-object v0, p0, Lcn/vr4p/patch/SubtitleStyleListener;->generation:Ljava/util/concurrent/atomic/AtomicInteger;

    invoke-virtual {v0}, Ljava/util/concurrent/atomic/AtomicInteger;->get()I

    move-result v0

    .line 47
    invoke-super {p0, p1, p2, v1}, Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;->UpdateSubtitle([JLjava/lang/String;Ljava/nio/ByteBuffer;)V

    .line 48
    if-eqz p2, :cond_3

    invoke-virtual {p2}, Ljava/lang/String;->isEmpty()Z

    move-result v2

    if-eqz v2, :cond_2

    goto :goto_1

    :cond_2
    new-instance v1, Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    invoke-direct {v1, p1, p2, p3, v0}, Lcn/vr4p/patch/SubtitleStyleListener$Cue;-><init>([JLjava/lang/String;ZI)V

    :cond_3
    :goto_1
    iput-object v1, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 49
    return-void

    .line 31
    :cond_4
    :goto_2
    iput-object v1, p0, Lcn/vr4p/patch/SubtitleStyleListener;->current:Lcn/vr4p/patch/SubtitleStyleListener$Cue;

    .line 32
    invoke-super {p0, p1, p2, p3}, Lcn/vr4p/oculus4xvrplayerov/V4VRSubtitleListener;->UpdateSubtitle([JLjava/lang/String;Ljava/nio/ByteBuffer;)V

    .line 33
    return-void
.end method
