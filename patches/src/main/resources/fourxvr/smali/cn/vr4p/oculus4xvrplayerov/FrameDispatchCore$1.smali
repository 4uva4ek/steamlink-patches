.class Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;
.super Ljava/lang/Object;
.source "FrameDispatchCore.java"

# interfaces
.implements Ljava/lang/Runnable;


# annotations
.annotation system Ldalvik/annotation/EnclosingMethod;
    value = Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->drain()V
.end annotation

.annotation system Ldalvik/annotation/InnerClass;
    accessFlags = 0x0
    name = null
.end annotation


# instance fields
.field final synthetic this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

.field final synthetic val$scene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;


# direct methods
.method constructor <init>(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;)V
    .locals 0
    .annotation system Ldalvik/annotation/MethodParameters;
        accessFlags = {
            0x8010,
            0x1010
        }
        names = {
            null,
            null
        }
    .end annotation

    .annotation system Ldalvik/annotation/Signature;
        value = {
            "()V"
        }
    .end annotation

    iput-object p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    iput-object p2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->val$scene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method


# virtual methods
.method public run()V
    .locals 7

    const/4 v0, 0x0

    const/4 v1, 0x1

    :try_start_0
    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->val$scene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    invoke-interface {v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;->load()V
    :try_end_0
    .catch Ljava/lang/RuntimeException; {:try_start_0 .. :try_end_0} :catch_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_1

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$100(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    move-result-object v2

    invoke-interface {v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;->frame()J

    move-result-wide v2

    iget-object v4, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v4}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$200(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Ljava/lang/Object;

    move-result-object v4

    monitor-enter v4

    :try_start_1
    iget-object v5, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v5, v2, v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$302(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;J)J

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v2, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$402(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v1, v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$502(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    monitor-exit v4

    :goto_0
    goto :goto_1

    :catchall_0
    move-exception v0

    monitor-exit v4
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    throw v0

    :catchall_1
    move-exception v2

    goto :goto_2

    :catch_0
    move-exception v2

    :try_start_2
    iget-object v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$000(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

    move-result-object v3

    invoke-interface {v3, v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;->report(Ljava/lang/Throwable;)V
    :try_end_2
    .catchall {:try_start_2 .. :try_end_2} :catchall_1

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$100(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    move-result-object v2

    invoke-interface {v2}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;->frame()J

    move-result-wide v2

    iget-object v4, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v4}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$200(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Ljava/lang/Object;

    move-result-object v4

    monitor-enter v4

    :try_start_3
    iget-object v5, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v5, v2, v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$302(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;J)J

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v2, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$402(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v1, v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$502(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    monitor-exit v4

    goto :goto_0

    :goto_1
    return-void

    :catchall_2
    move-exception v0

    monitor-exit v4
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_2

    throw v0

    :goto_2
    iget-object v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$100(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    move-result-object v3

    invoke-interface {v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;->frame()J

    move-result-wide v3

    iget-object v5, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v5}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$200(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Ljava/lang/Object;

    move-result-object v5

    monitor-enter v5

    :try_start_4
    iget-object v6, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v6, v3, v4}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$302(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;J)J

    iget-object v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v3, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$402(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;->this$0:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;

    invoke-static {v1, v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->access$502(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z

    monitor-exit v5
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_3

    throw v2

    :catchall_3
    move-exception v0

    :try_start_5
    monitor-exit v5
    :try_end_5
    .catchall {:try_start_5 .. :try_end_5} :catchall_3

    throw v0
.end method
