.class final Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;
.super Ljava/lang/Object;
.source "FrameDispatchCore.java"


# annotations
.annotation system Ldalvik/annotation/MemberClasses;
    value = {
        Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;,
        Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;,
        Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;,
        Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;
    }
.end annotation


# instance fields
.field private final clock:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

.field private completedAtFrame:J

.field private final errors:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

.field private generation:J

.field private loading:Z

.field private final lock:Ljava/lang/Object;

.field private pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

.field private waitingForFrame:Z

.field private windows:Ljava/util/LinkedHashMap;
    .annotation system Ldalvik/annotation/Signature;
        value = {
            "Ljava/util/LinkedHashMap<",
            "Ljava/lang/Long;",
            "Ljava/lang/Runnable;",
            ">;"
        }
    .end annotation
.end field

.field private final worker:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;


# direct methods
.method constructor <init>(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    new-instance v0, Ljava/lang/Object;

    invoke-direct {v0}, Ljava/lang/Object;-><init>()V

    iput-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    new-instance v0, Ljava/util/LinkedHashMap;

    invoke-direct {v0}, Ljava/util/LinkedHashMap;-><init>()V

    iput-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    iput-object p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->clock:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    iput-object p2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->worker:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;

    iput-object p3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->errors:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

    return-void
.end method

.method static synthetic access$000(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;
    .locals 0

    iget-object p0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->errors:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

    return-object p0
.end method

.method static synthetic access$100(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;
    .locals 0

    iget-object p0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->clock:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    return-object p0
.end method

.method static synthetic access$200(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;)Ljava/lang/Object;
    .locals 0

    iget-object p0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    return-object p0
.end method

.method static synthetic access$302(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;J)J
    .locals 0

    iput-wide p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->completedAtFrame:J

    return-wide p1
.end method

.method static synthetic access$402(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z
    .locals 0

    iput-boolean p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->waitingForFrame:Z

    return p1
.end method

.method static synthetic access$502(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Z)Z
    .locals 0

    iput-boolean p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->loading:Z

    return p1
.end method


# virtual methods
.method drain()V
    .locals 7

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    invoke-virtual {v1}, Ljava/util/LinkedHashMap;->isEmpty()Z

    move-result v1

    if-eqz v1, :cond_0

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    if-nez v1, :cond_0

    monitor-exit v0

    return-void

    :cond_0
    new-instance v1, Ljava/util/ArrayList;

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    invoke-virtual {v2}, Ljava/util/LinkedHashMap;->values()Ljava/util/Collection;

    move-result-object v2

    invoke-direct {v1, v2}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V

    iget-object v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    invoke-virtual {v2}, Ljava/util/LinkedHashMap;->clear()V

    iget-wide v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->generation:J

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_4

    invoke-virtual {v1}, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;

    move-result-object v0

    :goto_0
    invoke-interface {v0}, Ljava/util/Iterator;->hasNext()Z

    move-result v1

    if-eqz v1, :cond_2

    invoke-interface {v0}, Ljava/util/Iterator;->next()Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Ljava/lang/Runnable;

    iget-object v4, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v4

    :try_start_1
    iget-wide v5, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->generation:J

    cmp-long v5, v2, v5

    if-eqz v5, :cond_1

    monitor-exit v4

    goto :goto_2

    :cond_1
    monitor-exit v4
    :try_end_1
    .catchall {:try_start_1 .. :try_end_1} :catchall_0

    :try_start_2
    invoke-interface {v1}, Ljava/lang/Runnable;->run()V
    :try_end_2
    .catch Ljava/lang/RuntimeException; {:try_start_2 .. :try_end_2} :catch_0

    goto :goto_1

    :catch_0
    move-exception v1

    iget-object v4, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->errors:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

    invoke-interface {v4, v1}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;->report(Ljava/lang/Throwable;)V

    :goto_1
    goto :goto_0

    :catchall_0
    move-exception v0

    :try_start_3
    monitor-exit v4
    :try_end_3
    .catchall {:try_start_3 .. :try_end_3} :catchall_0

    throw v0

    :cond_2
    :goto_2
    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v1

    :try_start_4
    iget-wide v4, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->generation:J

    cmp-long v0, v2, v4

    if-nez v0, :cond_8

    iget-boolean v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->loading:Z

    if-eqz v0, :cond_3

    goto :goto_4

    :cond_3
    iget-boolean v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->waitingForFrame:Z

    const/4 v2, 0x0

    if-eqz v0, :cond_5

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->clock:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;

    invoke-interface {v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Clock;->frame()J

    move-result-wide v3

    iget-wide v5, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->completedAtFrame:J

    cmp-long v0, v3, v5

    if-nez v0, :cond_4

    monitor-exit v1

    return-void

    :cond_4
    iput-boolean v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->waitingForFrame:Z

    :cond_5
    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    const/4 v3, 0x0

    iput-object v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    monitor-exit v1
    :try_end_4
    .catchall {:try_start_4 .. :try_end_4} :catchall_3

    if-nez v0, :cond_6

    return-void

    :cond_6
    :try_start_5
    invoke-interface {v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;->apply()Z

    move-result v1

    if-eqz v1, :cond_7

    return-void

    :cond_7
    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v1
    :try_end_5
    .catch Ljava/lang/RuntimeException; {:try_start_5 .. :try_end_5} :catch_1

    const/4 v3, 0x1

    :try_start_6
    iput-boolean v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->loading:Z

    monitor-exit v1
    :try_end_6
    .catchall {:try_start_6 .. :try_end_6} :catchall_1

    :try_start_7
    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->worker:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;

    new-instance v3, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;

    invoke-direct {v3, p0, v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$1;-><init>(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;)V

    invoke-interface {v1, v3}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Worker;->start(Ljava/lang/Runnable;)V
    :try_end_7
    .catch Ljava/lang/RuntimeException; {:try_start_7 .. :try_end_7} :catch_1

    goto :goto_3

    :catchall_1
    move-exception v0

    :try_start_8
    monitor-exit v1
    :try_end_8
    .catchall {:try_start_8 .. :try_end_8} :catchall_1

    :try_start_9
    throw v0
    :try_end_9
    .catch Ljava/lang/RuntimeException; {:try_start_9 .. :try_end_9} :catch_1

    :catch_1
    move-exception v0

    iget-object v3, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v3

    :try_start_a
    iput-boolean v2, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->loading:Z

    monitor-exit v3
    :try_end_a
    .catchall {:try_start_a .. :try_end_a} :catchall_2

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->errors:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;

    invoke-interface {v1, v0}, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Errors;->report(Ljava/lang/Throwable;)V

    :goto_3
    return-void

    :catchall_2
    move-exception v0

    :try_start_b
    monitor-exit v3
    :try_end_b
    .catchall {:try_start_b .. :try_end_b} :catchall_2

    throw v0

    :cond_8
    :goto_4
    :try_start_c
    monitor-exit v1

    return-void

    :catchall_3
    move-exception v0

    monitor-exit v1
    :try_end_c
    .catchall {:try_start_c .. :try_end_c} :catchall_3

    throw v0

    :catchall_4
    move-exception v1

    :try_start_d
    monitor-exit v0
    :try_end_d
    .catchall {:try_start_d .. :try_end_d} :catchall_4

    throw v1
.end method

.method removeWindow(JLjava/lang/Runnable;)V
    .locals 2

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v0

    :try_start_0
    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    invoke-static {p1, p2}, Ljava/lang/Long;->valueOf(J)Ljava/lang/Long;

    move-result-object p1

    invoke-virtual {v1, p1, p3}, Ljava/util/LinkedHashMap;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;

    monitor-exit v0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p1
.end method

.method reset()V
    .locals 5

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v0

    :try_start_0
    iget-wide v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->generation:J

    const-wide/16 v3, 0x1

    add-long/2addr v1, v3

    iput-wide v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->generation:J

    iget-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->windows:Ljava/util/LinkedHashMap;

    invoke-virtual {v1}, Ljava/util/LinkedHashMap;->clear()V

    const/4 v1, 0x0

    iput-object v1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    monitor-exit v0

    return-void

    :catchall_0
    move-exception v1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw v1
.end method

.method scene(Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;)V
    .locals 1

    iget-object v0, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->lock:Ljava/lang/Object;

    monitor-enter v0

    :try_start_0
    iput-object p1, p0, Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore;->pendingScene:Lcn/vr4p/oculus4xvrplayerov/FrameDispatchCore$Scene;

    monitor-exit v0

    return-void

    :catchall_0
    move-exception p1

    monitor-exit v0
    :try_end_0
    .catchall {:try_start_0 .. :try_end_0} :catchall_0

    throw p1
.end method
