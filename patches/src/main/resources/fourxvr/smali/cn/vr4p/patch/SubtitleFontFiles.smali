.class public final Lcn/vr4p/patch/SubtitleFontFiles;
.super Ljava/lang/Object;
.source "SubtitleFontFiles.java"


# static fields
.field public static final FOLDER:Ljava/lang/String; = "4XPlayer/fonts"


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 14
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method static compareNames(Ljava/lang/String;Ljava/lang/String;)I
    .locals 1

    .line 41
    sget-object v0, Ljava/lang/String;->CASE_INSENSITIVE_ORDER:Ljava/util/Comparator;

    invoke-interface {v0, p0, p1}, Ljava/util/Comparator;->compare(Ljava/lang/Object;Ljava/lang/Object;)I

    move-result v0

    .line 42
    if-eqz v0, :cond_0

    goto :goto_0

    :cond_0
    invoke-virtual {p0, p1}, Ljava/lang/String;->compareTo(Ljava/lang/String;)I

    move-result v0

    :goto_0
    return v0
.end method

.method static isFontName(Ljava/lang/String;)Z
    .locals 7

    .line 35
    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v0

    add-int/lit8 v0, v0, -0x4

    .line 36
    const/4 v5, 0x0

    const/4 v6, 0x4

    const/4 v2, 0x1

    const-string v4, ".ttf"

    move-object v1, p0

    move v3, v0

    invoke-virtual/range {v1 .. v6}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    move-result v1

    if-nez v1, :cond_1

    const/4 v5, 0x0

    const/4 v6, 0x4

    const/4 v2, 0x1

    const-string v4, ".otf"

    move-object v1, p0

    move v3, v0

    invoke-virtual/range {v1 .. v6}, Ljava/lang/String;->regionMatches(ZILjava/lang/String;II)Z

    move-result p0

    if-eqz p0, :cond_0

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    goto :goto_1

    :cond_1
    :goto_0
    const/4 p0, 0x1

    :goto_1
    return p0
.end method

.method public static select(Ljava/io/File;)Ljava/io/File;
    .locals 6

    .line 18
    const/4 v0, 0x0

    if-nez p0, :cond_0

    move-object p0, v0

    goto :goto_0

    :cond_0
    invoke-virtual {p0}, Ljava/io/File;->listFiles()[Ljava/io/File;

    move-result-object p0

    .line 19
    :goto_0
    if-nez p0, :cond_1

    return-object v0

    .line 20
    :cond_1
    nop

    .line 21
    array-length v1, p0

    const/4 v2, 0x0

    :goto_1
    if-ge v2, v1, :cond_5

    aget-object v3, p0, v2

    .line 22
    invoke-virtual {v3}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v4

    .line 23
    const-string v5, "."

    invoke-virtual {v4, v5}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z

    move-result v5

    if-nez v5, :cond_4

    invoke-static {v4}, Lcn/vr4p/patch/SubtitleFontFiles;->isFontName(Ljava/lang/String;)Z

    move-result v5

    if-eqz v5, :cond_4

    invoke-virtual {v3}, Ljava/io/File;->isFile()Z

    move-result v5

    if-nez v5, :cond_2

    goto :goto_2

    .line 24
    :cond_2
    if-eqz v0, :cond_3

    invoke-virtual {v0}, Ljava/io/File;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-static {v4, v5}, Lcn/vr4p/patch/SubtitleFontFiles;->compareNames(Ljava/lang/String;Ljava/lang/String;)I

    move-result v4

    if-gez v4, :cond_4

    :cond_3
    move-object v0, v3

    .line 21
    :cond_4
    :goto_2
    add-int/lit8 v2, v2, 0x1

    goto :goto_1

    .line 26
    :cond_5
    return-object v0
.end method

.method public static usable(Ljava/io/File;)Z
    .locals 4

    .line 31
    invoke-virtual {p0}, Ljava/io/File;->length()J

    move-result-wide v0

    const-wide/16 v2, 0x0

    cmp-long v0, v0, v2

    if-lez v0, :cond_0

    invoke-virtual {p0}, Ljava/io/File;->canRead()Z

    move-result p0

    if-eqz p0, :cond_0

    const/4 p0, 0x1

    goto :goto_0

    :cond_0
    const/4 p0, 0x0

    :goto_0
    return p0
.end method
