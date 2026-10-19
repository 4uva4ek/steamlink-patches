.class public final Lcn/vr4p/patch/SubtitleFont;
.super Ljava/lang/Object;
.source "SubtitleFont.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "4XVRSubtitleFont"


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 15
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static load()[Landroid/graphics/Typeface;
    .locals 6

    .line 20
    const-string v0, "4XVRSubtitleFont"

    const/4 v1, 0x0

    :try_start_0
    new-instance v2, Ljava/io/File;

    .line 21
    invoke-static {}, Landroid/os/Environment;->getExternalStorageDirectory()Ljava/io/File;

    move-result-object v3

    const-string v4, "4XPlayer/fonts"

    invoke-direct {v2, v3, v4}, Ljava/io/File;-><init>(Ljava/io/File;Ljava/lang/String;)V

    .line 20
    invoke-static {v2}, Lcn/vr4p/patch/SubtitleFontFiles;->select(Ljava/io/File;)Ljava/io/File;

    move-result-object v2

    .line 22
    if-nez v2, :cond_0

    return-object v1

    .line 23
    :cond_0
    invoke-static {v2}, Lcn/vr4p/patch/SubtitleFontFiles;->usable(Ljava/io/File;)Z

    move-result v3

    if-nez v3, :cond_1

    .line 24
    const-string v2, "Subtitle font file is empty or unreadable; using default font"

    invoke-static {v0, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 25
    return-object v1

    .line 28
    :cond_1
    new-instance v3, Landroid/graphics/Typeface$Builder;

    invoke-direct {v3, v2}, Landroid/graphics/Typeface$Builder;-><init>(Ljava/io/File;)V

    invoke-virtual {v3}, Landroid/graphics/Typeface$Builder;->build()Landroid/graphics/Typeface;

    move-result-object v3

    .line 29
    if-nez v3, :cond_2

    .line 30
    const-string v2, "Subtitle font file is not a usable font; using default font"

    invoke-static {v0, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;)I

    .line 31
    return-object v1

    .line 33
    :cond_2
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, "Custom subtitle font loaded: "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v2}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v4, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v2

    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v2

    invoke-static {v0, v2}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    .line 34
    const/4 v2, 0x2

    new-array v4, v2, [Landroid/graphics/Typeface;

    const/4 v5, 0x0

    aput-object v3, v4, v5

    invoke-static {v3, v2}, Landroid/graphics/Typeface;->create(Landroid/graphics/Typeface;I)Landroid/graphics/Typeface;

    move-result-object v2

    const/4 v3, 0x1

    aput-object v2, v4, v3
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0

    return-object v4

    .line 35
    :catch_0
    move-exception v2

    .line 36
    const-string v3, "Subtitle font could not be loaded; using default font"

    invoke-static {v0, v3, v2}, Landroid/util/Log;->w(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)I

    .line 37
    return-object v1
.end method
