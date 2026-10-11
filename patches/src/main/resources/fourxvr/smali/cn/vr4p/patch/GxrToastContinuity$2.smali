.class Lcn/vr4p/patch/GxrToastContinuity$2;
.super Landroid/widget/Toast$Callback;
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
.field private acknowledged:Z

.field final synthetic this$0:Lcn/vr4p/patch/GxrToastContinuity;

.field final synthetic val$current:Landroid/widget/Toast;

.field final synthetic val$id:I


# direct methods
.method constructor <init>(Lcn/vr4p/patch/GxrToastContinuity;ILandroid/widget/Toast;)V
    .locals 0
    .annotation system Ldalvik/annotation/MethodParameters;
        accessFlags = {
            0x8010,
            0x1010,
            0x1010
        }
        names = {
            null,
            null,
            null
        }
    .end annotation

    .line 71
    iput-object p1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    iput p2, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$id:I

    iput-object p3, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$current:Landroid/widget/Toast;

    invoke-direct {p0}, Landroid/widget/Toast$Callback;-><init>()V

    return-void
.end method


# virtual methods
.method public onToastHidden()V
    .locals 3

    .line 80
    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$id:I

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$000(Lcn/vr4p/patch/GxrToastContinuity;)I

    move-result v1

    if-ne v0, v1, :cond_2

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$100(Lcn/vr4p/patch/GxrToastContinuity;)Landroid/widget/Toast;

    move-result-object v0

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$current:Landroid/widget/Toast;

    if-eq v0, v1, :cond_0

    goto :goto_0

    .line 81
    :cond_0
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$008(Lcn/vr4p/patch/GxrToastContinuity;)I

    .line 82
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$500(Lcn/vr4p/patch/GxrToastContinuity;)Ljava/lang/Runnable;

    move-result-object v0

    if-eqz v0, :cond_1

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$600(Lcn/vr4p/patch/GxrToastContinuity;)Landroid/os/Handler;

    move-result-object v0

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$500(Lcn/vr4p/patch/GxrToastContinuity;)Ljava/lang/Runnable;

    move-result-object v1

    invoke-virtual {v0, v1}, Landroid/os/Handler;->removeCallbacks(Ljava/lang/Runnable;)V

    .line 83
    :cond_1
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    const/4 v1, 0x0

    invoke-static {v0, v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$502(Lcn/vr4p/patch/GxrToastContinuity;Ljava/lang/Runnable;)Ljava/lang/Runnable;

    .line 84
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0, v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$102(Lcn/vr4p/patch/GxrToastContinuity;Landroid/widget/Toast;)Landroid/widget/Toast;

    .line 85
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    const-wide/16 v1, 0x0

    invoke-static {v0, v1, v2}, Lcn/vr4p/patch/GxrToastContinuity;->access$700(Lcn/vr4p/patch/GxrToastContinuity;J)V

    .line 86
    return-void

    .line 80
    :cond_2
    :goto_0
    return-void
.end method

.method public onToastShown()V
    .locals 2

    .line 74
    iget v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$id:I

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$000(Lcn/vr4p/patch/GxrToastContinuity;)I

    move-result v1

    if-ne v0, v1, :cond_2

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$100(Lcn/vr4p/patch/GxrToastContinuity;)Landroid/widget/Toast;

    move-result-object v0

    iget-object v1, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->val$current:Landroid/widget/Toast;

    if-ne v0, v1, :cond_2

    iget-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->acknowledged:Z

    if-eqz v0, :cond_0

    goto :goto_0

    .line 75
    :cond_0
    const/4 v0, 0x1

    iput-boolean v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->acknowledged:Z

    .line 76
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$200(Lcn/vr4p/patch/GxrToastContinuity;)Z

    move-result v0

    if-nez v0, :cond_1

    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    invoke-static {v0}, Lcn/vr4p/patch/GxrToastContinuity;->access$300(Lcn/vr4p/patch/GxrToastContinuity;)V

    return-void

    .line 77
    :cond_1
    iget-object v0, p0, Lcn/vr4p/patch/GxrToastContinuity$2;->this$0:Lcn/vr4p/patch/GxrToastContinuity;

    const/4 v1, 0x0

    invoke-static {v0, v1}, Lcn/vr4p/patch/GxrToastContinuity;->access$402(Lcn/vr4p/patch/GxrToastContinuity;I)I

    .line 78
    return-void

    .line 74
    :cond_2
    :goto_0
    return-void
.end method
