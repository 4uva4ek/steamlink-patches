.class public final Ldev/eni/hdrdirect/HdrBridge;
.super Ljava/lang/Object;
.source "HdrBridge.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static native beginMovieTransition()V
.end method

.method public static isHlg(Landroid/media/MediaFormat;Z)Z
    .locals 3

    if-eqz p0, :cond_1

    :try_start_0
    const-string v0, "color-standard"

    invoke-virtual {p0, v0}, Landroid/media/MediaFormat;->containsKey(Ljava/lang/String;)Z

    move-result v1

    if-eqz v1, :cond_1

    const-string v1, "color-transfer"

    invoke-virtual {p0, v1}, Landroid/media/MediaFormat;->containsKey(Ljava/lang/String;)Z

    move-result v2

    if-eqz v2, :cond_1

    invoke-virtual {p0, v0}, Landroid/media/MediaFormat;->getInteger(Ljava/lang/String;)I

    move-result v0

    const/4 v2, 0x6

    if-ne v0, v2, :cond_0

    invoke-virtual {p0, v1}, Landroid/media/MediaFormat;->getInteger(Ljava/lang/String;)I

    move-result v0

    const/4 v1, 0x7

    if-ne v0, v1, :cond_0

    const/4 v0, 0x1

    return v0

    :cond_0
    const/4 v0, 0x0

    return v0
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0

    :catch_0
    :cond_1
    return p1
.end method

.method public static native notifyMovie(IIIIIZZ)V
.end method

.method public static native notifySeek()V
.end method

.method public static native routeOutputSurface(Landroid/media/MediaCodec;Landroid/view/Surface;Landroid/media/MediaFormat;Ljava/lang/Object;)Landroid/view/Surface;
.end method

.method public static native routeResumeSurface(Landroid/media/MediaCodec;Landroid/view/Surface;)Landroid/view/Surface;
.end method
