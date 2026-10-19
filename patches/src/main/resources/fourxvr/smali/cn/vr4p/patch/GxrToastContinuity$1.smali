.class Lcn/vr4p/patch/GxrToastContinuity$1;
.super Landroid/view/View;
.source "GxrToastContinuity.java"


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcn/vr4p/patch/GxrToastContinuity;->show()V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lcn/vr4p/patch/GxrToastContinuity;


# direct methods
.method constructor <init>(Lcn/vr4p/patch/GxrToastContinuity;Landroid/content/Context;)V
    .locals 0
    .annotation system Ldalvik/annotation/MethodParameters;
        accessFlags = {
            0x8010,
            0x0
        }
        names = {
            null,
            null
        }
    .end annotation

    .line 61
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity$1;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-direct {p0, p2}, Landroid/view/View;-><init>(Landroid/content/Context;)V

    return-void
.end method


# virtual methods
.method protected onMeasure(II)V
    .locals 0

    .line 62
    const/4 p1, 0x2

    invoke-virtual {p0, p1, p1}, Lcn/vr4p/patch/GxrToastContinuity$1;->setMeasuredDimension(II)V

    return-void
.end method
