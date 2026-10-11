.class final Lcn/vr4p/patch/SubtitleStyleListener$Cue;
.super Ljava/lang/Object;
.source "SubtitleStyleListener.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingClass;
    value = Lcn/vr4p/patch/SubtitleStyleListener;
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x1a
    name = "Cue"
.end annotation


# instance fields
.field final generation:I

.field final italic:Z

.field final text:Ljava/lang/String;

.field final times:[J


# direct methods
.method constructor <init>([JLjava/lang/String;ZI)V
    .locals 0

    .line 21
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 22
    iput-object p1, p0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->times:[J

    iput-object p2, p0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->text:Ljava/lang/String;

    iput-boolean p3, p0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->italic:Z

    iput p4, p0, Lcn/vr4p/patch/SubtitleStyleListener$Cue;->generation:I

    .line 23
    return-void
.end method
