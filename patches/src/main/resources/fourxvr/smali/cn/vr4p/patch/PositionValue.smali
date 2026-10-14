.class public final Lcn/vr4p/patch/PositionValue;
.super Ljava/lang/Object;
.source "PositionValue.java"


# direct methods
.method private constructor <init>()V
    .locals 0

    .line 4
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static clamp(I)I
    .locals 1

    .line 5
    const/16 v0, 0x64

    invoke-static {v0, p0}, Ljava/lang/Math;->min(II)I

    move-result p0

    const/16 v0, -0x64

    invoke-static {v0, p0}, Ljava/lang/Math;->max(II)I

    move-result p0

    return p0
.end method

.method public static label(I)Ljava/lang/String;
    .locals 2

    .line 10
    invoke-static {p0}, Lcn/vr4p/patch/PositionValue;->clamp(I)I

    move-result p0

    .line 11
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    if-lez p0, :cond_0

    const-string v1, "+"

    goto :goto_0

    :cond_0
    const-string v1, ""

    :goto_0
    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object p0

    const-string v0, "%"

    invoke-virtual {p0, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object p0

    invoke-virtual {p0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    return-object p0
.end method

.method public static step(II)I
    .locals 0

    .line 7
    invoke-static {p0}, Lcn/vr4p/patch/PositionValue;->clamp(I)I

    move-result p0

    invoke-static {p1}, Ljava/lang/Integer;->signum(I)I

    move-result p1

    add-int/2addr p0, p1

    invoke-static {p0}, Lcn/vr4p/patch/PositionValue;->clamp(I)I

    move-result p0

    return p0
.end method
